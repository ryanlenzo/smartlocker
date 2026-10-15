package com.everest.locker.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SizeTest {

    @ParameterizedTest(name = "{0} locker can hold {1} package: {2}")
    @CsvSource({
            "SMALL,  SMALL,  true",
            "SMALL,  MEDIUM, false",
            "SMALL,  LARGE,  false",
            "MEDIUM, SMALL,  true",
            "MEDIUM, MEDIUM, true",
            "MEDIUM, LARGE,  false",
            "LARGE,  SMALL,  true",
            "LARGE,  MEDIUM, true",
            "LARGE,  LARGE,  true"
    })
    void lockerHoldsPackagesOfItsOwnSizeOrSmaller(Size lockerSize, Size packageSize, boolean expected) {
        assertThat(lockerSize.canHold(packageSize)).isEqualTo(expected);
    }

    @Test
    void sizesAreOrderedFromSmallestToLargest() {
        assertThat(Size.values()).containsExactly(Size.SMALL, Size.MEDIUM, Size.LARGE);
    }

    @Test
    void canHoldRejectsNull() {
        assertThatThrownBy(() -> Size.SMALL.canHold(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("packageSize");
    }
}
