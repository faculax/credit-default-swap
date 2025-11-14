# Integration Tests

This directory contains **integration tests** that test component interactions with real Spring beans and infrastructure.

## Characteristics

- **Spring Context** — Tests use `@SpringBootTest`, `@DataJpaTest`, or `@WebMvcTest`
- **Real or Embedded Infrastructure** — Tests use embedded H2 database, TestContainers, or mocked external services
- **Slower Execution** — Tests may take 100ms-2s due to Spring context initialization
- **Component Integration** — Tests verify that multiple components work together correctly

## Example

```java
package com.creditdefaultswap.integration.platform.service;

import com.creditdefaultswap.platform.service.CreditEventService;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreditEventServiceIntegrationTest {
    
    @Autowired
    private CreditEventService service;
    
    @Autowired
    private CreditEventRepository repository;
    
    @Test
    @StoryId(value = "UTS-402", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldPersistCreditEventToDatabase() {
        // Arrange
        CreateCreditEventRequest request = new CreateCreditEventRequest();
        request.setEventType(CreditEventType.BANKRUPTCY);
        
        // Act
        CreditEventResponse response = service.recordCreditEvent(1L, request);
        
        // Assert
        assertNotNull(response.getId());
        assertTrue(repository.findById(response.getId()).isPresent());
    }
}
```

## Structure

Mirror the production package structure:

```
integration/
├── platform/
│   ├── controller/
│   │   └── CreditEventControllerIntegrationTest.java
│   ├── repository/
│   │   └── CreditEventRepositoryIntegrationTest.java
│   └── service/
│       └── CashSettlementServiceIntegrationTest.java
└── testing/
    └── infrastructure/
        └── DatabaseIntegrationTest.java
```

## Running Integration Tests

```bash
# Run only integration tests
mvn test -P integration-tests

# Run all tests (unit + integration)
mvn test -P all-tests
```

## Tips

- Use `@Transactional` to automatically roll back database changes after each test
- Use `@ActiveProfiles("test")` to load test-specific configuration
- Consider using `@DirtiesContext` if tests modify shared state
- Use `@TestPropertySource` to override application properties for tests

---

**See Also**: [Backend Folder Conventions](../../../../../docs/testing/backend-folder-conventions.md)
