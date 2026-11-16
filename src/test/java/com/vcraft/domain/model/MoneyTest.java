package com.vcraft.domain.model;
import static org.assertj.core.api.Assertions.*;
import com.vcraft.domain.exception.InvalidInputException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class MoneyTest {
    @Test void rejectsRoundingAndDatabaseOverflow() {
        for(String value:new String[]{"1.001","10000000000000","0","-1"})
            assertThatThrownBy(() -> Money.positive(new BigDecimal(value))).isInstanceOf(InvalidInputException.class);
    }
    @Test void acceptsExactDatabaseLimitsAndInsignificantZeros() {
        for(String value:new String[]{"9999999999999.99","0.01","1.0000"})
            assertThat(Money.positive(new BigDecimal(value))).isEqualByComparingTo(value);
    }
}
