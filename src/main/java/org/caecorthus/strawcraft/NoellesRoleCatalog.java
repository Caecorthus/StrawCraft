package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;
import org.caecorthus.strawcraft.role.StrawRoleDefinition;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class NoellesRoleCatalog {
    private static final int KILLER_COLOR = 0xC13A38;
    private static final int GOOD_COLOR = 0x37095B;
    private static final int NEUTRAL_COLOR = 0x9EFE00;
    private static final int DEFAULT_MAX_SPRINT_TICKS = 200;

    private static final List<Entry> ROLES = List.of(
            killer("swapper"),
            killer("phantom"),
            killer("morphling"),
            disabledKiller("the_insane_damned_paranoid_killer"),
            killer("bomber"),
            killer("assassin"),
            killer("scavenger"),
            killer("serial_killer"),
            killer("silencer"),
            killer("poisoner"),
            killer("bandit"),
            good("time_keeper"),
            good("undercover"),
            good("conductor"),
            disabledGood("awesome_binglus"),
            good("bartender"),
            good("noisemaker"),
            good("voodoo"),
            good("coroner"),
            good("recaller"),
            good("toxicologist"),
            good("reporter"),
            good("professor"),
            good("attendant"),
            good("bodyguard"),
            good("survival_master"),
            good("engineer"),
            good("spiritualist"),
            good("detective"),
            good("waiter"),
            good("mermaid"),
            good("demon_hunter"),
            disabledNeutral("jester"),
            neutral("vulture"),
            neutral("corrupt_cop"),
            neutral("pathogen"),
            neutral("taotie")
    );

    private static final Set<Identifier> FIRST_ROUND_DISABLED_IDS = ROLES.stream()
            .filter(entry -> !entry.firstRoundEligible())
            .map(Entry::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private NoellesRoleCatalog() {
    }

    public static List<Entry> all() {
        return ROLES;
    }

    public static Optional<Entry> find(Identifier id) {
        return ROLES.stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst();
    }

    public static boolean isFirstRoundEligible(Identifier id) {
        return find(id)
                .map(Entry::firstRoundEligible)
                .orElse(false);
    }

    public static List<StrawRoleDefinition> definitions() {
        return ROLES.stream()
                .map(Entry::definition)
                .toList();
    }

    public static List<StrawRoleDefinition> firstRoundDefinitions() {
        return ROLES.stream()
                .filter(Entry::firstRoundEligible)
                .map(Entry::definition)
                .toList();
    }

    public static Set<Identifier> firstRoundDisabledIds() {
        return FIRST_ROUND_DISABLED_IDS;
    }

    public static void registerWithWathe() {
        registerInto(WatheRoles.ROLES, WatheRoles::registerRole);
    }

    static void registerInto(Iterable<Role> registeredRoles, Consumer<Role> registrar) {
        Set<Identifier> registeredIds = java.util.stream.StreamSupport.stream(registeredRoles.spliterator(), false)
                .map(Role::identifier)
                .collect(java.util.stream.Collectors.toSet());
        for (Entry entry : ROLES) {
            if (registeredIds.add(entry.id())) {
                registrar.accept(entry.watheRole());
            }
        }
    }

    private static Entry killer(String path) {
        return entry(path, StrawFaction.KILLER, true);
    }

    private static Entry disabledKiller(String path) {
        return entry(path, StrawFaction.KILLER, false);
    }

    private static Entry good(String path) {
        return entry(path, StrawFaction.GOOD, true);
    }

    private static Entry disabledGood(String path) {
        return entry(path, StrawFaction.GOOD, false);
    }

    private static Entry neutral(String path) {
        return entry(path, StrawFaction.NEUTRAL, true);
    }

    private static Entry disabledNeutral(String path) {
        return entry(path, StrawFaction.NEUTRAL, false);
    }

    private static Entry entry(String path, StrawFaction faction, boolean firstRoundEligible) {
        Identifier id = StrawCraft.id(path);
        boolean innocent = faction == StrawFaction.GOOD;
        boolean killerTools = faction == StrawFaction.KILLER;
        Role.MoodType moodType = faction == StrawFaction.KILLER ? Role.MoodType.FAKE : Role.MoodType.REAL;
        return new Entry(
                id,
                faction,
                firstRoundEligible,
                new Role(id, colorFor(faction), innocent, killerTools, moodType, DEFAULT_MAX_SPRINT_TICKS, false),
                new StrawRoleDefinition(id, faction, false, true, context -> true)
        );
    }

    private static int colorFor(StrawFaction faction) {
        return switch (faction) {
            case KILLER -> KILLER_COLOR;
            case NEUTRAL -> NEUTRAL_COLOR;
            case GOOD -> GOOD_COLOR;
            case NONE, WITCH -> 0xFFFFFF;
        };
    }

    public record Entry(
            Identifier id,
            StrawFaction faction,
            boolean firstRoundEligible,
            Role watheRole,
            StrawRoleDefinition definition
    ) {
    }
}
