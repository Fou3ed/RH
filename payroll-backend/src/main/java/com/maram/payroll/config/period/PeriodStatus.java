package com.maram.payroll.config.period;

import java.util.Set;

/**
 * Payroll period lifecycle. {@link #canTransitionTo} encodes the allowed moves.
 */
public enum PeriodStatus {

    DRAFT,
    LOCKED,
    PROCESSING,
    FINALIZED,
    PAID;

    public boolean canTransitionTo(PeriodStatus target) {
        return switch (this) {
            case DRAFT -> target == LOCKED;
            case LOCKED -> target == PROCESSING || target == DRAFT; // DRAFT = unlock
            case PROCESSING -> target == FINALIZED;
            case FINALIZED -> target == PAID;
            case PAID -> false;
        };
    }

    public static Set<PeriodStatus> mutableStatuses() {
        return Set.of(DRAFT);
    }
}
