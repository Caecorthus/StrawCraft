package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesRoleRegistrationTest {
    @Test
    void registrationAdapterAddsCatalogRolesOnlyOnce() {
        Set<Identifier> catalogIds = NoellesRoleCatalog.all().stream()
                .map(NoellesRoleCatalog.Entry::id)
                .collect(Collectors.toSet());
        List<Role> registeredRoles = new ArrayList<>(List.of(NoellesRoleCatalog.all().getFirst().watheRole()));
        List<Role> acceptedRoles = new ArrayList<>(registeredRoles);

        NoellesRoleCatalog.registerInto(registeredRoles, role -> {
            registeredRoles.add(role);
            acceptedRoles.add(role);
        });
        NoellesRoleCatalog.registerInto(registeredRoles, role -> {
            registeredRoles.add(role);
            acceptedRoles.add(role);
        });

        assertEquals(catalogIds.size(), acceptedRoles.size());
        assertTrue(acceptedRoles.stream().map(Role::identifier).collect(Collectors.toSet()).containsAll(catalogIds));
        acceptedRoles.stream()
                .map(Role::identifier)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .forEach((id, count) -> assertEquals(1, count, id + " should be registered only once"));
    }

    @Test
    void watheRegistrationUsesOfficialRoleRegistry() throws java.io.IOException {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/org/caecorthus/strawcraft/NoellesRoleCatalog.java"),
                java.nio.charset.StandardCharsets.UTF_8
        );

        assertTrue(source.contains("WatheRoles::registerRole"));
    }

    @Test
    void startupRegistersNoellesCatalogBeforeRuntimeBridge() throws java.io.IOException {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                java.nio.charset.StandardCharsets.UTF_8
        );

        int catalogRegistration = source.indexOf("NoellesRoleCatalog.registerWithWathe()");
        int runtimeBridge = source.indexOf("WatheOfficialBridge.register()");

        assertTrue(catalogRegistration >= 0);
        assertTrue(runtimeBridge >= 0);
        assertTrue(catalogRegistration < runtimeBridge);
    }
}
