# Frontend Structured Lineage Visualization

**Date:** 2025-11-11  
**Status:** ✅ DEPLOYED - Structured lineage now displayed in frontend

---

## Overview

The frontend now displays **structured lineage documents** in a user-friendly, expandable format. Each lineage event shows 5 key sections that provide comprehensive data governance visibility.

---

## New UI Components

### 📜 Structured Lineage Document Panel

Located between the **Stats/Audit** section and the **Debug** section, this panel displays structured lineage data with collapsible sections.

**Features:**
- ✅ **Origin Section** - Shows where data started (sources, systems, input datasets)
- ✅ **Path Section** - Timeline view of every hop data took through the system
- ✅ **Transformations Section** - Documents how data was changed
- ✅ **Consumers Section** - Lists who/what uses the data
- ✅ **Metadata Section** - Comprehensive compliance and audit trail

**Visual Design:**
- Expandable/collapsible sections (Origin and Path open by default)
- Color-coded badges for types/layers
- Progress bars for confidence scores
- Timeline visualization for path stages
- Grid layouts for metadata fields

---

## Section Details

### 📍 Origin Section (Open by Default)

**What It Shows:**
- Primary dataset name
- Source type (e.g., `database_table`)
- List of all input sources with details

**Visual Elements:**
- Dataset names displayed as green code blocks
- Input sources shown in bordered cards
- Source system badges (e.g., `user_interface`)
- Nested details with JSON formatting

**Example Display:**
```
📍 Origin - Where Data Started

Primary Dataset: cds_portfolios
Source Type: database_table

Input Sources:
┌─ portfolio_name_check [user_interface]
│  Dataset: cds_portfolios
│  { "name": "Frontend Demo Portfolio", ... }
│
└─ ui_portfolio_entry
   { "form": "portfolio_management", ... }
```

---

### 🛤️ Path Section (Open by Default)

**What It Shows:**
- Sequential timeline of data flow
- 4 stages: HTTP Endpoint → Service → Repository → Dataset
- Layer annotations for each stage
- Timestamps showing when each hop occurred

**Visual Elements:**
- Numbered circles (1, 2, 3, 4) showing sequence
- Connecting vertical lines between stages
- Layer badges: `presentation`, `business_logic`, `data_access`, `persistence`
- Stage-specific information:
  - HTTP: Method + endpoint
  - Service: Class + method
  - Repository: Interface + method + type
  - Dataset: Tables read/written

**Example Display:**
```
🛤️ Path - Every Hop Data Took

① ─┐
   │ http_endpoint [presentation] 17:48:49
   │ POST /api/cds-portfolios
   │
② ─┤
   │ service [business_logic] 17:48:49
   │ CdsPortfolioService.createPortfolio()
   │
③ ─┤
   │ repository [data_access] 17:48:49
   │ CdsPortfolioRepository.existsByNameIgnoreCase() (SpringData)
   │
④ ─┤
   │ repository [data_access] 17:48:49
   │ CdsPortfolioRepository.save() (SpringData)
   │
⑤ ─┘
    dataset [persistence]
    cds_portfolios (CREATE_PORTFOLIO)
    Tables Written: cds_portfolios
```

---

### 🔄 Transformations Section

**What It Shows:**
- Operation type (e.g., `CREATE_PORTFOLIO`)
- Business logic transformations
- Details about what changed

**Visual Elements:**
- Type badges: `operation`, `business_logic`
- Operation names as code blocks
- Expandable details with JSON formatting

**Example Display:**
```
🔄 Transformations - How Data Changed

┌─ operation: CREATE_PORTFOLIO
│  Primary data transformation for CREATE_PORTFOLIO
│
└─ business_logic: portfolio_created
   {
     "name": "Frontend Demo Portfolio",
     "status": "ACTIVE",
     "portfolio_id": 23
   }
```

---

### 📊 Consumers Section

**What It Shows:**
- Dataset consumers
- API response consumers
- Downstream systems (future)

**Visual Elements:**
- Type badges: `dataset`, `api_response`, `downstream_system`
- Consumer names as code blocks
- Descriptions in muted text

**Example Display:**
```
📊 Consumers - Who Uses This Data

┌─ dataset: cds_portfolios
│  Primary consumer - data persisted to cds_portfolios
│
└─ api_response: CdsPortfolioDTO
   Data returned to API client
```

---

### 📋 Metadata Section

**What It Shows:**
- Compliance information (recorded_at, user, run_id, source)
- Performance metrics (duration, start time)
- Tracking data (correlation ID, HTTP method, endpoint)
- Audit trail (IP address, user agent, session ID)
- Confidence scores with progress bars
- Automated capture flags

**Visual Elements:**
- 4-column grid layout:
  - Compliance
  - Performance
  - Tracking
  - Audit
- Progress bars for confidence scores
- Green checkmarks (✓) for good states
- Yellow warnings (⚠) for review flags
- Color-coded percentages (100% = green)

**Example Display:**
```
📋 Metadata - Compliance & Audit Trail

┌─ Compliance ──────────────┐  ┌─ Performance ─────────────┐
│ Recorded: 11/11/25 5:48 PM│  │ Duration: 227ms           │
│ User: system               │  │ Start: 11/11/25 5:48 PM   │
│ Run ID: portfolio-CREATE-23│  │                           │
│ Source: runtime            │  │                           │
└────────────────────────────┘  └───────────────────────────┘

┌─ Tracking ────────────────┐  ┌─ Audit ───────────────────┐
│ Correlation:               │  │ IP: 192.168.143.2         │
│ e3c41615-63bf-4e6c-8e50... │  │ User Agent: PowerShell/5.1│
│ HTTP: POST                 │  │                           │
│ Endpoint: /api/cds-port... │  │                           │
└────────────────────────────┘  └───────────────────────────┘

Lineage Confidence Scores:
controller → service   ████████████████████ 100%
service → repository   ████████████████████ 100%
repository → table     ████████████████████ 100%

✓ Automated Capture: Yes
✓ Manual Review: Not Required
```

---

## How to Use

### 1. Access Lineage Page

Navigate to: `http://localhost:3000/lineage`

### 2. Search for Lineage

**Option A: Search by Correlation ID** (Recommended for single operations)
1. Create a portfolio/trade via API
2. Check backend logs for correlation ID:
   ```powershell
   docker logs credit-default-swap-backend-1 --tail 20 | Select-String "correlation"
   ```
3. Enter correlation ID in the search box
4. Click "Fetch Lineage"

**Option B: Search by Dataset** (Shows all operations)
1. Select dataset from dropdown (e.g., `cds_portfolios`)
2. Click "Fetch Lineage"

**Option C: Search by Run ID**
1. Enter run ID (e.g., `portfolio-CREATE-23`)
2. Click "Fetch Lineage"

**Option D: Recent Activity**
1. Click "Recent Activity" radio button
2. Click "Fetch Lineage"

### 3. View Structured Lineage

Scroll down to the **📜 Structured Lineage Document** section.

**Sections:**
- **📍 Origin** - (Open by default) - Click to collapse/expand
- **🛤️ Path** - (Open by default) - Timeline view of data flow
- **🔄 Transformations** - Click to expand
- **📊 Consumers** - Click to expand
- **📋 Metadata** - Click to expand

### 4. Explore Details

- Click on any section header to expand/collapse
- Scroll through path timeline to see each hop
- Review confidence scores in metadata
- Check audit trail for compliance

---

## Testing Instructions

### Test Case 1: Create Portfolio and View Lineage

```powershell
# 1. Create portfolio
$body = @{ 
    name = 'Test Structured Lineage Portfolio'
    description = 'Testing frontend visualization' 
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/cds-portfolios" `
    -Method POST -ContentType "application/json" -Body $body

# 2. Get correlation ID
docker logs credit-default-swap-backend-1 --tail 20 | Select-String "correlation"

# 3. Open frontend
# Navigate to http://localhost:3000/lineage

# 4. Search by correlation ID
# Paste correlation ID and click "Fetch Lineage"

# 5. Verify structured lineage sections appear
```

### Test Case 2: Multiple Portfolios (Dataset View)

```powershell
# 1. Create 3 portfolios
1..3 | ForEach-Object {
    $body = @{ 
        name = "Portfolio $_"
        description = "Test portfolio $_" 
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/cds-portfolios" `
        -Method POST -ContentType "application/json" -Body $body
}

# 2. Open frontend and search by dataset "cds_portfolios"

# 3. Verify multiple operations appear in graph
```

---

## UI Layout

### Page Structure (Top to Bottom)

1. **Header** - Title and description
2. **Controls** - Search type selector and search inputs
3. **Stats Cards** (left) + **Audit Information** (right) - Side-by-side layout
4. **📜 Structured Lineage Document** ← NEW SECTION
   - Origin (expanded)
   - Path (expanded)
   - Transformations (collapsed)
   - Consumers (collapsed)
   - Metadata (collapsed)
5. **Debug Info** - Raw JSON data (collapsed)
6. **Lineage Graph** - React Flow visualization
7. **Events Table** - List of lineage events

---

## Color Scheme

Following AGENTS.md guidelines:

| Color | Usage |
|-------|-------|
| `RGB(0, 240, 0)` / `#00F000` | Green badges, code blocks, section headers |
| `RGB(60, 75, 97)` / `#3C4B61` | Dark backgrounds for cards |
| `RGB(0, 232, 247)` / `#00E8F7` | Accent color (not heavily used) |
| `RGB(30, 230, 190)` / `#1EE6BE` | Secondary green |
| `RGB(0, 255, 195)` / `#00FFC3` | Tertiary green |
| `RGB(255, 255, 255)` / `#FFFFFF` | Text color |

**Semantic Colors:**
- Success: Green (`text-fd-green`)
- Warning: Yellow (`text-yellow-400`)
- Error: Red (`text-red-400`)
- Muted: Gray (`text-fd-text-muted`)

---

## Benefits

### For Developers
- **Quick Debugging:** See exact path data took through system
- **Performance Analysis:** Duration and timestamps at each hop
- **Confidence Scores:** Know reliability of lineage capture

### For Data Governance
- **Complete Audit Trail:** IP, user, timestamps, correlation IDs
- **Source Tracking:** Know where data originated
- **Compliance Ready:** All required metadata captured

### For Business Users
- **Visual Timeline:** Easy-to-understand flow diagram
- **Transformation Tracking:** See what changed and why
- **Consumer Visibility:** Know who uses the data

---

## Technical Implementation

### Frontend Changes

**File:** `frontend/src/pages/LineagePage.tsx`

**Key Additions:**
- New `<details>` sections for each lineage component
- Conditional rendering based on `lineageEvents[0].outputs.origin/path/transformations/consumers/metadata`
- Timeline visualization for path section with numbered circles and connecting lines
- Progress bars for confidence scores
- Grid layouts for metadata (2x2 grid for compliance/performance/tracking/audit)

**Dependencies:**
- React (existing)
- Tailwind CSS (existing)
- No new packages required

### Backend (No Changes Required)

The backend already provides structured lineage documents via:
- `LineageService.buildLineageDocument()`
- Returns JSON with 5 sections in `lineage_events.outputs` column

### Data Flow

```
User Action (Create Portfolio)
  ↓
Backend AOP Aspects Capture Lineage
  ↓
LineageService.buildLineageDocument()
  ↓
Database (JSONB in lineage_events.outputs)
  ↓
Frontend API Call (getGraphForCorrelation/Dataset/Run/Recent)
  ↓
LineagePage.tsx Renders Structured Sections
  ↓
User Sees 5-Section Expandable View
```

---

## Future Enhancements

### Phase 1: Interactive Features (Recommended)
- Click on path stage to highlight corresponding graph node
- Filter transformations by type (operation vs business logic)
- Expand/collapse all sections with one button
- Export structured lineage as PDF/CSV

### Phase 2: Advanced Visualizations
- Sankey diagram for data flow
- Swimlane diagram showing layer transitions
- Timeline chart for performance analysis
- Interactive confidence score tooltips with explanations

### Phase 3: Search & Filtering
- Search within structured lineage (find specific transformation)
- Filter consumers by type
- Date range filtering for metadata
- Compare lineage across multiple operations

### Phase 4: Governance Integrations
- Link to OpenLineage external tools
- Export to compliance reporting systems
- Automated alerts for low confidence scores
- Integration with Marquez lineage platform

---

## Screenshots Reference

### Expected UI Layout

```
┌────────────────────────────────────────────────────────────┐
│ Data Lineage Explorer                                       │
│ Visualize data flow and transformations                    │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ 🔍 Search by Correlation ID ○ Dataset ○ Run ID ○ Recent   │
│ [Correlation ID Input]                     [Fetch Lineage] │
└────────────────────────────────────────────────────────────┘

┌──────────────┐  ┌───────────────────────────────────────┐
│ Graph Nodes  │  │ 🔐 Audit Information                  │
│     7        │  │ 👤 User: system                       │
├──────────────┤  │ 🌐 IP: 192.168.143.2                  │
│ Graph Edges  │  │ 🖥️ Browser: PowerShell/5.1            │
│     6        │  │ ⏱️ Duration: 227ms                    │
├──────────────┤  │                                       │
│ Node Types   │  │                                       │
│     5        │  │                                       │
└──────────────┘  └───────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ 📜 Structured Lineage Document                             │
│                                                            │
│ ▼ 📍 Origin - Where Data Started                          │
│   Primary Dataset: cds_portfolios                         │
│   Source Type: database_table                             │
│   Input Sources: [2 sources shown]                        │
│                                                            │
│ ▼ 🛤️ Path - Every Hop Data Took                           │
│   ① → ② → ③ → ④ → ⑤ (Timeline with details)              │
│                                                            │
│ ▶ 🔄 Transformations - How Data Changed                   │
│                                                            │
│ ▶ 📊 Consumers - Who Uses This Data                       │
│                                                            │
│ ▶ 📋 Metadata - Compliance & Audit Trail                  │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ ▶ 🔍 Debug: View Raw Lineage Data (Click to expand)       │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ Lineage Graph                                              │
│ [React Flow Visualization]                                 │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ Lineage Events (Table)                                     │
│ | Timestamp | Dataset | Operation | Run ID | User |        │
└────────────────────────────────────────────────────────────┘
```

---

## Troubleshooting

### Issue: Structured Lineage Not Showing

**Symptoms:**
- Graph and events appear, but no structured lineage section

**Possible Causes:**
1. Lineage events created before backend deployment
2. `outputs` field doesn't contain structured document

**Solution:**
```powershell
# Clear old lineage data
docker exec -it credit-default-swap-db-1 psql -U cdsuser -d cdsplatform -c "DELETE FROM lineage_events;"

# Create new portfolio
$body = @{ name = 'New Portfolio'; description = 'Test' } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/cds-portfolios" -Method POST -ContentType "application/json" -Body $body

# Verify structured output
docker exec -it credit-default-swap-db-1 psql -U cdsuser -d cdsplatform -c "SELECT outputs->'origin', outputs->'path' FROM lineage_events ORDER BY created_at DESC LIMIT 1;"
```

### Issue: Sections Not Expanding

**Symptoms:**
- Click on section header, nothing happens

**Possible Causes:**
1. Browser caching old JavaScript
2. Frontend build issue

**Solution:**
```powershell
# Rebuild frontend
docker-compose up --build -d frontend

# Clear browser cache (Ctrl+Shift+R in Chrome/Edge)
# Or open in incognito mode
```

### Issue: Confidence Scores Not Showing

**Symptoms:**
- Metadata section opens but confidence scores missing

**Possible Causes:**
1. `lineage_confidence` field not in metadata
2. Old lineage events

**Solution:**
Create new event after backend deployment (see Issue 1 solution)

---

## Validation Checklist

✅ **Backend Deployed:** Structured lineage generation working  
✅ **Frontend Deployed:** New UI sections added  
✅ **Database:** lineage_events contains origin/path/transformations/consumers/metadata  
✅ **UI Rendering:** All 5 sections display correctly  
✅ **Expandable Sections:** Click to expand/collapse works  
✅ **Path Timeline:** Numbered stages with connecting lines  
✅ **Confidence Scores:** Progress bars showing 100%  
✅ **Responsive Design:** Layout works on mobile/desktop  

---

## Conclusion

The frontend now provides a **production-ready, user-friendly view** of structured lineage documents. Users can:

- ✅ See complete data origin (sources, systems, inputs)
- ✅ Trace exact path through every layer (presentation → business → data access → persistence)
- ✅ Understand transformations (operation + business logic)
- ✅ Identify consumers (datasets, APIs, downstream systems)
- ✅ Access full audit trail (compliance, performance, tracking, audit info)

This implementation follows all data lineage best practices and provides comprehensive governance visibility.

**Next Steps:** Test with real user scenarios and consider Phase 1 enhancements (interactive features, export options).
