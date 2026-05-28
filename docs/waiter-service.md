# Waiter Service

Implemented slice:
- Waiter has a role-only service tray shop entry.
- Server-side use validates waiter role, living actor and target, a valid food/drink/service item, and an open Wathe eat/drink task.
- Official food and cocktail stacks are consumed through Wathe/Minecraft item consumption so existing task and poison item behavior stays attached.

Deferral:
- FoodPlatterBlock double-pickup: deferred. Wathe's current platter pickup path is block-interaction bytecode without a named public event or helper; StrawCraft does not fake a double-pickup rule until that seam can be contract-tested and mixed in narrowly.
