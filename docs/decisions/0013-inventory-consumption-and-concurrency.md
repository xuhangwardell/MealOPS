# ADR 0013: Inventory Consumption and Concurrency

## Status
Accepted for Node 8.

## Background
Consumption must select compatible batches deterministically, avoid oversubtraction, preserve an immutable ledger, and remain safe under concurrent requests.

## Decision

- Use a pure FEFO allocator ordered by `expiresOn ASC NULLS LAST, id ASC`.
- Only the same ingredient and canonical unit are compatible; no density or cross-dimension conversion.
- Build and validate the complete plan before mutation. Insufficient inventory returns `409 INSUFFICIENT_INVENTORY` with no changes.
- Add a monotonic batch `version` and update with compare-and-set (`id + expected version + unit + remaining >= amount`). A zero affected-row update returns `409 INVENTORY_CONCURRENT_MODIFICATION`; no automatic retry is performed.
- Persist a `CONSUME` transaction header and allocation rows in the same database transaction as batch CAS updates.
- Allocation rows record position, batch, expected version, amount, before and after values; database arithmetic constraints enforce `before = amount + after`.
- Expiry is accounting order only; expired batches are not automatically filtered.
- Idempotency, ETag/If-Match, adjustment/receive/discard, and transaction history APIs remain deferred.

## Reasons

Pure allocation is deterministic and independently testable. Optimistic CAS avoids holding database locks while still preventing lost updates. One transaction keeps batch state and ledger atomic, while the immutable ledger gives an auditable consumption record without introducing event sourcing.

## Alternatives

- Aggregate inventory per ingredient;
- `SELECT FOR UPDATE` pessimistic locking;
- silent automatic retry;
- no ledger or one ledger row per batch;
- automatic expired filtering;
- idempotency in this node.

## Why alternatives were not chosen

Aggregate totals lose batch expiry identity. Pessimistic locks increase contention and are unnecessary for the current bounded use case. Silent retries can change user-visible outcomes and are deferred until measured contention exists. A header plus allocations models one use case cleanly; event sourcing is excessive. Expiry policy is not the accounting layer. Idempotency requires a separate request identity and replay design.

## Trade-offs

Concurrent conflicts surface as `409` and callers must decide what to do. A repeated POST may consume twice because idempotency is intentionally absent. The ledger is consumption-only, not a complete event-sourced history. Canonical-unit compatibility prevents convenient but unsafe cross-dimension conversions.

## Revisit Conditions

Revisit after real contention measurements, client retry requirements, transaction history needs, or when adjustment/receive/discard and idempotency semantics are approved.
