package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class StrawRoleMeaningTest {
    @Test
    void killerCapableRoleCanUseKillerShopAndReceivesKillerAmmo() {
        Role role = role(WatheRoleIds.KILLER, false, true);

        assertEquals(StrawFaction.KILLER, factionFor(role));
        assertTrue(canUseKillerShop(role));
        assertEquals(Optional.of(GunAmmoFaction.KILLER), ammoFactionFor(role));
    }

    @Test
    void innocentRoleReceivesCivilianAmmoWithoutKillerShopAccess() {
        Role role = role(WatheRoleIds.CIVILIAN, true, false);

        assertEquals(StrawFaction.GOOD, factionFor(role));
        assertFalse(canUseKillerShop(role));
        assertEquals(Optional.of(GunAmmoFaction.CIVILIAN), ammoFactionFor(role));
    }

    @Test
    void vigilanteIdReceivesVigilanteLoadout() {
        Role role = role(WatheRoleIds.VIGILANTE, true, false);

        assertEquals(StrawFaction.GOOD, factionFor(role));
        assertTrue(receivesVigilanteLoadout(role));
        assertFalse(canUseKillerShop(role));
        assertEquals(Optional.of(GunAmmoFaction.POLICE), ammoFactionFor(role));
    }

    @Test
    void discoveryCivilianDoesNotReceivePrivilegedBehaviorOrAmmoFaction() {
        Role role = role(WatheRoleIds.DISCOVERY_CIVILIAN, true, false);

        assertEquals(StrawFaction.NONE, factionFor(role));
        assertFalse(canUseKillerShop(role));
        assertTrue(ammoFactionFor(role).isEmpty());
        assertFalse(receivesVigilanteLoadout(role));
    }

    @Test
    void noRoleAndLooseEndMapToNoneAndNeutralWithoutAmmoFallback() {
        Role noRole = role(WatheRoleIds.NO_ROLE, false, false);
        Role looseEnd = role(WatheRoleIds.LOOSE_END, false, false);

        assertEquals(StrawFaction.NONE, factionFor(noRole));
        assertEquals(StrawFaction.NEUTRAL, factionFor(looseEnd));
        assertTrue(ammoFactionFor(noRole).isEmpty());
        assertTrue(ammoFactionFor(looseEnd).isEmpty());
    }

    @Test
    void witchIsNotInferredFromWatheBooleans() {
        Role booleanNeutral = role(Identifier.of("strawcraft", "witch_like_fixture"), false, false);

        assertEquals(StrawFaction.NONE, factionFor(booleanNeutral));
        assertTrue(ammoFactionFor(booleanNeutral).isEmpty());
    }

    @Test
    void noellesCatalogRolesMapToBroadStrawCraftFactions() {
        assertEquals(StrawFaction.KILLER, factionFor(noellesRole("bomber")));
        assertEquals(StrawFaction.KILLER, factionFor(noellesRole("assassin")));
        assertEquals(StrawFaction.GOOD, factionFor(noellesRole("detective")));
        assertEquals(StrawFaction.GOOD, factionFor(noellesRole("bodyguard")));
        assertEquals(StrawFaction.NEUTRAL, factionFor(noellesRole("jester")));
        assertEquals(StrawFaction.NEUTRAL, factionFor(noellesRole("taotie")));
    }

    @Test
    void onlyUndercoverReceivesNoellesWalkieTalkieMeaning() {
        assertTrue(receivesUndercoverWalkieTalkie(noellesRole("undercover")));
        assertFalse(receivesUndercoverWalkieTalkie(noellesRole("conductor")));
        assertFalse(receivesUndercoverWalkieTalkie(noellesRole("scavenger")));
    }

    @Test
    void onlyNoellesDetectiveReceivesDetectiveInvestigation() {
        assertTrue(receivesDetectiveInvestigation(noellesRole("detective")));
        assertFalse(receivesDetectiveInvestigation(role(WatheRoleIds.VIGILANTE, true, false)));
        assertFalse(receivesDetectiveInvestigation(noellesRole("undercover")));
    }

    @Test
    void onlyNoellesVultureReceivesBodyFeast() {
        assertTrue(receivesVultureBodyFeast(noellesRole("vulture")));
        assertFalse(receivesVultureBodyFeast(noellesRole("detective")));
        assertFalse(receivesVultureBodyFeast(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesPathogenReceivesInfectionMeaning() {
        assertTrue(receivesPathogenInfection(noellesRole("pathogen")));
        assertFalse(receivesPathogenInfection(noellesRole("vulture")));
        assertFalse(receivesPathogenInfection(role(WatheRoleIds.LOOSE_END, false, false)));
    }

    @Test
    void onlyNoellesAssassinReceivesGuessMeaningAndKeepsKillerFaction() {
        Role assassin = noellesRole("assassin");

        assertTrue(receivesAssassinGuess(assassin));
        assertEquals(StrawFaction.KILLER, factionFor(assassin));
        assertTrue(canUseKillerShop(assassin));
        assertFalse(receivesAssassinGuess(noellesRole("bomber")));
        assertFalse(receivesAssassinGuess(noellesRole("detective")));
        assertFalse(receivesAssassinGuess(role(WatheRoleIds.KILLER, false, true)));
    }

    @Test
    void onlyNoellesBodyguardReceivesNearbyProtectionMeaning() {
        assertTrue(receivesBodyguardProtection(noellesRole("bodyguard")));
        assertFalse(receivesBodyguardProtection(noellesRole("professor")));
        assertFalse(receivesBodyguardProtection(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesRecallerReceivesRecallMeaning() {
        assertTrue(receivesRecallerRecall(noellesRole("recaller")));
        assertFalse(receivesRecallerRecall(noellesRole("detective")));
        assertFalse(receivesRecallerRecall(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesSwapperReceivesSwapMeaning() {
        assertTrue(receivesSwapperSwap(noellesRole("swapper")));
        assertFalse(receivesSwapperSwap(noellesRole("bomber")));
        assertFalse(receivesSwapperSwap(role(WatheRoleIds.KILLER, false, true)));
    }

    @Test
    void onlyNoellesReporterReceivesReporterMarkMeaning() {
        assertTrue(receivesReporterMark(noellesRole("reporter")));
        assertFalse(receivesReporterMark(noellesRole("detective")));
        assertFalse(receivesReporterMark(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesVoodooReceivesDeathBondMeaning() {
        assertTrue(receivesVoodooDeathBond(noellesRole("voodoo")));
        assertFalse(receivesVoodooDeathBond(noellesRole("reporter")));
        assertFalse(receivesVoodooDeathBond(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesPhantomReceivesInvisibilityMeaning() {
        assertTrue(receivesPhantomInvisibility(noellesRole("phantom")));
        assertFalse(receivesPhantomInvisibility(noellesRole("swapper")));
        assertFalse(receivesPhantomInvisibility(role(WatheRoleIds.KILLER, false, true)));
    }

    @Test
    void onlyNoellesSpiritualistReceivesProjectionMeaning() {
        assertTrue(receivesSpiritualistProjection(noellesRole("spiritualist")));
        assertFalse(receivesSpiritualistProjection(noellesRole("phantom")));
        assertFalse(receivesSpiritualistProjection(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void onlyNoellesMermaidReceivesWaterAdaptationMeaning() {
        assertTrue(receivesMermaidWaterAdaptation(noellesRole("mermaid")));
        assertFalse(receivesMermaidWaterAdaptation(noellesRole("detective")));
        assertFalse(receivesMermaidWaterAdaptation(role(WatheRoleIds.CIVILIAN, true, false)));
    }

    @Test
    void bartenderToxicologistAndPoisonerReceivePoisonVisibilityMeaning() {
        assertTrue(receivesPoisonVisibility(noellesRole("bartender")));
        assertTrue(receivesPoisonVisibility(noellesRole("toxicologist")));
        assertTrue(receivesPoisonVisibility(noellesRole("poisoner")));
        assertFalse(receivesPoisonVisibility(noellesRole("detective")));
        assertFalse(receivesPoisonVisibility(role(WatheRoleIds.CIVILIAN, true, false)));
        assertFalse(receivesToxicologistPoisonVisibility(noellesRole("bartender")));
    }

    @Test
    void nullAndUnknownRoleDoNotReceivePrivilegedBehavior() {
        Role unknownRole = role(Identifier.of("strawcraft", "unknown_fixture"), false, false);

        assertEquals(StrawFaction.NONE, factionFor(null));
        assertFalse(canUseKillerShop(null));
        assertFalse(receivesVigilanteLoadout(null));
        assertFalse(receivesUndercoverWalkieTalkie(null));
        assertFalse(receivesDetectiveInvestigation(null));
        assertFalse(receivesVultureBodyFeast(null));
        assertFalse(receivesPathogenInfection(null));
        assertFalse(receivesAssassinGuess(null));
        assertFalse(receivesBodyguardProtection(null));
        assertFalse(receivesRecallerRecall(null));
        assertFalse(receivesSwapperSwap(null));
        assertFalse(receivesReporterMark(null));
        assertFalse(receivesVoodooDeathBond(null));
        assertFalse(receivesPhantomInvisibility(null));
        assertFalse(receivesSpiritualistProjection(null));
        assertFalse(receivesMermaidWaterAdaptation(null));
        assertFalse(receivesPoisonVisibility(null));
        assertFalse(receivesToxicologistPoisonVisibility(null));
        assertTrue(ammoFactionFor(null).isEmpty());
        assertFalse(canUseKillerShop(unknownRole));
        assertFalse(receivesVigilanteLoadout(unknownRole));
        assertFalse(receivesUndercoverWalkieTalkie(unknownRole));
        assertFalse(receivesDetectiveInvestigation(unknownRole));
        assertFalse(receivesVultureBodyFeast(unknownRole));
        assertFalse(receivesPathogenInfection(unknownRole));
        assertFalse(receivesAssassinGuess(unknownRole));
        assertFalse(receivesBodyguardProtection(unknownRole));
        assertFalse(receivesRecallerRecall(unknownRole));
        assertFalse(receivesSwapperSwap(unknownRole));
        assertFalse(receivesReporterMark(unknownRole));
        assertFalse(receivesVoodooDeathBond(unknownRole));
        assertFalse(receivesPhantomInvisibility(unknownRole));
        assertFalse(receivesSpiritualistProjection(unknownRole));
        assertFalse(receivesMermaidWaterAdaptation(unknownRole));
        assertFalse(receivesPoisonVisibility(unknownRole));
        assertFalse(receivesToxicologistPoisonVisibility(unknownRole));
        assertTrue(ammoFactionFor(unknownRole).isEmpty());
    }

    private static boolean canUseKillerShop(Role role) {
        return invokeBoolean("canUseKillerShop", role);
    }

    private static boolean receivesVigilanteLoadout(Role role) {
        return invokeBoolean("receivesVigilanteLoadout", role);
    }

    private static boolean receivesUndercoverWalkieTalkie(Role role) {
        return invokeBoolean("receivesUndercoverWalkieTalkie", role);
    }

    private static boolean receivesDetectiveInvestigation(Role role) {
        return invokeBoolean("receivesDetectiveInvestigation", role);
    }

    private static boolean receivesVultureBodyFeast(Role role) {
        return invokeBoolean("receivesVultureBodyFeast", role);
    }

    private static boolean receivesPathogenInfection(Role role) {
        return invokeBoolean("receivesPathogenInfection", role);
    }

    private static boolean receivesAssassinGuess(Role role) {
        return invokeBoolean("receivesAssassinGuess", role);
    }

    private static boolean receivesBodyguardProtection(Role role) {
        return invokeBoolean("receivesBodyguardProtection", role);
    }

    private static boolean receivesRecallerRecall(Role role) {
        return invokeBoolean("receivesRecallerRecall", role);
    }

    private static boolean receivesSwapperSwap(Role role) {
        return invokeBoolean("receivesSwapperSwap", role);
    }

    private static boolean receivesReporterMark(Role role) {
        return invokeBoolean("receivesReporterMark", role);
    }

    private static boolean receivesVoodooDeathBond(Role role) {
        return invokeBoolean("receivesVoodooDeathBond", role);
    }

    private static boolean receivesPhantomInvisibility(Role role) {
        return invokeBoolean("receivesPhantomInvisibility", role);
    }

    private static boolean receivesSpiritualistProjection(Role role) {
        return invokeBoolean("receivesSpiritualistProjection", role);
    }

    private static boolean receivesMermaidWaterAdaptation(Role role) {
        return invokeBoolean("receivesMermaidWaterAdaptation", role);
    }

    private static boolean receivesPoisonVisibility(Role role) {
        return invokeBoolean("receivesPoisonVisibility", role);
    }

    private static boolean receivesToxicologistPoisonVisibility(Role role) {
        return invokeBoolean("receivesToxicologistPoisonVisibility", role);
    }

    private static Optional<GunAmmoFaction> ammoFactionFor(Role role) {
        Object result = invoke("ammoFactionFor", role);
        assertInstanceOf(Optional.class, result, "ammoFactionFor should return Optional<GunAmmoFaction>");
        @SuppressWarnings("unchecked")
        Optional<GunAmmoFaction> faction = (Optional<GunAmmoFaction>) result;
        return faction;
    }

    private static StrawFaction factionFor(Role role) {
        Object result = invoke("factionFor", role);
        assertInstanceOf(StrawFaction.class, result, "factionFor should return StrawFaction");
        return (StrawFaction) result;
    }

    private static boolean invokeBoolean(String methodName, Role role) {
        Object result = invoke(methodName, role);
        assertInstanceOf(Boolean.class, result, methodName + " should return boolean");
        return (Boolean) result;
    }

    private static Object invoke(String methodName, Role role) {
        try {
            Method method = meaningClass().getDeclaredMethod(methodName, Role.class);
            method.setAccessible(true);
            return method.invoke(null, role);
        } catch (NoSuchMethodException e) {
            return fail("StrawRoleMeaning should expose " + methodName + "(Role)");
        } catch (IllegalAccessException e) {
            return fail("StrawRoleMeaning." + methodName + "(Role) should be callable from StrawCraft tests");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            return fail("StrawRoleMeaning." + methodName + "(Role) should not throw, but threw " + cause);
        }
    }

    private static Class<?> meaningClass() {
        try {
            return Class.forName("org.caecorthus.strawcraft.StrawRoleMeaning");
        } catch (ClassNotFoundException e) {
            return fail("StrawRoleMeaning should centralize StrawCraft behavior meaning for Wathe roles");
        }
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }
}
