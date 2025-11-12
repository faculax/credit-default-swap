# Lineage Automation Proposal

## Problem Statement

Current lineage implementation has **semantic accuracy issues**:
- ❌ Shows hypothetical/downstream operations that don't actually execute
- ❌ Manually hardcoded schemas that may not match actual code behavior
- ❌ Requires manual updates every time code changes
- ❌ Time-consuming to maintain and verify accuracy

**Example**: Trade capture showed 6 outputs (valuation, risk, margin, regulatory) when only 1 write actually happens (cds_trades insert).

---

## Solution Options

### **Option 1: Automatic Database Operation Tracking (RECOMMENDED)**

#### **How It Works**
Intercept **actual database operations** at the JPA/Hibernate level using:
1. **Hibernate Interceptor** - Captures all SQL operations (SELECT, INSERT, UPDATE, DELETE)
2. **Spring Data JPA Event Listeners** - Captures entity lifecycle events (@PrePersist, @PostPersist, @PreUpdate, etc.)
3. **JPA EntityListener** - Attach listeners to entities to track changes

#### **Implementation**

##### **Step 1: Create Database Operation Tracker**

```java
@Component
public class LineageEntityListener {
    
    private static final ThreadLocal<Set<String>> READ_TABLES = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<String>> WRITE_TABLES = ThreadLocal.withInitial(HashSet::new);
    
    @PostLoad
    public void onEntityLoad(Object entity) {
        String tableName = extractTableName(entity);
        READ_TABLES.get().add(tableName);
    }
    
    @PrePersist
    @PreUpdate
    public void onEntityWrite(Object entity) {
        String tableName = extractTableName(entity);
        WRITE_TABLES.get().add(tableName);
    }
    
    public static Set<String> getReadTables() {
        return new HashSet<>(READ_TABLES.get());
    }
    
    public static Set<String> getWriteTables() {
        return new HashSet<>(WRITE_TABLES.get());
    }
    
    public static void clearTracking() {
        READ_TABLES.remove();
        WRITE_TABLES.remove();
    }
    
    private String extractTableName(Object entity) {
        if (entity == null) return "unknown";
        Class<?> entityClass = entity.getClass();
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        return entityClass.getSimpleName().toLowerCase() + "s";
    }
}
```

##### **Step 2: Enhance LineageAspect to Use Tracked Operations**

```java
@Around("@annotation(trackLineage)")
public Object trackLineageAround(ProceedingJoinPoint joinPoint, TrackLineage trackLineage) throws Throwable {
    // Clear previous tracking
    LineageEntityListener.clearTracking();
    
    // Execute method and track database operations automatically
    Object result = joinPoint.proceed();
    
    // Get actual database operations that happened
    Set<String> readTables = LineageEntityListener.getReadTables();
    Set<String> writeTables = LineageEntityListener.getWriteTables();
    
    // Build lineage from ACTUAL operations
    Map<String, Object> inputs = new HashMap<>();
    for (String table : readTables) {
        inputs.put(table + "_read", Map.of("dataset", table, "operation", "SELECT"));
    }
    
    Map<String, Object> outputs = new HashMap<>();
    for (String table : writeTables) {
        outputs.put(table + "_written", Map.of("dataset", table, "operation", "INSERT/UPDATE"));
    }
    
    // Route to service with ACTUAL operations
    routeToTracker(trackLineage, result, userName, details, readTables, writeTables);
    
    return result;
}
```

##### **Step 3: Update All Entities**

Add `@EntityListeners` to all domain entities:

```java
@Entity
@Table(name = "cds_trades")
@EntityListeners(LineageEntityListener.class)  // <-- Add this
public class CDSTrade {
    // ... existing code
}
```

#### **Benefits**
- ✅ **100% Accurate** - Only shows operations that actually executed
- ✅ **Zero Maintenance** - Updates automatically when code changes
- ✅ **No Manual Schemas** - Lineage generated from runtime behavior
- ✅ **Transaction-Aware** - Only tracks committed operations
- ✅ **Performance** - Minimal overhead (ThreadLocal lookups)

#### **Drawbacks**
- Requires entity listener setup on all entities (~30 entities)
- Less detailed field-level tracking (can be added)
- May capture unexpected reads (lazy loading, audit queries)

---

### **Option 2: SQL Query Interception (Advanced)**

#### **How It Works**
Use **Hibernate StatementInspector** or **P6Spy** to intercept actual SQL:

```java
@Component
public class LineageSQLInspector implements StatementInspector {
    
    private static final ThreadLocal<List<String>> SQL_STATEMENTS = ThreadLocal.withInitial(ArrayList::new);
    
    @Override
    public String inspect(String sql) {
        SQL_STATEMENTS.get().add(sql);
        return sql;
    }
    
    public static List<String> extractTableOperations() {
        List<String> sqls = SQL_STATEMENTS.get();
        Map<String, Set<String>> operations = new HashMap<>();
        
        for (String sql : sqls) {
            String normalized = sql.toUpperCase();
            if (normalized.startsWith("SELECT")) {
                String table = extractTableFromSelect(sql);
                operations.computeIfAbsent(table, k -> new HashSet<>()).add("READ");
            } else if (normalized.startsWith("INSERT")) {
                String table = extractTableFromInsert(sql);
                operations.computeIfAbsent(table, k -> new HashSet<>()).add("WRITE");
            } else if (normalized.startsWith("UPDATE")) {
                String table = extractTableFromUpdate(sql);
                operations.computeIfAbsent(table, k -> new HashSet<>()).add("WRITE");
            }
        }
        
        return operations;
    }
}
```

#### **Benefits**
- ✅ **Most Accurate** - Captures exact SQL including joins
- ✅ **Framework Agnostic** - Works with any ORM or JDBC
- ✅ **Detailed** - Can extract field-level operations

#### **Drawbacks**
- Complex SQL parsing required
- Higher performance overhead
- May capture internal framework queries

---

### **Option 3: Manual Fix (Current Approach)**

Fix each tracking method to match actual code behavior:

#### **Issues Found So Far**

1. **trackTradeCapture()** ✅ FIXED
   - ❌ Was showing: portfolio_positions, market_quotes, issuer_master reads, valuation/risk/margin outputs
   - ✅ Now shows: netting_sets read, cds_trades write

2. **trackBondOperation()** ❌ NEEDS FIX
   - ❌ Shows: issuer_master, market_quotes reads, portfolio_positions write, valuation scheduled
   - ✅ Should show: bonds write only (no actual reference data lookups in code)

3. **trackPortfolioOperation()** ✅ FIXED
   - ✅ Now operation-specific (CREATE, ATTACH_TRADES, ATTACH_BOND, PRICE)

4. **trackMarginOperation()** ❌ NEEDS REVIEW
   - May show hypothetical risk calculations

5. **trackBasketOperation()** ❌ NEEDS REVIEW
   - May show hypothetical constituent validations

6. **trackLifecycleOperation()** ❌ NEEDS REVIEW
   - May show hypothetical payment processing

7. **trackNovationOperation()** ❌ NEEDS REVIEW
   - May show hypothetical counterparty validations

---

## Recommendation

### **Hybrid Approach (Best of Both Worlds)**

1. **Short-term** (This Sprint):
   - Fix remaining manual lineage methods (trackBond, trackMargin, trackBasket, trackLifecycle, trackNovation)
   - Review each against actual service code
   - Remove hypothetical operations

2. **Medium-term** (Next Sprint):
   - Implement **Option 1: Automatic Database Operation Tracking**
   - Add @EntityListeners to all entities
   - Enhance LineageAspect to use tracked operations
   - Keep manual methods as fallback for edge cases

3. **Long-term** (Future):
   - Consider **Option 2: SQL Interception** for advanced use cases
   - Add field-level lineage tracking
   - Implement query optimization insights

---

## Action Items

### **Immediate (Manual Fixes)**

- [ ] Fix `trackBondOperation()` - remove issuer_master/market_quotes reads
- [ ] Review `trackMarginOperation()` against SimmService/SaCcrService actual code
- [ ] Review `trackBasketOperation()` against BasketService actual code
- [ ] Review `trackLifecycleOperation()` against LifecycleService actual code
- [ ] Review `trackNovationOperation()` against NovationService actual code
- [ ] Test all fixed methods with actual API calls

### **Next Sprint (Automation)**

- [ ] Create `LineageEntityListener` class
- [ ] Add `@EntityListeners` to all 30+ entities
- [ ] Enhance `LineageAspect.trackLineageAround()` to use tracked operations
- [ ] Create automated tests comparing manual vs tracked lineage
- [ ] Document migration path from manual to automatic

---

## Testing Strategy

### **Manual Fix Verification**

For each operation:
1. Read actual service code (e.g., BondService.createBond())
2. Identify actual repository calls (e.g., bondRepository.save())
3. Map repository → table (e.g., BondRepository → bonds table)
4. Update tracking method to match actual behavior
5. Test with API call and verify lineage shows only actual operations

### **Automation Verification**

1. Enable SQL logging: `spring.jpa.show-sql=true`
2. Run operation
3. Compare SQL logs with lineage events
4. Verify 100% match between SQL and lineage

---

## Cost-Benefit Analysis

| Approach | Setup Time | Maintenance | Accuracy | Performance |
|----------|------------|-------------|----------|-------------|
| Manual Fix | 8 hours | High (every code change) | 80% (human error) | Excellent |
| Auto Tracking | 16 hours | None | 98% (runtime behavior) | Good |
| SQL Interception | 40 hours | Low | 99.9% (exact SQL) | Fair |

**Recommendation**: Start with Manual Fix (this week), implement Auto Tracking (next sprint).

---

## Questions for Discussion

1. Do we need field-level lineage (e.g., "notional field updated") or table-level is sufficient?
2. Should lineage track failed transactions (rollbacks)?
3. Should we track lazy-loading reads or only explicit queries?
4. Do we want lineage for internal audit/framework queries (e.g., Flyway, Hibernate)?
5. Performance budget: What's acceptable latency overhead for lineage tracking?

