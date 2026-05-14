# StrawCraft

StrawCraft is a Caecorthus addon mod for Wathe.

## Current gameplay changes

- Restores vanilla Minecraft player hearts during Wathe rounds by letting `PlayerEntity.applyDamage` and `ServerPlayerEntity.onDeath` run normally.
- Converts Wathe direct kill requests, such as knife, gun, bat, grenade, and poison deaths, into normal vanilla damage instead of immediately moving the target to spectator.
- Keeps Wathe win-condition bookkeeping aware of vanilla deaths by marking a player dead after their normal `onDeath` finishes.
- Restores vanilla jumping during Wathe rounds.
- Restores vanilla walking and sprinting speed calculation.
- Disables Wathe stamina exhaustion and mood-based sprint blocking.

## Local Wathe dependency

Wathe is ARR, so this repository does not commit Wathe source or jars.

To build locally, place the matching Wathe jar here:

```text
libs/wathe-Parox-1.0.1.jar
```

Then run:

```bash
./gradlew build
```

The `libs/*.jar` path is intentionally ignored by git.
