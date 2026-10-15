package com.everest.locker.service;

import static com.everest.locker.entity.Size.LARGE;
import static com.everest.locker.entity.Size.SMALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everest.locker.entity.LockerStatus;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.repository.InMemoryLockerRepository;
import com.everest.locker.service.strategy.SmallestFitSelectionPolicy;
import com.everest.locker.support.RecordingLockerDoor;
import com.everest.locker.support.RecordingNotifier;
import com.everest.locker.support.SequenceCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LockerServiceLockersTest {

    private final LockerService service = new LockerService(
            new InMemoryLockerRepository(),
            new SmallestFitSelectionPolicy(),
            new SequenceCodeGenerator("111111"),
            new RecordingNotifier(),
            new RecordingLockerDoor());

    @Test
    void createsTheRequestedNumberOfAvailableLockers() {
        var created = service.addLockers(SMALL, 3);

        assertThat(created).containsExactly(
                new LockerView("L1", SMALL, LockerStatus.AVAILABLE),
                new LockerView("L2", SMALL, LockerStatus.AVAILABLE),
                new LockerView("L3", SMALL, LockerStatus.AVAILABLE));
    }

    @Test
    void idsKeepIncreasingAcrossRequests() {
        service.addLockers(SMALL, 2);

        assertThat(service.addLockers(LARGE, 1))
                .containsExactly(new LockerView("L3", LARGE, LockerStatus.AVAILABLE));
    }

    @Test
    void listsAllLockersWithTheirCurrentStatus() {
        service.addLockers(SMALL, 2);
        service.storeParcel(SMALL, "+60123456789");

        assertThat(service.listLockers()).containsExactly(
                new LockerView("L1", SMALL, LockerStatus.OCCUPIED),
                new LockerView("L2", SMALL, LockerStatus.AVAILABLE));
    }

    @Test
    void listIsEmptyBeforeAnyLockerIsCreated() {
        assertThat(service.listLockers()).isEmpty();
    }

    @Test
    void acceptsTheMaximumBatchSize() {
        assertThat(service.addLockers(SMALL, LockerService.MAX_LOCKERS_PER_REQUEST)).hasSize(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void rejectsCountsOutsideOneToOneHundred(int count) {
        assertThatThrownBy(() -> service.addLockers(SMALL, count))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Locker count must be between 1 and 100.");

        assertThat(service.listLockers()).isEmpty();
    }

    @Test
    void rejectsMissingSize() {
        assertThatThrownBy(() -> service.addLockers(null, 1))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Locker size is required.");
    }
}
