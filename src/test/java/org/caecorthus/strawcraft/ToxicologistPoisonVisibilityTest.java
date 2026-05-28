package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawRoleDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToxicologistPoisonVisibilityTest {
    @Test
    void toxicologistAndPoisonerCanSeeOfficialWathePoison() {
        assertTrue(ToxicologistPoisonVisibility.canSeePoison(noellesRole("toxicologist")));
        assertTrue(ToxicologistPoisonVisibility.canSeePoison(noellesRole("poisoner")));
    }

    @Test
    void otherRolesDoNotGainPoisonVisibility() {
        assertFalse(ToxicologistPoisonVisibility.canSeePoison(noellesRole("detective")));
        assertFalse(ToxicologistPoisonVisibility.canSeePoison(null));
    }

    @Test
    void toxicologistIsRuntimeSelectableAfterAntidoteBehaviorExists() {
        Set<Identifier> runtimeCandidates = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertTrue(NoellesRoleCatalog.find(StrawCraft.id("toxicologist")).isPresent());
        assertTrue(runtimeCandidates.contains(StrawCraft.id("toxicologist")));
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }
}
