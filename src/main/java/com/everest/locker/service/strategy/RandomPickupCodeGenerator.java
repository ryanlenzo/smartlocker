package com.everest.locker.service.strategy;

import com.everest.locker.entity.PickupCode;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Six-digit numeric codes, like a bank OTP: easy to type on a locker keypad.
 *
 * <p>The random source is injected: production wires a {@link java.security.SecureRandom}
 * so codes cannot be predicted, and tests can pass a fixed source.
 */
public class RandomPickupCodeGenerator implements PickupCodeGenerator {

    private static final int CODE_SPACE = 1_000_000;

    private final RandomGenerator random;

    public RandomPickupCodeGenerator(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public PickupCode next() {
        return new PickupCode("%06d".formatted(random.nextInt(CODE_SPACE)));
    }
}
