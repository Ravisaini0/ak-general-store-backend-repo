package com.akgeneralstore.util;

import java.math.BigDecimal;

public final class PriceUtil {

    private PriceUtil() {
    }

    public static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
