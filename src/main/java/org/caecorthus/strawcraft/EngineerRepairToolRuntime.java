package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public final class EngineerRepairToolRuntime {
    private static final String ENGINEER_LOCK_KEY_NAME = "strawcraft:engineer";

    private EngineerRepairToolRuntime() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(EngineerRepairToolRuntime::useBlock);
    }

    private static ActionResult useBlock(PlayerEntity user, World world, Hand hand, BlockHitResult hitResult) {
        if (world == null || user == null || hand == null || hitResult == null || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        SmallDoorBlockEntity door = findSmallDoor(world, hitResult.getBlockPos());
        boolean officialWatheDoor = door != null;
        ItemStack stack = player.getStackInHand(hand);
        boolean repairToolInHand = stack.isOf(StrawCraftItems.REPAIR_TOOL);

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role role = game.getRole(player);
        boolean cooldownReady = !player.getItemCooldownManager().isCoolingDown(StrawCraftItems.REPAIR_TOOL);

        // English: Keep every gameplay gate server-side before mutating official Wathe door state.
        // Chinese: 所有玩法条件都先在服务端确认，再修改官方 Wathe 门状态。
        // 中文：所有玩法条件都先在服务端确认，再修改官方 Wathe 门状态。
        if (!EngineerDoorRepairPolicy.shouldHandle(new EngineerDoorRepairPolicy.InteractionInput(
                world.isClient(),
                game.isRunning(),
                role,
                GameFunctions.isPlayerAliveAndSurvival(player),
                repairToolInHand,
                cooldownReady,
                officialWatheDoor
        ))) {
            return ActionResult.PASS;
        }

        EngineerDoorRepairPolicy.DoorAction action = EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(
                true,
                door.isBlasted(),
                door.isJammed(),
                door.getKeyName()
        ));
        if (action == EngineerDoorRepairPolicy.DoorAction.PASS) {
            return ActionResult.PASS;
        }

        apply(action, door);
        door.markDirty();
        door.sync();
        player.getItemCooldownManager().set(StrawCraftItems.REPAIR_TOOL, EngineerDoorRepairPolicy.COOLDOWN_TICKS);
        return ActionResult.SUCCESS;
    }

    private static SmallDoorBlockEntity findSmallDoor(World world, BlockPos pos) {
        BlockEntity target = world.getBlockEntity(pos);
        if (target instanceof SmallDoorBlockEntity door) {
            return door;
        }
        BlockEntity lowerTarget = world.getBlockEntity(pos.down());
        if (lowerTarget instanceof SmallDoorBlockEntity door) {
            return door;
        }
        BlockEntity upperTarget = world.getBlockEntity(pos.up());
        if (upperTarget instanceof SmallDoorBlockEntity door) {
            return door;
        }
        return null;
    }

    private static void apply(EngineerDoorRepairPolicy.DoorAction action, SmallDoorBlockEntity door) {
        switch (action) {
            case REPAIR_BLASTED -> door.setBlasted(false);
            case CLEAR_JAMMED -> door.setJammed(0);
            case UNLOCK -> door.setKeyName("");
            case LOCK -> door.setKeyName(ENGINEER_LOCK_KEY_NAME);
            case PASS -> {
            }
        }
    }
}
