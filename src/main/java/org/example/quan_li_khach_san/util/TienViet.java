package org.example.quan_li_khach_san.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component("tienViet")
public class TienViet {
    public String dinhDang(BigDecimal value) {
        if (value == null) return "—";
        // Mỗi lần định dạng dùng một đối tượng riêng để các yêu cầu đồng thời không dùng chung trạng thái.
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMinimumFractionDigits(value.stripTrailingZeros().scale() > 0 ? 2 : 0);
        format.setMaximumFractionDigits(2);
        return format.format(value) + " đ";
    }
}
