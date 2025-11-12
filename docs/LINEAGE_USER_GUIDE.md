# Lineage Visualization - User Guide

## Understanding "Why Both Portfolios Show on the Same Graph"

### 📊 What You're Seeing Is Normal!

When you create **two portfolios** and view lineage by **dataset**, both portfolios appear on the same graph. This is **expected behavior** because:

- **Portfolio A** → Writes to `cds_portfolios` table
- **Portfolio B** → Writes to `cds_portfolios` table  
- **Query**: `GET /api/lineage?dataset=cds_portfolios`
- **Result**: Shows **ALL** operations that touched the `cds_portfolios` table

### 🎯 How to View Individual Portfolio Lineage

To see lineage for **just one portfolio creation**, use **Correlation ID filtering**:

#### Option 1: Correlation ID (Recommended)

**What it is:** Every HTTP request gets a unique correlation ID that tracks the entire request flow.

**How to get it:**

1. **From Browser DevTools:**
   - Open DevTools (F12)
   - Go to Network tab
   - Create a portfolio
   - Click on the POST request
   - Look at Response Headers for `X-Correlation-ID` or check the response body

2. **From Backend Logs:**
   ```
   [2025-11-11 11:10:35] INFO  - Correlation ID: 550e8400-e29b-41d4-a716-446655440000
   ```

3. **From Database:**
   ```sql
   SELECT outputs->>'_correlation_id' as correlation_id
   FROM lineage_events 
   WHERE dataset = 'cds_portfolios'
   ORDER BY created_at DESC 
   LIMIT 10;
   ```

**Frontend Usage:**
1. Select "🔍 Search by Correlation ID" radio button
2. Paste the correlation ID
3. Click "Fetch Lineage"
4. **Result:** Shows ONLY that specific portfolio creation with full request trace

**What you'll see:**
```
🌐 POST /api/portfolios
  ↓
⚙️  PortfolioService.createPortfolio()
  ↓
💾 PortfolioRepository.save()
  ↓
📊 cds_portfolios [INSERT]
```

#### Option 2: Dataset Filtering (Shows All)

**When to use:** View all portfolio operations together to see patterns

**Frontend Usage:**
1. Select "📊 Search by Dataset"
2. Choose `cds_portfolios`
3. Click "Fetch Lineage"
4. **Result:** Shows ALL portfolio creations merged

**What you'll see:**
```
🌐 POST /api/portfolios (Request 1)
  ↓
📊 cds_portfolios [INSERT] (Portfolio A)

🌐 POST /api/portfolios (Request 2)
  ↓
📊 cds_portfolios [INSERT] (Portfolio B)
```

Both operations show on the same graph because they both interact with the same dataset.

#### Option 3: Recent Activity

**When to use:** Quick overview of recent lineage activity

**Frontend Usage:**
1. Select "⏱️ Recent Activity"
2. Click "Fetch Lineage"
3. **Result:** Shows last 100 operations across all datasets

---

## 🔍 Lineage View Comparison

| View Type | Scope | Use Case | Shows Multiple Operations? |
|-----------|-------|----------|----------------------------|
| **Correlation ID** | Single HTTP request | Track one specific operation (e.g., Portfolio A creation) | ❌ No - Only that request |
| **Dataset** | All operations on a table | See all portfolio creations together | ✅ Yes - All operations on that table |
| **Run ID** | Custom grouping | Batch operations with same run ID | ✅ Yes - All operations in that run |
| **Recent Activity** | Latest N operations | Quick monitoring | ✅ Yes - Recent operations across all datasets |

---

## 💡 Quick Answers to Common Questions

### Q: "Why do I see two portfolios on the same graph?"
**A:** You're viewing by **dataset**, which shows ALL operations on that table. Use **Correlation ID** to see individual operations.

### Q: "How do I separate Portfolio A from Portfolio B?"
**A:** Get the correlation ID for each portfolio creation and query by correlation ID separately.

### Q: "What's the difference between correlation ID and run ID?"
**A:**  
- **Correlation ID:** Auto-generated for every HTTP request (unique per request)
- **Run ID:** Custom identifier you can set for grouping related operations

### Q: "Can I see the full request trace (HTTP → Service → Repository → Database)?"
**A:** Yes! Use **Correlation ID** search with the new Graph API. It shows:
- 🌐 **Endpoint** node: HTTP request details
- ⚙️ **Service** nodes: Business logic calls
- 💾 **Repository** nodes: Data access layer
- 📊 **Dataset** nodes: Database tables

---

## 🚀 Example Workflow

### Scenario: Created two portfolios, want to see them separately

**Step 1:** Get correlation IDs from recent operations
```bash
curl http://localhost:8081/api/lineage?dataset=cds_portfolios | jq '.[] | {id, correlation_id: .outputs._correlation_id}'
```

**Step 2:** Pick the first correlation ID (Portfolio A)
```
550e8400-e29b-41d4-a716-446655440000
```

**Step 3:** Query by correlation ID
```bash
curl http://localhost:8081/api/lineage/graph/correlation/550e8400-e29b-41d4-a716-446655440000
```

**Step 4:** In frontend:
- Select "🔍 Search by Correlation ID"
- Paste: `550e8400-e29b-41d4-a716-446655440000`
- Click "Fetch Lineage"
- See ONLY Portfolio A's lineage

**Step 5:** Repeat for Portfolio B with its correlation ID

---

## 📝 Summary

✅ **Seeing both portfolios together is normal when viewing by dataset**  
✅ **Use Correlation ID to see individual portfolio creations**  
✅ **The Graph API provides rich multi-layer visualization**  
✅ **Each view type serves different purposes**

---

*Last Updated: 2025-11-11*
