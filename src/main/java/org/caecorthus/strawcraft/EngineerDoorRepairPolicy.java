package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

public final class EngineerDoorRepairPolicy {
    public static final Identifier ENGINEER_ROLE = StrawCraft.id("engineer");
    public static final int COOLDOWN_TICKS = 20 * 5;

    private EngineerDoorRepairPolicy() {
    }

    public static boolean shouldHandle(InteractionInput input) {
        return input != null
                && !input.clientWorld()
                && input.roundRunning()
                && StrawRoleMeaning.matchesRoleId(input.role(), ENGINEER_ROLE)
                && input.engineerAlive()
                && input.repairToolInHand()
                && input.cooldownReady()
                && input.officialWatheSmallDoorTarget();
    }

    public static DoorAction actionFor(DoorState state) {
        if (state == null || !state.officialWatheSmallDoorTarget()) {
            return DoorAction.PASS;
        }
        if (state.blasted()) {
            return DoorAction.REPAIR_BLASTED;
        }
        if (state.jammed()) {
            return DoorAction.CLEAR_JAMMED;
        }
        if (state.keyName() != null && !state.keyName().isBlank()) {
            return DoorAction.UNLOCK;
        }
        return DoorAction.LOCK;
    }

    public enum DoorAction {
        REPAIR_BLASTED,
        CLEAR_JAMMED,
        UNLOCK,
        LOCK,
        PASS
    }

    public record InteractionInput(
            boolean clientWorld,
            boolean roundRunning,
            Role role,
            boolean engineerAlive,
            boolean repairToolInHand,
            boolean cooldownReady,
            boolean officialWatheSmallDoorTarget
    ) {
        public InteractionInput withClientWorld(boolean value) {
            return new InteractionInput(value, roundRunning, role, engineerAlive, repairToolInHand, cooldownReady,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withRoundRunning(boolean value) {
            return new InteractionInput(clientWorld, value, role, engineerAlive, repairToolInHand, cooldownReady,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withRole(Role value) {
            return new InteractionInput(clientWorld, roundRunning, value, engineerAlive, repairToolInHand, cooldownReady,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withEngineerAlive(boolean value) {
            return new InteractionInput(clientWorld, roundRunning, role, value, repairToolInHand, cooldownReady,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withRepairToolInHand(boolean value) {
            return new InteractionInput(clientWorld, roundRunning, role, engineerAlive, value, cooldownReady,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withCooldownReady(boolean value) {
            return new InteractionInput(clientWorld, roundRunning, role, engineerAlive, repairToolInHand, value,
                    officialWatheSmallDoorTarget);
        }

        public InteractionInput withOfficialWatheSmallDoorTarget(boolean value) {
            return new InteractionInput(clientWorld, roundRunning, role, engineerAlive, repairToolInHand, cooldownReady,
                    value);
        }
    }

    public record DoorState(
            boolean officialWatheSmallDoorTarget,
            boolean blasted,
            boolean jammed,
            String keyName
    ) {
    }
}
