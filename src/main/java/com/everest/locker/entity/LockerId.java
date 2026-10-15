package com.everest.locker.entity;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifier of a locker, shown as {@code L1}, {@code L2}, ...
 *
 * <p>Kept numeric so lockers sort as L2 before L10, which plain string ordering gets wrong.
 */
public record LockerId(int number) implements Comparable<LockerId> {

    /** "L" (any case) followed by up to 9 digits, so the number always fits in an int. */
    private static final Pattern DISPLAY_FORM = Pattern.compile("[Ll](\\d{1,9})");

    public LockerId {
        if (number < 1) {
            throw new IllegalArgumentException("Locker number must be positive, was " + number);
        }
    }

    /**
     * Reads an id typed by a user, such as {@code "L12"} or {@code "l12"}.
     *
     * @return the id, or empty if the text is not a valid locker id
     */
    public static Optional<LockerId> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher matcher = DISPLAY_FORM.matcher(text.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int number = Integer.parseInt(matcher.group(1));
        return number < 1 ? Optional.empty() : Optional.of(new LockerId(number));
    }

    @Override
    public int compareTo(LockerId other) {
        return Integer.compare(number, other.number);
    }

    @Override
    public String toString() {
        return "L" + number;
    }
}
