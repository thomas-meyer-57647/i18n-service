package de.innologic.i18nservice.bundle.cache;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

/**
 * Exponiert BundleCache-Metriken über Micrometer/Actuator.
 *
 * Aufrufbeispiele:
 * - /actuator/metrics/i18n.bundle_cache.hit.count
 * - /actuator/metrics/i18n.bundle_cache.miss.count
 * - /actuator/metrics/i18n.bundle_cache.size
 */
@Component
public class BundleCacheMetricsBinder {

    public BundleCacheMetricsBinder(MeterRegistry registry, BundleCache cache) {
        Tags tags = Tags.of("cache", "bundle");

        Gauge.builder("i18n.bundle_cache.size", cache, BundleCache::estimatedSize)
                .tags(tags)
                .register(registry);

        FunctionCounter.builder("i18n.bundle_cache.hit.count", cache, c -> c.stats().hitCount())
                .tags(tags)
                .register(registry);

        FunctionCounter.builder("i18n.bundle_cache.miss.count", cache, c -> c.stats().missCount())
                .tags(tags)
                .register(registry);

        FunctionCounter.builder("i18n.bundle_cache.load.success.count", cache, c -> c.stats().loadSuccessCount())
                .tags(tags)
                .register(registry);

        FunctionCounter.builder("i18n.bundle_cache.load.failure.count", cache, c -> c.stats().loadFailureCount())
                .tags(tags)
                .register(registry);

        FunctionCounter.builder("i18n.bundle_cache.eviction.count", cache, c -> c.stats().evictionCount())
                .tags(tags)
                .register(registry);
    }
}
