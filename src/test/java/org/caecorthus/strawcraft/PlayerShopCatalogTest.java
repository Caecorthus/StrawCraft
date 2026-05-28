package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerShopCatalogTest {
    @Test
    void bomberPresentationKeepsOnlySafeExistingWatheEntriesAndOriginalPurchaseIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bomber", true, false),
                List.of(knife, p320, grenade, lockpick, poison)
        );

        assertEquals(List.of(grenade, lockpick), presentation.entries());
        assertEquals(List.of(2, 3), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertSame(grenade, presentation.entryAtVisibleIndex(0).orElseThrow().entry());
    }

    @Test
    void bomberPurchaseValidationRejectsHiddenStaleOrWrongRoleIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bomber", true, false),
                List.of(knife, p320, grenade, lockpick)
        );

        assertFalse(presentation.allowsWathePurchaseIndex(0));
        assertFalse(presentation.allowsWathePurchaseIndex(1));
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertTrue(presentation.allowsWathePurchaseIndex(3));
        assertFalse(presentation.allowsWathePurchaseIndex(99));
    }

    @Test
    void scavengerPresentationKeepsKnifeResetAndSafeToolsWithOriginalPurchaseIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry scorpion = entry("scorpion", 75, ShopEntry.Type.POISON);
        ShopEntry psycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);
        ShopEntry reset = entry("scavenger_reset_knife_cooldown", 150, ShopEntry.Type.WEAPON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("scavenger", true, false),
                List.of(knife, p320, grenade, lockpick, poison, scorpion, psycho, reset)
        );

        assertEquals(List.of(knife, reset, lockpick), presentation.entries());
        assertEquals(List.of(0, 7, 3), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
    }

    @Test
    void scavengerPurchaseValidationRejectsHiddenAndStaleKillerIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry reset = entry("scavenger_reset_knife_cooldown", 150, ShopEntry.Type.WEAPON);

        PlayerShopCatalog.Presentation killerPresentation = PlayerShopCatalog.presentationFor(
                role("killer", true, false),
                List.of(knife, p320, reset)
        );
        PlayerShopCatalog.Presentation scavengerPresentation = PlayerShopCatalog.presentationFor(
                role("scavenger", true, false),
                List.of(knife, p320, reset)
        );

        assertEquals(List.of(knife, p320), killerPresentation.entries());
        assertTrue(killerPresentation.allowsWathePurchaseIndex(1));
        assertFalse(killerPresentation.allowsWathePurchaseIndex(2));
        assertFalse(scavengerPresentation.allowsWathePurchaseIndex(1));
        assertTrue(scavengerPresentation.allowsWathePurchaseIndex(2));
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), List.of(knife, p320, reset), 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("scavenger", true, false), List.of(knife, p320, reset), 1));
    }

    @Test
    void timekeeperPresentationOnlyExposesTimerEntryWithOriginalPurchaseIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry timer = entry("timekeeper_subtract_time", 150, ShopEntry.Type.TOOL);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("timekeeper", false, true),
                List.of(knife, timer, lockpick)
        );

        assertEquals(List.of(timer), presentation.entries());
        assertEquals(List.of(1), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("timekeeper", false, true), List.of(knife, timer, lockpick), 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("timekeeper", false, true), List.of(knife, timer, lockpick), 0));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("bartender", false, true), List.of(knife, timer, lockpick), 1));
    }

    @Test
    void reporterPresentationOnlyExposesNoteEntryWithOriginalPurchaseIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry reporterNote = entry("reporter_note", 25, ShopEntry.Type.TOOL);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("reporter", false, true),
                List.of(knife, reporterNote, lockpick)
        );

        assertEquals(List.of(reporterNote), presentation.entries());
        assertEquals(List.of(1), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("reporter", false, true), List.of(knife, reporterNote, lockpick), 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("reporter", false, true), List.of(knife, reporterNote, lockpick), 0));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), List.of(knife, reporterNote, lockpick), 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("bartender", false, true), List.of(knife, reporterNote, lockpick), 1));
    }

    @Test
    void ordinaryKillerPresentationPreservesExistingKillerShopOrderAndP320Replacement() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("killer", true, false),
                List.of(knife, p320, grenade)
        );

        assertEquals(List.of(knife, p320, grenade), presentation.entries());
        assertEquals(List.of(0, 1, 2), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertEquals("p320", StrawShopEntry.idFor(presentation.entries().get(1)));
    }

    @Test
    void poisonerUsesOrdinaryKillerShopIncludingOfficialPoisonVialOnly() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry reset = entry("scavenger_reset_knife_cooldown", 150, ShopEntry.Type.WEAPON);
        ShopEntry reporterNote = entry("reporter_note", 25, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("poisoner", true, false),
                List.of(knife, p320, poison, reset, reporterNote)
        );

        assertEquals(List.of(knife, p320, poison), presentation.entries());
        assertEquals(List.of(0, 1, 2), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertFalse(presentation.allowsWathePurchaseIndex(3));
        assertFalse(presentation.allowsWathePurchaseIndex(4));
    }

    @Test
    void roleWithoutShopHasEmptyPresentation() {
        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bartender", false, true),
                List.of(entry("grenade", 350, ShopEntry.Type.WEAPON))
        );

        assertTrue(presentation.entries().isEmpty());
        assertFalse(presentation.allowsWathePurchaseIndex(0));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }

    private static Role role(String path, boolean killerTools, boolean innocent) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
