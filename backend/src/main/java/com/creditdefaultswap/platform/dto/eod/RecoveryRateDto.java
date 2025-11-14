package com.creditdefaultswap.platform.dto.eod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryRateDto {
    private String referenceEntityName;
    private String seniority;
    private BigDecimal recoveryRate;
    private String dataSource;
}
