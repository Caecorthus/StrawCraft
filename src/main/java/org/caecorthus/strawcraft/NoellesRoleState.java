package org.caecorthus.strawcraft;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NoellesRoleState {
    private static final String COOLDOWNS_KEY = "AbilityCooldownDeadlines";
    private static final String FLAGS_KEY = "Flags";
    private static final String TIMESTAMPS_KEY = "Timestamps";
    private static final String COUNTERS_KEY = "Counters";
    private static final String UUID_SETS_KEY = "UuidSets";
    private static final String NEUTRAL_WIN_CLAIMS_KEY = "NeutralWinClaims";
    private static final String CLAIM_ROLE_ID_KEY = "RoleId";
    private static final String CLAIM_TRIGGER_KEY = "Trigger";
    private static final String CLAIM_OPPONENT_UUID_KEY = "OpponentUuid";
    private static final String CLAIM_GAME_TIME_KEY = "GameTime";

    private final Map<String, Long> abilityCooldownDeadlines = new HashMap<>();
    private final Map<String, Boolean> flags = new HashMap<>();
    private final Map<String, Long> timestamps = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Set<UUID>> uuidSets = new HashMap<>();
    private final Map<String, NeutralWinClaim> neutralWinClaims = new HashMap<>();

    public void reset() {
        abilityCooldownDeadlines.clear();
        flags.clear();
        timestamps.clear();
        counters.clear();
        uuidSets.clear();
        neutralWinClaims.clear();
    }

    public boolean tryBeginAbilityCooldown(String abilityId, long now, int cooldownTicks) {
        // Store absolute deadlines so cooldowns survive saves without per-tick component logic.
        // 这里存绝对结束 tick，这样冷却可以存档，不需要每 tick 递减组件状态。
        if (isAbilityOnCooldown(abilityId, now)) {
            return false;
        }
        setAbilityCooldown(abilityId, now, cooldownTicks);
        return true;
    }

    public void setAbilityCooldown(String abilityId, long now, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            abilityCooldownDeadlines.remove(abilityId);
            return;
        }
        abilityCooldownDeadlines.put(abilityId, now + cooldownTicks);
    }

    public boolean isAbilityOnCooldown(String abilityId, long now) {
        return getRemainingAbilityCooldown(abilityId, now) > 0;
    }

    public int getRemainingAbilityCooldown(String abilityId, long now) {
        long deadline = abilityCooldownDeadlines.getOrDefault(abilityId, 0L);
        long remaining = Math.max(0L, deadline - now);
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    public void setFlag(String flag, boolean value) {
        if (value) {
            flags.put(flag, true);
        } else {
            flags.remove(flag);
        }
    }

    public boolean hasFlag(String flag) {
        return flags.getOrDefault(flag, false);
    }

    public void setTimestamp(String key, long tick) {
        timestamps.put(key, tick);
    }

    public OptionalLong getTimestamp(String key) {
        Long tick = timestamps.get(key);
        return tick == null ? OptionalLong.empty() : OptionalLong.of(tick);
    }

    public void setCounter(String key, int value) {
        if (value == 0) {
            counters.remove(key);
        } else {
            counters.put(key, value);
        }
    }

    public int getCounter(String key) {
        return counters.getOrDefault(key, 0);
    }

    public int incrementCounter(String key) {
        int value = getCounter(key) + 1;
        setCounter(key, value);
        return value;
    }

    public boolean addUuidToSet(String key, UUID uuid) {
        return uuidSets.computeIfAbsent(key, ignored -> new HashSet<>()).add(uuid);
    }

    public boolean uuidSetContains(String key, UUID uuid) {
        return uuidSets.getOrDefault(key, Set.of()).contains(uuid);
    }

    public Set<UUID> uuidSet(String key) {
        return Collections.unmodifiableSet(uuidSets.getOrDefault(key, Set.of()));
    }

    public void clearUuidSet(String key) {
        uuidSets.remove(key);
    }

    public void recordNeutralWinClaim(NeutralWinClaim claim) {
        neutralWinClaims.put(claim.roleId().toString(), claim);
    }

    public void clearNeutralWinClaim(Identifier roleId) {
        neutralWinClaims.remove(roleId.toString());
    }

    public Optional<NeutralWinClaim> neutralWinClaim(Identifier roleId) {
        return Optional.ofNullable(neutralWinClaims.get(roleId.toString()));
    }

    public Set<NeutralWinClaim> neutralWinClaims() {
        return Collections.unmodifiableSet(neutralWinClaims.values().stream().collect(Collectors.toSet()));
    }

    public void readFromNbt(NbtCompound nbt) {
        abilityCooldownDeadlines.clear();
        flags.clear();
        timestamps.clear();
        counters.clear();
        uuidSets.clear();
        neutralWinClaims.clear();

        NbtCompound cooldowns = nbt.getCompound(COOLDOWNS_KEY);
        for (String abilityId : cooldowns.getKeys()) {
            abilityCooldownDeadlines.put(abilityId, cooldowns.getLong(abilityId));
        }

        NbtCompound savedFlags = nbt.getCompound(FLAGS_KEY);
        for (String flag : savedFlags.getKeys()) {
            if (savedFlags.getBoolean(flag)) {
                flags.put(flag, true);
            }
        }

        NbtCompound savedTimestamps = nbt.getCompound(TIMESTAMPS_KEY);
        for (String key : savedTimestamps.getKeys()) {
            timestamps.put(key, savedTimestamps.getLong(key));
        }

        NbtCompound savedCounters = nbt.getCompound(COUNTERS_KEY);
        for (String key : savedCounters.getKeys()) {
            counters.put(key, savedCounters.getInt(key));
        }

        NbtCompound savedUuidSets = nbt.getCompound(UUID_SETS_KEY);
        for (String key : savedUuidSets.getKeys()) {
            NbtList savedUuids = savedUuidSets.getList(key, NbtElement.STRING_TYPE);
            Set<UUID> uuids = new HashSet<>();
            for (int index = 0; index < savedUuids.size(); index++) {
                uuids.add(UUID.fromString(savedUuids.getString(index)));
            }
            if (!uuids.isEmpty()) {
                uuidSets.put(key, uuids);
            }
        }

        NbtCompound savedNeutralWinClaims = nbt.getCompound(NEUTRAL_WIN_CLAIMS_KEY);
        for (String key : savedNeutralWinClaims.getKeys()) {
            readNeutralWinClaim(savedNeutralWinClaims.getCompound(key))
                    .ifPresent(this::recordNeutralWinClaim);
        }
    }

    public void writeToNbt(NbtCompound nbt) {
        NbtCompound cooldowns = new NbtCompound();
        abilityCooldownDeadlines.forEach(cooldowns::putLong);
        nbt.put(COOLDOWNS_KEY, cooldowns);

        NbtCompound savedFlags = new NbtCompound();
        flags.forEach(savedFlags::putBoolean);
        nbt.put(FLAGS_KEY, savedFlags);

        NbtCompound savedTimestamps = new NbtCompound();
        timestamps.forEach(savedTimestamps::putLong);
        nbt.put(TIMESTAMPS_KEY, savedTimestamps);

        NbtCompound savedCounters = new NbtCompound();
        counters.forEach(savedCounters::putInt);
        nbt.put(COUNTERS_KEY, savedCounters);

        NbtCompound savedUuidSets = new NbtCompound();
        uuidSets.forEach((key, uuids) -> {
            NbtList savedUuids = new NbtList();
            uuids.forEach(uuid -> savedUuids.add(NbtString.of(uuid.toString())));
            savedUuidSets.put(key, savedUuids);
        });
        nbt.put(UUID_SETS_KEY, savedUuidSets);

        NbtCompound savedNeutralWinClaims = new NbtCompound();
        neutralWinClaims.forEach((key, claim) -> savedNeutralWinClaims.put(key, writeNeutralWinClaim(claim)));
        nbt.put(NEUTRAL_WIN_CLAIMS_KEY, savedNeutralWinClaims);
    }

    private static Optional<NeutralWinClaim> readNeutralWinClaim(NbtCompound nbt) {
        Identifier roleId = Identifier.tryParse(nbt.getString(CLAIM_ROLE_ID_KEY));
        Identifier trigger = Identifier.tryParse(nbt.getString(CLAIM_TRIGGER_KEY));
        if (roleId == null || trigger == null) {
            return Optional.empty();
        }

        Optional<UUID> opponentUuid = Optional.empty();
        String savedOpponentUuid = nbt.getString(CLAIM_OPPONENT_UUID_KEY);
        if (!savedOpponentUuid.isBlank()) {
            opponentUuid = Optional.of(UUID.fromString(savedOpponentUuid));
        }
        return Optional.of(new NeutralWinClaim(roleId, trigger, opponentUuid, nbt.getLong(CLAIM_GAME_TIME_KEY)));
    }

    private static NbtCompound writeNeutralWinClaim(NeutralWinClaim claim) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(CLAIM_ROLE_ID_KEY, claim.roleId().toString());
        nbt.putString(CLAIM_TRIGGER_KEY, claim.trigger().toString());
        claim.opponentUuid().ifPresent(uuid -> nbt.putString(CLAIM_OPPONENT_UUID_KEY, uuid.toString()));
        nbt.putLong(CLAIM_GAME_TIME_KEY, claim.gameTime());
        return nbt;
    }

    public record NeutralWinClaim(
            Identifier roleId,
            Identifier trigger,
            Optional<UUID> opponentUuid,
            long gameTime
    ) {
        public NeutralWinClaim {
            // This records a role-owned win claim without forcing official Wathe to display it yet.
            // 这里只记录职业自己的胜利主张，不强行让官方 Wathe 现在展示胜利结果。
            java.util.Objects.requireNonNull(roleId, "roleId");
            java.util.Objects.requireNonNull(trigger, "trigger");
            java.util.Objects.requireNonNull(opponentUuid, "opponentUuid");
        }
    }
}
