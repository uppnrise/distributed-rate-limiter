package dev.bnacar.distributedratelimiter.util;

/**
 * Safe, linear-time '*' wildcard pattern matcher.
 * <p>
 * This intentionally avoids compiling user-controlled patterns into a
 * {@link java.util.regex.Pattern} regular expression. Converting an
 * attacker-controlled wildcard pattern (e.g. "a*a*a*a*a*a*a*a*b") into a
 * regex such as {@code a.*a.*a.*a.*a.*a.*a.*a.*b} and matching it against a
 * crafted, non-matching input can trigger catastrophic backtracking in the
 * regex engine (CWE-400 / ReDoS). The iterative two-pointer algorithm below
 * matches in O(text.length() * pattern.length()) time in the worst case,
 * with no backtracking blow-up, regardless of how many '*' wildcards the
 * pattern contains.
 */
public final class WildcardPatternMatcher {

    /**
     * Defensive upper bound on input sizes. Rate-limiting keys and
     * configuration patterns are short identifiers in practice; rejecting
     * unreasonably large inputs bounds the worst-case matching cost.
     */
    private static final int MAX_INPUT_LENGTH = 2048;

    private WildcardPatternMatcher() {
    }

    /**
     * Checks whether {@code text} matches {@code pattern}, where {@code *}
     * in the pattern matches any sequence of characters (including none).
     * All other characters must match literally.
     *
     * @param text    the value to test, e.g. a rate limiting key
     * @param pattern the wildcard pattern, e.g. {@code "user:*"}
     * @return {@code true} if {@code text} matches {@code pattern}
     */
    public static boolean matches(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        if (text.length() > MAX_INPUT_LENGTH || pattern.length() > MAX_INPUT_LENGTH) {
            return false;
        }
        if (pattern.equals("*")) {
            return true;
        }

        int textIndex = 0;
        int patternIndex = 0;
        int starPatternIndex = -1;
        int starTextIndex = 0;

        while (textIndex < text.length()) {
            if (patternIndex < pattern.length()
                    && pattern.charAt(patternIndex) == text.charAt(textIndex)) {
                textIndex++;
                patternIndex++;
            } else if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                starPatternIndex = patternIndex;
                starTextIndex = textIndex;
                patternIndex++;
            } else if (starPatternIndex != -1) {
                patternIndex = starPatternIndex + 1;
                starTextIndex++;
                textIndex = starTextIndex;
            } else {
                return false;
            }
        }

        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
        }

        return patternIndex == pattern.length();
    }
}
