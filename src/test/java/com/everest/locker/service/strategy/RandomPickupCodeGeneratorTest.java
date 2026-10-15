package com.everest.locker.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RandomPickupCodeGeneratorTest {

    @RepeatedTest(20)
    void generatesSixDigitCodes() {
        var generator = new RandomPickupCodeGenerator(new Random());

        assertThat(generator.next().value()).matches("\\d{6}");
    }

    @Test
    void padsSmallNumbersWithLeadingZeros() {
        var alwaysFortyTwo = new Random() {
            @Override
            public int nextInt(int bound) {
                return 42;
            }
        };

        assertThat(new RandomPickupCodeGenerator(alwaysFortyTwo).next().value()).isEqualTo("000042");
    }
}
