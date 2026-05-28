package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import org.caecorthus.strawcraft.AssassinGuessPolicy;
import org.caecorthus.strawcraft.CoronerInspectPayload;
import org.caecorthus.strawcraft.DetectiveInvestigationPayload;
import org.caecorthus.strawcraft.NoellesRoleStateComponent;
import org.caecorthus.strawcraft.PathogenInfectionPayload;
import org.caecorthus.strawcraft.PhantomInvisibilityPayload;
import org.caecorthus.strawcraft.RecallerRecallPayload;
import org.caecorthus.strawcraft.ReporterMarkPayload;
import org.caecorthus.strawcraft.SpiritualistProjectionPayload;
import org.caecorthus.strawcraft.StrawCraftEntities;
import org.caecorthus.strawcraft.StrawRoleMeaning;
import org.caecorthus.strawcraft.SwapperSwapPayload;
import org.caecorthus.strawcraft.VoodooBondPayload;
import org.caecorthus.strawcraft.VultureFeastPayload;
import org.caecorthus.strawcraft.map.StrawMapVotingComponent;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class StrawCraftClient implements ClientModInitializer {
    private static KeyBinding mapVoteKey;
    private static KeyBinding detectiveInvestigateKey;
    private static KeyBinding coronerInspectKey;
    private static KeyBinding vultureFeastKey;
    private static KeyBinding recallerRecallKey;
    private static KeyBinding swapperSwapKey;
    private static KeyBinding reporterMarkKey;
    private static KeyBinding voodooBondKey;
    private static KeyBinding phantomInvisibilityKey;
    private static KeyBinding spiritualistProjectionKey;
    private static KeyBinding pathogenInfectionKey;
    private static KeyBinding assassinGuessKey;
    private static UUID pendingSwapperTarget;
    private static boolean wasVotingActive;
    private static boolean autoOpenedVotingScreen;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(StrawCraftEntities.THROWING_AXE, FlyingItemEntityRenderer::new);

        mapVoteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.map_vote",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.strawcraft.keybinds"
        ));
        detectiveInvestigateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.detective_investigate",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.strawcraft.keybinds"
        ));
        coronerInspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.coroner_inspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.strawcraft.keybinds"
        ));
        vultureFeastKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.vulture_feast",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.strawcraft.keybinds"
        ));
        recallerRecallKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.recaller_recall",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.strawcraft.keybinds"
        ));
        swapperSwapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.swapper_swap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.strawcraft.keybinds"
        ));
        reporterMarkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.reporter_mark",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.strawcraft.keybinds"
        ));
        voodooBondKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.voodoo_bond",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.strawcraft.keybinds"
        ));
        phantomInvisibilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.phantom_invisibility",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.strawcraft.keybinds"
        ));
        spiritualistProjectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.spiritualist_project",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.strawcraft.keybinds"
        ));
        pathogenInfectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.pathogen_infect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.strawcraft.keybinds"
        ));
        assassinGuessKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strawcraft.assassin_guess",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.strawcraft.keybinds"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVotingScreen);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickDetectiveInvestigation);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickCoronerInspection);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVultureFeast);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickRecallerRecall);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickSwapperSwap);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickReporterMark);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVoodooBond);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickPhantomInvisibility);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickSpiritualistProjection);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickPathogenInfection);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickAssassinGuess);
    }

    private static void tickVotingScreen(MinecraftClient client) {
        if (client.world == null) {
            wasVotingActive = false;
            autoOpenedVotingScreen = false;
            return;
        }

        StrawMapVotingComponent voting = StrawMapVotingComponent.KEY.get(client.world.getScoreboard());
        boolean votingActive = voting.isVotingActive();
        if (votingActive && !wasVotingActive) {
            autoOpenedVotingScreen = false;
        }

        if (votingActive && !autoOpenedVotingScreen && !(client.currentScreen instanceof StrawMapVotingScreen)) {
            client.setScreen(new StrawMapVotingScreen());
            autoOpenedVotingScreen = true;
        }

        if (votingActive && mapVoteKey.wasPressed()) {
            if (client.currentScreen instanceof StrawMapVotingScreen && !voting.isRoulettePhase()) {
                client.setScreen(null);
            } else {
                client.setScreen(new StrawMapVotingScreen());
                autoOpenedVotingScreen = true;
            }
        }

        if (voting.isRoulettePhase() && !(client.currentScreen instanceof StrawMapVotingScreen)) {
            client.setScreen(new StrawMapVotingScreen());
        }

        if (!votingActive && client.currentScreen instanceof StrawMapVotingScreen) {
            client.setScreen(null);
        }
        wasVotingActive = votingActive;
    }

    private static void tickDetectiveInvestigation(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (!detectiveInvestigateKey.wasPressed()) {
            return;
        }

        // The client only forwards the aimed player; the server owns role, range, visibility, and cooldown validation.
        // 客户端只转发准星指向的玩家；身份、距离、可见性和冷却都由服务端最终判定。
        if (client.crosshairTarget instanceof EntityHitResult hitResult
                && hitResult.getEntity() instanceof PlayerEntity target
                && target != client.player) {
            ClientPlayNetworking.send(new DetectiveInvestigationPayload(target.getUuid()));
        }
    }

    private static void tickVultureFeast(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (vultureFeastKey.wasPressed()) {
            // The server searches official Wathe bodies and owns role, range, cooldown, and duplicate validation.
            // 服务端搜索官方 Wathe 尸体，并最终校验职业、距离、冷却和重复食用。
            ClientPlayNetworking.send(new VultureFeastPayload());
        }
    }

    private static void tickCoronerInspection(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (coronerInspectKey.wasPressed()) {
            // The server searches official Wathe bodies and owns all Coroner inspection validation.
            // 服务端搜索官方 Wathe 尸体，并拥有验尸能力的全部校验逻辑。
            ClientPlayNetworking.send(new CoronerInspectPayload());
        }
    }

    private static void tickRecallerRecall(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            pendingSwapperTarget = null;
            return;
        }

        if (recallerRecallKey.wasPressed()) {
            // Recaller sends no coordinates; the server stores and consumes the authoritative recall point.
            // Recaller 不发送坐标；服务端负责保存并消耗权威传送点。
            ClientPlayNetworking.send(new RecallerRecallPayload());
        }
    }

    private static void tickSwapperSwap(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            pendingSwapperTarget = null;
            return;
        }

        if (!swapperSwapKey.wasPressed()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)
                || !(hitResult.getEntity() instanceof PlayerEntity target)
                || target == client.player) {
            pendingSwapperTarget = null;
            client.player.sendMessage(Text.translatable("message.strawcraft.swapper.select_target")
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        UUID targetUuid = target.getUuid();
        if (pendingSwapperTarget == null) {
            pendingSwapperTarget = targetUuid;
            client.player.sendMessage(Text.translatable(
                    "message.strawcraft.swapper.selected_first",
                    target.getDisplayName()
            ).formatted(Formatting.AQUA), true);
            return;
        }
        if (pendingSwapperTarget.equals(targetUuid)) {
            pendingSwapperTarget = null;
            client.player.sendMessage(Text.translatable("message.strawcraft.swapper.selection_cleared")
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        // The client sends only the two selected UUIDs; the server owns every gameplay check.
        // 客户端只发送两个选中 UUID；所有玩法校验都由服务端负责。
        ClientPlayNetworking.send(new SwapperSwapPayload(pendingSwapperTarget, targetUuid));
        pendingSwapperTarget = null;
        client.player.sendMessage(Text.translatable("message.strawcraft.swapper.requested")
                .formatted(Formatting.GREEN), true);
    }

    private static void tickReporterMark(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (!reporterMarkKey.wasPressed()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)
                || !(hitResult.getEntity() instanceof PlayerEntity target)
                || target == client.player) {
            client.player.sendMessage(Text.translatable("message.strawcraft.reporter.select_target")
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        // Reporter sends only the aimed player's UUID; tracking state and every gameplay check stay server-owned.
        // 记者只发送准星指向玩家的 UUID；追踪状态和所有玩法判定都留在服务端。
        ClientPlayNetworking.send(new ReporterMarkPayload(target.getUuid()));
    }

    private static void tickVoodooBond(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (!voodooBondKey.wasPressed()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)
                || !(hitResult.getEntity() instanceof PlayerEntity target)
                || target == client.player) {
            client.player.sendMessage(Text.translatable("message.strawcraft.voodoo.select_target")
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        // Voodoo sends only the aimed player's UUID; the server owns the bond and all gameplay checks.
        // 巫毒只发送准星指向玩家的 UUID；绑定状态和所有玩法判定都留在服务端。
        ClientPlayNetworking.send(new VoodooBondPayload(target.getUuid()));
    }

    private static void tickPhantomInvisibility(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (phantomInvisibilityKey.wasPressed()) {
            // Phantom sends only empty intent; the server owns role, round, effect, and cooldown validation.
            // 幽灵只发送空意图；身份、回合、效果和冷却判定全部留在服务端。
            ClientPlayNetworking.send(new PhantomInvisibilityPayload());
        }
    }

    private static void tickSpiritualistProjection(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (spiritualistProjectionKey.wasPressed()) {
            // Spiritualist sends only empty intent; projection state and all forced-return checks stay server-owned.
            // 通灵者只发送空意图；投射状态和所有强制返回判定都由服务端掌控。
            ClientPlayNetworking.send(new SpiritualistProjectionPayload());
        }
    }

    private static void tickPathogenInfection(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (pathogenInfectionKey.wasPressed()) {
            // Pathogen sends only empty intent; the server owns target selection, role, range, visibility, and cooldown.
            // 病原体只发送空意图；目标选择、身份、距离、可见性和冷却都由服务端裁决。
            ClientPlayNetworking.send(new PathogenInfectionPayload());
        }
    }

    private static void tickAssassinGuess(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (!assassinGuessKey.wasPressed()) {
            return;
        }

        if (!StrawRoleMeaning.receivesAssassinGuess(GameWorldComponent.KEY.get(client.world).getRole(client.player))) {
            return;
        }

        if (!AssassinGuessPolicy.canGuess(NoellesRoleStateComponent.KEY.get(client.player), client.world.getTime())) {
            client.player.sendMessage(Text.translatable("message.strawcraft.assassin.not_ready")
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        client.setScreen(new AssassinGuessScreen());
    }
}
