package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.caecorthus.strawcraft.api.StrawDeathEvents;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoodooBondRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private VoodooBondRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(VoodooBondPayload.ID, VoodooBondPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VoodooBondPayload.ID, VoodooBondRuntime::handleBond);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(VoodooBondRuntime::handleRoleDeath);
    }

    private static void handleBond(VoodooBondPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity voodoo = context.player();
        ServerWorld world = voodoo.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(voodoo);
        ServerPlayerEntity target = voodoo.getServer().getPlayerManager().getPlayer(payload.target());
        boolean sameWorld = target != null && target.getServerWorld() == world;

        // Clients propose one UUID; role, round, distance, visibility, and cooldown stay server-owned.
        // 客户端只提出一个 UUID；身份、回合、距离、可见性和冷却都由服务端裁决。
        VoodooBondPolicy.ValidationResult result = VoodooBondPolicy.validate(new VoodooBondPolicy.InteractionInput(
                game.isRunning(),
                isVoodooRole(game.getRole(voodoo)),
                GameFunctions.isPlayerAliveAndSurvival(voodoo),
                target != null,
                target == voodoo,
                target != null && game.getRole(target) != null,
                target != null && GameFunctions.isPlayerAliveAndSurvival(target),
                sameWorld,
                target == null ? Double.POSITIVE_INFINITY : voodoo.squaredDistanceTo(target),
                target != null && voodoo.canSee(target),
                !roleState.isAbilityOnCooldown(VoodooBondPolicy.ABILITY_ID, currentGameTime)
        ));
        if (result.blocked()) {
            sendBlockedMessage(voodoo, roleState, currentGameTime, result);
            return;
        }

        roleState.setVoodooBondedTarget(target.getUuid());
        roleState.tryBeginAbilityCooldown(
                VoodooBondPolicy.ABILITY_ID,
                currentGameTime,
                VoodooBondPolicy.BOND_COOLDOWN_TICKS
        );
        voodoo.sendMessage(Text.translatable(
                "message.strawcraft.voodoo.bonded",
                target.getDisplayName()
        ).formatted(Formatting.GREEN), true);
    }

    static void handleRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        if (context.victimRoleId().filter(VoodooBondPolicy.VOODOO_ROLE::equals).isEmpty()) {
            return;
        }
        ServerPlayerEntity voodoo = context.world().getServer().getPlayerManager()
                .getPlayer(context.official().victimUuid());
        if (voodoo == null) {
            return;
        }

        NoellesRoleStateComponent voodooState = NoellesRoleStateComponent.KEY.get(voodoo);
        Optional<UUID> bondedTarget = voodooState.voodooBondedTarget();
        voodooState.clearVoodooBondedTarget();
        bondedTarget.ifPresent(targetUuid -> killBondedTarget(context, voodoo, targetUuid));
    }

    private static void killBondedTarget(
            StrawDeathEvents.RoleDeathContext context,
            ServerPlayerEntity voodoo,
            UUID targetUuid
    ) {
        ServerPlayerEntity target = context.world().getServer().getPlayerManager().getPlayer(targetUuid);
        if (target == null || target == voodoo) {
            return;
        }
        if (!(target.getServerWorld() == context.world())) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(context.world());
        if (!game.isRunning()
                || game.getRole(target) == null
                || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        GameFunctions.killPlayer(target, true, null, StrawDeathReasons.VOODOO);
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity voodoo,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            VoodooBondPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> voodoo.sendMessage(Text.translatable(
                    "message.strawcraft.voodoo.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case WRONG_DIMENSION -> voodoo.sendMessage(
                    Text.translatable("message.strawcraft.voodoo.wrong_dimension").formatted(Formatting.RED),
                    true
            );
            case TARGET_OUT_OF_REACH -> voodoo.sendMessage(
                    Text.translatable("message.strawcraft.voodoo.out_of_reach").formatted(Formatting.RED),
                    true
            );
            case INVALID_TARGET, TARGET_NOT_ACTIVE -> voodoo.sendMessage(
                    Text.translatable("message.strawcraft.voodoo.invalid_target").formatted(Formatting.RED),
                    true
            );
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(VoodooBondPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isVoodooRole(Role role) {
        return StrawRoleMeaning.receivesVoodooDeathBond(role);
    }
}
