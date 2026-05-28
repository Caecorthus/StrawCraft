package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawKillEvents;

public final class ProfessorIronManProtection {
    public static final String PROTECTION_FLAG = "professor_iron_man_protected";
    public static final String GRANTED_AT_TIMESTAMP = "professor_iron_man_granted_at";

    private ProfessorIronManProtection() {
    }

    public static void grant(NoellesRoleState state, long currentGameTime) {
        if (state.getTimestamp(GRANTED_AT_TIMESTAMP).isPresent()) {
            return;
        }
        // Grant once per round; NoellesRoleState reset clears this timestamp for the next game.
        // 每局只授予一次；NoellesRoleState reset 会清掉时间戳，让下一局重新授予。
        state.setFlag(PROTECTION_FLAG, true);
        state.setTimestamp(GRANTED_AT_TIMESTAMP, currentGameTime);
    }

    public static void grant(NoellesRoleStateComponent state, long currentGameTime) {
        if (state.getTimestamp(GRANTED_AT_TIMESTAMP).isPresent()) {
            return;
        }
        // Grant once per round; NoellesRoleState reset clears this timestamp for the next game.
        // 每局只授予一次；NoellesRoleState reset 会清掉时间戳，让下一局重新授予。
        state.setFlag(PROTECTION_FLAG, true);
        state.setTimestamp(GRANTED_AT_TIMESTAMP, currentGameTime);
    }

    public static StrawKillEvents.KillDecision beforeKill(NoellesRoleState state, Identifier deathReason) {
        if (!state.hasFlag(PROTECTION_FLAG) || isExcludedDeathReason(deathReason)) {
            return StrawKillEvents.KillDecision.pass();
        }
        consumeProtection(state);
        return StrawKillEvents.KillDecision.cancel();
    }

    public static StrawKillEvents.KillDecision beforeKill(Role victimRole, NoellesRoleState state, Identifier deathReason) {
        if (!StrawRoleMeaning.receivesProfessorIronManProtection(victimRole)) {
            return StrawKillEvents.KillDecision.pass();
        }
        return beforeKill(state, deathReason);
    }

    public static StrawKillEvents.KillDecision beforeKill(NoellesRoleStateComponent state, Identifier deathReason) {
        if (!state.hasFlag(PROTECTION_FLAG) || isExcludedDeathReason(deathReason)) {
            return StrawKillEvents.KillDecision.pass();
        }
        consumeProtection(state);
        return StrawKillEvents.KillDecision.cancel();
    }

    public static StrawKillEvents.KillDecision beforeKill(
            Role victimRole,
            NoellesRoleStateComponent state,
            Identifier deathReason
    ) {
        if (!StrawRoleMeaning.receivesProfessorIronManProtection(victimRole)) {
            return StrawKillEvents.KillDecision.pass();
        }
        return beforeKill(state, deathReason);
    }

    private static void consumeProtection(NoellesRoleState state) {
        // This consumes the migrated Iron Man buff before Wathe creates a body/spectator death.
        // 这里在 Wathe 创建尸体/旁观者死亡记录前消耗迁移来的 Iron Man 保护。
        state.setFlag(PROTECTION_FLAG, false);
    }

    private static void consumeProtection(NoellesRoleStateComponent state) {
        // This consumes the migrated Iron Man buff before Wathe creates a body/spectator death.
        // 这里在 Wathe 创建尸体/旁观者死亡记录前消耗迁移来的 Iron Man 保护。
        state.setFlag(PROTECTION_FLAG, false);
    }

    private static boolean isExcludedDeathReason(Identifier deathReason) {
        // Excluded deaths keep the one grant for the next ordinary Wathe kill.
        // 排除死因不消耗这次保护，留给下一次普通 Wathe 击杀。
        return StrawDeathReasons.SHOT_INNOCENT.equals(deathReason)
                || StrawDeathReasons.ASSASSINATED.equals(deathReason);
    }
}
