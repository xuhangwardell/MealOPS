# ADR 0018: Deterministic Recipe Candidate Filtering

## Status

Accepted for Node 13; implementation under review.

## Background

MealOps needs a reproducible eligible Recipe set before ranking and multi-meal planning can be designed. Node 11 already persists the single-user Planning Preferences profile. Node 13 is the first use of those persisted preferences, but must remain a hard-constraint filtering slice without ranking, inventory optimization, or MealPlan mutation.

## Decision

Use a pure Java `RecipeCandidateFilter` over all persisted Recipe aggregates and the current persisted Planning Preferences singleton. The only hard constraints are inclusive `maxCookingMinutes` and intersection with `excludedIngredientIds`. `defaultServings` does not affect eligibility. Inventory shortage never disqualifies a Recipe. Eligible candidates are derived, read-only, not persisted, and returned from `GET /api/v1/recipe-candidates`; an empty set is HTTP 200. Candidate representation is ordered by recipe ID solely for deterministic output, not recommendation ranking.

`RecipeRepository.findAll()` loads parents, ingredients, and steps through three batch queries and reconstructs aggregates in memory, avoiding per-Recipe child queries. V1 filtering remains in memory for clarity and unit-testability. No V7 migration is introduced.

## Reasons

- Pure hard-constraint rules are deterministic, independently testable, and database-independent.
- Persisted preferences remain the single source of filtering semantics; request-level overrides cannot bypass them.
- Inventory availability implies possible Shopping work, not Recipe ineligibility.
- Derived candidates avoid stale candidate tables and synchronization logic.
- V1 Recipe volume makes three-query loading and in-memory filtering acceptable.

## Alternatives

1. SQL filtering versus pure Java filtering.
2. Inventory as a hard constraint versus a future ranking signal.
3. Request-level temporary preferences versus persisted preferences.
4. Persisting candidates versus deriving them per request.
5. Returning rejection reasons now versus deferring explainability.

## Trade-offs

In-memory filtering reads complete Recipe aggregates and will not scale indefinitely. Recipe ID ordering provides stability but no relevance ordering. The API does not explain rejected Recipes. These costs keep Node 13 free of premature SQL planner rules, scoring, cache invalidation, and explanation models.

## Revisit Conditions

Revisit when Recipe volume makes full loading material, when ranking or inventory fitness is introduced, when users need rejection explanations, or when the Planner requires snapshot/version semantics. SQL pushdown may then be considered while preserving the pure rule contract.
