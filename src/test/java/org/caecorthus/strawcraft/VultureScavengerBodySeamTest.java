package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.role.StrawFaction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VultureScavengerBodySeamTest {
    @Test
    void neutralVultureCanConsumeScavengerHiddenBodyAndClearOnlyThatHiddenState() {
        UUID consumedVictim = new UUID(0L, 1L);
        UUID otherVictim = new UUID(0L, 2L);
        net.minecraft.util.Identifier world = net.minecraft.util.Identifier.of("minecraft", "overworld");
        ScavengerHiddenBodyState hiddenBodies = new ScavengerHiddenBodyState();
        NoellesRoleState vultureState = new NoellesRoleState();

        VultureBodyFeastPolicy.resetRoundState(vultureState, 4);
        hiddenBodies.recordHiddenBody(world, consumedVictim);
        hiddenBodies.recordHiddenBody(world, otherVictim);

        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(VultureBodyFeastPolicy.VULTURE_ROLE),
                StrawFaction.NEUTRAL
        ));

        VultureBodyFeastPolicy.FeastResult result =
                VultureBodyFeastPolicy.recordSuccessfulFeast(vultureState, consumedVictim, 120L);
        VultureBodyFeastRuntime.FeastRuntimeEffects effects =
                VultureBodyFeastRuntime.planRuntimeEffects(VultureBodyFeastRuntime.ValidationResult.ALLOWED, result);
        if (effects.consumeBody()) {
            hiddenBodies.clearHiddenBody(world, consumedVictim);
        }

        assertEquals(1, VultureBodyFeastPolicy.bodiesEaten(vultureState));
        assertFalse(hiddenBodies.isHiddenBody(world, consumedVictim));
        assertTrue(hiddenBodies.isHiddenBody(world, otherVictim));
    }

    @Test
    void scavengerHiddenBodyResetDoesNotEraseVultureNeutralWinClaim() {
        UUID consumedVictim = new UUID(0L, 1L);
        net.minecraft.util.Identifier world = net.minecraft.util.Identifier.of("minecraft", "overworld");
        ScavengerHiddenBodyState hiddenBodies = new ScavengerHiddenBodyState();
        NoellesRoleState vultureState = new NoellesRoleState();

        VultureBodyFeastPolicy.resetRoundState(vultureState, 2);
        hiddenBodies.recordHiddenBody(world, consumedVictim);
        VultureBodyFeastPolicy.recordSuccessfulFeast(vultureState, consumedVictim, 120L);

        hiddenBodies.clearWorld(world);

        assertTrue(vultureState.neutralWinClaim(VultureBodyFeastPolicy.VULTURE_ROLE).isPresent());
    }

    @Test
    void rendererCancellationStaysClientOnlyWhileServerFeastSearchUsesOfficialBodies() throws IOException {
        String runtime = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/VultureBodyFeastRuntime.java"),
                StandardCharsets.UTF_8
        );
        String rendererMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/client/PlayerBodyEntityRendererMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(runtime.contains("TypeFilter.equals(PlayerBodyEntity.class)"));
        assertTrue(runtime.contains("ScavengerHiddenBodies.clearConsumedBody(world, body)"));
        assertTrue(runtime.indexOf("ScavengerHiddenBodies.clearConsumedBody(world, body)")
                < runtime.indexOf("body.discard()"));
        assertFalse(runtime.contains("ScavengerHiddenBodyClientVisibility"));
        assertFalse(runtime.contains("shouldRender(body"));
        assertTrue(rendererMixin.contains("ScavengerHiddenBodyClientVisibility.shouldRender(body"));
        assertTrue(rendererMixin.contains("callbackInfo.cancel()"));
    }
}
