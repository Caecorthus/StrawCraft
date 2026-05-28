package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;

public final class DemonHunterPsychoPolicy {
    public static final Identifier DEMON_HUNTER_ROLE = StrawCraft.id("demon_hunter");
    public static final String ABILITY_ID = "demon_hunter_pistol";
    public static final int PSYCHO_RESPONSE_BULLETS = 2;
    public static final int SHOT_COOLDOWN_TICKS = 10;
    public static final double SHOT_RANGE = 32.0D;
    public static final double SHOT_RANGE_SQUARED = SHOT_RANGE * SHOT_RANGE;

    private DemonHunterPsychoPolicy() {
    }

    public static PsychoStartResult onPsychoStarted(PsychoStartInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()
                || !input.loudPsycho()
                || !input.demonHunterRole()
                || !input.demonHunterAlive()
                || !input.frenziedPlayerPresent()) {
            return PsychoStartResult.IGNORE;
        }
        if (input.hasDemonHunterPistol()) {
            return PsychoStartResult.ADD_BULLETS;
        }
        if (input.ownsOtherGun()) {
            return PsychoStartResult.TRACK_ONLY;
        }
        return PsychoStartResult.GIVE_PISTOL;
    }

    public static ShotResult evaluateShot(ShotInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.mainHandPistol()) {
            return ShotResult.NOT_PISTOL;
        }
        if (input.bullets() <= 0) {
            return ShotResult.NO_BULLETS;
        }
        if (!input.cooldownReady()) {
            return ShotResult.COOLDOWN;
        }
        if (input.targetPresent()
                && input.targetTracked()
                && input.targetActive()
                && input.sameWorld()
                && input.squaredDistance() <= SHOT_RANGE_SQUARED
                && input.canSee()) {
            return ShotResult.FIRED_KILLED;
        }
        return ShotResult.FIRED_MISSED;
    }

    public static int bulletsAfterShot(int bulletsBefore, ShotResult result) {
        Objects.requireNonNull(result, "result");
        if (!result.consumesBullet()) {
            return Math.max(0, bulletsBefore);
        }
        return Math.max(0, bulletsBefore - 1);
    }

    public enum PsychoStartResult {
        IGNORE(false, 0),
        TRACK_ONLY(true, 0),
        GIVE_PISTOL(true, PSYCHO_RESPONSE_BULLETS),
        ADD_BULLETS(true, PSYCHO_RESPONSE_BULLETS);

        private final boolean tracksFrenziedPlayer;
        private final int bulletsToAdd;

        PsychoStartResult(boolean tracksFrenziedPlayer, int bulletsToAdd) {
            this.tracksFrenziedPlayer = tracksFrenziedPlayer;
            this.bulletsToAdd = bulletsToAdd;
        }

        public boolean tracksFrenziedPlayer() {
            return tracksFrenziedPlayer;
        }

        public int bulletsToAdd() {
            return bulletsToAdd;
        }
    }

    public enum ShotResult {
        NOT_PISTOL(false, false),
        NO_BULLETS(false, false),
        COOLDOWN(false, false),
        FIRED_MISSED(true, false),
        FIRED_KILLED(true, true);

        private final boolean consumesBullet;
        private final boolean killsTarget;

        ShotResult(boolean consumesBullet, boolean killsTarget) {
            this.consumesBullet = consumesBullet;
            this.killsTarget = killsTarget;
        }

        public boolean consumesBullet() {
            return consumesBullet;
        }

        public boolean killsTarget() {
            return killsTarget;
        }
    }

    public record PsychoStartInput(
            boolean roundRunning,
            boolean loudPsycho,
            boolean demonHunterRole,
            boolean demonHunterAlive,
            boolean frenziedPlayerPresent,
            boolean hasDemonHunterPistol,
            boolean ownsOtherGun
    ) {
        public PsychoStartInput withLoudPsycho(boolean value) {
            return new PsychoStartInput(roundRunning, value, demonHunterRole, demonHunterAlive,
                    frenziedPlayerPresent, hasDemonHunterPistol, ownsOtherGun);
        }

        public PsychoStartInput withDemonHunterRole(boolean value) {
            return new PsychoStartInput(roundRunning, loudPsycho, value, demonHunterAlive,
                    frenziedPlayerPresent, hasDemonHunterPistol, ownsOtherGun);
        }

        public PsychoStartInput withDemonHunterAlive(boolean value) {
            return new PsychoStartInput(roundRunning, loudPsycho, demonHunterRole, value,
                    frenziedPlayerPresent, hasDemonHunterPistol, ownsOtherGun);
        }
    }

    public record ShotInput(
            boolean mainHandPistol,
            int bullets,
            boolean cooldownReady,
            boolean targetPresent,
            boolean targetTracked,
            boolean targetActive,
            boolean sameWorld,
            double squaredDistance,
            boolean canSee
    ) {
        public ShotInput withMainHandPistol(boolean value) {
            return new ShotInput(value, bullets, cooldownReady, targetPresent, targetTracked,
                    targetActive, sameWorld, squaredDistance, canSee);
        }

        public ShotInput withBullets(int value) {
            return new ShotInput(mainHandPistol, value, cooldownReady, targetPresent, targetTracked,
                    targetActive, sameWorld, squaredDistance, canSee);
        }

        public ShotInput withCooldownReady(boolean value) {
            return new ShotInput(mainHandPistol, bullets, value, targetPresent, targetTracked,
                    targetActive, sameWorld, squaredDistance, canSee);
        }

        public ShotInput withTargetTracked(boolean value) {
            return new ShotInput(mainHandPistol, bullets, cooldownReady, targetPresent, value,
                    targetActive, sameWorld, squaredDistance, canSee);
        }
    }
}
