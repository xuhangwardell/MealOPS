# ADR 0012: Inventory Batch Foundation

## Status

Accepted for Node 7; inventory mutation semantics remain deferred to Node 8.

## Background

MealOps needs factual inventory state for a canonical ingredient, including remaining quantity and optional expiry date. This slice establishes batch identity without implementing consumption, ledgers, locking, or FEFO deduction.

## Decision

- Track inventory by immutable batch rows, not one aggregate total per ingredient.
- `InventoryBatch` contains only `id`, `ingredientId`, canonical `Quantity remainingQuantity`, and nullable `LocalDate expiresOn`.
- New batches require amount `> 0`; reconstituted existing batches allow amount `>= 0`.
- Domain and database accept only canonical `g`, `ml`, and `piece`; API input may use `kg` and `l` and is converted before construction.
- Expired, current-day, future, and null expiry dates are factual data and are not rejected or automatically flagged.
- Available lists mean `remaining_amount > 0`, ordered by expiry ascending with nulls last, then ingredient and id.
- Node 7 provides create/get/list only. No update, delete, consume, transaction ledger, optimistic locking, or version column.
- PostgreSQL V3 migration owns explicit constraints and the ingredient foreign key without cascade delete.

## Reasons

Batch identity preserves expiry and future consumption traceability. Canonical units keep persistence and domain invariants simple. Keeping zero as a valid reconstituted state prepares Node 8 depletion without allowing zero-value creation. Deterministic ordering provides an FEFO foundation without implementing deduction.

## Alternatives

1. One inventory row per ingredient.
2. Store original `kg`/`l` units instead of canonical base units.
3. Reject expired input or calculate an expired flag now.
4. Force one dimension per ingredient or perform density conversion.
5. Add version/locking and mutation endpoints in this node.

## Why alternatives were not chosen

An ingredient total loses batch expiry identity. Original units would move conversion complexity into every consumer. Expiry is factual data and policy belongs to planning/consumption. A single ingredient may legitimately have mass and count batches; cross-dimension conversion requires ingredient-specific policy. Versioning and mutation require a separate transaction/ledger design and are Node 8 concerns.

## Trade-offs

The model is intentionally small and auditable, but callers cannot yet consume or adjust stock. Canonical conversion is limited to same-dimension units; no density or piece-to-mass conversion exists. The list is deterministic but not itself a consumption algorithm. No timestamps, status, or version are stored yet.

## Revisit Conditions

Revisit when Node 8 defines consumption and inventory ledger semantics, concurrent updates, idempotency, FEFO policy, or when a real requirement needs timestamps, status, optimistic locking, pagination, or ingredient-specific cross-dimension conversion.

## Relationship to Earlier ADRs

This continues ADR 0009 canonical `Quantity` semantics and does not change Node 6 Recipe Scaling DECIMAL128 behavior.
