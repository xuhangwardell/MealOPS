package com.xuhang.mealops.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IngredientNameTest {

    @Test
    void keepsChineseDisplayNameAndNormalizedValue() {
        IngredientName name = IngredientName.of("鸡蛋");

        assertThat(name.displayValue()).isEqualTo("鸡蛋");
        assertThat(name.normalizedValue()).isEqualTo("鸡蛋");
    }

    @Test
    void trimsAndCollapsesUnicodeWhitespace() {
        IngredientName name = IngredientName.of("\u2003 鸡蛋\u00a0\u2009 土豆 \n");

        assertThat(name.displayValue()).isEqualTo("鸡蛋 土豆");
    }

    @Test
    void appliesNfkcToFullWidthText() {
        IngredientName name = IngredientName.of("Ｅｇｇ");

        assertThat(name.displayValue()).isEqualTo("Egg");
        assertThat(name.normalizedValue()).isEqualTo("egg");
    }

    @Test
    void usesLocaleRootForCaseNormalization() {
        IngredientName name = IngredientName.of("EGG");

        assertThat(name.normalizedValue()).isEqualTo("egg");
    }

    @Test
    void rejectsBlankAndWhitespaceOnlyNames() {
        assertThatThrownBy(() -> IngredientName.of(null))
                .isInstanceOf(InvalidIngredientNameException.class);
        assertThatThrownBy(() -> IngredientName.of(""))
                .isInstanceOf(InvalidIngredientNameException.class);
        assertThatThrownBy(() -> IngredientName.of(" \u2003 \u00a0 "))
                .isInstanceOf(InvalidIngredientNameException.class);
    }

    @Test
    void acceptsExactlyOneHundredUnicodeCodePoints() {
        String displayValue = IngredientName.of("😀".repeat(100)).displayValue();

        assertThat(displayValue.codePointCount(0, displayValue.length())).isEqualTo(100);
    }

    @Test
    void rejectsMoreThanOneHundredUnicodeCodePoints() {
        assertThatThrownBy(() -> IngredientName.of("😀".repeat(101)))
                .isInstanceOf(InvalidIngredientNameException.class);
    }

    @Test
    void doesNotMergeDifferentSemanticText() {
        assertThat(IngredientName.of("鸡蛋").normalizedValue())
                .isNotEqualTo(IngredientName.of("土鸡蛋").normalizedValue());
    }
}
