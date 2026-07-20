# Feature: Shopping Cart

Every authenticated `BUYER` (or `ADMIN`, no restriction) has exactly one persistent cart,
stored server-side, keyed to their user id. It survives logout/login and device switches.
Prices follow the project convention: integer cents everywhere, no floats.

**Depends on:** [authentication](authentication.md) — cart endpoints require a valid JWT.

**Out of scope for v1:** guest/anonymous carts, merging a guest cart into an account on login, multiple saved carts per user.

---

## Data Model

### `Cart` (document, collection `carts`)
- `id`
- `userId` — unique index (one cart per user)
- `items` — `List<CartItem>`
- `updatedAt`

### `CartItem` (embedded)
- `productId`
- `quantity` — integer, must be > 0

Cart line prices and totals are **not** stored on the cart — always compute them from the
live `Product.price` at read/checkout time, so a later price change doesn't silently
affect an item already sitting in someone's cart from before the change.

---

## Endpoints (`/api/cart`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/cart` | Returns the caller's cart, with each line enriched with current product name/price/image and a computed total, plus a cart-level total. Creates an empty cart on first access if none exists. |
| `POST` | `/api/cart/items` | Add a product to the cart (`productId`, `quantity`). If the product is already in the cart, increases its quantity instead of duplicating the line. |
| `PATCH` | `/api/cart/items/{productId}` | Set a line's quantity directly. Quantity `0` or below removes the line. |
| `DELETE` | `/api/cart/items/{productId}` | Remove a single line from the cart. |
| `DELETE` | `/api/cart` | Empty the entire cart (e.g. after a successful checkout, or a manual "clear cart"). |

## Validation rules
- Product must exist and be `visible` to be added.
- Quantity must be a positive integer.
- Stock is **not reserved** when added to cart (matches the [payment](payment.md) plan's decision to skip stock reservation for v1) — availability is only meaningfully checked at checkout time, and even then only as a soft check (see payment doc). A cart is allowed to temporarily hold more than what's in stock.

---

## Checklist

- [ ] `Cart` document + `CartItem` embedded class
- [ ] `CartRepository` (find by `userId`)
- [ ] `CartService`: get-or-create cart for user, add item (merge quantity if present), update quantity, remove item, clear cart
- [ ] Cart response DTO: enrich each line with live product name/price/image, computed line total, computed cart total (all in cents)
- [ ] `CartController`: `GET /api/cart`
- [ ] `CartController`: `POST /api/cart/items`
- [ ] `CartController`: `PATCH /api/cart/items/{productId}`
- [ ] `CartController`: `DELETE /api/cart/items/{productId}`
- [ ] `CartController`: `DELETE /api/cart`
- [ ] Reject adding a non-existent or non-visible product (404/400)
- [ ] Reject non-positive quantities on add/update (400)
- [ ] Tests: add merges quantity on duplicate add, update to 0 removes line, totals computed correctly in cents, cart is user-scoped (one user can't see/modify another's cart)
