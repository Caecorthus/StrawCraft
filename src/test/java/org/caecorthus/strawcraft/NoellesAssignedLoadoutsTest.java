package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesAssignedLoadoutsTest {
    @Test
    void supportedOpeningLoadoutItemsResolveAgainstOfficialWatheJarOrStrawCraftRegistry() {
        Set<Identifier> officialWatheItemIds = officialWatheItemIds();
        Set<Identifier> registeredStrawCraftItemIds = registeredStrawCraftItemIds();

        for (NoellesAssignedLoadouts.ItemGrant grant : NoellesAssignedLoadouts.supportedItemGrantsForVerification()) {
            boolean supported = officialWatheItemIds.contains(grant.itemId())
                    || registeredStrawCraftItemIds.contains(grant.itemId());

            assertTrue(supported, grant.itemId() + " is marked supported but is not in official Wathe items");
        }
    }

    @Test
    void officialWatheJarDoesNotProvideSparkWalkieTalkieButDoesProvideNote() {
        Set<Identifier> officialWatheItemIds = officialWatheItemIds();

        assertTrue(officialWatheItemIds.contains(Identifier.of("wathe", "note")));
        assertFalse(officialWatheItemIds.contains(Identifier.of("wathe", "walkie_talkie")));
    }

    @Test
    void undercoverWalkieTalkieIsDeferredUntilOfficialOrStrawCraftBehaviorExists() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("undercover"), false);

        assertTrue(plan.assignmentItemGrants().isEmpty());
        assertEquals(List.of(new NoellesAssignedLoadouts.UnsupportedItemGrant(
                        Identifier.of("wathe", "walkie_talkie"),
                        1,
                        "XruiDD Undercover walkie-talkie depends on Spark-ver Wathe; official Wathe has no wathe:walkie_talkie item"
                )),
                plan.unsupportedItemGrants());
    }

    @Test
    void nonUndercoverRoleDoesNotReceiveWalkieTalkie() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("scavenger"), false);

        assertTrue(plan.assignmentItemGrants().isEmpty());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void awesomeBinglusReceivesOneAggregatedWatheNoteGrant() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("awesome_binglus"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(Identifier.of("wathe", "note"), 16)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void conductorReceivesOneRealStrawCraftMasterKeyGrant() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("conductor"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(StrawCraft.id("master_key"), 1)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void toxicologistReceivesOneRealStrawCraftAntidoteGrant() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("toxicologist"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(StrawCraft.id("antidote"), 1)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void noisemakerReceivesOneRealStrawCraftNoisemakerGrant() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("noisemaker"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(StrawCraft.id("noisemaker"), 1)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void engineerReceivesOneRealStrawCraftRepairToolGrant() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("engineer"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(StrawCraft.id("repair_tool"), 1)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void corruptCopReceivesOfficialWatheRevolverButNoFakeNeutralMasterKey() {
        RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole("corrupt_cop"), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(Identifier.of("wathe", "revolver"), 1)),
                plan.assignmentItemGrants());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    @Test
    void neutralRolesDoNotReceiveFakeMasterKeyPlaceholders() {
        for (String rolePath : List.of("vulture", "pathogen", "taotie")) {
            RoleAssignedLoadouts.AssignmentPlan plan = RoleAssignedLoadouts.planAssignedLoadout(noellesRole(rolePath), false);

            assertTrue(plan.assignmentItemGrants().isEmpty(), rolePath + " should not receive a fake neutral key");
            assertTrue(plan.unsupportedItemGrants().isEmpty(), rolePath + " should not carry fake neutral key metadata");
        }
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }

    private static Set<Identifier> officialWatheItemIds() {
        Set<Identifier> ids = new HashSet<>();
        for (Field field : WatheItems.class.getFields()) {
            if (Item.class.isAssignableFrom(field.getType())) {
                ids.add(Identifier.of("wathe", field.getName().toLowerCase(Locale.ROOT)));
            }
        }
        return ids;
    }

    private static Set<Identifier> registeredStrawCraftItemIds() {
        return Set.of(
                StrawCraftItems.MASTER_KEY_ID,
                StrawCraftItems.ANTIDOTE_ID,
                StrawCraftItems.NOISEMAKER_ID,
                StrawCraftItems.REPAIR_TOOL_ID
        );
    }
}
