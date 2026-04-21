package me.klouse.kauctionhouse.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberFormatUtil {

    private static final DecimalFormat NORMAL = new DecimalFormat("#,##0.##");
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    static {
        NORMAL.setDecimalFormatSymbols(SYMBOLS);
    }

    private NumberFormatUtil() {
    }

    public static String format(double amount) {
        double abs = Math.abs(amount);

        if (abs >= 1_000_000_000_000_000.0) {
            return formatCompact(amount / 1_000_000_000_000_000.0) + "Q";
        }
        if (abs >= 1_000_000_000_000.0) {
            return formatCompact(amount / 1_000_000_000_000.0) + "T";
        }
        if (abs >= 1_000_000_000.0) {
            return formatCompact(amount / 1_000_000_000.0) + "B";
        }
        if (abs >= 1_000_000.0) {
            return formatCompact(amount / 1_000_000.0) + "M";
        }
        if (abs >= 1_000.0) {
            return formatCompact(amount / 1_000.0) + "K";
        }

        return NORMAL.format(amount);
    }

    private static String formatCompact(double value) {
        double abs = Math.abs(value);

        DecimalFormat format;
        if (abs >= 100) {
            format = new DecimalFormat("0", SYMBOLS);
        } else if (abs >= 10) {
            format = new DecimalFormat("0.#", SYMBOLS);
        } else {
            format = new DecimalFormat("0.##", SYMBOLS);
        }

        String result = format.format(value);

        if (result.endsWith(".0")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }
}
