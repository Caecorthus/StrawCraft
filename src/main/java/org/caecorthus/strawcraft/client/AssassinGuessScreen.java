package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.AssassinGuessPayload;
import org.caecorthus.strawcraft.AssassinGuessPolicy;
import org.caecorthus.strawcraft.StrawRoleMeaning;

import java.util.List;
import java.util.UUID;

public final class AssassinGuessScreen extends Screen {
    private static final int PANEL_COLOR = 0xE0201014;
    private static final int PANEL_BORDER = 0xFFB44343;
    private static final int PANEL_INNER_BORDER = 0xFF3B2020;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private UUID selectedTarget;

    public AssassinGuessScreen() {
        super(Text.translatable("gui.strawcraft.assassin.title"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        clearChildren();
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return;
        }
        if (selectedTarget == null) {
            initTargetButtons();
        } else {
            initRoleButtons();
        }
    }

    private void initTargetButtons() {
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return;
        }
        List<AbstractClientPlayerEntity> targets = this.client.world.getPlayers().stream()
                .filter(player -> player != this.client.player)
                .toList();
        int columns = columnsFor(targets.size());
        int startX = gridStartX(columns);
        int startY = 54;
        for (int index = 0; index < targets.size(); index++) {
            AbstractClientPlayerEntity target = targets.get(index);
            int x = startX + (index % columns) * (BUTTON_WIDTH + BUTTON_GAP);
            int y = startY + (index / columns) * (BUTTON_HEIGHT + BUTTON_GAP);
            addDrawableChild(ButtonWidget.builder(target.getDisplayName(), button -> {
                selectedTarget = target.getUuid();
                clearAndInit();
            }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    private void initRoleButtons() {
        if (this.client == null || this.client.world == null) {
            return;
        }
        List<Role> roles = WatheRoles.ROLES.stream()
                .filter(role -> AssassinGuessPolicy.isGuessableRole(role, true))
                .toList();
        int columns = columnsFor(roles.size());
        int startX = gridStartX(columns);
        int startY = 54;
        for (int index = 0; index < roles.size(); index++) {
            Role role = roles.get(index);
            int x = startX + (index % columns) * (BUTTON_WIDTH + BUTTON_GAP);
            int y = startY + (index / columns) * (BUTTON_HEIGHT + BUTTON_GAP);
            addDrawableChild(ButtonWidget.builder(roleName(role), button -> {
                Identifier roleId = StrawRoleMeaning.roleIdFor(role).orElseThrow();
                ClientPlayNetworking.send(new AssassinGuessPayload(selectedTarget, roleId));
                close();
            }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.strawcraft.assassin.back"), button -> {
            selectedTarget = null;
            clearAndInit();
        }).dimensions(this.width / 2 - 40, this.height - 32, 80, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(20, 20, this.width - 20, this.height - 20, PANEL_COLOR);
        context.drawBorder(20, 20, this.width - 40, this.height - 40, PANEL_BORDER);
        context.drawBorder(23, 23, this.width - 46, this.height - 46, PANEL_INNER_BORDER);
        Text heading = selectedTarget == null
                ? Text.translatable("gui.strawcraft.assassin.pick_target")
                : Text.translatable("gui.strawcraft.assassin.pick_role");
        context.drawCenteredTextWithShadow(this.textRenderer, heading, this.width / 2, 34, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private int gridStartX(int columns) {
        int gridWidth = columns * BUTTON_WIDTH + Math.max(0, columns - 1) * BUTTON_GAP;
        return (this.width - gridWidth) / 2;
    }

    private static int columnsFor(int count) {
        if (count <= 1) {
            return 1;
        }
        return Math.min(3, count);
    }

    private static Text roleName(Role role) {
        return StrawRoleMeaning.roleIdFor(role)
                .map(roleId -> Text.literal(roleId.getPath().replace('_', ' ')))
                .orElse(Text.empty());
    }
}
