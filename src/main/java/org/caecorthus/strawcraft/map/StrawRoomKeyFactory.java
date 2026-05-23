package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class StrawRoomKeyFactory {
    private static final int WATHE_KEY_LORE_COLOR = 0xFF8800;

    private StrawRoomKeyFactory() {
    }

    public static ItemStack create(String keyName) {
        ItemStack stack = new ItemStack(WatheItems.KEY);
        // Official Wathe room keys match doors by the first lore line, so keep that contract exactly.
        // 官方 Wathe 房间钥匙通过第一行 lore 匹配门的 keyName；这里严格沿用这个约定。
        stack.set(DataComponentTypes.LORE, new LoreComponent(Text.literal(keyName)
                .getWithStyle(Style.EMPTY.withItalic(false).withColor(WATHE_KEY_LORE_COLOR))));
        return stack;
    }
}
