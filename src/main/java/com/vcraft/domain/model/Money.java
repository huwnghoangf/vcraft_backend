package com.vcraft.domain.model;
import com.vcraft.domain.exception.InvalidInputException;
import java.math.BigDecimal;
/** Matches PostgreSQL DECIMAL(15,2); reject silent database rounding and overflow. */
public final class Money {
    private Money() {}
    public static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new InvalidInputException("Số tiền phải lớn hơn 0");
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > 13)
            throw new InvalidInputException("Số tiền tối đa 13 chữ số nguyên và 2 chữ số thập phân");
        return amount;
    }
}
