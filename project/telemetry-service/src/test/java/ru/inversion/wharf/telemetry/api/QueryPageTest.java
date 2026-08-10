package ru.inversion.wharf.telemetry.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class QueryPageTest {

    @Test
    void cutsOversizedLimitToMaximum() {
        assertThat(QueryPage.size(1_000_000)).isEqualTo(QueryPage.MAX_LIMIT);
    }

    @Test
    void raisesNonPositiveLimitToOne() {
        assertThat(QueryPage.size(0)).isEqualTo(1);
        assertThat(QueryPage.size(-5)).isEqualTo(1);
    }

    @Test
    void treatsNegativePageAsFirst() {
        assertThat(QueryPage.of(-3, 50)).isEqualTo(PageRequest.of(0, 50));
        assertThat(QueryPage.offset(-3, 50)).isZero();
    }

    @Test
    void computesOffsetFromCappedSize() {
        assertThat(QueryPage.offset(2, 50)).isEqualTo(100);
        assertThat(QueryPage.offset(2, 1_000_000)).isEqualTo(2L * QueryPage.MAX_LIMIT);
    }
}
