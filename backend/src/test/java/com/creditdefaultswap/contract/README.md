# Contract Tests

This directory contains **contract tests** that verify API contracts between microservices.

## Characteristics

- **Consumer-Driven Contracts** — Contracts are defined by the consumer and verified by the provider
- **API Boundary Testing** — Tests verify HTTP API contracts, message formats, and service interactions
- **Framework** — Typically use Spring Cloud Contract or Pact
- **Provider-Side** — These tests run in the provider service (e.g., cds-platform provides contracts to gateway)

## Example (Spring Cloud Contract)

```java
package com.creditdefaultswap.contract.platform.api;

import com.creditdefaultswap.platform.testing.story.StoryId;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
class CreditEventApiContractTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
    }
    
    @Test
    @StoryId(value = "UTS-403", testType = StoryId.TestType.CONTRACT, microservice = "cds-platform")
    void shouldReturnCreditEventById() {
        // Contract tests are typically auto-generated from contract definitions
        // This is a placeholder for manual contract verification
    }
}
```

## Structure

Organize by API or service boundary:

```
contract/
└── platform/
    └── api/
        ├── CreditEventApiContractTest.java
        ├── TradeApiContractTest.java
        └── SettlementApiContractTest.java
```

## Running Contract Tests

```bash
# Run only contract tests
mvn test -P contract-tests

# Run all tests including contracts
mvn test -P all-tests
```

## Contract Definition

Contracts are typically defined in `src/test/resources/contracts/`:

```groovy
// src/test/resources/contracts/credit-events/shouldReturnCreditEventById.groovy
Contract.make {
    description "should return credit event by ID"
    request {
        method GET()
        url "/api/credit-events/1"
    }
    response {
        status 200
        body([
            id: 1,
            eventType: "BANKRUPTCY",
            eventDate: "2024-01-01"
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
```

## Tips

- **Consumer-Driven** — Consumers define the contract, providers verify it
- **Version Contracts** — Use semantic versioning for contract definitions
- **Stub Generation** — Spring Cloud Contract can generate stubs for consumers to use
- **CI Integration** — Run contract tests on every build to catch breaking changes early

---

**See Also**: 
- [Backend Folder Conventions](../../../../../docs/testing/backend-folder-conventions.md)
- [Spring Cloud Contract Documentation](https://spring.io/projects/spring-cloud-contract)
