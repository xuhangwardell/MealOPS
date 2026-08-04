package com.xuhang.mealops.recipe.domain;

import java.text.Normalizer;

public final class RecipeName {
    private final String value;
    private RecipeName(String value) { this.value = value; }
    public static RecipeName of(String raw) {
        if (raw == null) throw new InvalidRecipeException("Recipe name must not be null");
        String value = collapseWhitespace(Normalizer.normalize(raw, Normalizer.Form.NFKC));
        if (value.isBlank()) throw new InvalidRecipeException("Recipe name must not be blank");
        if (value.codePointCount(0, value.length()) > 100)
            throw new InvalidRecipeException("Recipe name must not exceed 100 Unicode code points");
        return new RecipeName(value);
    }
    private static String collapseWhitespace(String input) {
        StringBuilder out = new StringBuilder(); boolean pending = false;
        for (int i = 0; i < input.length();) {
            int cp = input.codePointAt(i); i += Character.charCount(cp);
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) { if (out.length() > 0) pending = true; continue; }
            if (pending) { out.append(' '); pending = false; }
            out.appendCodePoint(cp);
        }
        return out.toString();
    }
    public String value() { return value; }
}
