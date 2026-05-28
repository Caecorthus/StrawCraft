package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class PlayerShopCatalog {
    private PlayerShopCatalog() {
    }

    public static Presentation presentationFor(@Nullable Role role, List<ShopEntry> materializedEntries) {
        // Keep player-specific shop logic as a projection over Wathe's materialized global list.
        // 玩家专属商店只投影 Wathe 已物化的全局列表，避免改变 StoreBuyPayload(index) 的含义。
        if (role == null) {
            return Presentation.empty();
        }
        if (StrawRoleMeaning.usesBomberShop(role)) {
            return bomberPresentation(materializedEntries);
        }
        if (StrawRoleMeaning.usesScavengerShop(role)) {
            return scavengerPresentation(materializedEntries);
        }
        if (StrawRoleMeaning.usesTimekeeperShop(role)) {
            return timekeeperPresentation(materializedEntries);
        }
        if (StrawRoleMeaning.usesReporterShop(role)) {
            return reporterPresentation(materializedEntries);
        }
        if (StrawRoleMeaning.canUseKillerShop(role)) {
            return fullPresentation(materializedEntries);
        }
        return Presentation.empty();
    }

    public static boolean allowsPurchase(@Nullable PlayerEntity player, List<ShopEntry> materializedEntries, int wathePurchaseIndex) {
        if (player == null) {
            return true;
        }
        try {
            // The server re-derives the current role projection so stale or forged visible clicks
            // cannot buy entries hidden from that role.
            // 服务端会重新按当前职业投影商店，避免过期或伪造的可见点击购买该职业隐藏的条目。
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            return allowsPurchaseForRole(role, materializedEntries, wathePurchaseIndex);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static boolean allowsPurchaseForRole(@Nullable Role role, List<ShopEntry> materializedEntries, int wathePurchaseIndex) {
        return presentationFor(role, materializedEntries).allowsWathePurchaseIndex(wathePurchaseIndex);
    }

    private static Presentation fullPresentation(List<ShopEntry> materializedEntries) {
        List<VisibleEntry> visibleEntries = new ArrayList<>(materializedEntries.size());
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (!ScavengerShopLoadout.isResetKnifeCooldownEntry(entry)
                    && !ReporterShopLoadout.isReporterNoteEntry(entry)) {
                visibleEntries.add(new VisibleEntry(index, entry));
            }
        }
        return new Presentation(visibleEntries);
    }

    private static Presentation bomberPresentation(List<ShopEntry> materializedEntries) {
        List<VisibleEntry> grenades = new ArrayList<>();
        List<VisibleEntry> remainingEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (isGrenade(entry)) {
                grenades.add(new VisibleEntry(index, entry));
            } else if (!isBomberDeniedEntry(entry)) {
                remainingEntries.add(new VisibleEntry(index, entry));
            }
        }

        // Bomber can safely reuse Wathe's existing grenade and tool deliveries now.
        // Timed bombs and other Noelles-only items stay deferred until their runtime behavior exists in StrawCraft.
        // 炸弹客目前只复用 Wathe 已有的手雷和工具交付逻辑。
        // 定时炸弹等 Noelles 专属物品等 StrawCraft 有对应运行时行为后再接入。
        List<VisibleEntry> visibleEntries = new ArrayList<>(grenades.size() + remainingEntries.size());
        visibleEntries.addAll(grenades);
        visibleEntries.addAll(remainingEntries);
        return new Presentation(visibleEntries);
    }

    private static Presentation scavengerPresentation(List<ShopEntry> materializedEntries) {
        List<VisibleEntry> knives = new ArrayList<>();
        List<VisibleEntry> resetEntries = new ArrayList<>();
        List<VisibleEntry> remainingEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (isKnife(entry)) {
                knives.add(new VisibleEntry(index, entry));
            } else if (ScavengerShopLoadout.isResetKnifeCooldownEntry(entry)) {
                resetEntries.add(new VisibleEntry(index, entry));
            } else if (!isScavengerDeniedEntry(entry)) {
                remainingEntries.add(new VisibleEntry(index, entry));
            }
        }

        // Scavenger keeps a knife-first projection and only exposes the reset purchase
        // when Wathe's real knife entry exists in the materialized shop.
        // 清道夫商店先展示刀；只有 Wathe 真实刀条目存在时，才展示重置冷却购买项。
        List<VisibleEntry> visibleEntries = new ArrayList<>(knives.size() + resetEntries.size() + remainingEntries.size());
        visibleEntries.addAll(knives);
        if (!knives.isEmpty()) {
            visibleEntries.addAll(resetEntries);
        }
        visibleEntries.addAll(remainingEntries);
        return new Presentation(visibleEntries);
    }

    private static Presentation timekeeperPresentation(List<ShopEntry> materializedEntries) {
        List<VisibleEntry> timerEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (TimekeeperShopLoadout.isTimeSubtractionEntry(entry)) {
                timerEntries.add(new VisibleEntry(index, entry));
            }
        }
        return new Presentation(timerEntries);
    }

    private static Presentation reporterPresentation(List<ShopEntry> materializedEntries) {
        List<VisibleEntry> noteEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (ReporterShopLoadout.isReporterNoteEntry(entry)) {
                noteEntries.add(new VisibleEntry(index, entry));
            }
        }
        return new Presentation(noteEntries);
    }

    private static boolean isBomberDeniedEntry(ShopEntry entry) {
        return ScavengerShopLoadout.isResetKnifeCooldownEntry(entry)
                || ReporterShopLoadout.isReporterNoteEntry(entry)
                || matches(entry, "knife", stack -> stack.isOf(WatheItems.KNIFE))
                || matches(entry, "p320", stack -> false)
                || matches(entry, "revolver", stack -> stack.isOf(WatheItems.REVOLVER))
                || matches(entry, "poison_vial", stack -> stack.isOf(WatheItems.POISON_VIAL))
                || matches(entry, "scorpion", stack -> stack.isOf(WatheItems.SCORPION))
                || matches(entry, "psycho_mode", stack -> stack.isOf(WatheItems.PSYCHO_MODE));
    }

    private static boolean isScavengerDeniedEntry(ShopEntry entry) {
        return matches(entry, "p320", stack -> false)
                || ReporterShopLoadout.isReporterNoteEntry(entry)
                || matches(entry, "revolver", stack -> stack.isOf(WatheItems.REVOLVER))
                || matches(entry, "grenade", stack -> stack.isOf(WatheItems.GRENADE))
                || matches(entry, "poison_vial", stack -> stack.isOf(WatheItems.POISON_VIAL))
                || matches(entry, "scorpion", stack -> stack.isOf(WatheItems.SCORPION))
                || matches(entry, "psycho_mode", stack -> stack.isOf(WatheItems.PSYCHO_MODE));
    }

    private static boolean isKnife(ShopEntry entry) {
        return matches(entry, "knife", stack -> stack.isOf(WatheItems.KNIFE));
    }

    private static boolean isGrenade(ShopEntry entry) {
        return matches(entry, "grenade", stack -> stack.isOf(WatheItems.GRENADE));
    }

    private static boolean matches(ShopEntry entry, String path, Predicate<ItemStack> stackPredicate) {
        String id = StrawShopEntry.idFor(entry);
        if (path.equals(id) || id.endsWith(":" + path)) {
            return true;
        }
        ItemStack stack = entry.stack();
        return stack != null && !stack.isEmpty() && stackPredicate.test(stack);
    }

    public record Presentation(List<VisibleEntry> visibleEntries) {
        public Presentation {
            visibleEntries = List.copyOf(visibleEntries);
        }

        public static Presentation empty() {
            return new Presentation(List.of());
        }

        public static Presentation fromWatheOrder(List<ShopEntry> materializedEntries) {
            List<VisibleEntry> visibleEntries = new ArrayList<>(materializedEntries.size());
            for (int index = 0; index < materializedEntries.size(); index++) {
                visibleEntries.add(new VisibleEntry(index, materializedEntries.get(index)));
            }
            return new Presentation(visibleEntries);
        }

        public List<ShopEntry> entries() {
            return visibleEntries.stream().map(VisibleEntry::entry).toList();
        }

        public Optional<VisibleEntry> entryAtVisibleIndex(int visibleIndex) {
            if (visibleIndex < 0 || visibleIndex >= visibleEntries.size()) {
                return Optional.empty();
            }
            return Optional.of(visibleEntries.get(visibleIndex));
        }

        public boolean allowsWathePurchaseIndex(int wathePurchaseIndex) {
            return visibleEntries.stream()
                    .anyMatch(entry -> entry.wathePurchaseIndex() == wathePurchaseIndex);
        }
    }

    public record VisibleEntry(int wathePurchaseIndex, ShopEntry entry) {
    }
}
