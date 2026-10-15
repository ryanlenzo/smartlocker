package com.everest.locker.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @CsvSource({"L12, 12", "l7, 7", "' L3 ', 3"})
    void parsesTheDisplayedForm(String text, int expectedNumber) {
        assertThat(LockerId.parse(text)).contains(new LockerId(expectedNumber));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12", "L", "LX", "L0", "L-1", "L1.5", "LL1", "L9999999999"})
    void rejectsAnythingThatIsNotAValidLockerId(String text) {
        assertThat(LockerId.parse(text)).isEmpty();
    }
}
