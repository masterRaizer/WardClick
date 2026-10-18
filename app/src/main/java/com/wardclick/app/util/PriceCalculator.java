package com.wardclick.app.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class PriceCalculator {
    public static String formatPrice(BigDecimal amount) {
        // неразрывные пробелы: «1 701 000 ₽» не переносится на новую строку посреди числа
        if (amount == null) return "0\u00A0₽";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('\u00A0');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount) + "\u00A0₽";
    }

    public static BigDecimal calculateTotalPrice(BigDecimal pricePerNight, long nights) {
        if (pricePerNight == null || nights <= 0) return BigDecimal.ZERO;
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }
}
