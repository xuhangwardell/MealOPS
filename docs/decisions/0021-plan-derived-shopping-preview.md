# ADR 0021: Plan-Derived Shopping Preview

## Status

Accepted for Node 16; implementation under review.

## Background

Node 10 can calculate an ad-hoc Shopping Preview from request-provided Recipe selections. Node 12 persists MealPlan and MealSlot selections, while Node 15 can generate a complete DRAFT plan. MealOps now needs to derive one whole-plan shopping shortage view from that persisted plan without creating a second source of planning truth or mutating operational state.

## Decision

Node 16 adds `GET /api/v1/meal-plans/{id}/shopping-preview`. The persisted MealPlan is the only plan input, and every `MealPlanRecipeSelection.targetServings` is treated as stored fact. Current Planning Preferences are deliberately ignored: changing `defaultServings` after plan creation cannot change the requirements of an existing plan.

The read-only application service performs this sequence inside `@Transactional(readOnly = true)`:

1. load the MealPlan and apply the existing plan-state policy;
2. load all Recipe aggregates once and index them by ID;
3. scale every Slot selection with the existing `RecipeScaler` and its stored target servings;
4. aggregate all Slot contributions with `IngredientRequirementAggregator`;
5. load current available Inventory once;
6. calculate the result with the existing `ShoppingListCalculator`.

Repeated Recipe selections contribute once per Slot. A request-local `(recipeId, targetServings)` cache may reuse the pure scaled value, but does not deduplicate its contributions. Whole-plan requirements are aggregated before the single Inventory comparison; per-Slot Shopping Previews are never concatenated. This prevents the same physical Inventory from being counted independently against multiple Slots.

The preview is a live derived view. A later legal Inventory change can change the next response, while the MealPlan remains unchanged. Accounting semantics are inherited from Node 10: only positive remaining amounts count; compatible expired and null-expiry batches count; depleted batches do not; ingredient ID and canonical unit must match; there is no cross-dimension conversion or food-safety claim. Only positive shortages appear, and complete coverage returns HTTP 200 with `items: []`.

Complete DRAFT and CONFIRMED plans are previewable. An incomplete DRAFT fails with `MEAL_PLAN_INCOMPLETE`; a CANCELLED plan fails with `MEAL_PLAN_STATE_CONFLICT`; an unknown plan keeps `MEAL_PLAN_NOT_FOUND`. The existing Node 10 ad-hoc endpoint remains unchanged. Node 16 writes no MealPlan, Inventory, InventoryTransaction, reservation, or Shopping state and introduces no V7 migration.

## Reasons

- Persisted selections preserve the plan as the single source of planned Recipe and serving facts.
- Reusing scaling, aggregation, and Shopping calculation prevents divergent arithmetic.
- Whole-plan aggregation prevents Inventory double counting across Slots.
- Reading current Inventory makes the preview useful after legitimate stock changes.
- Keeping the result derived avoids stale Shopping rows and synchronization rules.
- Rejecting incomplete plans avoids presenting a misleading partial whole-plan shortage.

## Alternatives

1. Per-Slot previews followed by concatenation versus whole-plan aggregation before Inventory subtraction.
2. Current `PlanningPreferences.defaultServings` versus stored selection `targetServings`.
3. Generation-time Inventory snapshot versus current live Inventory.
4. Persisting Shopping Lists versus deriving the preview on every request.
5. Allowing an incomplete-plan partial preview versus rejecting incomplete plans.

## Why alternatives were not chosen

Per-Slot subtraction can count the same Inventory more than once and understate shortages. Current default servings would retroactively change a persisted plan. A generation-time snapshot becomes stale and Node 15 intentionally does not persist one. Persisted Shopping Lists require lifecycle and synchronization decisions that are outside this node. Partial previews would be presented as whole-plan results despite missing Slot selections.

## Trade-offs

Each request loads the MealPlan, all Recipes, and current available Inventory, then derives the result in memory. That is acceptable at V1 scale but does not provide a historical shopping snapshot. Current Inventory makes the result fresh but means identical requests at different times can differ. Accounting availability includes expired positive batches and therefore must not be interpreted as food-safe. Complete DRAFT plans can change later, so their preview is advisory rather than immutable.

## Revisit Conditions

Revisit when users need persisted Shopping Lists, purchase/order workflows, historical snapshots, Inventory reservation, plan version coupling, food-safety eligibility, retail package optimization, price/budget data, or Recipe/Inventory volume makes full in-memory derivation material.
