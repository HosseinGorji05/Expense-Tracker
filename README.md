# Expense Tracker API

[![Build and Test](https://github.com/HosseinGorji05/Expense-Tracker/actions/workflows/test.yml/badge.svg)](https://github.com/HosseinGorji05/Expense-Tracker/actions/workflows/test.yml)

A small REST API for tracking personal expenses — each expense has a description, amount, category, and date. It supports full CRUD plus filtering by category, with request validation and an integration test suite. Two AI endpoints (via Groq) suggest a category for a raw description and build a "where to cut" plan against a monthly budget. A minimal web UI is served from the same app at `/`.

## Tech stack

| Area        | Choice                           |
| ----------- | -------------------------------- |
| Language    | Java 17                          |
| Framework   | Spring Boot 4 (Web MVC)          |
| Persistence | Spring Data JPA + H2 (in-memory) |
| Validation  | Jakarta Bean Validation          |
| AI          | Groq LLM API (categorization + budget cut plan) |
| Testing     | JUnit 5 + Spring MockMvc         |
| Build       | Maven (wrapper included)         |
| Container   | Docker (multi-stage build)       |
| CI          | GitHub Actions                   |

## Run it locally

```bash
# Start the app (http://localhost:8080)
./mvnw spring-boot:run

# Run the test suite
./mvnw test
```

Open <http://localhost:8080/> for the web UI, or hit the API directly:

```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"description":"Groceries","amount":45.50,"category":"Food","date":"2026-09-01"}'

curl http://localhost:8080/api/expenses
```

The H2 web console is at <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:expensedb`, user `sa`, no password). Data is in-memory and resets on every restart.

## Run with Docker

```bash
docker build -t expense-tracker .
docker run -p 8080:8080 expense-tracker
```

## API

Base path: `/api/expenses`

| Method   | Path                         | Description                 | Success | Errors                         |
| -------- | ---------------------------- | --------------------------- | ------- | ------------------------------ |
| `GET`    | `/api/expenses`              | List all expenses           | `200`   | —                              |
| `GET`    | `/api/expenses?category={c}` | List expenses in a category | `200`   | —                              |
| `GET`    | `/api/expenses/{id}`         | Get one expense             | `200`   | `404` if not found             |
| `POST`   | `/api/expenses`              | Create an expense           | `201`   | `400` on validation failure    |
| `PUT`    | `/api/expenses/{id}`         | Replace an expense          | `200`   | `400` invalid, `404` not found |
| `DELETE` | `/api/expenses/{id}`         | Delete an expense           | `204`   | `404` if not found             |
| `POST`   | `/api/expenses/categorize`   | AI-suggest a category for a description | `200` | `400` if description is blank |
| `POST`   | `/api/budget/advice`         | AI plan for where to cut spending this month | `200` | `400` if budget/cut values invalid |

### Expense fields

| Field         | Type                  | Rules                       |
| ------------- | --------------------- | --------------------------- |
| `id`          | number                | assigned by the server      |
| `description` | string                | required, not blank         |
| `amount`      | number                | required, greater than zero |
| `category`    | string                | required, not blank         |
| `date`        | string (`YYYY-MM-DD`) | required                    |

### AI categorization

`POST /api/expenses/categorize` takes a raw description and asks an LLM (Groq,
`openai/gpt-oss-20b`) to classify it into one of a fixed set of categories:
`Food, Transportation, Entertainment, Utilities, Shopping, Health, Housing, Other`.

```bash
curl -X POST http://localhost:8080/api/expenses/categorize \
  -H "Content-Type: application/json" \
  -d '{"description":"Uber Eats order, $23.50"}'
# {"suggestedCategory":"Food","reasoning":"AI-suggested based on description"}
```

The LLM response is validated against the allowed list — anything unexpected, a
timeout, or a missing key all degrade gracefully to `Other`. The API key is read
from the `GROQ_API_KEY` environment variable (never committed):

```bash
export GROQ_API_KEY=your_key_here
```

Without the key the endpoint still works and returns `Other`. In CI the key is a
GitHub Actions secret; the test suite mocks the HTTP call and does not need it.

### Budget cut plan

`POST /api/budget/advice` with a monthly budget and a savings target:

```bash
curl -X POST http://localhost:8080/api/budget/advice \
  -H "Content-Type: application/json" \
  -d '{"monthlyBudget":2000,"cutTarget":300}'
```

It totals **this month's** expenses by category and asks the LLM which categories
to trim and by how much, preferring discretionary spending (Entertainment,
Shopping, Food) over essentials (Housing, Utilities, Health). The response
includes the category breakdown, per-category cut suggestions with reasons, the
projected spend after cuts, and an `aiGenerated` flag. Every LLM suggestion is
clamped to that category's actual spend and dropped if the category isn't real.
If the LLM is unavailable, a deterministic "trim the biggest discretionary
categories" heuristic runs instead (`aiGenerated: false`).

## Project layout

```
src/main/java/com/hosseingorji/expensetracker/
  model/Expense.java              entity + validation constraints
  repository/ExpenseRepository    Spring Data JPA repository
  service/ExpenseService.java     CRUD business logic
  service/GroqClient.java         shared Groq chat-completions wrapper
  service/CategorizationService   category suggestion + validation + fallback
  service/BudgetAdvisorService    monthly cut plan (LLM + heuristic fallback)
  dto/                            request/response shapes for the AI endpoints
  config/AppConfig.java           RestTemplate bean (timeouts)
  controller/ExpenseController    expense CRUD + categorize
  controller/BudgetController     budget advice
src/main/resources/static/index.html   web UI
src/test/java/...                       MockMvc + service tests (LLM calls mocked)
```

## CI

Every push and pull request to `main` runs `./mvnw test` and builds the Docker image via GitHub Actions ([`.github/workflows/test.yml`](.github/workflows/test.yml)).
