# Feature: Checkout & Payment (Abacate Pay / PIX)

Checkout turns a buyer's cart into an `Order`, and payment is collected via
[Abacate Pay](https://abacatepay.com), a Brazilian PIX-focused payment provider. PIX is
asynchronous: the buyer gets a QR code / copy-paste code, and confirmation arrives later
via webhook — there is no synchronous "charge succeeded" response like a card payment.

**Depends on:** [authentication](authentication.md) (checkout requires a logged-in buyer),
[shopping-cart](shopping-cart.md) (checkout consumes the caller's cart).

**⚠️ Before implementing:** nobody on this project has integrated Abacate Pay's API yet.
Pull their actual docs first to confirm: auth scheme (API key header?), the "create PIX
charge" request/response shape, webhook payload structure, and — critically — how to
verify a webhook actually came from Abacate Pay (signature header, shared secret, IP
allowlist, etc.). Everything below is the intended shape but names/fields will need
adjusting once the real API is in front of us.

**Out of scope for v1:** card payments, refunds, stock reservation / oversell
protection, cleanup of abandoned `PENDING` orders, retry logic for failed webhook
delivery.

---

## Data Model

### `Order` (document, collection `orders`)
- `id`
- `userId`
- `items` — `List<OrderItem>`, a **snapshot** taken at checkout time (name + unit price at
  time of purchase), independent of the live `Product` so later price/name changes don't
  rewrite history
- `totalCents` — integer, sum of line totals
- `status` — enum `PENDING` | `PAID` | `FAILED` | `CANCELLED`
- `abacatePaymentId` — external charge/payment id returned by Abacate Pay
- `pixQrCode` / `pixCopyPaste` — data returned by Abacate Pay to present to the buyer
- `createdAt`, `paidAt`

### `OrderItem` (embedded)
- `productId`, `productName`, `unitPriceCents`, `quantity`

---

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/checkout` | authenticated buyer | Reads the caller's cart, rejects if empty, creates a `PENDING` `Order` (price snapshot from live products), calls Abacate Pay to create a PIX charge, stores the returned QR/copy-paste + external id, returns order + payment info to the client. |
| `POST` | `/api/webhooks/abacate-pay` | **public**, signature-verified | Receives payment status updates from Abacate Pay. On success: verify signature, look up `Order` by `abacatePaymentId`, mark `PAID`, set `paidAt`, decrement `Product.stockQuantity` for each line. On failure: mark `FAILED`. Must be idempotent — a webhook may be delivered more than once. |
| `GET` | `/api/orders` | authenticated | List the caller's own orders. |
| `GET` | `/api/orders/{id}` | authenticated | Fetch one order — must belong to the caller, or caller is `ADMIN`. |

## Key decisions already made
- No stock reservation: stock is only decremented once an order is confirmed `PAID`, not when the `PENDING` order/charge is created. Accepted tradeoff: possible overselling between two buyers checking out the same low-stock item concurrently — explicitly deferred.
- No cleanup job for abandoned `PENDING` orders (buyer generates a PIX code and never pays) — deferred.
- Cart is left untouched by `POST /api/checkout` until payment actually confirms; clearing the cart happens when the webhook marks the order `PAID` (avoids wiping a cart for a payment that never completes).
- The webhook endpoint must be excluded from the JWT auth requirement (`permitAll` in the security filter chain) but still protected by verifying Abacate Pay's signature — do not leave it open with no verification at all.

## Idempotency requirement
Before applying a `PAID` transition and decrementing stock, check the order isn't
already `PAID` — Abacate Pay (like most webhook-based providers) may redeliver the same
event.

---

## Checklist

- [ ] Read Abacate Pay's API docs; confirm auth scheme, create-charge request/response shape, webhook payload shape, and webhook signature verification method
- [ ] Add Abacate Pay API key/config to `application.properties` (env-var backed, not committed)
- [ ] `Order` document + `OrderItem` embedded class
- [ ] `OrderRepository` (find by `userId`, find by `abacatePaymentId`)
- [ ] `AbacatePayClient` service wrapping the provider's HTTP API (create PIX charge)
- [ ] `CheckoutService`: build order from cart snapshot, reject empty cart, call `AbacatePayClient`, persist `PENDING` order
- [ ] `POST /api/checkout` endpoint
- [ ] Webhook signature verification logic
- [ ] `POST /api/webhooks/abacate-pay` endpoint, `permitAll` in security config, idempotent PAID/FAILED handling
- [ ] Stock decrement on `PAID` transition
- [ ] Clear the buyer's cart on `PAID` transition
- [ ] `GET /api/orders`, `GET /api/orders/{id}` with ownership check
- [ ] Tests: checkout rejects empty cart, order snapshot is immune to later product price changes, webhook is idempotent (processing the same event twice doesn't double-decrement stock), non-owner can't read another user's order
