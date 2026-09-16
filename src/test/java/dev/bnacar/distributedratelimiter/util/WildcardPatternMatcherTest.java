package dev.bnacar.distributedratelimiter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

/**
 * Tests for {@link WildcardPatternMatcher}, in particular that it behaves
 * equivalently to the previous regex-based implementation for legitimate
 * inputs, while remaining safe (linear time, no catastrophic backtracking)
 * against adversarial "ReDoS-style" wildcard patterns (CWE-400).
 */
class WildcardPatternMatcherTest {

    @Test
    void shouldMatchExactStringWithNoWildcard() {
        assertThat(WildcardPatternMatcher.matches("user:123", "user:123")).isTrue();
        assertThat(WildcardPatternMatcher.matches("user:123", "user:124")).isFalse();
    }

    @Test
    void shouldMatchAnythingForBareStar() {
        assertThat(WildcardPatternMatcher.matches("anything-at-all", "*")).isTrue();
        assertThat(WildcardPatternMatcher.matches("", "*")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "user:123,    user:*,      true",
        "user:abc,    user:*,      true",
        "admin:1,     user:*,      false",
        "user:admin,  '*:admin',   true",
        "system:admin,'*:admin',   true",
        "user:x,      '*:admin',   false",
        "api:v1:users, api:v1:*,   true",
        "api:v2:users, api:v1:*,   false",
    })
    void shouldMatchSingleWildcardPatterns(String key, String pattern, boolean expected) {
        assertThat(WildcardPatternMatcher.matches(key, pattern)).isEqualTo(expected);
    }

    @Test
    void shouldMatchMultipleWildcardsInOnePattern() {
        assertThat(WildcardPatternMatcher.matches("user:123:read", "user:*:read")).isTrue();
        assertThat(WildcardPatternMatcher.matches("user:123:write", "user:*:read")).isFalse();
        assertThat(WildcardPatternMatcher.matches("a-b-c-d", "a*b*c*d")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a-b-x-d", "a*b*c*d")).isFalse();
    }

    @Test
    void shouldTreatConsecutiveWildcardsAsSingleWildcard() {
        assertThat(WildcardPatternMatcher.matches("user:123", "user:**")).isTrue();
        assertThat(WildcardPatternMatcher.matches("anything", "***")).isTrue();
    }

    @Test
    void shouldTreatRegexMetacharactersAsLiteralCharacters() {
        // These characters have special meaning in regular expressions but must be
        // treated as plain literal characters by the wildcard matcher.
        assertThat(WildcardPatternMatcher.matches("a.b", "a.b")).isTrue();
        assertThat(WildcardPatternMatcher.matches("axb", "a.b")).isFalse();
        assertThat(WildcardPatternMatcher.matches("a+b", "a+b")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a(b)c", "a(b)c")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a[b]c", "a[b]c")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a{2}c", "a{2}c")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a|b", "a|b")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a^b$c", "a^b$c")).isTrue();
        assertThat(WildcardPatternMatcher.matches("a\\b", "a\\b")).isTrue();
    }

    @Test
    void shouldReturnFalseForNullInputs() {
        assertThat(WildcardPatternMatcher.matches(null, "user:*")).isFalse();
        assertThat(WildcardPatternMatcher.matches("user:1", null)).isFalse();
        assertThat(WildcardPatternMatcher.matches(null, null)).isFalse();
    }

    @Test
    void shouldRejectInputsExceedingMaxLength() {
        String hugeKey = "a".repeat(3000);
        String hugePattern = "a".repeat(3000) + "*";

        assertThat(WildcardPatternMatcher.matches(hugeKey, "a*")).isFalse();
        assertThat(WildcardPatternMatcher.matches("a", hugePattern)).isFalse();
    }

    @Test
    void shouldNotExhibitCatastrophicBacktrackingOnAdversarialPatterns() {
        // Classic ReDoS-style adversarial input: many wildcards followed by a
        // character that never appears in the text, which forces exponential
        // backtracking in a naive backtracking regex engine. The linear-time
        // matcher must resolve this near-instantly instead of hanging.
        String pattern = "*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*b";
        String key = "a".repeat(60);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            boolean result = WildcardPatternMatcher.matches(key, pattern);
            assertThat(result).isFalse();
        });
    }

    @Test
    void shouldNotExhibitCatastrophicBacktrackingOnLongNonMatchingInput() {
        String pattern = "*a*a*a*a*a*a*a*a*a*a*";
        String key = "a".repeat(2000);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
            WildcardPatternMatcher.matches(key, pattern));
    }
}
