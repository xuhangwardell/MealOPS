package com.xuhang.mealops.measurement.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UnitTest {
    @Test void resolvesStableCodesAndBaseUnits() {
        assertThat(Unit.fromCode("g")).contains(Unit.GRAM);
        assertThat(Unit.fromCode("kg")).contains(Unit.KILOGRAM);
        assertThat(Unit.fromCode("ml")).contains(Unit.MILLILITER);
        assertThat(Unit.fromCode("l")).contains(Unit.LITER);
        assertThat(Unit.fromCode("piece")).contains(Unit.PIECE);
        assertThat(Unit.fromCode(null)).isEmpty();
        assertThat(Unit.fromCode("KG")).isEmpty();
        assertThat(Unit.fromCode("abc")).isEmpty();
        assertThat(Unit.GRAM.baseUnit()).isEqualTo(Unit.GRAM);
        assertThat(Unit.KILOGRAM.baseUnit()).isEqualTo(Unit.GRAM);
        assertThat(Unit.MILLILITER.baseUnit()).isEqualTo(Unit.MILLILITER);
        assertThat(Unit.LITER.baseUnit()).isEqualTo(Unit.MILLILITER);
        assertThat(Unit.PIECE.baseUnit()).isEqualTo(Unit.PIECE);
        assertThat(Unit.GRAM.isBaseUnit()).isTrue();
        assertThat(Unit.KILOGRAM.isBaseUnit()).isFalse();
        assertThat(Unit.LITER.isBaseUnit()).isFalse();
    }
}
