# 📊 Daily P&L Dashboard - User Testing Guide

## Overview
The Daily P&L Dashboard provides comprehensive profit & loss analytics with full attribution breakdown, allowing you to understand exactly where your P&L is coming from.

---

## 🚀 Getting Started

### 1. Start the Application

```powershell
# Terminal 1: Start backend
cd d:\Repos\credit-default-swap\backend
mvn spring-boot:run

# Terminal 2: Start frontend
cd d:\Repos\credit-default-swap\frontend
npm start
```

### 2. Access the Dashboard
1. Open your browser to `http://localhost:3000`
2. Click on the **"Daily P&L"** button in the top navigation bar

---

## 📝 Prerequisites: Generate Test Data

Before you can see P&L, you need:
1. **Trades** - Create some CDS trades
2. **EOD Valuations** - Run the EOD valuation job for 2 dates (T-1 and T)
3. **P&L Calculation** - P&L is calculated automatically during EOD job

### Step 1: Create Test Trades

Using the UI:
1. Click **"New Single-Name CDS"**
2. Fill in trade details:
   - Reference Entity: `Test Corp`
   - Notional: `10,000,000`
   - Spread: `250` bps
   - Maturity Date: 1 year from today
   - Buy/Sell Protection: `BUY_PROTECTION`
   - Other required fields (use defaults)
3. Click **"Submit Trade"**
4. Repeat to create 3-5 trades with different entities and notionals

### Step 2: Run EOD Valuation Job (Day T-1)

Using Postman/curl:
```bash
# Replace with yesterday's date
POST http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=2025-11-12&triggeredBy=TEST_USER&dryRun=false
```

Or using PowerShell:
```powershell
$date1 = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$date1&triggeredBy=TEST_USER&dryRun=false" -Method POST
```

**Expected:** Job should complete with status `COMPLETED`. This creates the T-1 valuations.

### Step 3: Run EOD Valuation Job (Day T)

```powershell
$date2 = (Get-Date).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$date2&triggeredBy=TEST_USER&dryRun=false" -Method POST
```

**Expected:** Job should complete and **automatically calculate P&L** by comparing T vs T-1 valuations.

---

## 🧪 Testing Scenarios

### Test 1: View P&L Summary

**Steps:**
1. Navigate to **Daily P&L** page
2. Select today's date in the date picker
3. Observe the summary cards at the top

**Expected Results:**
- **Total P&L**: Shows aggregate P&L for all trades
- **Market P&L**: P&L from spread and rate moves (CS01 + IR01)
- **Theta P&L**: Time decay / carry component
- **Accrued P&L**: Change in accrued interest
- **Flags**: Count of large movers, unexplained, credit events, new trades

**What to Check:**
- ✅ Total P&L = Market P&L + Theta P&L + Accrued P&L + Unexplained
- ✅ Colors: Green for positive, Red for negative
- ✅ All values formatted as currency ($X,XXX)

---

### Test 2: View All Trades Tab

**Steps:**
1. Stay on the **"All Trades"** tab (default)
2. Scroll through the table

**Expected Results:**
- All trades with P&L for selected date
- Each row shows:
  - Trade ID
  - Reference Entity
  - Total P&L (with percentage change)
  - Attribution breakdown (Market, Theta, Accrued, Unexplained)
  - Flags (LARGE, UNEXP, CE, NEW)

**What to Check:**
- ✅ P&L values align with summary totals
- ✅ Flags appear correctly:
  - **LARGE** (orange): P&L > $50,000 or < -$50,000
  - **UNEXP** (yellow): Unexplained P&L > 10%
  - **NEW** (blue): Trade has no T-1 valuation

---

### Test 3: Top Winners Tab

**Steps:**
1. Click **"Top Winners"** tab
2. Review the list

**Expected Results:**
- Trades sorted by P&L (highest first)
- Top 20 winners displayed
- All P&L values should be positive or highest negatives

**What to Check:**
- ✅ Trades ordered correctly (descending P&L)
- ✅ Limited to 20 results
- ✅ P&L values in green

---

### Test 4: Top Losers Tab

**Steps:**
1. Click **"Top Losers"** tab
2. Review the list

**Expected Results:**
- Trades sorted by P&L (lowest first)
- Top 20 losers displayed
- Most P&L values should be negative

**What to Check:**
- ✅ Trades ordered correctly (ascending P&L)
- ✅ Limited to 20 results
- ✅ P&L values in red

---

### Test 5: Large Movers Tab

**Steps:**
1. Click **"Large Movers"** tab
2. Review flagged trades

**Expected Results:**
- Only trades with `largePnlFlag = true`
- These are trades with |P&L| > $50,000

**What to Check:**
- ✅ All displayed trades have LARGE flag
- ✅ P&L values are above threshold (> $50k or < -$50k)

---

### Test 6: Unexplained P&L Tab

**Steps:**
1. Click **"Unexplained"** tab
2. Review flagged trades

**Expected Results:**
- Only trades with `unexplainedPnlFlag = true`
- These have attribution gap > 10%

**What to Check:**
- ✅ All displayed trades have UNEXP flag
- ✅ Unexplained P&L column shows significant values
- ✅ Formula: `Unexplained = Total - (Market + Theta + Accrued + Credit Event)`

---

### Test 7: Date Selection

**Steps:**
1. Change the date picker to yesterday's date
2. Observe the data change

**Expected Results:**
- Summary cards update with T-1 data
- Trade table refreshes with T-1 P&L
- If no data exists for that date, see error message: "No P&L data found for YYYY-MM-DD"

**What to Check:**
- ✅ Date picker updates correctly
- ✅ API calls made with correct date parameter
- ✅ No data scenario handled gracefully

---

### Test 8: Attribution Breakdown Accuracy

**Steps:**
1. Pick a single trade from the "All Trades" tab
2. Note down the values:
   - Total P&L
   - Market P&L
   - Theta P&L
   - Accrued P&L
   - Unexplained P&L
3. Manually calculate: `Total = Market + Theta + Accrued + Unexplained`

**Expected Results:**
- Values should add up correctly (within rounding tolerance of $1)

**What to Check:**
- ✅ `Total P&L ≈ Market + Theta + Accrued + Credit Event + Unexplained`
- ✅ If unexplained > 10% of total, UNEXP flag shown

---

## 🐛 Troubleshooting

### Issue: "No P&L data found"

**Possible Causes:**
1. EOD job not run for that date
2. EOD job failed before P&L step
3. No trades exist with valuations for both T and T-1

**Solution:**
1. Check EOD job status:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/eod/valuation-jobs/by-date/$((Get-Date).ToString('yyyy-MM-dd'))"
   ```
2. Verify job completed successfully
3. Check job steps - ensure step 6 (CALCULATE_PNL) completed

### Issue: All P&L values are zero

**Possible Cause:**
- Market data hasn't changed between T-1 and T
- Valuations are identical

**Solution:**
- This is expected if market hasn't moved
- To test, modify credit spreads in database or use mock data

### Issue: Unexplained P&L is large

**Possible Cause:**
- Attribution model incomplete
- Missing risk factors (FX, recovery)
- Calculation bugs

**Expected:**
- Small unexplained (< 5% of total) is normal
- Large unexplained (> 10%) triggers flag for investigation

---

## 📊 API Endpoints Available

Test these directly with Postman/curl:

```bash
# Get P&L summary for a date
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/summary

# Get all P&L for a date
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13

# Get specific trade P&L
GET http://localhost:8080/api/eod/daily-pnl/trade/123?date=2025-11-13

# Get top winners (limit=20)
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/winners?limit=20

# Get top losers
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/losers?limit=20

# Get large movers
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/large-movers

# Get unexplained P&L
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/unexplained

# Get credit event P&L
GET http://localhost:8080/api/eod/daily-pnl/date/2025-11-13/credit-events
```

---

## ✅ Acceptance Criteria Checklist

- [ ] Dashboard loads without errors
- [ ] Date picker allows selecting any date
- [ ] Summary cards display correct totals
- [ ] Summary cards color-code P&L (green/red)
- [ ] All Trades tab shows complete list
- [ ] Top Winners shows sorted descending
- [ ] Top Losers shows sorted ascending
- [ ] Large Movers filtered correctly
- [ ] Unexplained filtered correctly
- [ ] Flags display correctly (LARGE, UNEXP, NEW, CE)
- [ ] Attribution adds up: Total ≈ Market + Theta + Accrued + Unexplained
- [ ] Currency formatting works ($X,XXX,XXX)
- [ ] Percentage formatting works (X.XX%)
- [ ] No data scenario handled gracefully
- [ ] Loading states shown during API calls
- [ ] Error messages displayed when API fails

---

## 🎯 Expected User Flow

1. User books trades during the day
2. At EOD, operations team triggers EOD job (or scheduled job runs automatically)
3. EOD job runs 7 steps including P&L calculation
4. Next morning, traders open Daily P&L dashboard
5. Review summary to see overnight P&L
6. Investigate large movers and unexplained P&L
7. Drill into specific trades to understand attribution
8. Use insights for risk management and hedging decisions

---

## 📞 Support

If you encounter issues:
1. Check browser console for JavaScript errors
2. Check backend logs: `backend/logs/` or terminal output
3. Verify database has data:
   ```sql
   SELECT COUNT(*) FROM daily_pnl_results WHERE pnl_date = '2025-11-13';
   ```
4. Test API endpoints directly with Postman
5. Review backend unit tests for expected behavior

---

**Happy Testing! 🚀**
