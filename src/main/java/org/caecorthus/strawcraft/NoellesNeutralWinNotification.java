package org.caecorthus.strawcraft;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class NoellesNeutralWinNotification {
    private NoellesNeutralWinNotification() {
    }

    public static List<ClaimNotice> collectClaims(Map<UUID, Set<NoellesRoleState.NeutralWinClaim>> claimsByPlayer) {
        Objects.requireNonNull(claimsByPlayer, "claimsByPlayer");
        return claimsByPlayer.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(claim -> notice(entry.getKey(), claim)))
                .sorted(Comparator
                        .comparingLong(ClaimNotice::gameTime)
                        .thenComparing(notice -> notice.playerUuid().toString())
                        .thenComparing(notice -> notice.roleId().toString()))
                .toList();
    }

    public static Messages messagesFor(ClaimNotice notice, Text claimantName) {
        Objects.requireNonNull(notice, "notice");
        Objects.requireNonNull(claimantName, "claimantName");
        Text roleName = Text.translatable("announcement.role." + notice.roleId().getPath());
        return new Messages(
                // This is a StrawCraft claim notice, not an official Wathe winner-screen replacement.
                // 这是 StrawCraft 的主张提示，不是替换官方 Wathe 赢家结算屏。
                Text.translatable("message.strawcraft.neutral_claim.broadcast", claimantName, roleName)
                        .formatted(Formatting.GOLD),
                Text.translatable("message.strawcraft.neutral_claim.actionbar", roleName)
                        .formatted(Formatting.GREEN)
        );
    }

    private static ClaimNotice notice(UUID playerUuid, NoellesRoleState.NeutralWinClaim claim) {
        return new ClaimNotice(playerUuid, claim.roleId(), claim.trigger(), claim.gameTime());
    }

    public record ClaimNotice(
            UUID playerUuid,
            Identifier roleId,
            Identifier trigger,
            long gameTime
    ) {
        public ClaimNotice {
            Objects.requireNonNull(playerUuid, "playerUuid");
            Objects.requireNonNull(roleId, "roleId");
            Objects.requireNonNull(trigger, "trigger");
        }
    }

    public record Messages(Text broadcast, Text actionbar) {
        public Messages {
            Objects.requireNonNull(broadcast, "broadcast");
            Objects.requireNonNull(actionbar, "actionbar");
        }
    }
}
