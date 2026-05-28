package org.caecorthus.strawcraft;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class StrawCraftEntities {
    public static final Identifier THROWING_AXE_ID = StrawCraft.id("throwing_axe");

    public static EntityType<ThrowingAxeEntity> THROWING_AXE;

    private StrawCraftEntities() {
    }

    public static void register() {
        THROWING_AXE = Registry.register(
                Registries.ENTITY_TYPE,
                THROWING_AXE_ID,
                EntityType.Builder.<ThrowingAxeEntity>create(ThrowingAxeEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5F, 0.5F)
                        .maxTrackingRange(4)
                        .trackingTickInterval(20)
                        .build(THROWING_AXE_ID.toString())
        );
    }
}
