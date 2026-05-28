package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;

import java.util.Optional;

public final class ScavengerHiddenBodyVisibility {
    public static final Identifier SCAVENGER_ROLE = StrawCraft.id("scavenger");

    private ScavengerHiddenBodyVisibility() {
    }

    public static boolean canSeeBody(
            boolean hiddenByScavenger,
            boolean spectator,
            Optional<Identifier> viewerRoleId,
            StrawFaction viewerFaction
    ) {
        // Conservative slice: hide only from ordinary passenger-side viewers.
        // 保守切片：只对普通乘客阵营隐藏，避免误挡杀手、中立和旁观者信息。
        if (!hiddenByScavenger || spectator) {
            return true;
        }

        if (viewerRoleId.filter(SCAVENGER_ROLE::equals).isPresent()) {
            return true;
        }

        return viewerFaction == StrawFaction.KILLER || viewerFaction == StrawFaction.NEUTRAL;
    }
}
