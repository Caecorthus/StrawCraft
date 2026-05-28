package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;

import java.util.Optional;
import java.util.UUID;

public final class NoellesNeutralWinPolicy {
    public static final Identifier JESTER_KILLED_TRIGGER = StrawCraft.id("jester_killed");

    private static final Identifier JESTER_ROLE = StrawCraft.id("jester");

    private NoellesNeutralWinPolicy() {
    }

    public static boolean recordOfficialDeathSideEffects(
            NoellesRoleState state,
            Optional<Identifier> victimRoleId,
            StrawDeathEvents.OfficialDeathContext context
    ) {
        Optional<NoellesRoleState.NeutralWinClaim> claim = neutralWinClaimForOfficialDeath(victimRoleId, context);
        claim.ifPresent(state::recordNeutralWinClaim);
        return claim.isPresent();
    }

    public static Optional<NoellesRoleState.NeutralWinClaim> neutralWinClaimForOfficialDeath(
            Optional<Identifier> victimRoleId,
            StrawDeathEvents.OfficialDeathContext context
    ) {
        if (victimRoleId.filter(JESTER_ROLE::equals).isEmpty() || context.killerUuid().isEmpty()) {
            return Optional.empty();
        }

        // XruiDD Jester depends on Spark win hooks; StrawCraft records the death-triggered claim only.
        // XruiDD 小丑依赖 Spark 胜利 hook；StrawCraft 这里只记录由死亡触发的胜利主张。
        return Optional.of(new NoellesRoleState.NeutralWinClaim(
                JESTER_ROLE,
                JESTER_KILLED_TRIGGER,
                context.killerUuid(),
                context.gameTime()
        ));
    }

    public static StrawWinEvents.WinContribution contributeRecordedNeutralWins(UUID playerUuid, NoellesRoleState state) {
        // Neutral claims are post-game notices only; Wathe still owns official winner-screen semantics.
        // 中立胜利声明只用于赛后通知；官方胜利界面语义仍由 Wathe 自己决定。
        return StrawWinEvents.WinContribution.none();
    }
}
