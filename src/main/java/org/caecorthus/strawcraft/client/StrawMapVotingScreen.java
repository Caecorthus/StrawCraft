package org.caecorthus.strawcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.caecorthus.strawcraft.map.StrawMapVoteOption;
import org.caecorthus.strawcraft.map.StrawMapVoting;
import org.caecorthus.strawcraft.map.StrawMapVotingComponent;

import java.util.List;

public final class StrawMapVotingScreen extends Screen {
    private static final int BG_TOP = 0xEE1A0505;
    private static final int BG_BOTTOM = 0xEE0F0202;
    private static final int BRASS = 0xFFD4AF37;
    private static final int BRASS_DIM = 0xFF8B735B;
    private static final int TICKET_BG = 0xFFFDF5E6;
    private static final int TICKET_BG_SELECTED = 0xFFFFF8F0;
    private static final int TICKET_BG_BOTTOM = 0xFFE0D5C0;
    private static final int TEXT_INK = 0xFF2F1B1B;
    private static final int TEXT_DIM = 0xFF6B5A5A;
    private static final int VELVET = 0xFFA00000;

    private int scrollRow;
    private int maxScrollRow;

    public StrawMapVotingScreen() {
        super(Text.translatable("gui.strawcraft.map_voting.title"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        StrawMapVotingComponent voting = getVoting();
        return voting == null || !voting.isRoulettePhase();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        StrawMapVotingComponent voting = getVoting();
        if (voting == null || !voting.isVotingActive()) {
            return;
        }

        context.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
        context.fill(0, 45, width, 46, BRASS);
        context.fill(0, 48, width, 49, BRASS);
        context.fill(0, height - 40, width, height - 39, BRASS);
        context.fill(0, height - 43, width, height - 42, BRASS);

        Text title = voting.isRoulettePhase()
                ? Text.translatable("gui.strawcraft.map_voting.selecting")
                : Text.translatable("gui.strawcraft.map_voting.title");
        drawCenteredText(context, title, width / 2, 18, BRASS);

        if (voting.isRoulettePhase()) {
            renderRouletteResult(context, voting);
        } else {
            renderCards(context, voting, mouseX, mouseY);
            drawCenteredText(
                    context,
                    Text.literal(Integer.toString(Math.max(0, voting.getVotingTicksRemaining() / 20))),
                    width / 2,
                    34,
                    voting.getVotingTicksRemaining() < 100 ? 0xFFFF5555 : BRASS
            );
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCards(DrawContext context, StrawMapVotingComponent voting, int mouseX, int mouseY) {
        List<StrawMapVoteOption> maps = voting.getAvailableMaps();
        int cols = columnsFor(width);
        int cardWidth = Math.min(170, (width - 60 - (cols - 1) * 12) / cols);
        int cardHeight = 126;
        int rows = Math.max(1, (height - 110) / (cardHeight + 12));
        maxScrollRow = Math.max(0, MathHelper.ceil((float) maps.size() / cols) - rows);
        scrollRow = MathHelper.clamp(scrollRow, 0, maxScrollRow);

        int gridWidth = cols * cardWidth + (cols - 1) * 12;
        int startX = (width - gridWidth) / 2;
        int startY = 58;
        int first = scrollRow * cols;
        int last = Math.min(maps.size(), first + rows * cols);
        int[] votes = voting.getVoteCounts();
        int totalVotes = 0;
        for (int vote : votes) {
            totalVotes += vote;
        }
        int myVote = client != null && client.player != null ? voting.getVotedMapIndex(client.player.getUuid()) : -1;

        for (int index = first; index < last; index++) {
            int visible = index - first;
            int col = visible % cols;
            int row = visible / cols;
            int x = startX + col * (cardWidth + 12);
            int y = startY + row * (cardHeight + 12);
            boolean hovered = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight;
            drawCard(context, maps.get(index), x, hovered ? y - 3 : y, cardWidth, cardHeight, index == myVote,
                    index < votes.length ? votes[index] : 0, totalVotes);
        }

        if (maxScrollRow > 0) {
            drawCenteredText(context, Text.literal((scrollRow + 1) + " / " + (maxScrollRow + 1)), width / 2, height - 32, BRASS_DIM);
        }
    }

    private void drawCard(DrawContext context, StrawMapVoteOption option, int x, int y, int cardWidth, int cardHeight,
                          boolean selected, int votes, int totalVotes) {
        context.fill(x + 4, y + 4, x + cardWidth + 4, y + cardHeight + 4, 0x66000000);
        context.fillGradient(x, y, x + cardWidth, y + cardHeight, selected ? TICKET_BG_SELECTED : TICKET_BG, TICKET_BG_BOTTOM);
        context.drawBorder(x + 2, y + 2, cardWidth - 4, cardHeight - 4, selected ? BRASS : TEXT_INK);
        context.drawBorder(x + 5, y + 5, cardWidth - 10, cardHeight - 10, BRASS_DIM);

        drawCenteredText(context, Text.literal(option.displayName()), x + cardWidth / 2, y + 15, TEXT_INK);
        drawCenteredText(context, Text.translatable("gui.strawcraft.map_voting.player_range", option.minPlayers(), option.maxPlayers()),
                x + cardWidth / 2, y + 30, TEXT_DIM);

        List<OrderedText> desc = textRenderer.wrapLines(Text.literal(option.description()), cardWidth - 20);
        int maxLines = Math.min(4, desc.size());
        for (int i = 0; i < maxLines; i++) {
            OrderedText line = desc.get(i);
            context.drawText(textRenderer, line, x + (cardWidth - textRenderer.getWidth(line)) / 2, y + 46 + i * 10, TEXT_DIM, false);
        }

        int barX = x + 12;
        int barY = y + cardHeight - 24;
        int barWidth = cardWidth - 24;
        context.fill(barX, barY, barX + barWidth, barY + 4, 0xFF4A3520);
        if (totalVotes > 0 && votes > 0) {
            context.fill(barX, barY, barX + (int) ((float) votes / totalVotes * barWidth), barY + 4, VELVET);
        }
        drawCenteredText(context, Text.translatable("gui.strawcraft.map_voting.votes", votes), x + cardWidth / 2, barY + 9, TEXT_DIM);
        if (selected) {
            drawCenteredText(context, Text.translatable("gui.strawcraft.map_voting.my_vote"), x + cardWidth / 2, y + cardHeight - 42, VELVET);
        }
    }

    private void renderRouletteResult(DrawContext context, StrawMapVotingComponent voting) {
        int selected = voting.getSelectedMapIndex();
        if (selected < 0 || selected >= voting.getAvailableMaps().size()) {
            return;
        }
        StrawMapVoteOption option = voting.getAvailableMaps().get(selected);
        int panelWidth = Math.min(360, width - 50);
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 42;
        context.fillGradient(x, y, x + panelWidth, y + 84, TICKET_BG, TICKET_BG_BOTTOM);
        context.drawBorder(x + 2, y + 2, panelWidth - 4, 80, BRASS);
        drawCenteredText(context, Text.literal(option.displayName()), width / 2, y + 22, TEXT_INK);
        drawCenteredText(context, Text.literal(option.description()), width / 2, y + 42, TEXT_DIM);
        drawCenteredText(context, Text.literal(Math.max(0, voting.getRouletteTicksRemaining() / 20) + "s"), width / 2, y + 62, VELVET);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        StrawMapVotingComponent voting = getVoting();
        if (voting == null || !voting.isVotingActive() || voting.isRoulettePhase()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int cols = columnsFor(width);
        int cardWidth = Math.min(170, (width - 60 - (cols - 1) * 12) / cols);
        int cardHeight = 126;
        int gridWidth = cols * cardWidth + (cols - 1) * 12;
        int startX = (width - gridWidth) / 2;
        int startY = 58;
        int first = scrollRow * cols;
        int last = Math.min(voting.getAvailableMaps().size(), first + Math.max(1, (height - 110) / (cardHeight + 12)) * cols);

        for (int index = first; index < last; index++) {
            int visible = index - first;
            int x = startX + (visible % cols) * (cardWidth + 12);
            int y = startY + (visible / cols) * (cardHeight + 12);
            if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
                ClientPlayNetworking.send(new StrawMapVoting.VotePayload(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0 && scrollRow < maxScrollRow) {
            scrollRow++;
            return true;
        }
        if (verticalAmount > 0 && scrollRow > 0) {
            scrollRow--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private StrawMapVotingComponent getVoting() {
        return client != null && client.world != null ? StrawMapVotingComponent.KEY.get(client.world.getScoreboard()) : null;
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private static int columnsFor(int width) {
        if (width < 420) {
            return 2;
        }
        if (width < 700) {
            return 3;
        }
        return 4;
    }
}
