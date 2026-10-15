package com.everest.locker.service;

import static com.everest.locker.entity.Size.MEDIUM;
import static com.everest.locker.entity.Size.SMALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.LockerStatus;
import com.everest.locker.exception.InvalidPickupCodeException;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.LockerEmptyException;
import com.everest.locker.exception.LockerNotFoundException;
import com.everest.locker.hardware.LockerDoor;
import com.everest.locker.repository.InMemoryLockerRepository;
import com.everest.locker.service.strategy.SmallestFitSelectionPolicy;
import com.everest.locker.support.RecordingLockerDoor;
import com.everest.locker.support.RecordingNotifier;
import com.everest.locker.support.SequenceCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LockerServiceRetrieveTest {

    private static final String CUSTOMER = "+60123456789";

    private final RecordingLockerDoor door = new RecordingLockerDoor();
    private final LockerService service = service(door, "111111", "222222");

    @Test
    void theRightCodeOpensTheLockerAndFreesIt() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);

        RetrievalResult result = service.retrieveParcel("L1", "111111");

        assertThat(result.lockerId()).isEqualTo("L1");
        assertThat(result.lockerSize()).isEqualTo(SMALL);
        assertThat(door.opened()).containsExactly(new LockerId(1));
        assertThat(statusOf("L1")).isEqualTo(LockerStatus.AVAILABLE);
    }

    @Test
    void theFreedLockerCanTakeTheNextPackage() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);
        service.retrieveParcel("L1", "111111");

        assertThat(service.storeParcel(SMALL, CUSTOMER).lockerId()).isEqualTo("L1");
    }

    @Test
    void aCodeWorksOnlyOnce() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);
        service.retrieveParcel("L1", "111111");

        assertThatThrownBy(() -> service.retrieveParcel("L1", "111111"))
                .isInstanceOf(LockerEmptyException.class)
                .hasMessage("Locker L1 is empty. There is no package to collect.");
    }

    @Test
    void aUsedCodeCanBeIssuedAgainForANewPackage() {
        LockerService sameCodeEveryTime = service(door, "111111");
        sameCodeEveryTime.addLockers(SMALL, 1);
        sameCodeEveryTime.storeParcel(SMALL, CUSTOMER);
        sameCodeEveryTime.retrieveParcel("L1", "111111");

        sameCodeEveryTime.storeParcel(SMALL, CUSTOMER);

        assertThat(sameCodeEveryTime.retrieveParcel("L1", "111111").lockerId()).isEqualTo("L1");
    }

    @Test
    void aWrongCodeIsRejectedAndChangesNothing() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);

        assertThatThrownBy(() -> service.retrieveParcel("L1", "999999"))
                .isInstanceOf(InvalidPickupCodeException.class)
                .hasMessage("The pickup code is not valid for locker L1.");

        assertThat(door.opened()).isEmpty();
        assertThat(statusOf("L1")).isEqualTo(LockerStatus.OCCUPIED);
        assertThat(service.retrieveParcel("L1", "111111").lockerId()).isEqualTo("L1");
    }

    @Test
    void aCodeOnlyOpensItsOwnLocker() {
        service.addLockers(SMALL, 2);
        service.storeParcel(SMALL, CUSTOMER);  // L1, code 111111
        service.storeParcel(SMALL, CUSTOMER);  // L2, code 222222

        assertThatThrownBy(() -> service.retrieveParcel("L2", "111111"))
                .isInstanceOf(InvalidPickupCodeException.class);
    }

    @Test
    void anEmptyLockerIsReported() {
        service.addLockers(MEDIUM, 1);

        assertThatThrownBy(() -> service.retrieveParcel("L1", "111111"))
                .isInstanceOf(LockerEmptyException.class);
        assertThat(door.opened()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"L99", "X1", "1", "L0"})
    void anUnknownOrMalformedLockerIdIsReportedAsNotFound(String lockerId) {
        service.addLockers(SMALL, 1);

        assertThatThrownBy(() -> service.retrieveParcel(lockerId, "111111"))
                .isInstanceOf(LockerNotFoundException.class)
                .hasMessage("Locker " + lockerId + " does not exist.");
    }

    @Test
    void lockerIdIsCaseInsensitiveAndTrimmed() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);

        assertThat(service.retrieveParcel(" l1 ", " 111111 ").lockerId()).isEqualTo("L1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aBlankLockerIdIsInvalid(String lockerId) {
        assertThatThrownBy(() -> service.retrieveParcel(lockerId, "111111"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Locker id is required.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aBlankPickupCodeIsInvalid(String pickupCode) {
        assertThatThrownBy(() -> service.retrieveParcel("L1", pickupCode))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Pickup code is required.");
    }

    @Test
    void ifTheDoorFailsToOpenThePackageStaysInTheLocker() {
        LockerDoor jammed = lockerId -> {
            throw new IllegalStateException("door motor fault");
        };
        LockerService withJammedDoor = service(jammed, "111111");
        withJammedDoor.addLockers(SMALL, 1);
        withJammedDoor.storeParcel(SMALL, CUSTOMER);

        assertThatThrownBy(() -> withJammedDoor.retrieveParcel("L1", "111111"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(withJammedDoor.listLockers().getFirst().status()).isEqualTo(LockerStatus.OCCUPIED);
    }

    private LockerStatus statusOf(String lockerId) {
        return service.listLockers().stream()
                .filter(view -> view.id().equals(lockerId))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private static LockerService service(LockerDoor door, String... codes) {
        return new LockerService(
                new InMemoryLockerRepository(),
                new SmallestFitSelectionPolicy(),
                new SequenceCodeGenerator(codes),
                new RecordingNotifier(),
                door);
    }
}
