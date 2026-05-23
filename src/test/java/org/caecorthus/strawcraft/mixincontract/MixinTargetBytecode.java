package org.caecorthus.strawcraft.mixincontract;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM9;

final class MixinTargetBytecode {
    private MixinTargetBytecode() {
    }

    static boolean hasMethod(String ownerClass, String methodName, String descriptor) {
        MethodSearchVisitor visitor = new MethodSearchVisitor(methodName, descriptor);
        read(ownerClass, visitor);
        return visitor.found();
    }

    static boolean hasMixinTarget(String ownerClass, String targetOwnerClass) {
        MixinTargetSearchVisitor visitor = new MixinTargetSearchVisitor(targetOwnerClass.replace('.', '/'));
        read(ownerClass, visitor);
        return visitor.found();
    }

    static MixinClassInspection inspectMixinClass(String ownerClass, String targetOwnerClass) {
        MixinClassVisitor visitor = new MixinClassVisitor(targetOwnerClass.replace('.', '/'));
        boolean classPresent = read(ownerClass, visitor);
        return new MixinClassInspection(classPresent, visitor.targetsExpectedOwner(), visitor.injections());
    }

    private static boolean read(String ownerClass, ClassVisitor visitor) {
        String resourceName = ownerClass.replace('.', '/') + ".class";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (var input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                return false;
            }

            new ClassReader(input).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return true;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + resourceName, exception);
        }
    }

    record MixinClassInspection(
            boolean classPresent,
            boolean targetsExpectedOwner,
            List<InjectionAnnotation> injections
    ) {
        boolean hasSoftInjectionFor(String methodName) {
            return injections.stream()
                    .filter(injection -> injection.targetsMethod(methodName))
                    .anyMatch(InjectionAnnotation::usesSoftSemantics);
        }

        String describeInjections() {
            if (injections.isEmpty()) {
                return "No mixin injection annotations were found.";
            }

            List<String> descriptions = new ArrayList<>();
            for (InjectionAnnotation injection : injections) {
                descriptions.add(injection.describe());
            }
            return String.join("\n", descriptions);
        }
    }

    record InjectionAnnotation(
            String annotationName,
            String handlerMethod,
            List<String> methodTargets,
            Boolean remap,
            Integer require
    ) {
        boolean targetsMethod(String methodName) {
            return methodTargets.stream().anyMatch(target -> matchesMethodName(target, methodName));
        }

        private static boolean matchesMethodName(String target, String methodName) {
            int descriptorStart = target.indexOf('(');
            String targetMethodName = descriptorStart == -1 ? target : target.substring(0, descriptorStart);
            return targetMethodName.equals(methodName);
        }

        private boolean usesSoftSemantics() {
            return Boolean.FALSE.equals(remap) && Integer.valueOf(0).equals(require);
        }

        private String describe() {
            return annotationName
                    + " on " + handlerMethod
                    + " method=" + methodTargets
                    + " remap=" + remap
                    + " require=" + require;
        }
    }

    private static final class MethodSearchVisitor extends ClassVisitor {
        private final String methodName;
        private final String descriptor;
        private boolean found;

        private MethodSearchVisitor(String methodName, String descriptor) {
            super(ASM9);
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (this.methodName.equals(name) && this.descriptor.equals(descriptor)) {
                found = true;
            }
            return null;
        }

        private boolean found() {
            return found;
        }
    }

    private static final class MixinTargetSearchVisitor extends ClassVisitor {
        private final String targetOwnerInternalName;
        private boolean found;

        private MixinTargetSearchVisitor(String targetOwnerInternalName) {
            super(ASM9);
            this.targetOwnerInternalName = targetOwnerInternalName;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                return null;
            }

            return new MixinAnnotationVisitor(targetOwnerInternalName) {
                @Override
                void markFound() {
                    found = true;
                }
            };
        }

        private boolean found() {
            return found;
        }
    }

    private abstract static class MixinAnnotationVisitor extends AnnotationVisitor {
        private final String targetOwnerInternalName;

        private MixinAnnotationVisitor(String targetOwnerInternalName) {
            super(ASM9);
            this.targetOwnerInternalName = targetOwnerInternalName;
        }

        @Override
        public void visit(String name, Object value) {
            visitTargetValue(name, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (!"value".equals(name) && !"targets".equals(name)) {
                return null;
            }

            return new AnnotationVisitor(ASM9) {
                @Override
                public void visit(String name, Object value) {
                    visitTargetValue(name, value);
                }
            };
        }

        private void visitTargetValue(String name, Object value) {
            if (!"value".equals(name) && !"targets".equals(name) && name != null) {
                return;
            }

            if (value instanceof Type type && targetOwnerInternalName.equals(type.getInternalName())) {
                markFound();
            }
            if (value instanceof String target && matchesStringTarget(target)) {
                markFound();
            }
        }

        private boolean matchesStringTarget(String target) {
            return targetOwnerInternalName.equals(target)
                    || targetOwnerInternalName.equals(target.replace('.', '/'));
        }

        abstract void markFound();
    }

    private static final class MixinClassVisitor extends ClassVisitor {
        private final String targetOwnerInternalName;
        private final List<InjectionAnnotation> injections = new ArrayList<>();
        private boolean targetsExpectedOwner;

        private MixinClassVisitor(String targetOwnerInternalName) {
            super(ASM9);
            this.targetOwnerInternalName = targetOwnerInternalName;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                return null;
            }

            return new MixinAnnotationVisitor(targetOwnerInternalName) {
                @Override
                void markFound() {
                    targetsExpectedOwner = true;
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (!isMixinInjectionAnnotation(descriptor)) {
                        return null;
                    }

                    return new InjectionAnnotationVisitor(simpleAnnotationName(descriptor), name, injections);
                }
            };
        }

        private boolean targetsExpectedOwner() {
            return targetsExpectedOwner;
        }

        private List<InjectionAnnotation> injections() {
            return List.copyOf(injections);
        }
    }

    private static boolean isMixinInjectionAnnotation(String descriptor) {
        return descriptor.startsWith("Lorg/spongepowered/asm/mixin/injection/")
                || descriptor.startsWith("Lcom/llamalad7/mixinextras/injector/");
    }

    private static String simpleAnnotationName(String descriptor) {
        int slash = descriptor.lastIndexOf('/');
        return "@" + descriptor.substring(slash + 1, descriptor.length() - 1);
    }

    private static final class InjectionAnnotationVisitor extends AnnotationVisitor {
        private final String annotationName;
        private final String handlerMethod;
        private final List<InjectionAnnotation> injections;
        private final List<String> methodTargets = new ArrayList<>();
        private Boolean remap;
        private Integer require;

        private InjectionAnnotationVisitor(String annotationName, String handlerMethod, List<InjectionAnnotation> injections) {
            super(ASM9);
            this.annotationName = annotationName;
            this.handlerMethod = handlerMethod;
            this.injections = injections;
        }

        @Override
        public void visit(String name, Object value) {
            if ("method".equals(name) && value instanceof String methodTarget) {
                methodTargets.add(methodTarget);
            }
            if ("remap".equals(name) && value instanceof Boolean remapValue) {
                remap = remapValue;
            }
            if ("require".equals(name) && value instanceof Integer requireValue) {
                require = requireValue;
            }
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (!"method".equals(name)) {
                return null;
            }

            return new AnnotationVisitor(ASM9) {
                @Override
                public void visit(String name, Object value) {
                    if (value instanceof String methodTarget) {
                        methodTargets.add(methodTarget);
                    }
                }
            };
        }

        @Override
        public void visitEnd() {
            injections.add(new InjectionAnnotation(
                    annotationName,
                    handlerMethod,
                    List.copyOf(methodTargets),
                    remap,
                    require
            ));
        }
    }
}
