package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PathogenWinPolicy {
    public static final Identifier PATHOGEN_ROLE = PathogenInfectionPolicy.PATHOGEN_ROLE;
    public static final Identifier INFECTION_WIN_TRIGGER = StrawCraft.id("pathogen_infection_complete");

    private PathogenWinPolicy() {
    }

    public static boolean recordNeutralWinIfComplete(
            NoellesRoleState state,
            UUID pathogenUuid,
            List<Participant> participants,
            long gameTime
    ) {
        Objects.requireNonNull(state, "state");
        if (!isComplete(pathogenUuid, participants) || state.neutralWinClaim(PATHOGEN_ROLE).isPresent()) {
            return false;
        }

        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                PATHOGEN_ROLE,
                INFECTION_WIN_TRIGGER,
                Optional.empty(),
                gameTime
        ));
        return true;
    }

    public static boolean recordNeutralWinIfComplete(
            NoellesRoleStateComponent state,
            UUID pathogenUuid,
            List<Participant> participants,
            long gameTime
    ) {
        Objects.requireNonNull(state, "state");
        if (!isComplete(pathogenUuid, participants) || state.neutralWinClaim(PATHOGEN_ROLE).isPresent()) {
            return false;
        }

        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                PATHOGEN_ROLE,
                INFECTION_WIN_TRIGGER,
                Optional.empty(),
                gameTime
        ));
        return true;
    }

    public static boolean isComplete(UUID pathogenUuid, List<Participant> participants) {
        Objects.requireNonNull(pathogenUuid, "pathogenUuid");
        List<Participant> copiedParticipants = List.copyOf(participants);
        return copiedParticipants.stream()
                .filter(Participant::assigned)
                .filter(Participant::alive)
                .filter(participant -> !participant.uuid().equals(pathogenUuid))
                .filter(participant -> !participant.pathogen())
                .allMatch(participant -> participant.infectedBy().filter(pathogenUuid::equals).isPresent());
    }

    public record Participant(
            UUID uuid,
            boolean assigned,
            boolean alive,
            boolean pathogen,
            Optional<UUID> infectedBy
    ) {
        public Participant {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(infectedBy, "infectedBy");
        }
    }
}
