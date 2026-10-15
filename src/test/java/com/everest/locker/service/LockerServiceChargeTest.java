package com.everest.locker.service;

import static com.everest.locker.entity.Size.SMALL;
import static com.everest.locker.support.LockerServiceBuilder.START;
import static com.everest.locker.support.LockerServiceBuilder.aLockerService;
import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.config.AdjustableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LockerServiceChargeTest {

    private static final String CUSTOMER = "+60123456789";

    private final AdjustableClock clock = new AdjustableClock(START);
    private final LockerService service = aLockerService().withClock(clock).withCodes("111111").build();

    @Test
    void recordsWhenThePackageWasStored() {
        service.addLockers(SMALL, 1);

        assertThat(service.storeParcel(SMALL, CUSTOMER).storedAt()).isEqualTo(START);
    }

    @Test
    void chargesForTheTimeThePackageSpentInTheLocker() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);
        clock.advance(Duration.ofDays(12));

        RetrievalResult result = service.retrieveParcel("L1", "111111");

        assertThat(result.storedAt()).isEqualTo(START);
        assertThat(result.retrievedAt()).isEqualTo(START.plus(Duration.ofDays(12)));
        assertThat(result.chargeableDays()).isEqualTo(12);
        assertThat(result.storageCharge()).isEqualByComparingTo("21.00");
    }

    @Test
    void anImmediatePickupIsChargedOneDay() {
        service.addLockers(SMALL, 1);
        service.storeParcel(SMALL, CUSTOMER);

        RetrievalResult result = service.retrieveParcel("L1", "111111");

        assertThat(result.chargeableDays()).isEqualTo(1);
        assertThat(result.storageCharge()).isEqualByComparingTo("1.00");
    }

    @Test
    void theChargeStartsFromWhenThisPackageWasStoredNotFromWhenTheLockerWasCreated() {
        service.addLockers(SMALL, 1);
        clock.advance(Duration.ofDays(30));
        service.storeParcel(SMALL, CUSTOMER);
        clock.advance(Duration.ofHours(30));

        assertThat(service.retrieveParcel("L1", "111111").chargeableDays()).isEqualTo(2);
    }
}
