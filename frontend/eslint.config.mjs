import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
    {
        ignores: ["node_modules/**", "unpackage/**", "dist/**", "coverage/**"]
    },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    ...pluginVue.configs["flat/recommended"],
    {
        files: ["**/*.ts", "**/*.vue"],
        languageOptions: {
            globals: {
                ...globals.browser,
                uni: "readonly"
            },
            parserOptions: {
                parser: tseslint.parser,
                extraFileExtensions: [".vue"]
            }
        },
        rules: {
            "@typescript-eslint/no-explicit-any": "error",
            "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
            "vue/html-indent": ["error", 4],
            "vue/script-indent": ["error", 4, { "baseIndent": 0, "switchCase": 1 }],
            "vue/first-attribute-linebreak": "off",
            "vue/html-closing-bracket-newline": "off",
            "vue/max-attributes-per-line": "off",
            "vue/multi-word-component-names": "off",
            "vue/singleline-html-element-content-newline": "off"
        }
    },
    {
        files: ["**/*.d.ts"],
        rules: {
            "@typescript-eslint/no-empty-object-type": "off",
            "@typescript-eslint/no-unused-vars": "off"
        }
    },
    {
        files: ["*.config.ts", "tests/**/*.ts"],
        languageOptions: {
            globals: globals.node
        }
    }
);
