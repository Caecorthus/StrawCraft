package org.caecorthus.strawcraft.api;

import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StrawShopEvents {
    public static final Event<BuildEntries> BUILD_ENTRIES = EventFactory.createArrayBacked(
            BuildEntries.class,
            listeners -> context -> {
                for (BuildEntries listener : listeners) {
                    listener.buildEntries(context);
                }
            }
    );

    @Deprecated
    public static final Event<ModifyEntries> MODIFY_ENTRIES = EventFactory.createArrayBacked(
            ModifyEntries.class,
            listeners -> (player, context) -> {
                for (ModifyEntries listener : listeners) {
                    listener.modifyEntries(player, context);
                }
            }
    );

    public static final Event<BeforePurchase> BEFORE_PURCHASE = EventFactory.createArrayBacked(
            BeforePurchase.class,
            listeners -> (player, context) -> {
                for (BeforePurchase listener : listeners) {
                    listener.beforePurchase(player, context);
                    if (!context.allowed()) {
                        return;
                    }
                }
            }
    );

    public static final Event<AfterPurchase> AFTER_PURCHASE = EventFactory.createArrayBacked(
            AfterPurchase.class,
            listeners -> (player, receipt) -> {
                for (AfterPurchase listener : listeners) {
                    listener.afterPurchase(player, receipt);
                }
            }
    );

    private StrawShopEvents() {
    }

    public static List<ShopEntry> buildEntries(List<ShopEntry> baseEntries) {
        ShopContext context = new ShopContext(baseEntries);
        BUILD_ENTRIES.invoker().buildEntries(context);
        MODIFY_ENTRIES.invoker().modifyEntries(null, context);
        return context.entries();
    }

    @Deprecated
    public static List<ShopEntry> buildEntries(@Nullable PlayerEntity player, List<ShopEntry> baseEntries) {
        return buildEntries(baseEntries);
    }

    @Deprecated
    public static List<ShopEntry> modifyEntries(@Nullable PlayerEntity player, List<ShopEntry> baseEntries) {
        return buildEntries(player, baseEntries);
    }

    public static PurchaseContext beforePurchase(@Nullable PlayerEntity player, ShopEntry entry, int index) {
        PurchaseContext context = new PurchaseContext(entry, index);
        BEFORE_PURCHASE.invoker().beforePurchase(player, context);
        return context;
    }

    public static void afterPurchase(@Nullable PlayerEntity player, PurchaseReceipt receipt) {
        AFTER_PURCHASE.invoker().afterPurchase(player, receipt);
    }

    public static void afterPurchase(@Nullable PlayerEntity player, PurchaseContext context) {
        afterPurchase(player, context.receipt());
    }

    public interface BuildEntries {
        void buildEntries(ShopContext context);
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

        public int size() {
            return entries.size();
        }

        public ShopEntry getEntry(int index) {
            return entries.get(index);
        }

        public ShopEntry setEntry(int index, ShopEntry entry) {
            return entries.set(index, entry);
        }

        public ShopEntry removeEntry(int index) {
            return entries.remove(index);
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

        public void addEntry(int index, ShopEntry entry) {
            entries.add(index, entry);
        }

        private List<ShopEntry> entries() {
            return List.copyOf(entries);
        }
    }

    public interface BeforePurchase {
        void beforePurchase(@Nullable PlayerEntity player, PurchaseContext context);
    }

    public interface AfterPurchase {
        void afterPurchase(@Nullable PlayerEntity player, PurchaseReceipt receipt);
    }

    public static final class PurchaseContext {
        private final ShopEntry entry;
        private final int index;
        private final int originalPrice;
        private final Map<String, String> metadata = new HashMap<>();
        private boolean allowed = true;
        private int price;
        private Text denyReason;

        private PurchaseContext(ShopEntry entry, int index) {
            this.entry = entry;
            this.index = index;
            this.originalPrice = entry.price();
            this.price = originalPrice;
        }

        public ShopEntry entry() {
            return entry;
        }

        public int index() {
            return index;
        }

        public int originalPrice() {
            return originalPrice;
        }

        public int price() {
            return price;
        }

        public boolean allowed() {
            return allowed;
        }

        public @Nullable Text denyReason() {
            return denyReason;
        }

        public Map<String, String> metadata() {
            return Map.copyOf(metadata);
        }

        public void allow() {
            allowed = true;
            denyReason = null;
        }

        public void allow(int modifiedPrice) {
            allow();
            setPrice(modifiedPrice);
        }

        public void setPrice(int price) {
            this.price = Math.max(0, price);
        }

        public void deny() {
            deny(null);
        }

        public void deny(@Nullable Text reason) {
            allowed = false;
            denyReason = reason;
        }

        public void putMetadata(String key, String value) {
            metadata.put(key, value);
        }

        public PurchaseReceipt receipt() {
            return new PurchaseReceipt(entry, index, price, metadata());
        }
    }

    public record PurchaseReceipt(ShopEntry entry, int index, int pricePaid, Map<String, String> metadata) {
    }
}
