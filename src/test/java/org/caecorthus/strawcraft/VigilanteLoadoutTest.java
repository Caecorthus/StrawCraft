package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigilanteLoadoutTest {
    @Test
    void vigilanteLoadoutUsesTaczRhino357() {
        assertTrue(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.VIGILANTE));
        assertEquals(Identifier.of("tacz", "rhino357"), VigilanteLoadout.VIGILANTE_GUN_ID);

        NbtCompound customData = VigilanteLoadout.createRhino357CustomData().copyNbt();
        assertEquals("tacz:rhino357", customData.getString("GunId"));
    }

    @Test
    void killerAndVeteranLoadoutsAreNotChanged() {
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.KILLER));
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.VETERAN));
    }
}
