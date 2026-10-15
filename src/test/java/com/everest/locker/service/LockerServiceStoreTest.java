package com.everest.locker.service;

import static com.everest.locker.entity.Size.LARGE;
import static com.everest.locker.entity.Size.MEDIUM;
import static com.everest.locker.entity.Size.SMALL;
import static com.everest.locker.support.LockerServiceBuilder.aLockerService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.LockerStatus;
import com.everest.locker.entity.PickupCode;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.exception.NotificationFailedException;
import com.everest.locker.notification.Notifier;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import com.everest.locker.support.RecordingNotifier;
import com.everest.locker.support.SequenceCodeGenerator;
import org.junit.jupiter.api.Test;

class LockerServiceStoreTest {

    private static final String CUSTOMER = "+60123456789";

    private final RecordingNotifier notifier = new RecordingNotifier();

    @Test
    void storesInTheSmallestFittingLockerAndReturnsItsId() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);
        service.addLockers(LARGE, 1);   // L1
        service.addLockers(MEDIUM, 1);  // L2
        service.addLockers(SMALL, 1);   // L3

        StoreResult result = service.storeParcel(MEDIUM, CUSTOMER);

        assertThat(result.lockerId()).isEqualTo("L2");
        assertThat(result.lockerSize()).isEqualTo(MEDIUM);
        assertThat(statusOf(service, "L2")).isEqualTo(LockerStatus.OCCUPIED);
    }

    @Test
    void sendsThePickupCodeToTheCustomerNotToTheCaller() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);
        service.addLockers(SMALL, 1);

        service.storeParcel(SMALL, CUSTOMER);

        assertThat(notifier.sent()).containsExactly(
                new RecordingNotifier.Sent(CUSTOMER, new LockerId(1), new PickupCode("111111")));
    }

    @Test
    void usesABiggerLockerWhenTheExactSizeIsFull() {
        LockerService service = service(new SequenceCodeGenerator("111111", "222222"), notifier);
        service.addLockers(SMALL, 1);
        service.addLockers(LARGE, 1);

        service.storeParcel(SMALL, CUSTOMER);
        StoreResult second = service.storeParcel(SMALL, CUSTOMER);

        assertThat(second.lockerId()).isEqualTo("L2");
    }

    @Test
    void failsWhenNoLockerFitsAndChangesNothing() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);
        service.addLockers(SMALL, 2);

        assertThatThrownBy(() -> service.storeParcel(LARGE, CUSTOMER))
                .isInstanceOf(NoSuitableLockerException.class)
                .hasMessage("No available locker can fit a LARGE package. The package cannot be stored.");

        assertThat(service.listLockers()).allMatch(view -> view.status() == LockerStatus.AVAILABLE);
        assertThat(notifier.sent()).isEmpty();
    }

    @Test
    void failsWhenAllLockersAreOccupied() {
        LockerService service = service(new SequenceCodeGenerator("111111", "222222"), notifier);
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);

        assertThatThrownBy(() -> service.storeParcel(SMALL, CUSTOMER))
                .isInstanceOf(NoSuitableLockerException.class);
    }

    @Test
    void failsWhenNoLockersExist() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);

        assertThatThrownBy(() -> service.storeParcel(SMALL, CUSTOMER))
                .isInstanceOf(NoSuitableLockerException.class);
    }

    @Test
    void regeneratesTheCodeWhenItIsAlreadyInUse() {
        LockerService service = service(new SequenceCodeGenerator("111111", "111111", "222222"), notifier);
        service.addLockers(SMALL, 2);

        service.storeParcel(SMALL, CUSTOMER);
        service.storeParcel(SMALL, CUSTOMER);

        assertThat(notifier.sent())
                .extracting(sent -> sent.pickupCode().value())
                .containsExactly("111111", "222222");
    }

    @Test
    void givesUpWhenAUniqueCodeCannotBeGeneratedAndLeavesTheLockerFree() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);
        service.addLockers(SMALL, 2);
        service.storeParcel(SMALL, CUSTOMER);

        assertThatThrownBy(() -> service.storeParcel(SMALL, CUSTOMER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique pickup code");

        assertThat(statusOf(service, "L2")).isEqualTo(LockerStatus.AVAILABLE);
    }

    @Test
    void releasesTheLockerAndCodeWhenTheCustomerCannotBeNotified() {
        Notifier broken = (contact, lockerId, code) -> {
            throw new RuntimeException("SMS gateway down");
        };
        LockerService service = service(new SequenceCodeGenerator("111111"), broken);
        service.addLockers(SMALL, 1);

        assertThatThrownBy(() -> service.storeParcel(SMALL, CUSTOMER))
                .isInstanceOf(NotificationFailedException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        assertThat(statusOf(service, "L1")).isEqualTo(LockerStatus.AVAILABLE);
    }

    @Test
    void theSameCodeCanBeReusedAfterAFailedNotification() {
        var flaky = new Notifier() {
            int calls;

            @Override
            public void sendPickupCode(String contact, LockerId lockerId, PickupCode code) {
                if (calls++ == 0) {
                    throw new RuntimeException("SMS gateway down");
                }
            }
        };
        LockerService service = service(new SequenceCodeGenerator("111111"), flaky);
        service.addLockers(SMALL, 1);

        assertThatThrownBy(() -> service.storeParcel(SMALL, CUSTOMER))
                .isInstanceOf(NotificationFailedException.class);

        assertThat(service.storeParcel(SMALL, CUSTOMER).lockerId()).isEqualTo("L1");
    }

    @Test
    void rejectsMissingPackageSize() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);

        assertThatThrownBy(() -> service.storeParcel(null, CUSTOMER))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Package size is required.");
    }

    @Test
    void rejectsBlankCustomerContact() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);

        assertThatThrownBy(() -> service.storeParcel(SMALL, "  "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Customer contact is required.");
    }

    @Test
    void rejectsOverlongCustomerContact() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);

        assertThatThrownBy(() -> service.storeParcel(SMALL, "x".repeat(255)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Customer contact must be at most 254 characters.");
    }

    @Test
    void trimsTheCustomerContactBeforeNotifying() {
        LockerService service = service(new SequenceCodeGenerator("111111"), notifier);
        service.addLockers(SMALL, 1);

        service.storeParcel(SMALL, "  " + CUSTOMER + " ");

        assertThat(notifier.sent().getFirst().customerContact()).isEqualTo(CUSTOMER);
    }

    private static LockerService service(PickupCodeGenerator codes, Notifier notifier) {
        return aLockerService().withCodes(codes).withNotifier(notifier).build();
    }

    private static LockerStatus statusOf(LockerService service, String lockerId) {
        return service.listLockers().stream()
                .filter(view -> view.id().equals(lockerId))
                .findFirst()
                .orElseThrow()
                .status();
    }
}
