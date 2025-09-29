# CDS Trading Platform - Data Flow

## 🔄 Frontend to Backend Integration

### Data Flow Architecture:

```
Frontend (React)
    ↓ HTTP Request
Gateway (Spring Cloud Gateway) :8081
    ↓ Routes /api/** to backend
Backend (Spring Boot) :8080
    ↓ JPA/Hibernate
PostgreSQL Database :5432
```

### API Integration:

1. **Frontend Form Submission:**
   - User fills CDS trade form
   - Form calls `cdsTradeService.createTrade()`
   - Service makes POST to `http://localhost:8081/api/cds-trades`

2. **Gateway Routing:**
   - Gateway receives request on port 8081
   - Routes `/api/**` to backend at `http://localhost:8080`
   - Handles CORS for frontend communication

3. **Backend Processing:**
   - `CDSTradeController` receives POST request
   - `CDSTradeService` processes business logic
   - `CDSTradeRepository` persists to database via JPA
   - Returns saved trade with ID and timestamps

4. **Frontend Response:**
   - Receives `CDSTradeResponse` with backend-generated ID
   - Shows success confirmation modal
   - Displays real trade ID and creation timestamp

### Environment Variables:

- Frontend: `REACT_APP_API_BASE_URL=http://localhost:8081/api`
- Gateway: `BACKEND_URI=http://localhost:8080` 
- Backend: Database connection via `application.yml`

### Database Schema:

The `cds_trades` table stores all trade data with:
- Auto-generated ID (primary key)
- All CDS trade fields from the form
- Created/updated timestamps
- Proper indexes for querying

### Starting the Services:

```bash
# Start all services with Docker Compose
docker-compose up --build

# Or start individually:
# 1. Database: docker-compose up db
# 2. Backend: mvn spring-boot:run (port 8080)  
# 3. Gateway: mvn spring-boot:run (port 8081)
# 4. Frontend: npm start (port 3000)
```

### API Endpoints Available:

- `POST /api/cds-trades` - Create new trade
- `GET /api/cds-trades` - List all trades  
- `GET /api/cds-trades/{id}` - Get specific trade
- `PUT /api/cds-trades/{id}` - Update trade
- `DELETE /api/cds-trades/{id}` - Delete trade
- `GET /api/cds-trades/count` - Get trade count
- Various filtering endpoints by entity, counterparty, status

The integration is now **fully connected** - form submissions will persist to the database and return real backend-generated trade IDs.