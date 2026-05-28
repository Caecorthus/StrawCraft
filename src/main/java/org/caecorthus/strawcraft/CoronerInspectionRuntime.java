package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CoronerInspectionRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private CoronerInspectionRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(CoronerInspectPayload.ID, CoronerInspectPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CoronerInspectPayload.ID, CoronerInspectionRuntime::handleInspect);
    }

    private static void handleInspect(CoronerInspectPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity coroner = context.player();
        ServerWorld world = coroner.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role role = game.getRole(coroner);
        Optional<PlayerBodyEntity> nearestBody = nearestBody(coroner);
        Optional<StrawCorpseMetadata.CorpseMetadata> metadata = nearestBody
                .map(PlayerBodyEntity::getPlayerUuid)
                .flatMap(StrawCorpseMetadata::byDeadPlayer);
        boolean hiddenBody = nearestBody
                .map(body -> hiddenForViewer(world, body, coroner, role))
                .orElse(false);

        // The client sends intent only; the server owns body search, role checks, and corpse metadata lookup.
        // 客户端只发送意图；服务端负责尸体搜索、职业校验和尸体元数据查询。
        CoronerInspectionPolicy.ValidationResult validation = CoronerInspectionPolicy.validateInteraction(
                new CoronerInspectionPolicy.InteractionInput(
                        game.isRunning(),
                        isCoronerRole(role),
                        GameFunctions.isPlayerAliveAndSurvival(coroner),
                        nearestBody.isPresent(),
                        nearestBody.map(body -> body.squaredDistanceTo(coroner)).orElse(Double.POSITIVE_INFINITY),
                        metadata.isPresent(),
                        hiddenBody
                )
        );
        if (validation != CoronerInspectionPolicy.ValidationResult.ALLOWED) {
            return;
        }

        CoronerInspectionPolicy.InspectionResult result =
                CoronerInspectionPolicy.inspect(metadata.orElseThrow(), currentGameTime);
        coroner.sendMessage(resultMessage(result), true);
    }

    private static Optional<PlayerBodyEntity> nearestBody(ServerPlayerEntity coroner) {
        return coroner.getServerWorld()
                .getEntitiesByType(
                        TypeFilter.equals(PlayerBodyEntity.class),
                        coroner.getBoundingBox().expand(CoronerInspectionPolicy.INSPECT_RANGE),
                        body -> body.squaredDistanceTo(coroner) <= CoronerInspectionPolicy.INSPECT_RANGE_SQUARED
                )
                .stream()
                .min(Comparator.comparingDouble(body -> body.squaredDistanceTo(coroner)));
    }

    private static boolean hiddenForViewer(
            ServerWorld world,
            PlayerBodyEntity body,
            ServerPlayerEntity viewer,
            @Nullable Role viewerRole
    ) {
        boolean hiddenByScavenger = ScavengerHiddenBodies.isHiddenBody(world, body)
                || ((ScavengerHiddenBodyEntity) body).strawcraft$isHiddenByScavenger();
        return !ScavengerHiddenBodyVisibility.canSeeBody(
                hiddenByScavenger,
                viewer.isSpectator(),
                StrawRoleMeaning.roleIdFor(viewerRole),
                StrawRoleMeaning.factionFor(viewerRole)
        );
    }

    private static boolean isCoronerRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesCoronerInspection(role);
    }

    private static Text resultMessage(CoronerInspectionPolicy.InspectionResult result) {
        return Text.translatable(
                "message.strawcraft.coroner.inspect",
                result.deathReason().toString(),
                result.elapsedTicks(),
                result.elapsedSeconds()
        ).formatted(Formatting.AQUA);
    }
}
