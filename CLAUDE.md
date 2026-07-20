# Project Conventions

## Money / Prices
- All price fields are integers representing **cents** (e.g., $1.99 → `199`)
- Never use `double` or `float` for prices — floating-point arithmetic causes rounding errors
- Convert to dollars only for display purposes (divide by 100)

## Stack
- Java 17 + Spring Boot 3.2
- MongoDB (via Spring Data)
- Lombok for boilerplate reduction (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Springdoc OpenAPI for API docs

## In-progress feature specs — `ai-development/`
Before starting work on auth, cart, or checkout/payments, read the matching file in
`ai-development/` first — it has the full data model, endpoints, and a checkbox list of
what's done vs. outstanding:
- `ai-development/authentication.md`
- `ai-development/shopping-cart.md`
- `ai-development/payment.md`

When you finish a piece of work described there, tick its checkbox (`- [ ]` → `- [x]`) in
that file so the next session (human or model) knows what's already implemented. If a
detail in one of these files turns out to be wrong or outdated once you've looked at the
real code, update the file rather than leaving it stale. `PLAN.md` also has a short
pointer to these same files.
