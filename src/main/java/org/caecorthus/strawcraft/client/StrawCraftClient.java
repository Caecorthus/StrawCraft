package org.caecorthus.strawcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.EntityHitResult;
import org.caecorthus.strawcraft.CoronerInspectPayload;
import org.caecorthus.strawcraft.DetectiveInvestigationPayload;
import org.caecorthus.strawcraft.RecallerRecallPayload;
import org.caecorthus.strawcraft.VultureFeastPayload;
import org.caecorthus.strawcraft.map.StrawMapVotingComponent;
import org.lwjgl.glfw.GLFW;

public final class StrawCraftClient implements ClientModInitializer {
    private static KeyBinding mapVoteKey;
    private static KeyBinding detectiveInvestigateKey;
    private static KeyBinding coronerInspectKey;
    private static KeyBinding vultureFeastKey;
    private static KeyBinding recallerRecallKey;
    private static boolean wasVotingActive;
    private static boolean autoOpenedVotingScreen;

    @Override
    public void onInitializeClient() {
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
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVotingScreen);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickDetectiveInvestigation);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickCoronerInspection);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVultureFeast);
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickRecallerRecall);
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
            return;
        }

        if (recallerRecallKey.wasPressed()) {
            // Recaller sends no coordinates; the server stores and consumes the authoritative recall point.
            // Recaller 不发送坐标；服务端负责保存并消耗权威传送点。
            ClientPlayNetworking.send(new RecallerRecallPayload());
        }
    }
}
