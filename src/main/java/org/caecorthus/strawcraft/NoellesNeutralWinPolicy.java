package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NoellesNeutralWinPolicy {
    public static final Identifier JESTER_KILLED_TRIGGER = StrawCraft.id("jester_killed");

    private static final Identifier JESTER_ROLE = StrawCraft.id("jester");
    private static final Identifier CLEAR_WINNER_SCREEN_ROLE = StrawCraft.id("loose_end");

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
        if (victimRoleId.filter(JESTER_ROLE::equals).isPresent()) {
            // EN: Spark Jester cancels the first innocent shot into stasis and wins only if that target later dies.
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static boolean canOverrideLooseEndWinner(Identifier roleId) {
        return JesterWinPolicy.JESTER_ROLE.equals(roleId)
                || VultureBodyFeastPolicy.VULTURE_ROLE.equals(roleId)
                || CorruptCopMomentPolicy.CORRUPT_COP_ROLE.equals(roleId)
                || PathogenWinPolicy.PATHOGEN_ROLE.equals(roleId)
                || TaotieSwallowPolicy.TAOTIE_ROLE.equals(roleId);
    }

    public static Identifier clearWinnerScreenRole() {
        return CLEAR_WINNER_SCREEN_ROLE;
    }

    public static int winnerScreenPriority(Identifier roleId) {
        if (JesterWinPolicy.JESTER_ROLE.equals(roleId)) {
            return 0;
        }
        if (VultureBodyFeastPolicy.VULTURE_ROLE.equals(roleId)) {
            return 1;
        }
        if (CorruptCopMomentPolicy.CORRUPT_COP_ROLE.equals(roleId)) {
            return 2;
        }
        if (PathogenWinPolicy.PATHOGEN_ROLE.equals(roleId)) {
            return 3;
        }
        if (TaotieSwallowPolicy.TAOTIE_ROLE.equals(roleId)) {
            return 4;
        }
        return Integer.MAX_VALUE;
    }

    public static StrawWinEvents.WinContribution contributeRecordedNeutralWins(UUID playerUuid, NoellesRoleState state) {
        StrawWinEvents.WinContribution.Builder contribution = StrawWinEvents.WinContribution.builder();
        contributeRecordedNeutralWins(playerUuid, state, contribution);
        return contribution.build();
    }

    public static void contributeRecordedNeutralWins(
            UUID playerUuid,
            NoellesRoleState state,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        contributeRecordedNeutralWins(playerUuid, state.neutralWinClaims(), contribution);
    }

    public static void contributeRecordedNeutralWins(
            UUID playerUuid,
            Set<NoellesRoleState.NeutralWinClaim> claims,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        claims.stream()
                .filter(claim -> canOverrideLooseEndWinner(claim.roleId()))
                .sorted(java.util.Comparator
                        .comparingLong(NoellesRoleState.NeutralWinClaim::gameTime)
                        .thenComparingInt(claim -> winnerScreenPriority(claim.roleId()))
                        .thenComparing(claim -> claim.roleId().toString()))
                .forEach(claim -> contribution
                        .replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)
                        .addExtraWinner(playerUuid, claim.roleId(), claim.trigger()));
    }
}
