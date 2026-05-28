package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class NoellesRoleWelcome {
    private NoellesRoleWelcome() {
    }

    static Messages messagesFor(Role role) {
        Identifier roleId = StrawRoleMeaning.roleIdFor(role).orElseThrow();
        Text roleName = roleName(roleId);
        Text goal = roleGoal(roleId);
        return new Messages(
                Text.translatable("message.strawcraft.role_rewrite.role", roleName),
                Text.translatable("message.strawcraft.role_rewrite.goal", goal),
                Text.translatable("message.strawcraft.role_rewrite.actionbar", roleName)
        );
    }

    private static Text roleName(Identifier roleId) {
        return Text.translatable("announcement.role." + roleId.getPath());
    }

    private static Text roleGoal(Identifier roleId) {
        return Text.translatable("announcement.goal." + roleId.getPath());
    }

    record Messages(Text role, Text goal, Text actionbar) {
    }
}
