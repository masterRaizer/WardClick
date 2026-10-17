package com.wardclick.app.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class PriceCalculator {
    public static String formatPrice(BigDecimal amount) {
        if (amount == null) return "0 ₽";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount) + " ₽";
    }

    public static BigDecimal calculateTotalPrice(BigDecimal pricePerNight, long nights) {
        if (pricePerNight == null || nights <= 0) return BigDecimal.ZERO;
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }
}
