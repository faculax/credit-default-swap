package com.creditdefaultswap.platform.dto.eod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSnapshotRequest {
    private LocalDate snapshotDate;
    private String capturedBy;
    private List<CdsSpreadDto> cdsSpreads;
    private List<IrCurveDto> irCurves;
    private List<FxRateDto> fxRates;
    private List<RecoveryRateDto> recoveryRates;
}
