package com.everest.locker.support;

import com.everest.locker.entity.PickupCode;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Test fake: hands out the given codes in order, then repeats the last one forever. */
public class SequenceCodeGenerator implements PickupCodeGenerator {

    private final Deque<String> codes;
    private String last;

    public SequenceCodeGenerator(String... codes) {
        this.codes = new ArrayDeque<>(List.of(codes));
    }

    @Override
    public PickupCode next() {
        if (!codes.isEmpty()) {
            last = codes.poll();
        }
        return new PickupCode(last);
    }
}
