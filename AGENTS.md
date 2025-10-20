
---

# 🤖 Agents Guide

Welcome to the **Agents’ Collective** — our space for collaborative, focused, and fun coding sessions.
This document sets the tone and gives you the essentials to dive in quickly.

---

## 🎨 Application Look & Feel
- **Fonts**: Arial, Georgia  
- **Colours**:  
  - RGB(255, 255, 255)  
  - RGB(0, 240, 0)  
  - RGB(60, 75, 97)  
  - RGB(0, 232, 247)  
  - RGB(30, 230, 190)  
  - RGB(0, 255, 195)  

---

## 🌱 Vibes

* **Collaborate > Isolate** → ask, share, pair.
* **Consistency > Creativity (in scaffolding)** → when in doubt, follow the patterns.
* **Flow > Formality** → small iterations, working demos, quick feedback.
* **Simplicity > Cleverness** → clean, understandable solutions win.

---

## 🏗️ Service Architecture

We’re building **Spring Boot Java services**, stitched together with **Postgres** for persistence.
Each service lives as its own folder/module under the root project.

* **Framework**: Spring Boot
* **Database**: PostgreSQL
* **Infrastructure**: Docker Compose at the root level orchestrates everything

---

## 📦 Adding a New Service

When creating a new backend service, **don’t start from scratch** — follow these steps:

1. **Choose a template service**

   * Pick an existing service that feels closest to what you need.
   * Copy its structure and configs.

2. **Update identifiers**

   * Rename the service module, package names, and main application class.
   * Adjust service name in `application.yml`.

3. **Add Postgres schema (if needed)**

   * Update the database config for your service in `docker-compose.yml`.
   * Apply schema migrations (`flyway` or SQL init scripts if used).

4. **Register in Docker Compose**

   * Define your new service container.
   * Add dependencies (e.g., Postgres).
   * Make sure ports don’t clash with existing ones.

5. **Test integration**

   * Run `docker-compose up --build` at root level.
   * Verify your new service spins up and connects to its DB.

---

## ⚡ Vibe Session Rituals

* **Kick-off (5 mins):** quick sync — what’s today’s focus?
* **Deep Work (25–40 mins):** silent or paired coding.
* **Checkpoint (5–10 mins):** share progress, blockers, fun hacks.
* **Iterate:** repeat cycles until wrap-up.
* **Close (5 mins):** commit, push, and celebrate wins 🎉.

---

## 🛠️ Useful Commands

Spin up all services:

```sh
docker-compose up --build
```

Spin down everything:

```sh
docker-compose down -v
```

Run a single service locally:

```sh
./mvnw spring-boot:run
```

---

## 🧭 Principles

* **Consistency is a feature.** New services should feel like old services.
* **Infrastructure is shared.** Don't reinvent; extend the Docker Compose.
* **Documentation beats memory.** Update this file when workflows change.
* **Keep it light.** The goal is flow, not bureaucracy.

---

## 🔒 Security & Quality Standards

### ⚠️ CRITICAL: All Code MUST Follow These Rules

#### 1. **Logging Security** ✅ MANDATORY
```java
// ❌ NEVER DO THIS - CRLF Injection Risk
logger.info("User {} logged in", userInput);
logger.error("Failed for {} and {}", portfolioId, basketId);

// ✅ ALWAYS DO THIS - Sanitize or use safe patterns
logger.info("User {} logged in", sanitizeForLog(userInput));
// OR configure Logback with replace pattern (see logback-spring.xml)

// Add this utility to every service:
private String sanitizeForLog(Object obj) {
    return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
}
```

#### 2. **Random Number Generation** 🎲
```java
// ❌ NEVER for security/production simulations
Random random = new Random();

// ✅ ALWAYS for production
import java.security.SecureRandom;
private static final SecureRandom secureRandom = new SecureRandom();

// Exception: Demo/test data ONLY (must be clearly labeled)
// DemoCreditEventService.java - acceptable with warning logs
```

#### 3. **String Case Operations** 🔤
```java
// ❌ NEVER - Turkish locale bypass vulnerability
if(input.toUpperCase().equals("ADMIN")) { }
if(frequency.toUpperCase().equals("QUARTERLY")) { }

// ✅ ALWAYS specify Locale.ROOT
if(input.toUpperCase(Locale.ROOT).equals("ADMIN")) { }

// ✅ BETTER - use case-insensitive comparison
if("ADMIN".equalsIgnoreCase(input)) { }

// ✅ BEST - use enums instead of string comparisons
CouponFrequency.valueOf(input.toUpperCase(Locale.ROOT))
```

#### 4. **Exception Handling** 🚨
```java
// ❌ NEVER expose stack traces or internal details to clients
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(Exception ex) {
    return ResponseEntity.status(500)
        .body(new ErrorResponse(ex.getMessage())); // ❌ Information leak
}

// ✅ ALWAYS log details server-side, return safe message to client
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(Exception ex, WebRequest request) {
    String correlationId = UUID.randomUUID().toString();
    logger.error("Error [{}]: {}", correlationId, ex.getMessage(), ex);
    
    return ResponseEntity.status(500).body(new ErrorResponse(
        "An internal error occurred. Reference: " + correlationId
    ));
}
```

#### 5. **Input Validation** 🛡️
```java
// ✅ ALWAYS validate controller inputs
@PostMapping("/portfolios")
public ResponseEntity<Portfolio> create(@Valid @RequestBody PortfolioRequest req) {
    // @Valid triggers validation
}

// ✅ ALWAYS add constraints to DTOs
public class PortfolioRequest {
    @NotBlank(message = "Name required")
    @Size(max = 255, message = "Name too long")
    @Pattern(regexp = "^[a-zA-Z0-9-_ ]+$", message = "Invalid characters")
    private String name;
}
```

#### 6. **REST Endpoint Security** 🔐
```java
// ✅ CONTEXT-AWARE security based on endpoint type

// PUBLIC endpoints - Must be explicitly documented
/**
 * Public health endpoint - no authentication required.
 * Rate limiting applied at gateway level.
 */
@GetMapping("/api/public/health")
public ResponseEntity<HealthStatus> getHealth() {
    // Implementation
}

// USER-FACING endpoints - Require authentication
@RestController
@RequestMapping("/api/portfolios")
@PreAuthorize("hasRole('USER')")
public class PortfolioController {
    
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessPortfolio(#id)")
    public ResponseEntity<Portfolio> get(@PathVariable Long id) {
        // Implementation
    }
}

// INTERNAL service endpoints - Service token authentication
/**
 * Internal service endpoint - authenticated via service token.
 * Security configured in WebSecurityConfig for service-to-service calls.
 */
@RestController
@RequestMapping("/internal/risk-calculations")
public class InternalRiskController {
    // Implementation
}

// ✅ ALWAYS have SecurityConfig (but configure appropriately)
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/internal/**").hasRole("SERVICE")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**Authorization Decision Guide:**
- **Public endpoints** (health, info): No `@PreAuthorize` needed, but MUST document
- **Internal APIs**: Use service token validation, not user roles
- **User data endpoints**: Use `@PreAuthorize("hasRole('USER')")`
- **Admin operations**: Use `@PreAuthorize("hasRole('ADMIN')")`
- **Resource ownership**: Use custom expressions like `@PreAuthorize("@securityService.canAccess(#id)")`

#### 7. **Database Queries** 💾
```java
// ❌ NEVER use string concatenation
String query = "SELECT * FROM users WHERE name = '" + userName + "'";

// ✅ ALWAYS use parameterized queries/JPA
@Query("SELECT u FROM User u WHERE u.name = :name")
User findByName(@Param("name") String name);

// ✅ OR use JPA method names
User findByName(String name);
```

#### 8. **Sensitive Data** 🔑
```java
// ❌ NEVER log sensitive data
logger.info("User password: {}", password);
logger.debug("Credit card: {}", cardNumber);

// ✅ ALWAYS mask or omit
logger.info("User authenticated: {}", username);
logger.debug("Payment processed: ****{}",
    cardNumber.substring(cardNumber.length() - 4));

// ✅ NEVER commit secrets
// Use application-{profile}.yml with .gitignore
// Use environment variables for production
```

---

## ✅ Quality Gate Checklist

Before committing ANY code, verify:

- [ ] **No CRLF injection** - All logging uses `sanitizeForLog()` or safe patterns
- [ ] **No predictable random** - `SecureRandom` for production, `Random` only in demo code with warnings
- [ ] **Locale specified** - All `.toUpperCase()/.toLowerCase()` use `Locale.ROOT`
- [ ] **No information leakage** - Exception handlers return generic messages
- [ ] **Input validated** - All `@RequestBody` has `@Valid`, DTOs have constraints
- [ ] **Endpoints secured** - Controllers have security annotations
- [ ] **SQL safe** - No string concatenation in queries
- [ ] **No secrets** - No passwords, API keys, or tokens in code
- [ ] **Tests pass** - `./mvnw test` succeeds
- [ ] **SpotBugs clean** - `./mvnw spotbugs:check` passes

---

## 🔍 Running Quality Checks Locally

```bash
# Run all tests
./mvnw test

# Run SpotBugs security scan
./mvnw spotbugs:check

# Run Checkstyle
./mvnw checkstyle:check

# Run OWASP dependency check
./mvnw dependency-check:check

# Run all quality checks
./mvnw clean verify
```

---

## 📚 Security Resources

- **OWASP Top 10**: https://owasp.org/www-project-top-ten/
- **CWE Top 25**: https://cwe.mitre.org/top25/
- **Java Security Guidelines**: https://www.oracle.com/java/technologies/javase/seccodeguide.html
- **Spring Security**: https://docs.spring.io/spring-security/reference/

---

## 🎯 Code Generation Rules for AI Agents

When generating code, ALWAYS:

1. **Start with security** - Apply security patterns from the start, not as an afterthought
2. **Use safe defaults** - SecureRandom, Locale.ROOT, parameterized queries
3. **Validate inputs** - Every external input must be validated
4. **Sanitize outputs** - Especially logs and error messages
5. **Follow existing patterns** - Look at similar code in the codebase first
6. **Add TODO comments** - If security implementation is deferred: `// TODO: Add authentication - see SecurityConfig`
7. **Test security** - Include tests that verify security controls work

---

## 🚫 Anti-Patterns to AVOID

```java
// ❌ Don't trust client input
String role = request.getParameter("role");
user.setRole(role); // User can set themselves as ADMIN!

// ❌ Don't use null checks for authorization
if (user != null) { /* allow access */ } // Authentication ≠ Authorization

// ❌ Don't disable security features
@CrossOrigin(origins = "*") // Opens CSRF vulnerabilities
http.csrf().disable(); // Only for non-browser clients

// ❌ Don't use weak algorithms
MessageDigest md = MessageDigest.getInstance("MD5"); // Use BCrypt/Argon2
SecureRandom.getInstance("SHA1PRNG").setSeed(1234); // Don't seed in production

// ❌ Don't catch and ignore security exceptions
try {
    // security check
} catch (SecurityException e) {
    // ignore - ❌ Silent security failure!
}
```

---

## 🤝 Service Template Checklist

When cloning a service for a new feature:

### Files to Update:
- [ ] `pom.xml` - Update `<artifactId>`, `<name>`, `<description>`
- [ ] `application.yml` - Update service name, port
- [ ] Main application class - Rename and update `@SpringBootApplication`
- [ ] Package structure - Rename base package
- [ ] `Dockerfile` - Update labels and JAR name
- [ ] `docker-compose.yml` - Add service entry with unique ports
- [ ] Database migrations - Create in `src/main/resources/db/migration`
- [ ] README.md - Update service description

### Security Checklist for New Service:
- [ ] Add `spring-boot-starter-security` dependency
- [ ] Create `SecurityConfig.java`
- [ ] Add `sanitizeForLog()` utility to base service class
- [ ] Configure Logback with CRLF protection in `logback-spring.xml`
- [ ] Add `@Valid` annotations to all controllers
- [ ] Add validation constraints to all DTOs
- [ ] Review `spotbugs-security-include.xml` for suppressions
- [ ] Add security tests

---

## 📝 Commit Message Format

```
<type>(<scope>): <subject>

<body>

Security: <any security implications>
```

**Types:** feat, fix, refactor, docs, test, chore, security
**Example:**
```
security(logging): Prevent CRLF injection in all loggers

- Add sanitizeForLog() utility method
- Configure Logback with replace pattern
- Update all logger calls in controllers

Security: Fixes CWE-117 CRLF Injection vulnerability
```

---
