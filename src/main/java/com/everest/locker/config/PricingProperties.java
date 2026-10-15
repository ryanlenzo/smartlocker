package com.everest.locker.config;

import com.everest.locker.service.strategy.ChargeTier;
import com.everest.locker.service.strategy.TieredStorageChargePolicy;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Storage pricing from {@code locker.pricing} in application.yml: the unit rate X and the
 * tiers. Changing prices, or adding a free grace period, needs no code change.
 */
@ConfigurationProperties(prefix = "locker.pricing")
public record PricingProperties(BigDecimal unitRate, List<Tier> tiers) {

    /** One tier as written in the configuration; {@code toDay} is left out for the last tier. */
    public record Tier(int fromDay, Integer toDay, BigDecimal multiplier) {
    }

    TieredStorageChargePolicy toChargePolicy() {
        List<ChargeTier> chargeTiers = tiers == null ? List.of() : tiers.stream()
                .map(tier -> new ChargeTier(tier.fromDay(), tier.toDay(), tier.multiplier()))
                .toList();
        return new TieredStorageChargePolicy(unitRate, chargeTiers);
    }
}
