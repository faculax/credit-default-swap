# Unit Tests

This directory contains **pure unit tests** that test individual classes or methods in isolation.

## Characteristics

- **No Spring Context** — Tests use `@ExtendWith(MockitoExtension.class)`, not `@SpringBootTest`
- **Mocked Dependencies** — All dependencies are mocked using Mockito or similar
- **Fast Execution** — Each test should run in under 100ms
- **High Coverage** — Unit tests should cover the majority of business logic

## Example

```java
package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.service.CreditEventService;
import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {
    
    @Mock
    private CreditEventRepository repository;
    
    @InjectMocks
    private CreditEventService service;
    
    @Test
    @StoryId(value = "UTS-401", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void shouldCalculateNotionalCorrectly() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(mockEvent));
        
        // Act
        BigDecimal result = service.calculateNotional(1L);
        
        // Assert
        assertEquals(BigDecimal.valueOf(1000000), result);
        verify(repository, times(1)).findById(1L);
    }
}
```

## Structure

Mirror the production package structure:

```
unit/
├── platform/
│   ├── service/
│   │   └── CreditEventServiceTest.java
│   ├── util/
│   │   └── DateUtilsTest.java
│   └── validator/
│       └── NotionalValidatorTest.java
└── testing/
    └── validation/
        └── LabelValidatorTest.java
```

## Running Unit Tests

```bash
# Run all unit tests (default)
mvn test

# Explicitly specify unit tests profile
mvn test -P unit-tests
```

---

**See Also**: [Backend Folder Conventions](../../../../../docs/testing/backend-folder-conventions.md)
