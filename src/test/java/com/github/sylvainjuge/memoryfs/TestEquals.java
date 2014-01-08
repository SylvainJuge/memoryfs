package com.github.sylvainjuge.memoryfs;

import static org.fest.assertions.api.Assertions.assertThat;

public final class TestEquals {

    private TestEquals() {
        // uncallable constructor
    }

    /**
     * Tests {@link Object#equals(Object)} and {@link Object#hashCode()} consistency between all pairs of {@code a}.
     *
     * @param shouldEqual true to test equality, false to test non-equality
     * @param o           objects to compare
     * @param <T>
     */
    @SafeVarargs
    public static <T> void checkHashCodeEqualsConsistency(boolean shouldEqual, T... o) {
        assertThat(o.length).isGreaterThanOrEqualTo(1);
        for (int i = 0; i < o.length; i++) {
            for (int j = 0; j < o.length; j++) {
                if (j <= i) {
                    if (shouldEqual) {
                        assertThat(o[i]).isEqualTo(o[j]);
                        assertThat(o[j]).isEqualTo(o[i]);
                        assertThat(o[i].hashCode()).isEqualTo(o[j].hashCode());
                    } else if (j < i) {
                        assertThat(o[i]).isNotEqualTo(o[j]);
                        assertThat(o[j]).isNotEqualTo(o[i]);
                        assertThat(o[i].hashCode()).isNotEqualTo(o[j].hashCode());
                    }
                }
            }
        }
    }
}
