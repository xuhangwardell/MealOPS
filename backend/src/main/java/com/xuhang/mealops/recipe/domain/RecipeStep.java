package com.xuhang.mealops.recipe.domain;

import java.text.Normalizer;

public record RecipeStep(int position, String instruction) {
    public RecipeStep {
        if (position <= 0) throw new InvalidRecipeException("Recipe step position must be positive");
        if (instruction == null) throw new InvalidRecipeException("Recipe step instruction must not be null");
        instruction = trimUnicodeWhitespace(Normalizer.normalize(instruction, Normalizer.Form.NFKC));
        if (instruction.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)))
            throw new InvalidRecipeException("Recipe step instruction must not be blank");
        if (instruction.codePointCount(0, instruction.length()) > 1000)
            throw new InvalidRecipeException("Recipe step instruction must not exceed 1000 Unicode code points");
    }
    private static String trimUnicodeWhitespace(String value) {
        int start = 0, end = value.length();
        while (start < end) { int cp = value.codePointAt(start); if (!isWhitespace(cp)) break; start += Character.charCount(cp); }
        while (end > start) { int cp = value.codePointBefore(end); if (!isWhitespace(cp)) break; end -= Character.charCount(cp); }
        return value.substring(start, end);
    }
    private static boolean isWhitespace(int cp) { return Character.isWhitespace(cp) || Character.isSpaceChar(cp); }
}
