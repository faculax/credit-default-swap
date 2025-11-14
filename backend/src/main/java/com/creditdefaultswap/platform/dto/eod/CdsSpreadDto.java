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
public class CdsSpreadDto {
    private String referenceEntityName;
    private String tenor;
    private String currency;
    private String seniority;
    private String restructuringClause;
    private BigDecimal spread;
    private String dataSource;
}
