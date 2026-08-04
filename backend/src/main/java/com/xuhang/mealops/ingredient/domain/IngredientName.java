package com.xuhang.mealops.ingredient.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class IngredientName {

    private static final int MAX_CODE_POINTS = 100;

    private final String displayValue;
    private final String normalizedValue;

    private IngredientName(String displayValue) {
        this.displayValue = displayValue;
        this.normalizedValue = displayValue.toLowerCase(Locale.ROOT);
    }

    public static IngredientName of(String rawValue) {
        if (rawValue == null) {
            throw new InvalidIngredientNameException("Ingredient name must not be null");
        }

        String normalizedForm = Normalizer.normalize(rawValue, Normalizer.Form.NFKC);
        String displayValue = collapseWhitespace(normalizedForm);
        if (displayValue.isEmpty()) {
            throw new InvalidIngredientNameException("Ingredient name must not be blank");
        }
        if (displayValue.codePointCount(0, displayValue.length()) > MAX_CODE_POINTS) {
            throw new InvalidIngredientNameException("Ingredient name must not exceed 100 Unicode code points");
        }
        return new IngredientName(displayValue);
    }

    public String displayValue() {
        return displayValue;
    }

    public String normalizedValue() {
        return normalizedValue;
    }

    private static String collapseWhitespace(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean pendingSpace = false;
        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            if (isUnicodeWhitespace(codePoint)) {
                if (result.length() > 0) {
                    pendingSpace = true;
                }
                continue;
            }
            if (pendingSpace) {
                result.append(' ');
                pendingSpace = false;
            }
            result.appendCodePoint(codePoint);
        }
        return result.toString();
    }

    private static boolean isUnicodeWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
