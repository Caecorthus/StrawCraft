package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsaneParanoidKillerPolicyTest {
    private static final InsaneParanoidKillerPolicy.VoiceSender DEAD_SENDER =
            new InsaneParanoidKillerPolicy.VoiceSender(true, false);
    private static final InsaneParanoidKillerPolicy.VoiceTarget INSANE_KILLER =
            new InsaneParanoidKillerPolicy.VoiceTarget(InsaneParanoidKillerPolicy.ROLE_ID, true, 24.0D);

    @Test
    void insaneParanoidKillerRoleIdUsesTheLegacySparkPathInStrawCraftNamespace() {
        assertEquals(StrawCraft.id("the_insane_damned_paranoid_killer"), InsaneParanoidKillerPolicy.ROLE_ID);
    }

    @Test
    void deadVoiceForwardingRequiresDeadUnswallowedSenderAliveInsaneTargetAndVoiceRange() {
        assertTrue(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                DEAD_SENDER,
                INSANE_KILLER,
                24.0D
        ));

        assertFalse(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                new InsaneParanoidKillerPolicy.VoiceSender(false, false),
                INSANE_KILLER,
                24.0D
        ));
        assertFalse(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                new InsaneParanoidKillerPolicy.VoiceSender(true, true),
                INSANE_KILLER,
                24.0D
        ));
        assertFalse(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                DEAD_SENDER,
                new InsaneParanoidKillerPolicy.VoiceTarget(StrawCraft.id("swapper"), true, 24.0D),
                24.0D
        ));
        assertFalse(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                DEAD_SENDER,
                new InsaneParanoidKillerPolicy.VoiceTarget(InsaneParanoidKillerPolicy.ROLE_ID, false, 24.0D),
                24.0D
        ));
        assertFalse(InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                DEAD_SENDER,
                new InsaneParanoidKillerPolicy.VoiceTarget(InsaneParanoidKillerPolicy.ROLE_ID, true, 24.1D),
                24.0D
        ));
    }

    @Test
    void hallucinationVisualRequiresConfigMoodBelowDepressedAndShuffledMapping() {
        assertTrue(InsaneParanoidKillerPolicy.shouldRenderHallucinatedPlayer(true, 1, 2, true));

        assertFalse(InsaneParanoidKillerPolicy.shouldRenderHallucinatedPlayer(false, 1, 2, true));
        assertFalse(InsaneParanoidKillerPolicy.shouldRenderHallucinatedPlayer(true, 2, 2, true));
        assertFalse(InsaneParanoidKillerPolicy.shouldRenderHallucinatedPlayer(true, 1, 2, false));
    }
}
