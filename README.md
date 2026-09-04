# Expense Tracker API

[![Build and Test](https://github.com/HosseinGorji05/Expense-Tracker/actions/workflows/test.yml/badge.svg)](https://github.com/HosseinGorji05/Expense-Tracker/actions/workflows/test.yml)

A small REST API for tracking personal expenses — each expense has a description, amount, category, and date. It supports full CRUD plus filtering by category, with request validation and an integration test suite. A minimal web UI is served from the same app at `/`.

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4 (Web MVC) |
| Persistence | Spring Data JPA + H2 (in-memory) |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5 + Spring MockMvc |
| Build | Maven (wrapper included) |
| Container | Docker (multi-stage build) |
| CI | GitHub Actions |

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

| Method | Path | Description | Success | Errors |
|---|---|---|---|---|
| `GET` | `/api/expenses` | List all expenses | `200` | — |
| `GET` | `/api/expenses?category={c}` | List expenses in a category | `200` | — |
| `GET` | `/api/expenses/{id}` | Get one expense | `200` | `404` if not found |
| `POST` | `/api/expenses` | Create an expense | `201` | `400` on validation failure |
| `PUT` | `/api/expenses/{id}` | Replace an expense | `200` | `400` invalid, `404` not found |
| `DELETE` | `/api/expenses/{id}` | Delete an expense | `204` | `404` if not found |

### Expense fields

| Field | Type | Rules |
|---|---|---|
| `id` | number | assigned by the server |
| `description` | string | required, not blank |
| `amount` | number | required, greater than zero |
| `category` | string | required, not blank |
| `date` | string (`YYYY-MM-DD`) | required |

## Project layout

```
src/main/java/com/hosseingorji/expensetracker/
  model/Expense.java            entity + validation constraints
  repository/ExpenseRepository  Spring Data JPA repository
  service/ExpenseService.java   business logic
  controller/ExpenseController  REST endpoints
src/main/resources/static/index.html   web UI
src/test/java/.../ExpenseControllerTest.java   MockMvc integration tests
```

## CI

Every push and pull request to `main` runs `./mvnw test` and builds the Docker image via GitHub Actions ([`.github/workflows/test.yml`](.github/workflows/test.yml)).
