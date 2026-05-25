# ALTEREGO-2026-BE

2026 KUAD Alter Ego archive store backend.

This project is based on the 2025 SOMA store backend and keeps the same API shape:

- `GET /api/store/teams`
- `GET /api/store/items`
- `GET /api/store/items/team/{teamId}`
- `GET /api/store/items/{itemId}`
- `POST /api/store/checkout`
- `GET /api/store/receipts/{receiptId}`
- `POST /api/admin/login`
- `GET /api/admin/receipts`
- `GET /api/admin/receipts/export`

## Local Setup

Create `src/main/resources/application.properties` from `src/main/resources/application.example.properties`.

For quick local testing, the example uses H2 in-memory DB. For production, use MySQL/RDS and set the same properties through the deploy workflow or EC2 file.
