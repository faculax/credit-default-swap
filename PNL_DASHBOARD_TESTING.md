# P&L Dashboard Enhancement - Testing Guide

## Quick Test Checklist

### 🎯 Pre-Test Setup
1. Ensure all containers are running:
   ```powershell
   docker ps | Select-String "credit-default-swap"
   ```
2. Verify backend is healthy (should show "Up X hours (healthy)")
3. Navigate to: http://localhost:3000/daily-pnl

---

## Test Scenarios

### ✅ Test 1: Attribution Chart Display
**Steps:**
1. Select date: `2025-01-15`
2. Wait for data to load

**Expected Results:**
- [ ] Attribution chart appears below summary cards
- [ ] Shows 6 colored bars:
  - Blue: Market P&L
  - Green: Theta P&L
  - Cyan: Accrued P&L
  - Red: Credit Event P&L
  - Purple: Trade P&L
  - Yellow: Unexplained P&L
- [ ] Each bar shows dollar amount on right side
- [ ] Bar widths proportional to absolute values
- [ ] "Hide" button present in top-right of chart

---

### ✅ Test 2: Attribution Chart Toggle
**Steps:**
1. Click "Hide" button on attribution chart
2. Observe chart collapses
3. Click "Show P&L Attribution Chart" button

**Expected Results:**
- [ ] Chart collapses smoothly when "Hide" clicked
- [ ] "Show P&L Attribution Chart" button appears
- [ ] Chart expands again when "Show" clicked
- [ ] State persists during tab switches

---

### ✅ Test 3: Entity Drill-Down View
**Steps:**
1. Click "🏢 Entity View" button
2. Observe entities listed
3. Click first entity row to expand
4. Click again to collapse

**Expected Results:**
- [ ] View switches from table to entity grouping
- [ ] Entities sorted by absolute P&L (largest first)
- [ ] Each entity shows:
  - Entity name
  - Trade count
  - Total P&L (color-coded)
- [ ] Expanding entity reveals:
  - 4 summary metrics (Market/Theta/Accrued/Trade count)
  - Nested table of individual trades
  - Trade details (ID, P&L components, flags)
- [ ] Clicking row toggles expand/collapse (▶/▼ arrow changes)

---

### ✅ Test 4: Entity View Multiple Expansions
**Steps:**
1. In Entity View, expand 3 different entities
2. Verify each expands independently

**Expected Results:**
- [ ] Multiple entities can be expanded simultaneously
- [ ] Each maintains its own expand/collapse state
- [ ] Scrolling works smoothly
- [ ] Trade tables render correctly for each entity

---

### ✅ Test 5: Switch Between Views
**Steps:**
1. Start in Table View (📊)
2. Switch to Entity View (🏢)
3. Expand an entity
4. Switch back to Table View
5. Switch to Entity View again

**Expected Results:**
- [ ] Active button highlighted with primary color background
- [ ] Inactive button shows border only
- [ ] Content switches smoothly
- [ ] Entity expansion state does NOT persist (collapses all when switching)
- [ ] Tab selection (All/Winners/Losers) persists across view switches

---

### ✅ Test 6: CSV Export
**Steps:**
1. Ensure data is loaded (any date)
2. Click "📥 Export CSV" button
3. Open downloaded file

**Expected Results:**
- [ ] File downloads immediately
- [ ] Filename: `daily-pnl-2025-01-15.csv` (with actual date)
- [ ] CSV opens in Excel/Google Sheets correctly
- [ ] Headers present:
  - Trade ID, Entity, Total P&L, Market Movement, Time Decay, Accrued Interest, Credit Events, New Trades, Unexplained
- [ ] All visible trades included
- [ ] Numbers formatted correctly (2 decimal places)
- [ ] No quotes around entity names (unless contains comma)

---

### ✅ Test 7: CSV Export with Empty Data
**Steps:**
1. Select a date with no data (e.g., `2025-12-31`)
2. Observe "Export CSV" button

**Expected Results:**
- [ ] Button is disabled (grayed out)
- [ ] Cursor shows "not-allowed" on hover
- [ ] Clicking does nothing
- [ ] No error messages

---

### ✅ Test 8: Attribution with Large P&L Values
**Steps:**
1. Load date with significant P&L (use test data from `setup-daily-pnl-test-data.ps1`)
2. Observe attribution bars

**Expected Results:**
- [ ] Largest bar fills ~100% width
- [ ] Smaller bars scale proportionally
- [ ] Very small values still visible (minimum bar width)
- [ ] Dollar amounts readable at all sizes
- [ ] No bar overflow or layout breaking

---

### ✅ Test 9: Attribution with Negative P&L
**Steps:**
1. Find date with negative P&L (check "Top Losers" tab first)
2. View attribution chart

**Expected Results:**
- [ ] Negative values show in red text
- [ ] Positive values show in green text
- [ ] Bar widths based on absolute value
- [ ] All bars align left (no negative space to left of bars)

---

### ✅ Test 10: Responsive Design (Desktop)
**Steps:**
1. Test at 1920x1080 resolution
2. Resize browser to 1366x768

**Expected Results:**
- [ ] Layout adjusts gracefully
- [ ] No horizontal scrolling on main container
- [ ] Tables scroll horizontally if needed
- [ ] Attribution bars stay within container
- [ ] Entity groups don't overlap
- [ ] Export button stays visible

---

### ✅ Test 11: Tab Interaction (Known Limitation)
**Steps:**
1. Switch to "Top Winners" tab
2. Switch to Entity View
3. Expand an entity

**Expected Results (Current Behavior):**
- [ ] ⚠️ Entity view shows ALL trades (ignores tab filter)
- [ ] Entity totals include all trades, not just winners
- [ ] **NOTE**: This is a known limitation documented in PNL_DASHBOARD_ENHANCEMENTS.md

**Future Fix:**
- Filter entity grouping by active tab
- Only aggregate trades matching tab criteria

---

### ✅ Test 12: Performance with Large Data Sets
**Steps:**
1. Load date with 100+ trades (if available)
2. Switch to Entity View
3. Expand multiple entities
4. Export to CSV

**Expected Results:**
- [ ] Initial load completes in <2 seconds
- [ ] Entity view renders in <1 second
- [ ] Expanding entities feels instant (<100ms)
- [ ] CSV export completes in <1 second
- [ ] No browser freezing or lag
- [ ] Smooth scrolling

---

## Bug Reporting Template

If you find issues, report using this format:

```markdown
**Test**: Test #X - [Test Name]
**Step**: Step Y
**Expected**: [What should happen]
**Actual**: [What actually happened]
**Browser**: Chrome/Firefox/Edge [Version]
**Screenshot**: [Attach if relevant]
**Console Errors**: [Check browser console - F12]
```

---

## Smoke Test (Quick Validation)

Run this 2-minute test after any deployment:

1. ✅ Open Daily P&L Dashboard
2. ✅ Select date `2025-01-15`
3. ✅ Verify attribution chart appears
4. ✅ Click "Entity View"
5. ✅ Expand one entity
6. ✅ Click "Export CSV"
7. ✅ Open CSV file

**If all pass**: ✅ Deploy to next environment  
**If any fail**: ❌ Rollback and investigate

---

## Test Data Setup

If you need fresh test data:

```powershell
# Run test data setup script
.\setup-daily-pnl-test-data.ps1

# Creates data for:
# - 2025-01-15 (mixed P&L, good for testing)
# - 2025-01-16 (mostly positive)
# - 2025-01-17 (mostly negative)
```

---

## Known Issues (Non-Blocking)

1. **Entity View Tab Filtering**: Entity view shows all trades regardless of active tab
   - **Workaround**: Use Table View for tab-filtered data
   - **Fix Priority**: LOW (cosmetic, documented)

2. **CSV Export Tab Filtering**: CSV exports all trades, not respecting tabs
   - **Workaround**: Switch to desired tab in Table View first (visual confirmation)
   - **Fix Priority**: MEDIUM (user expectation mismatch)

3. **Mobile View**: Not optimized for screens <768px wide
   - **Workaround**: Use desktop/tablet
   - **Fix Priority**: LOW (admin tool, desktop primary use case)

---

## Success Criteria

All tests must pass for production release:
- [ ] All ✅ checkboxes marked in Tests 1-12
- [ ] No critical console errors
- [ ] CSV export works correctly
- [ ] Performance acceptable (<2s load)
- [ ] No visual glitches or overlaps
- [ ] Works in Chrome, Firefox, Edge (latest versions)

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-14  
**Author**: GitHub Copilot (AI Agent)  
**Related Docs**: PNL_DASHBOARD_ENHANCEMENTS.md, QUICK_START_DAILY_PNL.md
