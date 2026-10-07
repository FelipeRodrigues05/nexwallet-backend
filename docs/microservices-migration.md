# Microservices migration

## Current deployable boundary

The repository now produces one application image that can run in three explicit roles:

| Role                 | Responsibility                                                                | RabbitMQ input                                          |
| -------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------- |
| `api`                | HTTP API, scheduled jobs, command publishing, response handling               | `transaction.response.queue`, `transfer.response.queue` |
| `transaction-worker` | Apply queued credits/debits and publish completion                            | `transaction.queue`                                     |
| `transfer-worker`    | Turn a transfer into debit/credit transaction commands and publish completion | `transfer.queue`                                        |

Set the role with `NEXWALLET_RUNTIME_ROLE`. `docker compose up --build` starts all
three processes. Worker roles are non-web processes and do not run scheduled jobs or
client seeders. The old `domain` component tree, including its competing listeners, is
excluded from application scanning and must not run alongside the current workers.

This is a transitional deployment boundary. The processes still use the same schema
and artifact because the current JPA model contains cross-package object relationships.
It removes competing queue consumers and establishes independent scaling before data
ownership is split.

## Rules for the target architecture

Each deployable service owns its database and Flyway history. Other services keep IDs,
not JPA relationships to foreign aggregates. Cross-service state changes use RabbitMQ;
queries that need an immediate answer use versioned HTTP APIs. Events use a stable
envelope containing `eventId`, `eventType`, `version`, `occurredAt`, `correlationId`,
`causationId`, and `payload`. Consumers must be idempotent and keep processed event IDs.
Publishing and aggregate changes must use an outbox transaction. Failed messages go to
service-specific dead-letter queues and are retried with bounded backoff.

RabbitMQ is the chosen broker because it is already part of the project. Kafka should
only replace it after a measured need for replayable, high-volume event streams; running
both brokers during decomposition would add operational cost without fixing boundaries.

## Target services and contracts

| Current module | Target deployable     | Owned data                                        | Primary messages                                                          |
| -------------- | --------------------- | ------------------------------------------------- | ------------------------------------------------------------------------- |
| `auth`         | `auth-service`        | OAuth clients, credentials/tokens                 | `auth.client-created`, `auth.credential-changed`                          |
| `user`         | `user-service`        | User profile and identity                         | `user.created`, `user.updated`                                            |
| `twofa`        | `twofa-service`       | OTP and trusted devices                           | `twofa.challenge-created`, `twofa.verified`                               |
| `account`      | `account-service`     | Accounts, balances, balance ledger                | `account.opened`, `account.funds-reserved`, `account.balance-changed`     |
| `transaction`  | `transaction-service` | Financial transaction journal                     | `transaction.requested`, `transaction.completed`, `transaction.failed`    |
| `transfer`     | `transfer-service`    | Transfer lifecycle/saga                           | `transfer.requested`, `transfer.completed`, `transfer.failed`             |
| `pix`          | `pix-service`         | PIX keys, limits, contacts, PIX operations        | `pix.requested`, `pix.completed`, `pix.refunded`                          |
| `boleto`       | `boleto-service`      | Boleto issue/payment lifecycle                    | `boleto.issued`, `boleto.payment-requested`, `boleto.paid`                |
| `card`         | `card-service`        | Cards, bills, installments, chargebacks, cashback | `card.purchase-requested`, `card.purchase-approved`, `card.bill-closed`   |
| `savings`      | `savings-service`     | Savings boxes and movements                       | `savings.deposit-requested`, `savings.withdraw-requested`                 |
| `investment`   | `investment-service`  | Product catalog, positions, earnings              | `investment.applied`, `investment.redeemed`, `investment.earnings-posted` |
| `loan`         | `loan-service`        | Loans and installments                            | `loan.approved`, `loan.disbursement-requested`, `loan.installment-paid`   |
| `limit`        | `limit-service`       | Operational limits and usage                      | `limit.reservation-requested`, `limit.reserved`, `limit.released`         |
| `compliance`   | `compliance-service`  | KYC/AML cases and credit assessments              | `compliance.kyc-completed`, `compliance.aml-flagged`                      |
| `openfinance`  | `openfinance-service` | Consents and provider connections                 | `openfinance.consent-authorized`, `openfinance.consent-revoked`           |
| `scheduling`   | `scheduling-service`  | Scheduled payment definitions/runs                | `schedule.due`, `schedule.executed`, `schedule.failed`                    |
| `audit`        | `audit-service`       | Immutable audit records                           | consumes all domain events; publishes `audit.recorded` only when needed   |

`common` and `shared` are not business services and must not own runtime data. They become
small versioned libraries containing only event envelopes, observability conventions,
and generic error types. Business DTOs and entities must stay in their owning service.
The duplicate `domain` tree is legacy code and is removed after endpoint parity is
verified; it does not become another service.

## Extraction sequence

1. **Stabilize messaging.** The first slice implements independent API/worker roles and
   removes duplicate consumers. The next slice adds event envelopes, idempotency,
   publisher confirms, and an outbox.
2. **Identity and controls.** Extract `auth`, `user`, `twofa`, `compliance`, and `audit`.
   Put JWT validation at an API gateway and pass an authenticated subject downstream.
3. **Ledger core.** Make `account` the balance owner, then give `transaction`, `transfer`,
   and `limit` their own schemas. Replace entity parameters with IDs and implement the
   transfer reservation/commit/compensation saga.
4. **Payment rails.** Extract `pix`, `boleto`, and `scheduling`. Scheduled work emits a
   command to the owning payment service instead of calling its Java service class.
5. **Banking products.** Extract `card`, `savings`, `investment`, and `loan`; all money
   movement goes through account/transaction contracts. Extract `openfinance` last
   because it reads several projections.
6. **Remove the monolith shell.** Route all public APIs through the gateway, remove the
   legacy `domain` package and shared schema, and deploy each service independently.

An extraction is complete only when the service builds and migrates independently,
has no imports from another business service, owns its schema credentials, exposes
health/metrics, has contract and failure-path tests, and can be deployed without
restarting any other service.
