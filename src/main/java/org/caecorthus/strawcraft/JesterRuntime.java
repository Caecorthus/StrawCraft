package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JesterRuntime {
    public static final int PSYCHO_TICKS = JesterWinPolicy.PSYCHO_TICKS;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private JesterRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(JesterRuntime::tickServer);
        StrawKillEvents.BEFORE_KILL.register(JesterRuntime::beforeKill);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(JesterRuntime::handleRoleDeath);
        StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register(JesterRuntime::collectWinContributions);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            GameWorldComponent game = GameWorldComponent.KEY.get(world);
            if (!game.isRunning()) {
                continue;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (isJester(game.getRole(player))) {
                    tickJester(player);
                }
            }
        }
    }

    private static void tickJester(ServerPlayerEntity player) {
        NoellesRoleStateComponent component = NoellesRoleStateComponent.KEY.get(player);
        NoellesRoleState.JesterMomentState state = component.jesterMomentState();
        if (state.inStasis() && state.stasisTicks() > 0) {
            int remaining = state.stasisTicks() - 1;
            holdInStasis(player, state);
            if (remaining <= 0) {
                startPsychoMode(player, state);
                return;
            }
            component.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                    state.won(),
                    true,
                    remaining,
                    false,
                    0,
                    state.targetKiller(),
                    state.stasisX(),
                    state.stasisY(),
                    state.stasisZ()
            ));
            return;
        }

        if (state.inPsychoMode() && state.psychoModeTicks() > 0) {
            int remaining = state.psychoModeTicks() - 1;
            component.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                    state.won(),
                    false,
                    0,
                    remaining > 0,
                    Math.max(remaining, 0),
                    state.targetKiller(),
                    state.stasisX(),
                    state.stasisY(),
                    state.stasisZ()
            ));
            if (remaining <= 0) {
                killJesterAfterTimeout(player);
            }
        }
    }

    private static void killJesterAfterTimeout(ServerPlayerEntity player) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        psycho.setArmour(0);
        psycho.setPsychoTicks(0);
        GameFunctions.killPlayer(player, true, null, StrawDeathReasons.JESTER_TIMEOUT);
    }

    private static void holdInStasis(ServerPlayerEntity player, NoellesRoleState.JesterMomentState state) {
        player.teleport(state.stasisX(), state.stasisY(), state.stasisZ(), false);
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.velocityModified = true;
    }

    private static void startPsychoMode(ServerPlayerEntity player, NoellesRoleState.JesterMomentState state) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        psycho.setArmour(GameConstants.PSYCHO_MODE_ARMOUR);
        psycho.setPsychoTicks(PSYCHO_TICKS);
        grantIfMissing(player, WatheItems.BAT);
        NoellesRoleStateComponent.KEY.get(player).setJesterMomentState(new NoellesRoleState.JesterMomentState(
                state.won(),
                false,
                0,
                true,
                PSYCHO_TICKS,
                state.targetKiller(),
                state.stasisX(),
                state.stasisY(),
                state.stasisZ()
        ));
    }

    private static StrawKillEvents.KillDecision beforeKill(
            PlayerEntity victim,
            @Nullable PlayerEntity killer,
            Identifier deathReason
    ) {
        if (victim == null || victim.getWorld().isClient()) {
            return StrawKillEvents.KillDecision.pass();
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        if (!game.isRunning()) {
            return StrawKillEvents.KillDecision.pass();
        }

        Role victimRole = game.getRole(victim);
        NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(victim);
        JesterWinPolicy.KillAttemptDecision decision = JesterWinPolicy.evaluateKillAttempt(
                new JesterWinPolicy.KillAttemptInput(
                        isJester(victimRole),
                        state.jesterMomentState().inStasis(),
                        state.jesterMomentState().inPsychoMode(),
                        Optional.ofNullable(killer).map(PlayerEntity::getUuid),
                        killer != null && isInnocent(game.getRole(killer)),
                        deathReason
                )
        );
        if (decision.action() == JesterWinPolicy.KillAttemptAction.ENTER_STASIS) {
            UUID targetKiller = decision.targetKiller().orElseThrow();
            // EN: Source Jester turns an innocent gun death into stasis, then hunts that shooter as the win target.
            // CN: 源版小丑会把无辜者枪杀转成静滞，并把开枪者记录为后续胜利目标。
            state.clearNeutralWinClaim(JesterWinPolicy.JESTER_ROLE);
            state.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                    false,
                    true,
                    decision.stasisTicks(),
                    false,
                    0,
                    Optional.of(targetKiller),
                    victim.getX(),
                    victim.getY(),
                    victim.getZ()
            ));
        } else if (decision.action() == JesterWinPolicy.KillAttemptAction.RESET_STASIS_AND_ALLOW_DEATH) {
            state.clearJesterMomentState();
        }
        return new StrawKillEvents.KillDecision(decision.cancelWatheKill());
    }

    private static void handleRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        ServerWorld world = context.world();
        UUID deadPlayer = context.official().victimUuid();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (UUID jesterUuid : game.getRoles().keySet()) {
            PlayerEntity jester = world.getPlayerByUuid(jesterUuid);
            if (jester == null || !GameFunctions.isPlayerAliveAndSurvival(jester) || !isJester(game.getRole(jester))) {
                continue;
            }
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(jester);
            if (JesterWinPolicy.targetDeathCompletesWin(state.jesterMomentState().targetKiller(), deadPlayer)) {
                state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                        JesterWinPolicy.JESTER_ROLE,
                        JesterWinPolicy.TARGET_KILLED_TRIGGER,
                        Optional.of(deadPlayer),
                        context.official().gameTime()
                ));
                state.setJesterMomentState(withWon(state.jesterMomentState()));
            }
        }

        if (context.victimRoleId().filter(JesterWinPolicy.JESTER_ROLE::equals).isPresent()) {
            PlayerEntity victim = world.getPlayerByUuid(deadPlayer);
            if (victim != null) {
                NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(victim);
                state.clearNeutralWinClaim(JesterWinPolicy.JESTER_ROLE);
                state.clearJesterMomentState();
            }
        }
    }

    private static void collectWinContributions(
            StrawWinEvents.WinContext context,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        if (context.world().isEmpty()) {
            return;
        }
        ServerWorld world = context.world().orElseThrow();
        for (StrawWinEvents.Participant participant : context.participants()) {
            if (!participant.alive() || participant.roleId().filter(JesterWinPolicy.JESTER_ROLE::equals).isEmpty()) {
                continue;
            }
            PlayerEntity player = world.getPlayerByUuid(participant.playerUuid());
            if (player == null) {
                continue;
            }
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(player);
            collectWinContribution(participant.playerUuid(), state, context.defaultWin(), contribution);
        }
    }

    static void collectWinContribution(
            UUID playerUuid,
            NoellesRoleStateComponent state,
            StrawWinEvents.DefaultWin defaultWin,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        collectWinContribution(playerUuid, state.jesterMomentState(), state.neutralWinClaim(JesterWinPolicy.JESTER_ROLE).isPresent(),
                defaultWin, contribution);
    }

    static void collectWinContribution(
            UUID playerUuid,
            NoellesRoleState state,
            StrawWinEvents.DefaultWin defaultWin,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        collectWinContribution(playerUuid, state.jesterMomentState(), state.neutralWinClaim(JesterWinPolicy.JESTER_ROLE).isPresent(),
                defaultWin, contribution);
    }

    private static void collectWinContribution(
            UUID playerUuid,
            NoellesRoleState.JesterMomentState state,
            boolean hasNeutralClaim,
            StrawWinEvents.DefaultWin defaultWin,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        JesterWinPolicy.WinCheckDecision decision = JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(
                        true,
                        hasNeutralClaim || state.won(),
                        state.inPsychoMode(),
                        defaultWin
                )
        );
        if (decision == JesterWinPolicy.WinCheckDecision.NEUTRAL_WIN) {
            contribution
                    .replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)
                    .addExtraWinner(
                            playerUuid,
                            JesterWinPolicy.JESTER_ROLE,
                            JesterWinPolicy.TARGET_KILLED_TRIGGER
                    );
            return;
        }
        if (decision == JesterWinPolicy.WinCheckDecision.BLOCK_DEFAULT_WIN) {
            contribution.suppressDefaultWin();
        }
    }

    private static NoellesRoleState.JesterMomentState withWon(NoellesRoleState.JesterMomentState state) {
        return new NoellesRoleState.JesterMomentState(
                true,
                state.inStasis(),
                state.stasisTicks(),
                state.inPsychoMode(),
                state.psychoModeTicks(),
                state.targetKiller(),
                state.stasisX(),
                state.stasisY(),
                state.stasisZ()
        );
    }

    private static boolean isJester(@Nullable Role role) {
        return StrawRoleMeaning.receivesJesterMoment(role);
    }

    private static boolean isInnocent(@Nullable Role role) {
        return StrawRoleMeaning.isInnocent(role);
    }

    private static void grantIfMissing(ServerPlayerEntity player, Item item) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                return;
            }
        }
        player.giveItemStack(item.getDefaultStack());
    }
}
