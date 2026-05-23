package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinTargetBytecodeTest {
    @Test
    void injectionMethodTargetsUseExactMethodNamesWithOptionalDescriptors() {
        assertAll(
                () -> assertTrue(injectionTargeting("wathe$limitSprint").targetsMethod("wathe$limitSprint")),
                () -> assertTrue(injectionTargeting("wathe$limitSprint(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")
                        .targetsMethod("wathe$limitSprint")),
                () -> assertFalse(injectionTargeting("wathe$limitSprintExtra").targetsMethod("wathe$limitSprint")),
                () -> assertFalse(injectionTargeting("wathe$limitSprintExtra()V").targetsMethod("wathe$limitSprint"))
        );
    }

    private static MixinTargetBytecode.InjectionAnnotation injectionTargeting(String methodTarget) {
        return new MixinTargetBytecode.InjectionAnnotation(
                "@Inject",
                "handler",
                List.of(methodTarget),
                false,
                0
        );
    }
}
