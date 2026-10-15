package com.everest.locker.service;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.Parcel;
import com.everest.locker.entity.PickupCode;
import com.everest.locker.entity.Size;
import com.everest.locker.entity.StoredParcel;
import com.everest.locker.exception.InvalidPickupCodeException;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.LockerEmptyException;
import com.everest.locker.exception.LockerNotFoundException;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.exception.NotificationFailedException;
import com.everest.locker.hardware.LockerDoor;
import com.everest.locker.notification.Notifier;
import com.everest.locker.repository.LockerRepository;
import com.everest.locker.service.strategy.LockerSelectionPolicy;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import com.everest.locker.service.strategy.StorageCharge;
import com.everest.locker.service.strategy.StorageChargePolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private final LockerDoor lockerDoor;
    private final StorageChargePolicy chargePolicy;
    private final Clock clock;

    /** Codes of packages currently in a locker; each must be unique. */
    private final Set<PickupCode> activeCodes = ConcurrentHashMap.newKeySet();

    public LockerService(LockerRepository lockers,
                         LockerSelectionPolicy selectionPolicy,
                         PickupCodeGenerator codeGenerator,
                         Notifier notifier,
                         LockerDoor lockerDoor,
                         StorageChargePolicy chargePolicy,
                         Clock clock) {
        this.lockers = Objects.requireNonNull(lockers);
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy);
        this.codeGenerator = Objects.requireNonNull(codeGenerator);
        this.notifier = Objects.requireNonNull(notifier);
        this.lockerDoor = Objects.requireNonNull(lockerDoor);
        this.chargePolicy = Objects.requireNonNull(chargePolicy);
        this.clock = Objects.requireNonNull(clock);
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

        StoredParcel stored = new StoredParcel(parcel, reserveUniqueCode(), clock.instant());
        Locker locker = occupyFirstFree(candidates, stored);
        notifyCustomer(locker, stored);
        return new StoreResult(locker.id().toString(), locker.size(), stored.storedAt());
    }

    /**
     * Lets a customer collect their package with the locker id and pickup code they received,
     * and returns the storage charge for the time it spent in the locker.
     * The code is checked before the door opens, so a wrong code never opens or changes
     * anything. The package is only released once the door has opened.
     *
     * @throws LockerNotFoundException if no locker has that id
     * @throws LockerEmptyException if there is nothing to collect (e.g. already collected)
     * @throws InvalidPickupCodeException if the code does not belong to the package in the locker
     */
    public RetrievalResult retrieveParcel(String lockerId, String pickupCode) {
        if (lockerId == null || lockerId.isBlank()) {
            throw new InvalidRequestException("Locker id is required.");
        }
        if (pickupCode == null || pickupCode.isBlank()) {
            throw new InvalidRequestException("Pickup code is required.");
        }
        Locker locker = LockerId.parse(lockerId)
                .flatMap(lockers::findById)
                .orElseThrow(() -> new LockerNotFoundException(lockerId.strip()));
        PickupCode code = new PickupCode(pickupCode.strip());

        StoredParcel stored = locker.occupant()
                .orElseThrow(() -> new LockerEmptyException(locker.id()));
        if (!stored.pickupCode().equals(code)) {
            throw new InvalidPickupCodeException(locker.id());
        }

        lockerDoor.open(locker.id());
        StoredParcel released = locker.releaseIfCodeMatches(code)
                .orElseThrow(() -> new LockerEmptyException(locker.id()));  // collected by a parallel request
        activeCodes.remove(code);

        Instant retrievedAt = clock.instant();
        StorageCharge charge = chargePolicy.chargeFor(Duration.between(released.storedAt(), retrievedAt));
        return new RetrievalResult(locker.id().toString(), locker.size(),
                released.storedAt(), retrievedAt, charge.chargeableDays(), charge.amount());
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
