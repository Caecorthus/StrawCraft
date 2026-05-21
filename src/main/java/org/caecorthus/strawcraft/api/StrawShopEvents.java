package org.caecorthus.strawcraft.api;

import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StrawShopEvents {
    public static final Event<ModifyEntries> MODIFY_ENTRIES = EventFactory.createArrayBacked(
            ModifyEntries.class,
            listeners -> (player, context) -> {
                for (ModifyEntries listener : listeners) {
                    listener.modifyEntries(player, context);
                }
            }
    );

    private StrawShopEvents() {
    }

    public static List<ShopEntry> modifyEntries(@Nullable PlayerEntity player, List<ShopEntry> baseEntries) {
        ShopContext context = new ShopContext(baseEntries);
        MODIFY_ENTRIES.invoker().modifyEntries(player, context);
        return context.entries();
    }

    public interface ModifyEntries {
        void modifyEntries(@Nullable PlayerEntity player, ShopContext context);
    }

    public static final class ShopContext {
        private final List<ShopEntry> entries;

        private ShopContext(List<ShopEntry> entries) {
            this.entries = new ArrayList<>(entries);
        }

        public List<ShopEntry> getEntries() {
            return List.copyOf(entries);
        }

        public void replaceEntries(List<ShopEntry> nextEntries) {
            // Keep mutation centralized so adapters can preserve Wathe's index-based purchase contract.
            // 把变更集中在 context 里，方便适配层维持 Wathe 基于 index 的购买契约。
            entries.clear();
            entries.addAll(nextEntries);
        }

        public void clearEntries() {
            entries.clear();
        }

        public void addEntry(ShopEntry entry) {
            entries.add(entry);
        }

        private List<ShopEntry> entries() {
            return List.copyOf(entries);
        }
    }
}
