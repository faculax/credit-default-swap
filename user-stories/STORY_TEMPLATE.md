# Story [ID] – [Story Title]

<!-- 
INSTRUCTIONS:
- Replace [ID] with story identifier (e.g., 3.1, 5.2)
- Replace [Story Title] with concise, descriptive title
- Fill in ALL sections below
- Remove these instruction comments before submitting
- See docs/WRITING_CRITERIA.md for best practices
-->

**As a [role]**,  
I want [capability]  
So that [benefit].

<!-- 
EXAMPLE:
**As a credit trader**,
I want to create a single-name CDS trade with all required fields
So that I can accurately record my CDS positions in the system.
-->

## ✅ Acceptance Criteria

<!-- 
INSTRUCTIONS:
- List TESTABLE conditions that must be met
- Use Given/When/Then format when applicable
- Be specific and unambiguous
- Number criteria (AC 1.1, AC 1.2, etc.)
- Each criterion should map to 1+ test scenarios
-->

**AC 1.1:** [Criterion description]
- Given [precondition]
- When [action]
- Then [expected result]

**AC 1.2:** [Criterion description]
- [Specific condition that must be met]

**AC 2.1:** [Error handling criterion]
- Given [invalid state]
- When [action attempted]
- Then [error behavior]

<!-- 
EXAMPLES:

**AC 1.1:** Should create CDS trade with valid data
- Given a user has permissions to create trades
- When they submit a trade with all required fields (reference entity, notional, spread, maturity)
- Then the trade should be persisted with a unique trade ID and return 201 Created

**AC 1.2:** Should validate required fields
- Reference entity must not be empty
- Notional must be positive number
- Spread must be in basis points (bps)
- Maturity must be future date

**AC 2.1:** Should reject trade with missing reference entity
- Given a trade request with missing reference entity
- When the user submits the trade
- Then the system should return 400 Bad Request with error message "Reference entity is required"
-->

## 🧪 Test Scenarios

<!-- 
INSTRUCTIONS:
- List SPECIFIC test cases to validate acceptance criteria
- Number scenarios (1, 2, 3, etc.)
- Use descriptive names
- Use Given/When/Then format
- Map scenarios to acceptance criteria (e.g., "Tests AC 1.1")
-->

### 1. [Scenario Name] (Tests AC X.X)

Given [precondition or initial state]  
When [action or event]  
Then [expected outcome or result]

### 2. [Error Scenario Name] (Tests AC X.X)

Given [invalid or edge case condition]  
When [action attempted]  
Then [error handling behavior]

<!-- 
EXAMPLES:

### 1. Valid Single-Name CDS Trade Creation (Tests AC 1.1)

Given a user is authenticated and has trade creation permissions
And they have valid trade data:
  - Reference Entity: "TESLA INC"
  - Notional: 10,000,000 USD
  - Spread: 150 bps
  - Maturity: 2028-12-20
When they submit the trade via POST /api/trades
Then the system should:
  - Return 201 Created
  - Generate unique trade ID
  - Return trade details in response body
  - Persist trade in database

### 2. Trade Rejection - Missing Reference Entity (Tests AC 2.1)

Given a user submits a trade with:
  - Reference Entity: "" (empty)
  - Notional: 10,000,000 USD
  - Spread: 150 bps
  - Maturity: 2028-12-20
When they submit the trade via POST /api/trades
Then the system should:
  - Return 400 Bad Request
  - Include error message: "Reference entity is required"
  - Not persist any trade data
-->

## 🧱 Services Involved

<!-- 
INSTRUCTIONS:
- Check ALL services this story affects
- Use EXACT values: backend, frontend, gateway, risk-engine
- Remove services that are NOT involved
- See docs/SERVICES_DECISION_MATRIX.md for guidance
-->

- [ ] `backend` - Backend API service (Spring Boot)
- [ ] `frontend` - Frontend UI application (React)
- [ ] `gateway` - API Gateway service
- [ ] `risk-engine` - Risk calculation service

<!-- 
EXAMPLES:

For a simple REST API story:
- [x] `backend` - Backend API service (Spring Boot)

For a UI feature story:
- [x] `backend` - Backend API service (Spring Boot)
- [x] `frontend` - Frontend UI application (React)

For a complete flow:
- [x] `backend` - Backend API service (Spring Boot)
- [x] `frontend` - Frontend UI application (React)
- [x] `gateway` - API Gateway service
-->

## 🛠 Implementation Guidance

<!-- 
INSTRUCTIONS:
- Provide technical hints for developers
- Reference existing code/patterns to follow
- Mention architectural decisions
- List any constraints or considerations
- Optional section - include if helpful
-->

### Backend Implementation

- **Endpoint**: [HTTP method and path]
- **Controller**: [Which controller class]
- **Service**: [Business logic location]
- **Repository**: [Data access layer]
- **Validation**: [Validation rules to implement]

### Frontend Implementation

- **Component**: [Which React component(s)]
- **Form**: [Form fields and validation]
- **API Integration**: [Which API endpoints to call]
- **State Management**: [Redux/Context usage]

### Database

- **Tables**: [Which tables are affected]
- **Migrations**: [Schema changes needed]

<!-- 
EXAMPLE:

### Backend Implementation

- **Endpoint**: POST /api/trades
- **Controller**: CDSTradeController
- **Service**: CDSTradeService (business logic, validation)
- **Repository**: CDSTradeRepository (JPA)
- **Validation**: Use javax.validation annotations (@NotNull, @Positive)

### Frontend Implementation

- **Component**: CDSTradeForm (src/components/trade/CDSTradeForm.tsx)
- **Form**: Use react-hook-form for validation
- **API Integration**: Call POST /api/trades via axios
- **State Management**: Update Redux tradeSlice after successful creation

### Database

- **Tables**: cds_trade
- **Migrations**: V1__create_cds_trade_table.sql (already exists)
-->

## 📦 Deliverables

<!-- 
INSTRUCTIONS:
- List ALL artifacts this story produces
- Include code files, tests, documentation
- Be specific about file locations
-->

- [ ] Backend code:
  - Controller: [path/to/controller]
  - Service: [path/to/service]
  - Repository: [path/to/repository]
  - Tests: [path/to/tests]

- [ ] Frontend code:
  - Component: [path/to/component]
  - Form: [path/to/form]
  - Tests: [path/to/tests]

- [ ] Database:
  - Migration script: [path/to/migration]
  - Schema documentation: [path/to/docs]

- [ ] Tests:
  - Backend unit tests (service, repository)
  - Backend integration tests (controller, end-to-end)
  - Frontend component tests
  - Frontend integration tests
  - Flow tests (if applicable)

- [ ] Documentation:
  - API documentation (Swagger/OpenAPI)
  - User guide updates (if applicable)

<!-- 
EXAMPLE:

- [x] Backend code:
  - Controller: backend/src/main/java/com/cds/platform/trade/CDSTradeController.java
  - Service: backend/src/main/java/com/cds/platform/trade/CDSTradeService.java
  - Repository: backend/src/main/java/com/cds/platform/trade/CDSTradeRepository.java
  - Tests: backend/src/test/java/com/cds/platform/trade/

- [x] Frontend code:
  - Component: frontend/src/components/trade/CDSTradeForm.tsx
  - Tests: frontend/src/__tests__/CDSTradeForm.test.tsx

- [x] Tests:
  - 8 backend unit tests (service validation)
  - 5 backend integration tests (controller endpoints)
  - 6 frontend component tests
  - 2 flow tests (create + retrieve)
-->

## ⏭ Dependencies / Links

<!-- 
INSTRUCTIONS:
- List other stories this depends on
- Link to related documentation
- Reference design documents, Jira tickets, etc.
- Optional section - include if applicable
-->

- **Depends on**: 
  - Story [X.X] - [Title] (must be completed first)
  
- **Related stories**:
  - Story [Y.Y] - [Title] (similar functionality)
  
- **Design docs**:
  - [Link to design document]
  
- **External references**:
  - [Link to ISDA documentation, specs, etc.]

<!-- 
EXAMPLE:

- **Depends on**: 
  - Story 2.1 - Database Schema Setup (must be completed first)
  
- **Related stories**:
  - Story 3.2 - Create CDS Index Trade (similar pattern)
  - Story 3.3 - Update CDS Trade (extends this functionality)
  
- **Design docs**:
  - CDS Trade Data Model: docs/design/cds-trade-model.md
  
- **External references**:
  - ISDA CDS Standard Model: https://www.isda.org/...
-->

---

## 📝 Notes

<!-- 
OPTIONAL SECTION:
- Add any additional context, open questions, or decisions made
- Remove this section if not needed
-->

---

**Epic**: [Epic Number - Epic Name]  
**Story Points**: [Estimate]  
**Priority**: [High/Medium/Low]  
**Author**: [Your Name]  
**Date Created**: [YYYY-MM-DD]  
**Last Updated**: [YYYY-MM-DD]
