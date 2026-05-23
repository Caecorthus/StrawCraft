package org.caecorthus.strawcraft.map;

import net.minecraft.util.Identifier;

record MapVoteFinishPlan(
        Identifier dimensionId,
        Identifier gameModeId,
        Identifier mapEffectId
) {
    static MapVoteFinishPlan from(StrawMapVoteOption selected) {
        return new MapVoteFinishPlan(selected.dimensionId(), selected.gameModeId(), selected.mapEffectId());
    }
}
