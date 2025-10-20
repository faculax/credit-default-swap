# AI Agent Code Generation Instructions

> **Purpose**: Mandatory rules for AI assistants generating code for this project.

## 🎯 Mission Statement

Generate secure, maintainable, production-ready code that passes all quality gates on first commit.

---

## 🚨 CRITICAL RULES - ALWAYS ENFORCE

### 1. Security-First Mindset

**Every code snippet must be secure by default.**

```java
// When generating ANY logging statement:
logger.info("Processing request for user {}", sanitizeForLog(userId));
// NEVER: logger.info("Processing request for user {}", userId);

// When generating ANY random numbers:
private static final SecureRandom random = new SecureRandom();
// NEVER: Random random = new Random();

// When generating ANY string operations:
if (input.toUpperCase(Locale.ROOT).equals("EXPECTED")) { }
// NEVER: if (input.toUpperCase().equals("EXPECTED")) { }

// When generating ANY exception handlers:
String correlationId = UUID.randomUUID().toString();
logger.error("Error [{}]: {}", correlationId, ex.getMessage(), ex);
return ResponseEntity.status(500).body("Error ref: " + correlationId);
// NEVER: return ResponseEntity.status(500).body(ex.getMessage());
```

### 2. Input Validation - MANDATORY

**Every controller endpoint MUST validate input.**

```java
// Required pattern for all @PostMapping, @PutMapping:
@PostMapping("/api/resource")
public ResponseEntity<Resource> create(
    @Valid @RequestBody ResourceRequest request) {
    // ✅ @Valid triggers validation
}

// All DTOs MUST have constraints:
public class ResourceRequest {
    @NotBlank(message = "Name required")
    @Size(max = 255, message = "Name too long")
    @Pattern(regexp = "^[a-zA-Z0-9-_ ]+$", message = "Invalid characters")
    private String name;
}
```

### 3. Authorization - CONTEXT-AWARE

**Apply authorization appropriate to the service type.**

```java
// ✅ PUBLIC APIs - Document explicitly
/**
 * Public endpoint - no authentication required.
 * Rate limiting applied via gateway.
 */
@GetMapping("/api/public/health")
public ResponseEntity<HealthStatus> getHealth() { }

// ✅ USER-FACING APIs - Require authentication
@RestController
@RequestMapping("/api/portfolios")
@PreAuthorize("hasRole('USER')")
public class PortfolioController { }

// ✅ ADMIN APIs - Require specific roles
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> delete(@PathVariable Long id) { }

// ✅ INTERNAL APIs - Service-to-service authentication
/**
 * Internal service endpoint - authenticated via service token.
 * Security configured in WebSecurityConfig.
 */
@RestController
@RequestMapping("/internal/calculations")
public class InternalCalculationController { }

// ✅ ACTUATOR endpoints - Configured in application.yml
// management.endpoints.web.exposure.include=health,metrics
// management.endpoint.health.show-details=when-authorized
```

**Authorization Decision Matrix:**

| Endpoint Type | Authentication Required | Authorization Pattern | Example |
|--------------|------------------------|----------------------|---------|
| **Public Health/Info** | ❌ No | None (document clearly) | `/api/public/health` |
| **Actuator Endpoints** | ✅ Yes | Configured in application.yml | `/actuator/health` |
| **Internal Service APIs** | ✅ Yes | Service token validation | `/internal/*` |
| **User Data APIs** | ✅ Yes | `@PreAuthorize("hasRole('USER')")` | `/api/portfolios` |
| **Admin Operations** | ✅ Yes | `@PreAuthorize("hasRole('ADMIN')")` | `/api/admin/*` |
| **Resource Ownership** | ✅ Yes | Custom `@PreAuthorize` expression | `@PreAuthorize("@securityService.canAccess(#id)")` |

**When NOT to use @PreAuthorize:**
- Public health check endpoints (must be clearly documented)
- Internal service-to-service calls (use service token validation)
- Actuator endpoints (controlled by application.yml configuration)
- Gateway routing endpoints (authorization at target service)

### 4. Safe Exception Handling - MANDATORY
    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9-_ ]+$")
    private String name;
    
    @NotNull
    @Min(1) @Max(1000)
    private Integer quantity;
}
```

### 3. Endpoint Security - MANDATORY

**Every new controller MUST have security.**

```java
// Template for all new controllers:
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {
    
    private final ResourceService service;
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Resource>> getAll() {
        // Implementation
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> create(
        @Valid @RequestBody ResourceRequest request) {
        // Implementation
    }
}
```

### 4. Logging Best Practices

**Use structured logging with sanitization.**

```java
// Template for all logging:
@Slf4j
public class SomeService {
    
    private String sanitizeForLog(Object obj) {
        return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
    }
    
    public void process(String input) {
        log.info("Processing input: {}", sanitizeForLog(input));
        
        try {
            // logic
            log.debug("Success for input: {}", sanitizeForLog(input));
        } catch (Exception ex) {
            String correlationId = UUID.randomUUID().toString();
            log.error("Error [{}] processing input: {}", 
                correlationId, sanitizeForLog(input), ex);
            throw new ProcessingException("Error ref: " + correlationId, ex);
        }
    }
}
```

---

## 📋 Code Generation Checklist

Before generating ANY code block, verify:

- [ ] **Logging**: Uses `sanitizeForLog()` or safe pattern
- [ ] **Random**: Uses `SecureRandom` (or documented exception)
- [ ] **Strings**: Case operations use `Locale.ROOT`
- [ ] **Exceptions**: Never expose internal details
- [ ] **Validation**: DTOs have `@Valid` and constraints
- [ ] **Security**: Endpoints have `@PreAuthorize`
- [ ] **SQL**: Uses JPA/parameterized queries (no concatenation)
- [ ] **Imports**: Adds all required imports
- [ ] **Lombok**: Uses appropriate annotations (`@Slf4j`, `@RequiredArgsConstructor`)

---

## 🏗️ Service Layer Patterns

### Service Class Template
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {
    
    private final ResourceRepository repository;
    
    private String sanitizeForLog(Object obj) {
        return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
    }
    
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        log.debug("Finding all resources");
        return repository.findAll();
    }
    
    @Transactional
    public Resource create(ResourceRequest request) {
        log.info("Creating resource: {}", sanitizeForLog(request.getName()));
        
        // Validation logic
        validate(request);
        
        // Business logic
        Resource resource = new Resource();
        resource.setName(request.getName());
        
        Resource saved = repository.save(resource);
        log.info("Created resource with ID: {}", saved.getId());
        
        return saved;
    }
    
    private void validate(ResourceRequest request) {
        // Custom validation beyond annotations
        if (repository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Resource already exists");
        }
    }
}
```

### Repository Interface Template
```java
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
    // Use method naming conventions
    boolean existsByName(String name);
    
    List<Resource> findByStatus(String status);
    
    // Use @Query for complex queries
    @Query("SELECT r FROM Resource r WHERE r.createdDate > :date")
    List<Resource> findRecentResources(@Param("date") LocalDate date);
}
```

### Exception Handler Template
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            request.getDescription(false).replaceAll("[\r\n]", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An internal error occurred. Reference: " + correlationId,
            request.getDescription(false).replaceAll("[\r\n]", "")
        );
        
        return ResponseEntity.status(500).body(error);
    }
}
```

---

## 🧪 Test Generation Rules

### Unit Test Template
```java
@SpringBootTest
@Slf4j
class ResourceServiceTest {
    
    @Mock
    private ResourceRepository repository;
    
    @InjectMocks
    private ResourceService service;
    
    @Test
    @DisplayName("Should create resource with valid input")
    void shouldCreateResource() {
        // Given
        ResourceRequest request = new ResourceRequest();
        request.setName("Test Resource");
        
        Resource expected = new Resource();
        expected.setId(1L);
        expected.setName("Test Resource");
        
        when(repository.save(any(Resource.class))).thenReturn(expected);
        
        // When
        Resource result = service.create(request);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Resource", result.getName());
        verify(repository, times(1)).save(any(Resource.class));
    }
    
    @Test
    @DisplayName("Should throw exception for duplicate name")
    void shouldThrowExceptionForDuplicate() {
        // Given
        ResourceRequest request = new ResourceRequest();
        request.setName("Duplicate");
        
        when(repository.existsByName("Duplicate")).thenReturn(true);
        
        // When/Then
        assertThrows(DuplicateResourceException.class, 
            () -> service.create(request));
    }
}
```

### Integration Test Template
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql")
class ResourceControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateResourceViaApi() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("API Test");
        
        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("API Test"));
    }
}
```

---

## 🚫 Anti-Patterns - NEVER GENERATE

```java
// ❌ NEVER: Raw SQL with concatenation
String sql = "SELECT * FROM users WHERE name = '" + name + "'";

// ❌ NEVER: Unvalidated input
@PostMapping("/api/resource")
public ResponseEntity create(@RequestBody Resource resource) {
    // Missing @Valid
}

// ❌ NEVER: Exposing entities directly
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userRepository.findById(id).get());
    // Should use DTO + proper error handling
}

// ❌ NEVER: Catch and ignore
try {
    // code
} catch (Exception e) {
    // ignored - silent failure!
}

// ❌ NEVER: Hardcoded values
String apiKey = "sk_live_1234567890";
String password = "admin123";

// ❌ NEVER: Mutable static fields
public static List<String> cache = new ArrayList<>();

// ❌ NEVER: Empty catch blocks
try {
    dangerousOperation();
} catch (Exception e) {
    // TODO: handle this
}
```

---

## 🎨 Code Style Conventions

### Naming
- Classes: `PascalCase` (ResourceController)
- Methods: `camelCase` (findById)
- Constants: `UPPER_SNAKE_CASE` (MAX_RETRIES)
- Packages: `lowercase` (com.creditdefaultswap.platform)

### Structure
```
service/
├── controller/
│   ├── ResourceController.java
│   └── GlobalExceptionHandler.java
├── service/
│   ├── ResourceService.java
│   └── ValidationService.java
├── repository/
│   └── ResourceRepository.java
├── model/
│   ├── Resource.java
│   └── ResourceStatus.java
├── dto/
│   ├── ResourceRequest.java
│   └── ResourceResponse.java
└── config/
    └── SecurityConfig.java
```

### Lombok Usage
- Use `@RequiredArgsConstructor` instead of constructor injection
- Use `@Slf4j` instead of manual logger creation
- Use `@Data` for DTOs
- Use `@Builder` for complex object creation
- Avoid `@ToString` on entities (can cause LazyInitializationException)

---

## 📦 Dependency Management

### Always Include in New Services
```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## 🔍 Pre-Commit Mental Checklist

Before suggesting code, ask yourself:

1. **Would this pass SpotBugs?** Check for security patterns
2. **Is input validated?** Every user input must be validated
3. **Is output sanitized?** Especially logs and errors
4. **Are tests included?** Unit tests for business logic
5. **Is it documented?** JavaDoc for public methods
6. **Follows patterns?** Matches existing code style
7. **No magic numbers?** Use constants or enums
8. **Error handling?** Proper exception handling

---

## 🎓 When Unsure

If you're uncertain about a security or quality decision:

1. **Choose the most secure option** - Better safe than sorry
2. **Add a TODO comment** - `// TODO: Security review needed for X`
3. **Suggest alternatives** - Present options to the user
4. **Reference documentation** - Link to Spring Security docs, OWASP, etc.
5. **Be explicit** - Tell the user this needs review

---

## 📞 Updates & Maintenance

This document should be updated when:
- New security vulnerabilities are discovered
- Quality gate rules change
- Framework versions are upgraded
- Team coding standards evolve

**Current Version**: 1.0  
**Last Updated**: October 16, 2025  
**Next Review**: January 2026

---

## ✅ Quick Reference Card

```java
// Logging
logger.info("Message: {}", sanitizeForLog(input));

// Random
SecureRandom random = new SecureRandom();

// String Case
input.toUpperCase(Locale.ROOT)

// Validation
@Valid @RequestBody ResourceRequest request

// Security
@PreAuthorize("hasRole('USER')")

// Exception
String id = UUID.randomUUID().toString();
logger.error("Error [{}]", id, ex);
return "Error ref: " + id;

// Query
@Query("SELECT r FROM Resource r WHERE r.id = :id")
```

---

**Remember**: Code quality is not optional. Every line of code reflects on the entire team. Generate code you'd be proud to have reviewed by security experts.
