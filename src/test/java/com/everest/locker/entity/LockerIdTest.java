package com.everest.locker.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LockerIdTest {

    @Test
    void isDisplayedWithAnLPrefix() {
        assertThat(new LockerId(7)).hasToString("L7");
    }

    @Test
    void ordersNumericallyNotAlphabetically() {
        List<LockerId> sorted = Stream.of(new LockerId(10), new LockerId(2), new LockerId(1))
                .sorted()
                .toList();

        assertThat(sorted).containsExactly(new LockerId(1), new LockerId(2), new LockerId(10));
    }

    @Test
    void numberMustBePositive() {
        assertThatThrownBy(() -> new LockerId(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
