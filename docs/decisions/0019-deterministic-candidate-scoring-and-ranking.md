# ADR 0019: Deterministic Candidate Scoring and Ranking

## Status

Accepted for Node 14; implementation under review.

## Background

Node 13 produces the Recipe set that satisfies persisted hard constraints. MealOps now needs a transparent and reproducible ordering of those eligible candidates before a future Planner can select Recipes or construct MealPlans. The available signals are the persisted `defaultServings`, scaled ingredient requirements, current accounting inventory availability, and estimated cooking time. Amounts from mass, volume, and count dimensions cannot be summed into one meaningful shortage quantity.

## Decision

Node 14 ranks only Node 13 eligible candidates. For every eligible Recipe, it reuses `RecipeScaler` with the persisted `PlanningPreferences.defaultServings`, `IngredientRequirementAggregator`, and the pure `ShoppingListCalculator` over one request-level `InventoryBatchRepository.findAvailable()` result.

The transparent scorecard contains `inventoryCoverageScore` and `shortageIngredientCount`, without a composite total. For each requirement line:

```text
lineCoverage = min(available / required, 1)
inventoryCoverageScore = sum(lineCoverage) / requirementCount
```

Each requirement line has equal weight, so mass, volume, and count amounts are never added across dimensions. A line contributes to `shortageIngredientCount` when compatible accounting availability is less than its requirement. Arithmetic uses `BigDecimal`, exact division when possible, and `MathContext.DECIMAL128` for non-terminating results.

The strict lexicographic ranking policy is:

1. inventory coverage descending;
2. shortage ingredient count ascending;
3. estimated minutes ascending;
4. recipe ID ascending.

Ranks are consecutive and one-based. Recipe ID is only the final deterministic tie-break. The derived ranking is exposed through `GET /api/v1/recipe-candidate-rankings`, is not persisted, and does not mutate Inventory or MealPlan. Empty rankings return HTTP 200. No V7 migration is introduced.

Accounting availability preserves Node 10 semantics: positive remaining batches with the same ingredient and canonical unit count; expired and null-expiry batches count; depleted batches do not; there is no cross-dimension conversion or food-safety interpretation.

## Reasons

- Reusing the Node 13 filter prevents a second eligibility policy.
- Reusing scaling, requirement aggregation, and Shopping accounting keeps one mathematical source for each rule.
- Normalized per-line coverage is dimensionless and comparable across Recipes.
- Lexicographic ordering is inspectable and avoids unsupported arbitrary weights.
- Loading Recipes and Inventory once avoids candidate-level database N+1 reads.
- Deriving the current ranking avoids stale score snapshots and cache invalidation.

## Alternatives

1. Weighted composite score versus lexicographic ranking.
2. Raw shortage amount sum versus normalized per-requirement coverage.
3. Inventory as a hard filter versus a soft ranking signal.
4. Persisted rankings versus request-time derived rankings.
5. Expiry penalty now versus deferring food-safety policy.

## Why alternatives were not chosen

A weighted total would encode product preferences that have not been validated and would obscure why one Recipe wins. Raw shortage sums mix grams, millilitres, and pieces. Inventory shortage does not make a Recipe ineligible because Shopping can fill it. Persisted rankings would immediately become stale when preferences, Recipes, or Inventory change. Expiry penalties would incorrectly merge accounting availability with a food-safety policy that has not been designed.

## Trade-offs

The scorecard and comparator are deterministic and explainable, but equal-weight requirement lines are a deliberately simple V1 policy. Every ranking request loads all Recipes and available inventory and calculates scores in memory. Expired inventory can improve accounting coverage even though it must not be interpreted as safe to consume. No price, retail package, nutrition, waste, MealType, or multi-meal interaction is represented.

## Revisit Conditions

Revisit when product evidence supports weights or a different ordering policy, Recipe/Inventory volume makes in-memory derivation material, food-safety usability is explicitly designed, price/package data becomes available, ranking snapshots are required, or the Planner needs multi-meal optimization and explanation semantics.
