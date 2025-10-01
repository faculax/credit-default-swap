package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.AmendTradeRequest;
import com.creditdefaultswap.platform.dto.NotionalAdjustmentRequest;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for trade lifecycle operations including coupon schedules,
 * accruals, amendments, and notional adjustments.
 */
@RestController
@RequestMapping("/api/lifecycle")
public class LifecycleController {

    @Autowired
    private CouponScheduleService couponScheduleService;

    @Autowired
    private AccrualService accrualService;

    @Autowired
    private TradeAmendmentService tradeAmendmentService;

    @Autowired
    private NotionalAdjustmentService notionalAdjustmentService;

    // Coupon Schedule Endpoints

    @PostMapping("/trades/{tradeId}/coupon-schedule")
    public ResponseEntity<List<CouponPeriod>> generateCouponSchedule(@PathVariable Long tradeId) {
        List<CouponPeriod> schedule = couponScheduleService.generateImmSchedule(tradeId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/trades/{tradeId}/coupon-schedule")
    public ResponseEntity<List<CouponPeriod>> getCouponSchedule(@PathVariable Long tradeId) {
        List<CouponPeriod> schedule = couponScheduleService.getCouponPeriods(tradeId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/trades/{tradeId}/coupon-schedule/range")
    public ResponseEntity<List<CouponPeriod>> getCouponScheduleInRange(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CouponPeriod> schedule = couponScheduleService.getCouponPeriodsInRange(tradeId, startDate, endDate);
        return ResponseEntity.ok(schedule);
    }

    // Accrual Endpoints

    @PostMapping("/trades/{tradeId}/accruals/daily")
    public ResponseEntity<AccrualEvent> postDailyAccrual(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accrualDate) {
        AccrualEvent accrual = accrualService.postDailyAccrual(tradeId, accrualDate);
        return ResponseEntity.ok(accrual);
    }

    @PostMapping("/trades/{tradeId}/accruals/period")
    public ResponseEntity<List<AccrualEvent>> postAccrualsForPeriod(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccrualEvent> accruals = accrualService.postAccrualsForPeriod(tradeId, startDate, endDate);
        return ResponseEntity.ok(accruals);
    }

    @GetMapping("/trades/{tradeId}/accruals")
    public ResponseEntity<List<AccrualEvent>> getAccrualEvents(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccrualEvent> accruals = accrualService.getAccrualEvents(tradeId, startDate, endDate);
        return ResponseEntity.ok(accruals);
    }

    @GetMapping("/trades/{tradeId}/accruals/cumulative")
    public ResponseEntity<Map<String, BigDecimal>> getCurrentCumulativeAccrual(@PathVariable Long tradeId) {
        BigDecimal cumulative = accrualService.getCurrentCumulativeAccrual(tradeId);
        return ResponseEntity.ok(Map.of("cumulativeAccrual", cumulative));
    }

    @GetMapping("/trades/{tradeId}/accruals/net-cash")
    public ResponseEntity<Map<String, BigDecimal>> getNetCashForPayment(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {
        BigDecimal netCash = accrualService.calculateNetCashForPayment(tradeId, paymentDate);
        return ResponseEntity.ok(Map.of("netCashAmount", netCash));
    }

    // Amendment Endpoints

    @PostMapping("/trades/{tradeId}/amendments")
    public ResponseEntity<List<TradeAmendment>> amendTrade(
            @PathVariable Long tradeId,
            @RequestBody AmendTradeRequest request) {
        List<TradeAmendment> amendments = tradeAmendmentService.amendTrade(
                tradeId, request.getAmendments(), request.getAmendmentDate(), request.getAmendedBy());
        return ResponseEntity.ok(amendments);
    }

    @GetMapping("/trades/{tradeId}/amendments")
    public ResponseEntity<List<TradeAmendment>> getTradeAmendments(@PathVariable Long tradeId) {
        List<TradeAmendment> amendments = tradeAmendmentService.getTradeAmendments(tradeId);
        return ResponseEntity.ok(amendments);
    }

    @GetMapping("/trades/{tradeId}/amendments/version/{version}")
    public ResponseEntity<List<TradeAmendment>> getAmendmentsForVersion(
            @PathVariable Long tradeId,
            @PathVariable Integer version) {
        List<TradeAmendment> amendments = tradeAmendmentService.getAmendmentsForVersion(tradeId, version);
        return ResponseEntity.ok(amendments);
    }

    // Notional Adjustment Endpoints

    @PostMapping("/trades/{tradeId}/notional-adjustments")
    public ResponseEntity<NotionalAdjustment> adjustNotional(
            @PathVariable Long tradeId,
            @RequestBody NotionalAdjustmentRequest request) {
        NotionalAdjustment adjustment = notionalAdjustmentService.adjustNotional(
                tradeId,
                request.getAdjustmentDate(),
                request.getAdjustmentType(),
                request.getAdjustmentAmount(),
                request.getAdjustmentReason()
        );
        return ResponseEntity.ok(adjustment);
    }

    @GetMapping("/trades/{tradeId}/notional-adjustments")
    public ResponseEntity<List<NotionalAdjustment>> getNotionalAdjustments(@PathVariable Long tradeId) {
        List<NotionalAdjustment> adjustments = notionalAdjustmentService.getNotionalAdjustments(tradeId);
        return ResponseEntity.ok(adjustments);
    }

    @PostMapping("/trades/{tradeId}/partial-termination")
    public ResponseEntity<NotionalAdjustment> partiallyTerminate(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
            @RequestParam BigDecimal terminationAmount,
            @RequestParam(required = false) String reason) {
        NotionalAdjustment termination = notionalAdjustmentService.partiallyTerminate(
                tradeId, terminationDate, terminationAmount, reason);
        return ResponseEntity.ok(termination);
    }

    @PostMapping("/trades/{tradeId}/full-termination")
    public ResponseEntity<NotionalAdjustment> fullyTerminate(
            @PathVariable Long tradeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
            @RequestParam(required = false) String reason) {
        NotionalAdjustment termination = notionalAdjustmentService.fullyTerminate(
                tradeId, terminationDate, reason);
        return ResponseEntity.ok(termination);
    }

    // Utility Endpoints

    @GetMapping("/trades/{tradeId}/summary")
    public ResponseEntity<Map<String, Object>> getLifecycleSummary(@PathVariable Long tradeId) {
        Map<String, Object> summary = Map.of(
                "couponPeriods", couponScheduleService.getCouponPeriods(tradeId).size(),
                "cumulativeAccrual", accrualService.getCurrentCumulativeAccrual(tradeId),
                "amendments", tradeAmendmentService.getTradeAmendments(tradeId).size(),
                "notionalAdjustments", notionalAdjustmentService.getNotionalAdjustments(tradeId).size()
        );
        return ResponseEntity.ok(summary);
    }
}