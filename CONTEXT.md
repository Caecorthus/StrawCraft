# StrawCraft Context

StrawCraft is a Caecorthus addon mod that changes Wathe gameplay without modifying Wathe source. This context keeps the project language stable around Wathe integration and TACZ weapon replacement.

## Language

**Wathe Round**:
A running Murder Mystery match owned by Wathe state.
_Avoid_: game, session

**Vanilla Health Bridge**:
The StrawCraft rule set that converts Wathe kill requests into normal Minecraft damage and death bookkeeping.
_Avoid_: damage handler, death fix

**Wathe Round Participant Lifecycle**:
The StrawCraft rule set that stops tracking a player when they leave alive Wathe Round participation, and mirrors vanilla deaths into Wathe bookkeeping.
_Avoid_: cleanup hook, death cleanup

**Role Loadout**:
Items granted because Wathe assigned a player role.
_Avoid_: kit, starting inventory

**Killer Shop**:
Wathe's round shop entries available to killer-capable roles.
_Avoid_: store, buy menu

**Shop Screen State**:
The client-side view state that turns Wathe shop entry data into button availability, price text, and status text.
_Avoid_: shop logic, button rendering

**TACZ Gun Profile**:
A supported TACZ gun plus its ammo identity and refill limits.
_Avoid_: gun id, weapon config

**TACZ Gun Stack**:
An ItemStack of `tacz:modern_kinetic_gun` with custom data that identifies one TACZ gun.
_Avoid_: TACZ item, gun item

**Ammo Refill Cycle**:
The state machine that waits on low loaded ammo, grants missing ammo, and then waits for a reload before starting again.
_Avoid_: ammo timer, refill cooldown

**Map Voting State Machine**:
The StrawCraft rule set that decides map voting phases, countdowns, vote counts, and final map selection.
_Avoid_: map vote component, voting UI logic

**Map Voting Adapter**:
The Fabric, CCA, Wathe, network, and server-world wiring that applies a **Map Voting State Machine** transition.
_Avoid_: voting logic, map vote handler

## Relationships

- A **Role Loadout** may grant a **TACZ Gun Stack** from a **TACZ Gun Profile**.
- The **Killer Shop** may sell a **TACZ Gun Stack** from a **TACZ Gun Profile**.
- The **Shop Screen State** displays **Killer Shop** entries without deciding what the **Killer Shop** contains.
- An **Ammo Refill Cycle** observes a **TACZ Gun Stack** and uses its **TACZ Gun Profile**.
- An **Ammo Refill Cycle** decides when a **TACZ Gun Stack** receives a StrawCraft ammo cycle id.
- The **Vanilla Health Bridge** operates during a **Wathe Round**.
- The **Vanilla Health Bridge** cancels Wathe kill requests after applying any matching vanilla damage.
- The **Wathe Round Participant Lifecycle** clears StrawCraft runtime state when a player is no longer an alive **Wathe Round** participant.
- The **Wathe Round Participant Lifecycle** mirrors vanilla deaths into Wathe bookkeeping without changing death reason policy.
- The **Map Voting Adapter** owns CCA sync, NBT persistence, network payloads, and teleport side effects.
- The **Map Voting State Machine** owns voting phases and emits the selected map for the **Map Voting Adapter** to apply.

## Example Dialogue

> **Dev:** "Should the **Role Loadout** pass `tacz:rhino357` directly?"
> **Domain expert:** "No. Use the **TACZ Gun Profile** so the **TACZ Gun Stack** and **Ammo Refill Cycle** agree on the same supported gun."

## Flagged Ambiguities

- "gun id" can mean a raw TACZ `GunId` string or a supported StrawCraft **TACZ Gun Profile**; resolved: gameplay modules should use **TACZ Gun Profile**, and only the **TACZ Gun Stack** module writes raw TACZ custom data.
