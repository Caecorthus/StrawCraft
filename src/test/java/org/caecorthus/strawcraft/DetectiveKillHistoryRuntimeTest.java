package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectiveKillHistoryRuntimeTest {
    private static final Identifier OVERWORLD = Identifier.of("minecraft", "overworld");
    private static final Identifier NETHER = Identifier.of("minecraft", "the_nether");

    @BeforeEach
    void resetHistory() {
        DetectiveKillHistoryRuntime.resetAll();
        DetectiveKillHistoryRuntime.registerEvents();
    }

    @Test
    void officialAttributedDeathRecordsSuspiciousRuntimeHistory() {
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(new StrawDeathEvents.OfficialDeathContext(
                victim,
                Optional.of(killer),
                false,
                GameConstants.DeathReasons.GUN,
                100L,
                true,
                true
        ));

        assertEquals(DetectiveInvestigationPolicy.Result.SUSPICIOUS,
                DetectiveKillHistoryRuntime.investigate(killer, 101L));
    }

    @Test
    void officialAttributedDeathRecordsOnlyInPublishedWorldHistory() {
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(new StrawDeathEvents.OfficialDeathContext(
                OVERWORLD,
                victim,
                Optional.of(killer),
                false,
                GameConstants.DeathReasons.GUN,
                100L,
                true,
                true
        ));

        assertEquals(DetectiveInvestigationPolicy.Result.SUSPICIOUS,
                DetectiveKillHistoryRuntime.investigate(OVERWORLD, killer, 101L));
        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveKillHistoryRuntime.investigate(NETHER, killer, 101L));
        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveKillHistoryRuntime.investigate(killer, 101L));
    }

    @Test
    void runtimeHistoryIsScopedByWorld() {
        UUID killer = UUID.randomUUID();

        DetectiveKillHistoryRuntime.recordKill(OVERWORLD, killer, UUID.randomUUID(), GameConstants.DeathReasons.BAT, 200L);

        assertEquals(DetectiveInvestigationPolicy.Result.SUSPICIOUS,
                DetectiveKillHistoryRuntime.investigate(OVERWORLD, killer, 201L));
        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveKillHistoryRuntime.investigate(NETHER, killer, 201L));
    }

    @Test
    void investigationCooldownUsesSharedNoellesRoleState() {
        NoellesRoleState state = new NoellesRoleState();
        NoellesRoleState otherState = new NoellesRoleState();

        assertEquals(DetectiveKillHistoryRuntime.CooldownAttempt.ALLOWED,
                DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(state, 100L));
        assertEquals(DetectiveKillHistoryRuntime.INVESTIGATION_COOLDOWN_TICKS,
                state.getRemainingAbilityCooldown(DetectiveKillHistoryRuntime.DETECTIVE_INVESTIGATE_COOLDOWN, 100L));

        assertEquals(DetectiveKillHistoryRuntime.CooldownAttempt.COOLDOWN,
                DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(state, 101L));
        assertEquals(DetectiveKillHistoryRuntime.INVESTIGATION_COOLDOWN_TICKS - 1,
                state.getRemainingAbilityCooldown(DetectiveKillHistoryRuntime.DETECTIVE_INVESTIGATE_COOLDOWN, 101L));
        assertEquals(DetectiveKillHistoryRuntime.CooldownAttempt.ALLOWED,
                DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(otherState, 101L));
        assertEquals(DetectiveKillHistoryRuntime.CooldownAttempt.ALLOWED,
                DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(state,
                        100L + DetectiveKillHistoryRuntime.INVESTIGATION_COOLDOWN_TICKS));
    }

    @Test
    void resetClearsDetectiveInvestigationCooldown() {
        NoellesRoleState state = new NoellesRoleState();

        DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(state, 100L);
        state.reset();

        assertEquals(DetectiveKillHistoryRuntime.CooldownAttempt.ALLOWED,
                DetectiveKillHistoryRuntime.tryBeginInvestigationCooldown(state, 101L));
    }

    @Test
    void adapterCanRecordFutureDirectKillSeamsWithoutSparkEvents() {
        UUID killer = UUID.randomUUID();

        DetectiveKillHistoryRuntime.recordKill(killer, UUID.randomUUID(), GameConstants.DeathReasons.BAT, 200L);

        assertEquals(DetectiveInvestigationPolicy.Result.SUSPICIOUS,
                DetectiveKillHistoryRuntime.investigate(killer, 201L));
    }

    @Test
    void unattributedAndResetRuntimeHistoryAreClear() {
        UUID victim = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(new StrawDeathEvents.OfficialDeathContext(
                victim,
                Optional.empty(),
                false,
                GameConstants.DeathReasons.KNIFE,
                100L,
                true,
                true
        ));

        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveKillHistoryRuntime.investigate(target, 101L));

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(new StrawDeathEvents.OfficialDeathContext(
                victim,
                Optional.of(target),
                false,
                GameConstants.DeathReasons.KNIFE,
                102L,
                true,
                true
        ));
        DetectiveKillHistoryRuntime.resetAll();

        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveKillHistoryRuntime.investigate(target, 103L));
    }
}
