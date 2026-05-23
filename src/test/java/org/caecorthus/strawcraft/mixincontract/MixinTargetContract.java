package org.caecorthus.strawcraft.mixincontract;

record MixinTargetContract(
        String mixinClass,
        String targetOwnerClass,
        String dependencyOwnerClass,
        String methodName,
        String descriptor,
        Requirement requirement,
        String reason
) {
    enum Requirement {
        REQUIRED,
        OPTIONAL
    }

    String displayName() {
        return mixinClass + " -> " + targetOwnerClass + "#" + methodName + descriptor;
    }
}
