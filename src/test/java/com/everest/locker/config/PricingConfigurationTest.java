package com.everest.locker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.service.strategy.StorageChargePolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Pricing comes from configuration: here X is overridden, the tiers come from application.yml. */
@SpringBootTest(properties = "locker.pricing.unit-rate=2.50")
class PricingConfigurationTest {

    @Autowired
    private StorageChargePolicy chargePolicy;

    @Test
    void usesTheConfiguredUnitRateAndTiers() {
        // 5 x 2.50 + 5 x 5.00 + 2 x 7.50
        assertThat(chargePolicy.chargeFor(Duration.ofDays(12)).amount()).isEqualByComparingTo("52.50");
    }
}
