package com.everest.locker.service;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.Parcel;
import com.everest.locker.entity.PickupCode;
import com.everest.locker.entity.Size;
import com.everest.locker.entity.StoredParcel;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.exception.NotificationFailedException;
import com.everest.locker.notification.Notifier;
import com.everest.locker.repository.LockerRepository;
import com.everest.locker.service.strategy.LockerSelectionPolicy;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The locker system's use cases. Plain Java with no framework dependency: every collaborator
 * is an interface passed in through the constructor, so any interface (REST, CLI, ...) can
 * drive it and tests can swap in fakes.
 */
public class LockerService {

    public static final int MAX_LOCKERS_PER_REQUEST = 100;
    static final int MAX_CONTACT_LENGTH = 254;
    static final int MAX_CODE_ATTEMPTS = 10;

    private final LockerRepository lockers;
    private final LockerSelectionPolicy selectionPolicy;
    private final PickupCodeGenerator codeGenerator;
    private final Notifier notifier;

    /** Codes of packages currently in a locker; each must be unique. */
    private final Set<PickupCode> activeCodes = ConcurrentHashMap.newKeySet();

    public LockerService(LockerRepository lockers,
                         LockerSelectionPolicy selectionPolicy,
                         PickupCodeGenerator codeGenerator,
                         Notifier notifier) {
        this.lockers = Objects.requireNonNull(lockers);
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy);
        this.codeGenerator = Objects.requireNonNull(codeGenerator);
        this.notifier = Objects.requireNonNull(notifier);
    }

    /** Creates {@code count} empty lockers of the given size. */
    public List<LockerView> addLockers(Size size, int count) {
        if (size == null) {
            throw new InvalidRequestException("Locker size is required.");
        }
        if (count < 1 || count > MAX_LOCKERS_PER_REQUEST) {
            throw new InvalidRequestException(
                    "Locker count must be between 1 and " + MAX_LOCKERS_PER_REQUEST + ".");
        }
        List<LockerView> created = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Locker locker = new Locker(lockers.nextId(), size);
            lockers.save(locker);
            created.add(LockerView.of(locker));
        }
        return created;
    }

    /** Every locker with its current availability, ordered by id. */
    public List<LockerView> listLockers() {
        return lockers.findAll().stream().map(LockerView::of).toList();
    }

    /**
     * Stores a package in the smallest available locker that fits and sends the pickup code to
     * the customer.
     *
     * @throws NoSuitableLockerException if no available locker can hold the package
     * @throws NotificationFailedException if the customer could not be sent the code; the
     *         package is then not stored, because nobody could ever collect it
     */
    public StoreResult storeParcel(Size size, String customerContact) {
        Parcel parcel = new Parcel(requireSize(size), requireContact(customerContact));

        List<Locker> candidates = selectionPolicy.candidates(lockers.findAll(), size);
        if (candidates.isEmpty()) {
            throw new NoSuitableLockerException(size);
        }

        StoredParcel stored = new StoredParcel(parcel, reserveUniqueCode());
        Locker locker = occupyFirstFree(candidates, stored);
        notifyCustomer(locker, stored);
        return new StoreResult(locker.id().toString(), locker.size());
    }

    private Locker occupyFirstFree(List<Locker> candidates, StoredParcel stored) {
        for (Locker locker : candidates) {
            if (locker.tryOccupy(stored)) {
                return locker;
            }
        }
        activeCodes.remove(stored.pickupCode());
        throw new NoSuitableLockerException(stored.parcel().size());
    }

    private void notifyCustomer(Locker locker, StoredParcel stored) {
        try {
            notifier.sendPickupCode(stored.parcel().customerContact(), locker.id(), stored.pickupCode());
        } catch (RuntimeException e) {
            locker.release();
            activeCodes.remove(stored.pickupCode());
            throw new NotificationFailedException(e);
        }
    }

    private PickupCode reserveUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            PickupCode code = codeGenerator.next();
            if (activeCodes.add(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique pickup code after " + MAX_CODE_ATTEMPTS + " attempts");
    }

    private static Size requireSize(Size size) {
        if (size == null) {
            throw new InvalidRequestException("Package size is required.");
        }
        return size;
    }

    private static String requireContact(String contact) {
        if (contact == null || contact.isBlank()) {
            throw new InvalidRequestException("Customer contact is required.");
        }
        String trimmed = contact.strip();
        if (trimmed.length() > MAX_CONTACT_LENGTH) {
            throw new InvalidRequestException(
                    "Customer contact must be at most " + MAX_CONTACT_LENGTH + " characters.");
        }
        return trimmed;
    }
}
