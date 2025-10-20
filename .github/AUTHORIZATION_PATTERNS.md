# 🔐 Authorization Patterns Guide

## Overview

**Not all endpoints require `@PreAuthorize` annotations.** Authorization strategy depends on endpoint type, caller identity, and data sensitivity. This guide helps you choose the right pattern.

---

## 🎯 Quick Decision Matrix

| Endpoint Type | Authentication | Authorization | Example |
|--------------|----------------|---------------|---------|
| **Public Health/Status** | ❌ No | None | `/api/public/health` |
| **Actuator Endpoints** | ✅ Yes (configurable) | application.yml | `/actuator/health` |
| **Internal Service APIs** | ✅ Yes | Service token | `/internal/calculations` |
| **Public User Data** | ✅ Yes | `@PreAuthorize("hasRole('USER')")` | `/api/portfolios` |
| **Resource Ownership** | ✅ Yes | Custom expression | `/api/portfolios/{id}` |
| **Admin Operations** | ✅ Yes | `@PreAuthorize("hasRole('ADMIN')")` | `/api/admin/users` |

---

## 📋 Pattern Details

### Pattern 1: Public Endpoints (No Authorization)

**Use When:**
- Health check endpoints
- Public information (no sensitive data)
- Pre-authentication endpoints (login, registration)
- Public documentation/API specs

**Requirements:**
- ⚠️ **Must be explicitly documented**
- ⚠️ **Must apply rate limiting at gateway level**
- ⚠️ **Must not expose sensitive data**

**Example:**
```java
/**
 * Public health endpoint - no authentication required.
 * Rate limiting: 100 req/min per IP (configured at gateway).
 * Exposes: Service status, version (non-sensitive data only).
 */
@RestController
@RequestMapping("/api/public")
public class PublicApiController {
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> getHealth() {
        return ResponseEntity.ok(new HealthStatus("UP", appVersion));
    }
    
    @GetMapping("/info")
    public ResponseEntity<AppInfo> getInfo() {
        // Only non-sensitive configuration
        return ResponseEntity.ok(new AppInfo(serviceName, version));
    }
}
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                // ... other rules
            );
        return http.build();
    }
}
```

---

### Pattern 2: Actuator Endpoints

**Use When:**
- Spring Boot Actuator endpoints
- Operational monitoring
- Health checks for load balancers

**Requirements:**
- Configure in `application.yml`
- Restrict sensitive endpoints

**Example:**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()  // Load balancer
                .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
                // ... other rules
            );
        return http.build();
    }
}
```

---

### Pattern 3: Internal Service-to-Service APIs

**Use When:**
- Backend service calls another backend service
- Microservice communication
- Internal calculation/processing endpoints

**Requirements:**
- Use service authentication tokens
- Not accessible from public internet
- Network-level isolation (Kubernetes NetworkPolicy, VPC)

**Example:**
```java
/**
 * Internal service endpoint - authenticated via service token.
 * Security: Service-to-service JWT token validation.
 * Network: Only accessible within cluster/VPC.
 */
@RestController
@RequestMapping("/internal/risk-calculations")
public class InternalRiskController {
    
    @PostMapping("/calculate")
    public ResponseEntity<RiskResult> calculate(
            @RequestHeader("X-Service-Token") String serviceToken,
            @Valid @RequestBody RiskRequest request) {
        
        // Token validation handled by security filter
        return ResponseEntity.ok(riskService.calculate(request));
    }
}
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").hasRole("SERVICE")
                // ... other rules
            )
            .addFilterBefore(serviceTokenFilter(), 
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    
    @Bean
    public ServiceTokenFilter serviceTokenFilter() {
        return new ServiceTokenFilter(serviceTokenValidator);
    }
}

// Custom filter for service token validation
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) {
        if (request.getRequestURI().startsWith("/internal/")) {
            String token = request.getHeader("X-Service-Token");
            if (!serviceTokenValidator.isValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            // Set authentication for hasRole("SERVICE") to work
            SecurityContextHolder.getContext()
                .setAuthentication(new ServiceAuthentication(token));
        }
        filterChain.doFilter(request, response);
    }
}
```

---

### Pattern 4: User-Facing APIs (Basic Authentication)

**Use When:**
- Endpoints require user to be logged in
- No specific role required
- Any authenticated user can access

**Requirements:**
- User must be authenticated
- Use `@PreAuthorize("hasRole('USER')")`

**Example:**
```java
/**
 * User-facing API - requires authentication.
 * Authorization: Any authenticated user with USER role.
 */
@RestController
@RequestMapping("/api/portfolios")
@PreAuthorize("hasRole('USER')")
public class PortfolioController {
    
    @GetMapping
    public ResponseEntity<List<Portfolio>> list() {
        // All authenticated users can list portfolios
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return ResponseEntity.ok(portfolioService.findByUserId(userId));
    }
}
```

---

### Pattern 5: Resource Ownership Checks

**Use When:**
- User can only access their own data
- Need to verify resource belongs to authenticated user
- Fine-grained authorization

**Requirements:**
- Implement custom security service
- Use SpEL expressions in `@PreAuthorize`

**Example:**
```java
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {
    
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessPortfolio(#id)")
    public ResponseEntity<Portfolio> get(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.findById(id));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canModifyPortfolio(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portfolioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

// Custom security service
@Service("securityService")
public class SecurityService {
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    public boolean canAccessPortfolio(Long portfolioId) {
        String currentUser = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElse(null);
        
        return portfolio != null && 
               portfolio.getOwnerId().equals(currentUser);
    }
    
    public boolean canModifyPortfolio(Long portfolioId) {
        // Same as canAccessPortfolio, or add additional checks
        return canAccessPortfolio(portfolioId);
    }
}
```

---

### Pattern 6: Admin-Only Operations

**Use When:**
- Administrative operations
- System-level configuration
- User management

**Requirements:**
- User must have ADMIN role
- Use `@PreAuthorize("hasRole('ADMIN')")`

**Example:**
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> listAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/system/config")
    public ResponseEntity<SystemConfig> updateConfig(
            @Valid @RequestBody SystemConfig config) {
        return ResponseEntity.ok(configService.update(config));
    }
}
```

---

## 🚫 Anti-Patterns

### ❌ Don't: Trust Client Input for Roles
```java
// ❌ NEVER DO THIS
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
    User user = new User();
    user.setRole(request.getRole());  // Client controls their own role!
    return ResponseEntity.ok(userService.save(user));
}

// ✅ DO THIS
@PostMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
    User user = new User();
    user.setRole(Role.USER);  // Server controls role assignment
    return ResponseEntity.ok(userService.save(user));
}
```

### ❌ Don't: Use Null Checks for Authorization
```java
// ❌ AUTHENTICATION ≠ AUTHORIZATION
@GetMapping("/{id}")
public ResponseEntity<Portfolio> get(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {  // Only checks if logged in, not if authorized!
        return ResponseEntity.ok(portfolioService.findById(id));
    }
    return ResponseEntity.status(401).build();
}

// ✅ DO THIS
@GetMapping("/{id}")
@PreAuthorize("@securityService.canAccessPortfolio(#id)")
public ResponseEntity<Portfolio> get(@PathVariable Long id) {
    return ResponseEntity.ok(portfolioService.findById(id));
}
```

### ❌ Don't: Disable Security for Development
```java
// ❌ Don't comment out security
// @PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) { }

// ✅ Use Spring profiles if needed
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @environment.acceptsProfiles('dev')")
public ResponseEntity<Void> delete(@PathVariable Long id) { }
```

---

## 📝 Documentation Checklist

When creating an endpoint, document:

- [ ] **Endpoint type** (public, internal, user-facing, admin)
- [ ] **Authentication required?** (yes/no)
- [ ] **Authorization strategy** (@PreAuthorize, service token, none)
- [ ] **Rate limiting** (if applicable)
- [ ] **Data sensitivity** (what data is exposed)
- [ ] **Network restrictions** (internal-only, public internet)

**Example Documentation:**
```java
/**
 * Calculate portfolio risk metrics.
 * 
 * Endpoint Type: User-facing API
 * Authentication: Required (JWT token)
 * Authorization: User must own the portfolio
 * Rate Limiting: 10 req/min per user (gateway)
 * Data Sensitivity: Financial data - requires encryption in transit
 * Network: Public internet via API gateway
 * 
 * @param portfolioId Portfolio identifier
 * @return Risk metrics for the portfolio
 * @throws PortfolioNotFoundException if portfolio doesn't exist
 * @throws AccessDeniedException if user doesn't own portfolio
 */
@GetMapping("/{portfolioId}/risk")
@PreAuthorize("@securityService.canAccessPortfolio(#portfolioId)")
public ResponseEntity<RiskMetrics> calculateRisk(
        @PathVariable Long portfolioId) {
    // Implementation
}
```

---

## 🎓 Best Practices

1. **Default to Secure**: When in doubt, require authentication
2. **Document Public Endpoints**: Any endpoint without `@PreAuthorize` must be documented
3. **Principle of Least Privilege**: Grant minimum necessary permissions
4. **Fail Securely**: Access denied by default, explicitly allow
5. **Test Authorization**: Write tests that verify authorization rules
6. **Audit Logs**: Log authorization decisions for security review
7. **Review Regularly**: Quarterly review of authorization patterns

---

## 🧪 Testing Authorization

### Unit Test Example
```java
@Test
@WithMockUser(roles = "USER")
void testUserCanAccessOwnPortfolio() {
    when(securityService.canAccessPortfolio(1L)).thenReturn(true);
    
    ResponseEntity<Portfolio> response = controller.get(1L);
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
}

@Test
@WithMockUser(roles = "USER")
void testUserCannotAccessOthersPortfolio() {
    when(securityService.canAccessPortfolio(999L)).thenReturn(false);
    
    assertThrows(AccessDeniedException.class, () -> {
        controller.get(999L);
    });
}
```

### Integration Test Example
```java
@Test
void testPublicHealthEndpointAccessibleWithoutAuth() {
    given()
        .when()
            .get("/api/public/health")
        .then()
            .statusCode(200);
}

@Test
void testPortfolioEndpointRequiresAuth() {
    given()
        .when()
            .get("/api/portfolios/1")
        .then()
            .statusCode(401);  // Unauthorized
}

@Test
void testPortfolioEndpointWithValidToken() {
    given()
        .header("Authorization", "Bearer " + validToken)
        .when()
            .get("/api/portfolios/1")
        .then()
            .statusCode(200);
}
```

---

## 📞 Need Help?

1. Review this guide for common patterns
2. Check **AGENTS.md** for security standards
3. Consult **AI_AGENT_INSTRUCTIONS.md** for code generation rules
4. Ask in the team security channel

---

**Last Updated:** 2025-10-16  
**Version:** 1.0.0
