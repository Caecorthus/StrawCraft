package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NoellesAssignedLoadouts {
    static final Identifier WATHE_WALKIE_TALKIE = Identifier.of("wathe", "walkie_talkie");
    static final Identifier WATHE_NOTE = Identifier.of("wathe", "note");
    static final Identifier STRAW_MASTER_KEY = StrawCraft.id("master_key");
    static final Identifier STRAW_ANTIDOTE = StrawCraft.id("antidote");
    static final Identifier STRAW_NOISEMAKER = StrawCraft.id("noisemaker");

    private static final String UNDERCOVER_WALKIE_TALKIE_UNSUPPORTED =
            "XruiDD Undercover walkie-talkie depends on Spark-ver Wathe; official Wathe has no wathe:walkie_talkie item";

    private static final List<OpeningLoadout> OPENING_LOADOUTS = List.of(
            unsupported("undercover", WATHE_WALKIE_TALKIE, 1, UNDERCOVER_WALKIE_TALKIE_UNSUPPORTED),
            supported("awesome_binglus", WATHE_NOTE, 16),
            supported("conductor", STRAW_MASTER_KEY, 1),
            supported("toxicologist", STRAW_ANTIDOTE, 1),
            supported("noisemaker", STRAW_NOISEMAKER, 1)
    );

    private NoellesAssignedLoadouts() {
    }

    static LoadoutPlan assignmentPlan(Role role) {
        if (role == null) {
            return LoadoutPlan.empty();
        }
        Map<Identifier, Integer> supportedCounts = new LinkedHashMap<>();
        List<UnsupportedItemGrant> unsupportedGrants = new ArrayList<>();
        for (OpeningLoadout loadout : OPENING_LOADOUTS) {
            if (!StrawRoleMeaning.matchesRoleId(role, loadout.roleId())) {
                continue;
            }
            for (ItemGrant grant : loadout.itemGrants()) {
                supportedCounts.merge(grant.itemId(), grant.count(), Integer::sum);
            }
            for (UnsupportedItemGrant grant : loadout.unsupportedItemGrants()) {
                if (!unsupportedGrants.contains(grant)) {
                    unsupportedGrants.add(grant);
                }
            }
        }
        List<ItemGrant> itemGrants = supportedCounts.entrySet().stream()
                .map(entry -> new ItemGrant(entry.getKey(), entry.getValue()))
                .toList();
        return new LoadoutPlan(itemGrants, unsupportedGrants);
    }

    static void giveAssignedItems(PlayerEntity player, List<ItemGrant> itemGrants) {
        for (ItemGrant grant : itemGrants) {
            for (ItemStack stack : createAssignmentStacks(grant)) {
                player.giveItemStack(stack);
            }
        }
    }

    private static List<ItemStack> createAssignmentStacks(ItemGrant grant) {
        // Resolve by id so missing Spark-only/custom items never become fake gameplay items.
        // 按 id 解析；Spark-only/自定义物品缺失时不会变成假的可游玩道具。
        return Registries.ITEM.getOrEmpty(grant.itemId())
                .map(item -> stacksFor(item, grant.count()))
                .orElseGet(List::of);
    }

    private static List<ItemStack> stacksFor(Item item, int count) {
        int maxCount = Math.max(1, item.getMaxCount());
        int remaining = count;
        List<ItemStack> stacks = new ArrayList<>();
        while (remaining > 0) {
            int stackCount = Math.min(remaining, maxCount);
            stacks.add(new ItemStack(item, stackCount));
            remaining -= stackCount;
        }
        return stacks;
    }

    private static OpeningLoadout supported(String rolePath, Identifier itemId, int count) {
        return new OpeningLoadout(StrawCraft.id(rolePath), List.of(new ItemGrant(itemId, count)), List.of());
    }

    private static OpeningLoadout unsupported(String rolePath, Identifier itemId, int count, String reason) {
        return new OpeningLoadout(StrawCraft.id(rolePath), List.of(), List.of(new UnsupportedItemGrant(itemId, count, reason)));
    }

    static List<ItemGrant> supportedItemGrantsForVerification() {
        // Verifier seam: every supported gameplay grant must be backed by official Wathe or a real StrawCraft item.
        // 验证用边界：所有标记为支持的发放物品，都必须来自官方 Wathe 或真实注册的 StrawCraft 物品。
        return OPENING_LOADOUTS.stream()
                .flatMap(loadout -> loadout.itemGrants().stream())
                .toList();
    }

    record LoadoutPlan(List<ItemGrant> itemGrants, List<UnsupportedItemGrant> unsupportedItemGrants) {
        LoadoutPlan {
            itemGrants = List.copyOf(itemGrants);
            unsupportedItemGrants = List.copyOf(unsupportedItemGrants);
        }

        private static LoadoutPlan empty() {
            return new LoadoutPlan(List.of(), List.of());
        }
    }

    record ItemGrant(Identifier itemId, int count) {
        ItemGrant {
            if (count < 1) {
                throw new IllegalArgumentException("count must be positive");
            }
        }
    }

    record UnsupportedItemGrant(Identifier itemId, int count, String reason) {
        UnsupportedItemGrant {
            if (count < 1) {
                throw new IllegalArgumentException("count must be positive");
            }
        }
    }

    private record OpeningLoadout(Identifier roleId, List<ItemGrant> itemGrants, List<UnsupportedItemGrant> unsupportedItemGrants) {
        private OpeningLoadout {
            itemGrants = List.copyOf(itemGrants);
            unsupportedItemGrants = List.copyOf(unsupportedItemGrants);
        }
    }
}
