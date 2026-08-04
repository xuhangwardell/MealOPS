# ADR 0017: Meal Plan and Meal Slot Lifecycle

## Status

Accepted for Node 12; implementation under review.

## Background

MealOps needs a persistent, explicitly scheduled meal plan before a future Planner can generate plans. This node must model a 1–3 day planning window and lifecycle without applying preferences or mutating inventory.

## Decision

Use a relational `MealPlan` aggregate with persistent `MealSlot` children (Flyway V6). Slots use the stable meal types BREAKFAST, LUNCH and DINNER, are unique by date and type, and may be unassigned while a plan is DRAFT. Plans are created as DRAFT, support full-replacement PUT while DRAFT, transition DRAFT→CONFIRMED or DRAFT/CONFIRMED→CANCELLED, and have no COMPLETED state yet. Confirm requires every slot to have a valid Recipe selection. Recipe existence is validated before writes. Conditional SQL protects lifecycle transitions; there is no general version column and concurrent successful draft replacements are last-write-wins.

## Reasons

The aggregate keeps schedule and slots atomic, makes ordering deterministic, and leaves Planner, preferences application, shopping and inventory side effects outside this vertical slice.

## Alternatives

- Ephemeral plans versus persisted aggregates.
- Automatically generating all daily slots versus explicit requested slots.
- Requiring assignments at creation versus nullable assignments in DRAFT.
- PATCH slot operations versus full replacement PUT.
- General optimistic versioning versus conditional lifecycle SQL.
- Applying Planning Preferences now versus deferring interpretation to Planner.

## Trade-offs

Relational parent/child rows provide durable history and database constraints but require replacement transactions. Full replacement is simple and deterministic, while last-write-wins can lose a concurrent draft update. The slot date-in-range invariant remains in the domain because a basic CHECK cannot safely inspect the parent row without triggers.

## Revisit Conditions

Revisit when authentication, multi-user ownership, Planner generation, recipe ranking, inventory reservation/consumption, completion workflows, or meaningful concurrent editing requirements are introduced.
