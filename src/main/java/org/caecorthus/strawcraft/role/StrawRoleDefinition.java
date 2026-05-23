package org.caecorthus.strawcraft.role;

import net.minecraft.util.Identifier;

import java.util.function.Predicate;

public record StrawRoleDefinition(
        Identifier id,
        StrawFaction faction,
        boolean fallback,
        boolean uniqueSpecial,
        Predicate<StrawRoleSelectionContext> appearance
) {
    public StrawRoleDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Role definition id cannot be null");
        }
        if (faction == null) {
            throw new IllegalArgumentException("Role definition faction cannot be null");
        }
        if (appearance == null) {
            appearance = context -> true;
        }
    }

    boolean canAppear(StrawRoleSelectionContext context) {
        return !context.disabledRoles().contains(id) && appearance.test(context);
    }
}
