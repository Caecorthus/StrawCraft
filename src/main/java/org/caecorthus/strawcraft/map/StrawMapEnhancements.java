package org.caecorthus.strawcraft.map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public record StrawMapEnhancements(
        Gravity gravity,
        Movement movement,
        Jump jump,
        Optional<Scenery> scenery,
        Optional<Visibility> visibility,
        Optional<Fog> fog,
        Optional<CameraShake> cameraShake,
        Optional<Ambience> ambience,
        InteractionBlacklist interactionBlacklist
) {
    public static final Gravity DEFAULT_GRAVITY = new Gravity(1.0F);
    public static final Movement DEFAULT_MOVEMENT = new Movement(1.0F, 1.0F);
    public static final Jump DEFAULT_JUMP = new Jump(true, 0.0F);
    public static final InteractionBlacklist DEFAULT_INTERACTION_BLACKLIST = new InteractionBlacklist(Set.of(), Set.of());
    public static final StrawMapEnhancements DEFAULT = new StrawMapEnhancements(
            DEFAULT_GRAVITY,
            DEFAULT_MOVEMENT,
            DEFAULT_JUMP,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            DEFAULT_INTERACTION_BLACKLIST
    );

    public StrawMapEnhancements(
            Gravity gravity,
            Movement movement,
            Jump jump,
            Optional<Scenery> scenery,
            Optional<Visibility> visibility,
            Optional<Fog> fog,
            Optional<CameraShake> cameraShake,
            Optional<Ambience> ambience
    ) {
        this(
                gravity,
                movement,
                jump,
                scenery,
                visibility,
                fog,
                cameraShake,
                ambience,
                DEFAULT_INTERACTION_BLACKLIST
        );
    }

    public StrawMapEnhancements {
        gravity = gravity == null ? DEFAULT_GRAVITY : gravity;
        movement = movement == null ? DEFAULT_MOVEMENT : movement;
        jump = jump == null ? DEFAULT_JUMP : jump;
        scenery = scenery == null ? Optional.empty() : scenery;
        visibility = visibility == null ? Optional.empty() : visibility;
        fog = fog == null ? Optional.empty() : fog;
        cameraShake = cameraShake == null ? Optional.empty() : cameraShake;
        ambience = ambience == null ? Optional.empty() : ambience;
        interactionBlacklist = interactionBlacklist == null ? DEFAULT_INTERACTION_BLACKLIST : interactionBlacklist;
    }

    public boolean hasServerRuntimeEnhancements() {
        return changesGravity() || changesMovement() || changesJump();
    }

    public boolean changesGravity() {
        return gravity.gravityMultiplier() != 1.0F;
    }

    public boolean changesMovement() {
        return movement.walkSpeedMultiplier() != 1.0F || movement.sprintSpeedMultiplier() != 1.0F;
    }

    public boolean changesJump() {
        return !jump.allowed();
    }

    public record Gravity(float gravityMultiplier) {
    }

    public record Movement(float walkSpeedMultiplier, float sprintSpeedMultiplier) {
    }

    public record Jump(boolean allowed, float staminaCost) {
    }

    public record Scenery(int heightOffset, int minX, int maxX, int minZ, int maxZ) {
    }

    public record Visibility(int day, int night, int sundown) {
    }

    public record Fog(float start, float endMoving, float endStationary, int nightColor) {
    }

    public record CameraShake(
            boolean enabled,
            float amplitudeIndoor,
            float amplitudeOutdoor,
            float strengthIndoor,
            float strengthOutdoor
    ) {
    }

    public record Ambience(
            boolean requireTrainMoving,
            Optional<String> insideSound,
            Optional<String> outsideSound
    ) {
        public Ambience {
            insideSound = insideSound == null ? Optional.empty() : insideSound;
            outsideSound = outsideSound == null ? Optional.empty() : outsideSound;
        }
    }

    public record InteractionBlacklist(Set<Identifier> blocks, Set<TagKey<Block>> blockTags) {
        public InteractionBlacklist {
            blocks = blocks == null ? Set.of() : Set.copyOf(blocks);
            blockTags = blockTags == null ? Set.of() : Set.copyOf(blockTags);
        }

        public static TagKey<Block> blockTag(Identifier id) {
            return TagKey.of(RegistryKeys.BLOCK, id);
        }

        public boolean isEmpty() {
            return blocks.isEmpty() && blockTags.isEmpty();
        }

        public boolean blocksInteraction(BlockState state) {
            if (state == null) {
                return false;
            }
            return blocksInteraction(Registries.BLOCK.getId(state.getBlock()), state::isIn);
        }

        public boolean blocksInteraction(Identifier blockId, Predicate<TagKey<Block>> tagPredicate) {
            if (blockId != null && blocks.contains(blockId)) {
                return true;
            }
            if (tagPredicate == null) {
                return false;
            }
            for (TagKey<Block> blockTag : blockTags) {
                if (tagPredicate.test(blockTag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
