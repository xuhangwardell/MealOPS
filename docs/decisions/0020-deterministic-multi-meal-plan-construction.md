# ADR 0020: Deterministic Multi-Meal Plan Construction

## Status

Accepted for Node 15; implementation under review.

## Background

Node 12 provides the persistent MealPlan and MealSlot lifecycle, Node 13 derives Recipes that satisfy persisted hard constraints, and Node 14 supplies transparent inventory-aware scoring and deterministic ranking. MealOps now needs the first end-to-end construction use case for a 1–3 day, multi-slot MealPlan. The result must be reproducible, persist through the existing aggregate boundary, and account for ingredients already allocated to earlier planned meals without mutating real Inventory.

## Decision

Node 15 is MealOps' first actual Planner and uses a deterministic greedy policy. A generation request contains only `startDate`, `endDate`, and a non-empty unique set of `mealTypes`. The application builds Slots in date ascending order and MealType business order, reads the persisted singleton Planning Preferences, loads all Recipes once, applies the Node 13 hard filter, loads available Inventory once, and creates an immutable in-memory accounting snapshot.

Each eligible Recipe is scaled once to `defaultServings`, and its ingredient requirements are aggregated once before slot planning begins. For every Slot, the planner:

1. scores all eligible candidates against the current accounting snapshot using the Node 14 scorecard;
2. applies the Node 14 deterministic comparator;
3. selects rank 1;
4. assigns that Recipe and `defaultServings` to the Slot;
5. deducts its requirements from a new snapshot, saturating each compatible ingredient/unit balance at zero.

Shortage never disqualifies a Recipe, and repetition is allowed because no diversity policy exists yet. The snapshot never simulates purchased stock: an unmet amount remains a shortage and does not become later availability. Expired and null-expiry positive batches retain the inherited accounting-availability semantics, which makes no food-safety claim. The completed schedule is persisted as one DRAFT through the existing MealPlan repository and requires the existing explicit confirm operation. Generation is exposed as `POST /api/v1/meal-plans/generate`, returns HTTP 201 with a Location header, and introduces no V7 migration.

If the hard filter produces no eligible Recipe, generation fails with HTTP 409 and stable code `MEAL_PLAN_NO_ELIGIBLE_RECIPE` before any MealPlan row is written. Real Inventory quantities, versions, reservations, consumption ledgers, and Shopping state are never changed.

## Reasons

- Reusing Node 13 and Node 14 keeps hard constraints and ranking in one source of truth.
- Loading Preferences, Recipes, and Inventory once bounds database reads per generation request.
- Precomputing scaled requirements prevents repeated scaling and aggregation for every Slot.
- An immutable rolling snapshot models cross-meal accounting consumption without creating false Inventory facts.
- Greedy construction is deterministic, easy to test, and sufficient to close the first multi-meal vertical slice.
- Reusing existing MealPlan persistence preserves the aggregate and lifecycle boundary.

## Alternatives

1. Static ranking once versus rolling re-ranking after each Slot.
2. A Planner-specific ranking policy versus reusing the Node 14 scorer and ranker.
3. Global combinatorial optimization versus greedy slot-by-slot construction.
4. Beam search or backtracking versus selecting the current rank 1.
5. Persistent Inventory reservation versus an ephemeral accounting simulation.
6. Re-scoring against unchanged Inventory versus rolling deduction after each selection.
7. Disallowing shortages versus allowing Shopping to satisfy future gaps.
8. Enforcing an anti-repeat rule versus having no unvalidated diversity policy.
9. MealType-specific suitability versus treating all eligible Recipes equally.
10. Persisting planning snapshots or candidate scores versus persisting only the resulting MealPlan.
11. Auto-confirming generated plans versus creating a reviewable DRAFT.
12. New planner tables and V7 versus reusing the existing MealPlan schema.

## Why alternatives were not chosen

A static ranking would double-count the same availability across Slots, while rolling re-ranking reflects earlier simulated allocations. A Planner-specific comparator would duplicate Node 14 policy and allow API ranking and planning to diverge. Global optimization, beam search, and backtracking require an objective function and search-budget decisions not yet validated by product evidence. Real Inventory mutation or reservation would incorrectly turn a proposal into an operational stock event. Using unchanged Inventory for every Slot would overstate coverage, while shortage as a hard filter would contradict the Shopping boundary. Diversity and MealType suitability have no current domain contracts. Persisted snapshots and scores would become stale and add schema without a current retrieval use case. Auto-confirm would remove the user's explicit review boundary. The existing MealPlan schema already stores the complete generated result.

## Trade-offs

The greedy result is explainable and stable but is not globally optimal and may repeat the same Recipe. Earlier Slots can consume accounting availability that might have produced a better overall combination for later Slots. The snapshot uses accounting availability, not a food-safety guarantee, and does not model price, retail package size, waste, expiry penalty, nutrition, MealType suitability, or user diversity preferences. A generation request produces one plan, not multiple alternatives. Lasting Inventory effects occur only through future explicit workflows, not generation. Because no reservation is created, concurrent or separate generation requests may reuse the same physical Inventory when constructing their independent proposals.

## Revisit Conditions

Revisit when product requirements demand multiple plan alternatives, global objectives, diversity rules, MealType suitability, food-safety policy, price/package optimization, inventory reservation, or planning explainability. Also revisit if Recipe or slot volume makes in-memory re-scoring material, or if persisted planning snapshots are required for audit or resume semantics.
