package org.example.quan_li_khach_san.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component("tienViet")
public class TienViet {
    public String format(BigDecimal value) {
        if (value == null) return "—";
        // NumberFormat is mutable and not thread-safe: create it per rendering call.
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMinimumFractionDigits(value.stripTrailingZeros().scale() > 0 ? 2 : 0);
        format.setMaximumFractionDigits(2);
        return format.format(value) + " đ";
    }
}
