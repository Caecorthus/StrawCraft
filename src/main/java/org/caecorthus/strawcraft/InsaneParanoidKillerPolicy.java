package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

public final class InsaneParanoidKillerPolicy {
    public static final Identifier ROLE_ID = StrawCraft.id("the_insane_damned_paranoid_killer");

    private InsaneParanoidKillerPolicy() {
    }

    /**
     * Spark forwards dead/spectator voice only to nearby alive insane-killer players.
     * Spark 只把死亡/旁观者语音转发给附近仍存活的疯狂杀手玩家。
     */
    public static boolean shouldForwardDeadVoiceToInsaneKiller(
            VoiceSender sender,
            VoiceTarget target,
            double voiceDistance
    ) {
        return sender.deadOrSpectator()
                && !sender.swallowed()
                && ROLE_ID.equals(target.roleId())
                && target.aliveAndPlaying()
                && target.distanceToSender() <= voiceDistance;
    }

    /**
     * Spark's insanity hallucination is client-side and requires the configured morph shuffle.
     * Spark 的疯狂幻觉属于客户端表现，并且依赖已配置好的变形映射。
     */
    public static boolean shouldRenderHallucinatedPlayer(
            boolean configEnabled,
            int localMoodRank,
            int depressedMoodRank,
            boolean shuffledMappingExists
    ) {
        return configEnabled
                && localMoodRank < depressedMoodRank
                && shuffledMappingExists;
    }

    public record VoiceSender(boolean deadOrSpectator, boolean swallowed) {
    }

    public record VoiceTarget(Identifier roleId, boolean aliveAndPlaying, double distanceToSender) {
    }
}
