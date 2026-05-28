package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemonHunterPsychoPolicyTest {
    @Test
    void loudPsychoTracksAndArmsAliveDemonHuntersWithoutGuns() {
        DemonHunterPsychoPolicy.PsychoStartResult result = DemonHunterPsychoPolicy.onPsychoStarted(
                new DemonHunterPsychoPolicy.PsychoStartInput(
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        false
                )
        );

        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.GIVE_PISTOL, result);
        assertTrue(result.tracksFrenziedPlayer());
        assertEquals(2, result.bulletsToAdd());
    }

    @Test
    void loudPsychoAddsBulletsToExistingDemonHunterPistol() {
        DemonHunterPsychoPolicy.PsychoStartResult result = DemonHunterPsychoPolicy.onPsychoStarted(
                new DemonHunterPsychoPolicy.PsychoStartInput(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true
                )
        );

        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.ADD_BULLETS, result);
        assertTrue(result.tracksFrenziedPlayer());
        assertEquals(2, result.bulletsToAdd());
    }

    @Test
    void loudPsychoTracksButDoesNotGrantPistolWhenAnotherGunIsOwned() {
        DemonHunterPsychoPolicy.PsychoStartResult result = DemonHunterPsychoPolicy.onPsychoStarted(
                new DemonHunterPsychoPolicy.PsychoStartInput(
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true
                )
        );

        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.TRACK_ONLY, result);
        assertTrue(result.tracksFrenziedPlayer());
        assertEquals(0, result.bulletsToAdd());
    }

    @Test
    void silentPsychoAndInactiveDemonHuntersAreIgnored() {
        DemonHunterPsychoPolicy.PsychoStartInput allowed = new DemonHunterPsychoPolicy.PsychoStartInput(
                true,
                true,
                true,
                true,
                true,
                false,
                false
        );

        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.IGNORE,
                DemonHunterPsychoPolicy.onPsychoStarted(allowed.withLoudPsycho(false)));
        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.IGNORE,
                DemonHunterPsychoPolicy.onPsychoStarted(allowed.withDemonHunterRole(false)));
        assertEquals(DemonHunterPsychoPolicy.PsychoStartResult.IGNORE,
                DemonHunterPsychoPolicy.onPsychoStarted(allowed.withDemonHunterAlive(false)));
    }

    @Test
    void validShotAttemptConsumesBulletAndOnlyTrackedActiveTargetsAreKilled() {
        DemonHunterPsychoPolicy.ShotInput shot = new DemonHunterPsychoPolicy.ShotInput(
                true,
                2,
                true,
                true,
                true,
                true,
                true,
                16.0D,
                true
        );

        DemonHunterPsychoPolicy.ShotResult killed = DemonHunterPsychoPolicy.evaluateShot(shot);
        assertEquals(DemonHunterPsychoPolicy.ShotResult.FIRED_KILLED, killed);
        assertTrue(killed.consumesBullet());
        assertTrue(killed.killsTarget());
        assertEquals(1, DemonHunterPsychoPolicy.bulletsAfterShot(2, killed));

        DemonHunterPsychoPolicy.ShotResult missed = DemonHunterPsychoPolicy.evaluateShot(shot.withTargetTracked(false));
        assertEquals(DemonHunterPsychoPolicy.ShotResult.FIRED_MISSED, missed);
        assertTrue(missed.consumesBullet());
        assertFalse(missed.killsTarget());
    }

    @Test
    void pistolCooldownMissingAmmoAndWrongHandBlockShotWithoutConsumingBullets() {
        DemonHunterPsychoPolicy.ShotInput shot = new DemonHunterPsychoPolicy.ShotInput(
                true,
                1,
                true,
                true,
                true,
                true,
                true,
                16.0D,
                true
        );

        assertFalse(DemonHunterPsychoPolicy.evaluateShot(shot.withMainHandPistol(false)).consumesBullet());
        assertFalse(DemonHunterPsychoPolicy.evaluateShot(shot.withBullets(0)).consumesBullet());
        assertFalse(DemonHunterPsychoPolicy.evaluateShot(shot.withCooldownReady(false)).consumesBullet());
    }
}
