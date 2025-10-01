package com.creditdefaultswap.platform.model;

public enum TradeStatus {
    PENDING,
    ACTIVE,
    CREDIT_EVENT_RECORDED,
    TRIGGERED,
    SETTLED_CASH,
    SETTLED_PHYSICAL,
    CANCELLED
}