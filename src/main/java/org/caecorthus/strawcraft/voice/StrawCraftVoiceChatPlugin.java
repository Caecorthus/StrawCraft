package org.caecorthus.strawcraft.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.caecorthus.strawcraft.InsaneParanoidKillerPolicy;
import org.caecorthus.strawcraft.NoellesRoleStateComponent;
import org.caecorthus.strawcraft.StrawCraft;
import org.caecorthus.strawcraft.StrawRoleMeaning;

public final class StrawCraftVoiceChatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return StrawCraft.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoicechatPlugin.super.initialize(api);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::forwardDeadVoiceToInsaneKillers);
    }

    private void forwardDeadVoiceToInsaneKillers(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null || !(senderConnection.getPlayer().getPlayer() instanceof ServerPlayerEntity sender)) {
            return;
        }

        VoicechatServerApi api = event.getVoicechat();
        GameWorldComponent game = GameWorldComponent.KEY.get(sender.getServerWorld());
        boolean deadOrSpectator = sender.interactionManager.getGameMode().equals(GameMode.SPECTATOR)
                || GameFunctions.isPlayerEliminated(sender);
        InsaneParanoidKillerPolicy.VoiceSender voiceSender = new InsaneParanoidKillerPolicy.VoiceSender(
                deadOrSpectator,
                NoellesRoleStateComponent.KEY.get(sender).isTaotieSwallowed()
        );

        for (ServerPlayerEntity target : sender.getServerWorld().getPlayers()) {
            Role role = game.getRole(target);
            InsaneParanoidKillerPolicy.VoiceTarget voiceTarget = new InsaneParanoidKillerPolicy.VoiceTarget(
                    StrawRoleMeaning.roleIdFor(role).orElse(null),
                    GameFunctions.isPlayerAliveAndSurvival(target),
                    sender.distanceTo(target)
            );
            if (!InsaneParanoidKillerPolicy.shouldForwardDeadVoiceToInsaneKiller(
                    voiceSender,
                    voiceTarget,
                    api.getVoiceChatDistance()
            )) {
                continue;
            }

            VoicechatConnection targetConnection = api.getConnectionOf(target.getUuid());
            if (targetConnection == null) {
                continue;
            }
            api.sendLocationalSoundPacketTo(
                    targetConnection,
                    event.getPacket().locationalSoundPacketBuilder()
                            .position(api.createPosition(sender.getX(), sender.getY(), sender.getZ()))
                            .distance((float) api.getVoiceChatDistance())
                            .build()
            );
        }
    }
}
