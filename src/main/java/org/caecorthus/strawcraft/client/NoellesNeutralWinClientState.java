package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.NoellesNeutralWinPolicy;
import org.caecorthus.strawcraft.NoellesNeutralWinResultPayload;

import java.util.Optional;
import java.util.UUID;

public final class NoellesNeutralWinClientState {
    private static UUID winnerUuid;
    private static Identifier winnerRoleId;

    private NoellesNeutralWinClientState() {
    }

    public static void accept(NoellesNeutralWinResultPayload payload) {
        // EN: Non-neutral payloads are clear signals sent before ordinary Wathe win screens.
        // CN: 非中立 payload 是普通 Wathe 胜利界面前发送的清理信号。
        if (!NoellesNeutralWinPolicy.canOverrideLooseEndWinner(payload.roleId())) {
            clear();
            return;
        }
        winnerUuid = payload.winnerUuid();
        winnerRoleId = payload.roleId();
    }

    public static void clear() {
        winnerUuid = null;
        winnerRoleId = null;
    }

    public static Optional<Text> endTextFor(GameFunctions.WinStatus winStatus, Text fallbackWinnerName) {
        if (winStatus != GameFunctions.WinStatus.LOOSE_END || winnerRoleId == null) {
            return Optional.empty();
        }
        return Optional.of(Text.translatable(
                "announcement.win.strawcraft." + winnerRoleId.getPath(),
                fallbackWinnerName
        ));
    }

    public static Optional<UUID> winnerUuid() {
        return Optional.ofNullable(winnerUuid);
    }
}
