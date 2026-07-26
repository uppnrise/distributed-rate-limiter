import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // The generated UI primitives intentionally colocate component helpers
      // and hooks; Vite still reloads these modules correctly.
      "react-refresh/only-export-components": "off",
      "@typescript-eslint/no-unused-vars": "off",
      // React Hooks 7 adds compiler-oriented rules that were not part of this
      // project's previous lint contract. Adopt them separately from the
      // security-driven dependency upgrade.
      "react-hooks/purity": "off",
      "react-hooks/set-state-in-effect": "off",
      "react-hooks/static-components": "off",
    },
  },
);
