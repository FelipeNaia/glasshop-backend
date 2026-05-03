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
