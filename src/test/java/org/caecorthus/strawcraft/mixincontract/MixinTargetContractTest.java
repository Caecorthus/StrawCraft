package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.caecorthus.strawcraft.mixincontract.MixinTargetContract.Requirement.REQUIRED;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinTargetContractTest {
    @TestFactory
    Stream<DynamicTest> softMixinTargetsMatchCurrentWatheDependencyBytecode() {
        return MixinTargetContracts.softTargets().stream()
                .map(contract -> DynamicTest.dynamicTest(contract.displayName(), () -> assertContract(contract)));
    }

    private static void assertContract(MixinTargetContract contract) {
        boolean targetsOwner = MixinTargetBytecode.hasMixinTarget(
                contract.dependencyOwnerClass(),
                contract.targetOwnerClass()
        );
        boolean exists = MixinTargetBytecode.hasMethod(
                contract.dependencyOwnerClass(),
                contract.methodName(),
                contract.descriptor()
        );

        if (contract.requirement() == REQUIRED) {
            assertTrue(targetsOwner, () -> "Required Mixin target owner drifted: " + contract.displayName()
                    + "\nDependency bytecode owner: " + contract.dependencyOwnerClass()
                    + "\nReason: " + contract.reason()
                    + "\nThe dependency owner no longer declares @Mixin(" + contract.targetOwnerClass() + ").");
            assertTrue(exists, () -> "Required Mixin target drifted: " + contract.displayName()
                    + "\nDependency bytecode owner: " + contract.dependencyOwnerClass()
                    + "\nReason: " + contract.reason()
                    + "\nCheck the official Wathe dependency before relying on soft require = 0.");
        }

        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                contract.mixinClass(),
                contract.targetOwnerClass()
        );
        assertTrue(mixin.classPresent(), () -> "StrawCraft Mixin class is missing from the test classpath: "
                + contract.mixinClass()
                + "\nContract: " + contract.displayName()
                + "\nReason: " + contract.reason());
        assertTrue(mixin.targetsExpectedOwner(), () -> "StrawCraft Mixin owner drifted: " + contract.displayName()
                + "\nMixin bytecode owner: " + contract.mixinClass()
                + "\nExpected @Mixin target: " + contract.targetOwnerClass()
                + "\nReason: " + contract.reason());
        assertTrue(mixin.hasSoftInjectionFor(contract.methodName()), () -> "StrawCraft soft injection drifted: "
                + contract.displayName()
                + "\nMixin bytecode owner: " + contract.mixinClass()
                + "\nExpected an injection/redirect/wrap annotation with method containing \""
                + contract.methodName() + "\", remap = false, and require = 0."
                + "\nFound annotations:\n" + mixin.describeInjections()
                + "\nReason: " + contract.reason());
    }
}
