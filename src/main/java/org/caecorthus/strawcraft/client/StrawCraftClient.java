package org.caecorthus.strawcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.caecorthus.strawcraft.map.StrawMapVotingComponent;
import org.lwjgl.glfw.GLFW;

public final class StrawCraftClient implements ClientModInitializer {
    private static KeyBinding mapVoteKey;
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
        ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVotingScreen);
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
}
