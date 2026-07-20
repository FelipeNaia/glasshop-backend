# Plan: Product Features

## Goals

- Products have an ordered list of images (by priority).
- Dedicated `ImageController` to upload images and reorder them.
- `GET` endpoint to retrieve an image.
- `ProductController` returns a paginated, filterable list of products.
- Products support tags (e.g. `male`, `female`).
- Products have a toggleable visibility flag used by the frontend filter.

---

## Data Model

### `ProductImage` (embedded document)
- `id` — unique identifier (to reference when reordering)
- `url` — stored image path/URL
- `priority` — integer (lower = displayed first)

### `Product` (document)
- existing fields...
- `List<ProductImage> images` — default empty list, sorted by `priority` ascending on read
- `List<String> tags` — e.g. `["male", "summer"]`
- `boolean visible` — controls whether the product appears in public listings (default `true`)

---

## Image Controller (`/api/images`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/images/products/{productId}` | Upload an image and attach it to a product |
| `PUT` | `/api/images/products/{productId}/reorder` | Reorder images by submitting an ordered list of image IDs |
| `GET` | `/api/images/{imageId}` | Retrieve/serve a single image by ID |

### Notes
- Upload stores the file (local storage or object storage TBD) and creates a `ProductImage` entry with an auto-assigned priority.
- Reorder accepts a list of image IDs in the desired order; the service reassigns `priority` values accordingly (e.g. index 0 → priority 0, index 1 → priority 1, ...).

---

## Product Controller (`/api/products`)

| Method | Path | Description |
|--------|------|-------------|| 
| `GET` | `/api/products` | Paginated list of products with filters |
| `PATCH` | `/api/products/{id}/visibility` | Toggle product visibility on/off |

### Pagination & Filter Parameters (`GET /api/products`)
- `page`, `size` — standard Spring pagination
- `tags` — filter by one or more tags (e.g. `?tags=male&tags=summer`)
- `visible` — filter by visibility (`true` / `false`); frontend uses this to show only visible products
- Additional filters (e.g. name search, price range) can be added later

### Visibility Toggle
- `PATCH /api/products/{id}/visibility` flips the `visible` boolean.
- The `GET /api/products` endpoint accepts `?visible=true` so the frontend only fetches publicly visible products.

---

## Steps

1. **`ProductImage` class** — embedded document with `id`, `url`, `priority`.
2. **Update `Product` document** — add `images`, `tags`, and `visible` fields.
3. **Update DTOs** — request/response payloads for images, tags, and visibility.
4. **Image storage service** — handle file upload and return a stored URL/path.
5. **`ImageService`** — upload image to product, reorder images, fetch image by ID.
6. **`ImageController`** — wire up upload, reorder, and GET endpoints.
7. **`ProductService`** — paginated query with tag and visibility filters; sort images by priority on read.
8. **`ProductController`** — paginated GET with filters; PATCH visibility toggle.
9. **Tests** — image sort order, reorder logic, pagination filters, visibility toggle.

---

# Plan: Users, Auth, Cart & Checkout

Full detail, data models, endpoint tables, and per-item progress checkboxes live in
`ai-development/` — that's the source of truth for what's implemented vs. outstanding.
This section is just a map of the three tickets and how they relate.

## Goals

- Two user roles: `BUYER` (self-registers, builds a cart, checks out) and `ADMIN`
  (manages the product catalog — creates products, changes prices, uploads photos).
- Stateless JWT authentication (access token + revocable refresh token).
- Persistent per-user shopping cart.
- Checkout via Abacate Pay (PIX) — asynchronous, webhook-confirmed payment.

## Tickets

| Ticket | File | Depends on |
|--------|------|------------|
| Authentication & Authorization | [`ai-development/authentication.md`](ai-development/authentication.md) | — |
| Shopping Cart | [`ai-development/shopping-cart.md`](ai-development/shopping-cart.md) | Authentication |
| Checkout & Payment (Abacate Pay) | [`ai-development/payment.md`](ai-development/payment.md) | Authentication, Shopping Cart |

## Notes
- Existing `ProductController`/`ImageController` write endpoints (create/update product,
  upload/reorder/delete images) become `ADMIN`-only as part of the Authentication ticket;
  read endpoints stay public.
- Explicitly deferred for v1 (see individual tickets for details): forgot/reset password,
  email verification, login rate limiting, stock reservation/oversell protection, refunds,
  abandoned-order cleanup.
