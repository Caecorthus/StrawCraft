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
        ShopEntry timedBomb = entry("timed_bomb", 300, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bomber", true, false),
                List.of(knife, p320, grenade, timedBomb, lockpick, poison)
        );

        assertEquals(List.of(grenade, timedBomb, lockpick), presentation.entries());
        assertEquals(List.of(2, 3, 4), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertSame(grenade, presentation.entryAtVisibleIndex(0).orElseThrow().entry());
        assertSame(timedBomb, presentation.entryAtVisibleIndex(1).orElseThrow().entry());
    }

    @Test
    void bomberPurchaseValidationRejectsHiddenStaleOrWrongRoleIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);
        ShopEntry timedBomb = entry("timed_bomb", 300, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bomber", true, false),
                List.of(knife, p320, grenade, timedBomb, lockpick)
        );

        assertFalse(presentation.allowsWathePurchaseIndex(0));
        assertFalse(presentation.allowsWathePurchaseIndex(1));
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertTrue(presentation.allowsWathePurchaseIndex(3));
        assertTrue(presentation.allowsWathePurchaseIndex(4));
        assertFalse(presentation.allowsWathePurchaseIndex(99));
    }

    @Test
    void scavengerPresentationKeepsKnifeResetAndSafeToolsWithOriginalPurchaseIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = entry("grenade", 350, ShopEntry.Type.WEAPON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        ShopEntry timedBomb = entry("timed_bomb", 300, ShopEntry.Type.WEAPON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry scorpion = entry("scorpion", 75, ShopEntry.Type.POISON);
        ShopEntry psycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);
        ShopEntry reset = entry("scavenger_reset_knife_cooldown", 150, ShopEntry.Type.WEAPON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("scavenger", true, false),
                List.of(knife, p320, grenade, lockpick, timedBomb, poison, scorpion, psycho, reset)
        );

        assertEquals(List.of(knife, reset, lockpick), presentation.entries());
        assertEquals(List.of(0, 8, 3), presentation.visibleEntries().stream()
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
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry scorpion = entry("scorpion", 75, ShopEntry.Type.POISON);
        ShopEntry psycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);
        ShopEntry timedBomb = entry("timed_bomb", 300, ShopEntry.Type.WEAPON);
        ShopEntry silentPsycho = entry(SilencerShopLoadout.SILENT_PSYCHO_ENTRY_ID, 350, ShopEntry.Type.POISON);
        ShopEntry poisonNeedle = entry(PoisonerShopLoadout.POISON_NEEDLE_ENTRY_ID, 50, ShopEntry.Type.POISON);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("killer", true, false),
                List.of(knife, p320, grenade, poison, scorpion, psycho, timedBomb, silentPsycho, poisonNeedle)
        );

        assertEquals(List.of(knife, p320, grenade, poison, scorpion, psycho), presentation.entries());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertEquals("p320", StrawShopEntry.idFor(presentation.entries().get(1)));
        assertFalse(presentation.allowsWathePurchaseIndex(6));
        assertFalse(presentation.allowsWathePurchaseIndex(7));
        assertFalse(presentation.allowsWathePurchaseIndex(8));
    }

    @Test
    void poisonerPresentationOnlyExposesPoisonEntriesWithOriginalPurchaseIndices() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry p320 = entry("p320", 300, ShopEntry.Type.WEAPON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry scorpion = entry("scorpion", 75, ShopEntry.Type.POISON);
        ShopEntry poisonNeedle = entry(PoisonerShopLoadout.POISON_NEEDLE_ENTRY_ID, 50, ShopEntry.Type.POISON);
        ShopEntry psycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);
        ShopEntry defenseVial = entry("defense_vial", 100, ShopEntry.Type.TOOL);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        ShopEntry reset = entry("scavenger_reset_knife_cooldown", 150, ShopEntry.Type.WEAPON);
        ShopEntry reporterNote = entry("reporter_note", 25, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("poisoner", true, false),
                List.of(knife, p320, poison, scorpion, poisonNeedle, psycho, defenseVial, lockpick, reset, reporterNote)
        );

        assertEquals(List.of("poison_vial", "scorpion", PoisonerShopLoadout.POISON_NEEDLE_ENTRY_ID), presentation.entries().stream()
                .map(StrawShopEntry::idFor)
                .toList());
        assertEquals(List.of(50, 50, PoisonerShopLoadout.POISON_NEEDLE_PRICE), presentation.entries().stream()
                .map(ShopEntry::price)
                .toList());
        assertEquals(List.of(2, 3, 4), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertTrue(presentation.allowsWathePurchaseIndex(3));
        assertTrue(presentation.allowsWathePurchaseIndex(4));
        assertFalse(presentation.allowsWathePurchaseIndex(0));
        assertFalse(presentation.allowsWathePurchaseIndex(1));
        assertFalse(presentation.allowsWathePurchaseIndex(5));
        assertFalse(presentation.allowsWathePurchaseIndex(6));
        assertFalse(presentation.allowsWathePurchaseIndex(7));
        assertFalse(presentation.allowsWathePurchaseIndex(8));
        assertFalse(presentation.allowsWathePurchaseIndex(9));
        assertEquals(75, poison.price());
        assertEquals(75, scorpion.price());
    }

    @Test
    void nonPoisonerRolesCannotSeeOrBuyHiddenPoisonNeedleEntryByStaleIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry poisonNeedle = entry(PoisonerShopLoadout.POISON_NEEDLE_ENTRY_ID, 50, ShopEntry.Type.POISON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        List<ShopEntry> entries = List.of(knife, poisonNeedle, poison);

        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("bomber", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("scavenger", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("waiter", false, true), entries, 1));
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("poisoner", true, false), entries, 1));
    }

    @Test
    void silencerPresentationOnlyExposesSilentPsychoEntryAtSparkPriceWithOriginalPurchaseIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry officialPsycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);
        ShopEntry silentPsycho = entry(SilencerShopLoadout.SILENT_PSYCHO_ENTRY_ID, 350, ShopEntry.Type.POISON);
        ShopEntry lockpick = entry("lockpick", 50, ShopEntry.Type.TOOL);
        List<ShopEntry> entries = List.of(knife, officialPsycho, silentPsycho, lockpick);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("silencer", true, false),
                entries
        );

        assertEquals(List.of(silentPsycho), presentation.entries());
        assertEquals(List.of(SilencerShopLoadout.SILENT_PSYCHO_PRICE), presentation.entries().stream()
                .map(ShopEntry::price)
                .toList());
        assertEquals(List.of(2), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertFalse(presentation.allowsWathePurchaseIndex(0));
        assertFalse(presentation.allowsWathePurchaseIndex(1));
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertFalse(presentation.allowsWathePurchaseIndex(3));
    }

    @Test
    void nonSilencerRolesCannotSeeOrBuyHiddenSilentPsychoEntryByStaleIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry silentPsycho = entry(SilencerShopLoadout.SILENT_PSYCHO_ENTRY_ID, 350, ShopEntry.Type.POISON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        List<ShopEntry> entries = List.of(knife, silentPsycho, poison);

        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("bomber", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("scavenger", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("poisoner", true, false), entries, 1));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("waiter", false, true), entries, 1));
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("silencer", true, false), entries, 1));
    }

    @Test
    void bartenderPresentationOnlyExposesDefenseVialWithOriginalPurchaseIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry defenseVial = entry("defense_vial", 100, ShopEntry.Type.TOOL);
        ShopEntry waiterService = entry("waiter_service_tray", 50, ShopEntry.Type.TOOL);
        ShopEntry timer = entry("timekeeper_subtract_time", 150, ShopEntry.Type.TOOL);
        ShopEntry reporterNote = entry("reporter_note", 25, ShopEntry.Type.TOOL);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("bartender", false, true),
                List.of(knife, poison, defenseVial, waiterService, timer, reporterNote)
        );

        assertEquals(List.of(defenseVial), presentation.entries());
        assertEquals(List.of(2), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertFalse(presentation.allowsWathePurchaseIndex(0));
        assertFalse(presentation.allowsWathePurchaseIndex(1));
        assertTrue(presentation.allowsWathePurchaseIndex(2));
        assertFalse(presentation.allowsWathePurchaseIndex(3));
        assertFalse(presentation.allowsWathePurchaseIndex(4));
        assertFalse(presentation.allowsWathePurchaseIndex(5));
    }

    @Test
    void nonBartenderRolesCannotSeeOrBuyHiddenDefenseVialEntry() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry defenseVial = entry("defense_vial", 100, ShopEntry.Type.TOOL);
        List<ShopEntry> entries = List.of(knife, poison, defenseVial);

        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("detective", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("timekeeper", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("reporter", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("poisoner", true, false), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), entries, 2));
    }

    @Test
    void waiterPresentationOnlyExposesServiceEntryWithOriginalPurchaseIndex() {
        ShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        ShopEntry defenseVial = entry("defense_vial", 100, ShopEntry.Type.TOOL);
        ShopEntry waiterService = entry("waiter_service_tray", 50, ShopEntry.Type.TOOL);
        ShopEntry reporterNote = entry("reporter_note", 25, ShopEntry.Type.TOOL);
        List<ShopEntry> entries = List.of(knife, defenseVial, waiterService, reporterNote);

        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("waiter", false, true),
                entries
        );

        assertEquals(List.of(waiterService), presentation.entries());
        assertEquals(List.of(2), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
        assertTrue(PlayerShopCatalog.allowsPurchaseForRole(role("waiter", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("waiter", false, true), entries, 0));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("bartender", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("reporter", false, true), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("killer", true, false), entries, 2));
        assertFalse(PlayerShopCatalog.allowsPurchaseForRole(role("poisoner", true, false), entries, 2));
    }

    @Test
    void roleWithoutShopHasEmptyPresentation() {
        PlayerShopCatalog.Presentation presentation = PlayerShopCatalog.presentationFor(
                role("detective", false, true),
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
