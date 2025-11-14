# 🎯 Quick Start: Testing Daily P&L on the UI

## TL;DR - 3 Steps to See Your P&L

### 1️⃣ Create Test Trades (UI)
1. Open `http://localhost:3000`
2. Click **"New Single-Name CDS"**
3. Create 3-5 trades with different notionals

### 2️⃣ Run EOD Job for 2 Days (API)

**PowerShell:**
```powershell
# Day 1 (T-1) - Yesterday
$date1 = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$date1&triggeredBy=TEST&dryRun=false" -Method POST

# Wait ~30 seconds for job to complete

# Day 2 (T) - Today
$date2 = (Get-Date).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$date2&triggeredBy=TEST&dryRun=false" -Method POST
```

**Bash:**
```bash
# Day 1 (T-1)
curl -X POST "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$(date -d yesterday +%Y-%m-%d)&triggeredBy=TEST&dryRun=false"

# Day 2 (T)
curl -X POST "http://localhost:8080/api/eod/valuation-jobs/trigger?valuationDate=$(date +%Y-%m-%d)&triggeredBy=TEST&dryRun=false"
```

### 3️⃣ View P&L Dashboard (UI)
1. Go to `http://localhost:3000`
2. Click **"Daily P&L"** button in top nav
3. Select today's date
4. 🎉 See your P&L with full attribution!

---

## 📊 What You'll See

### Summary Cards (Top)
- **Total P&L**: Overall profit/loss
- **Market P&L**: From spread & rate moves (CS01 + IR01)
- **Theta P&L**: Time decay / carry
- **Accrued P&L**: Accrued interest change
- **Flags**: Large movers, unexplained, new trades

### Tabs (Bottom)
- **All Trades**: Complete list with attribution breakdown
- **Top Winners**: Best performing trades
- **Top Losers**: Worst performing trades
- **Large Movers**: |P&L| > $50,000
- **Unexplained**: Attribution gap > 10%

---

## 🧪 Quick Verification

Check P&L was calculated:
```powershell
# PowerShell
$date = (Get-Date).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/api/eod/daily-pnl/date/$date/summary"
```

Expected response:
```json
{
  "date": "2025-11-13",
  "totalTrades": 5,
  "totalPnl": 12500.00,
  "totalMarketPnl": 8000.00,
  "totalThetaPnl": 3500.00,
  "totalAccruedPnl": 1000.00,
  "largeMoversCount": 0,
  "unexplainedCount": 0,
  "creditEventsCount": 0,
  "newTradesCount": 5
}
```

---

## ❓ Troubleshooting

**"No P&L data found"**
- Did you run EOD job for **both** T-1 and T?
- P&L requires 2 days of valuations to compare

**P&L is all zeros**
- Expected if market hasn't moved between days
- Try creating trades on different days

**Backend won't start**
```powershell
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

**Frontend won't start**
```powershell
cd frontend
npm install
npm start
```

---

## 📖 Full Documentation

See [`DAILY_PNL_TESTING_GUIDE.md`](./DAILY_PNL_TESTING_GUIDE.md) for:
- Detailed test scenarios
- API endpoint reference
- Attribution accuracy checks
- Acceptance criteria

---

## 🏗️ Architecture

**Backend:**
- Controller: `DailyPnlController` - REST API endpoints
- Service: `DailyPnlService` - Business logic with attribution
- Model: `DailyPnlResult` - Entity with P&L breakdown
- API: `/api/eod/daily-pnl/*`

**Frontend:**
- Component: `DailyPnlDashboard.tsx`
- Route: Click "Daily P&L" in nav bar
- Features: Date picker, summary cards, tabbed views, color-coded P&L

**Database:**
- Table: `daily_pnl_results`
- Unique constraint: `(pnl_date, trade_id)`
- Calculated during: EOD job step 6 (CALCULATE_PNL)

---

**Need help?** Check logs or run tests:
```powershell
# Backend tests
cd backend
mvn test -Dtest=DailyPnlServiceTest

# View logs
Get-Content backend\logs\application.log -Tail 50
```
