package exchange.core;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Account {
    Map<String, AtomicLong> assets = new ConcurrentHashMap<>();
    Map<String, AtomicLong> reserved = new ConcurrentHashMap<>();
    String id;
    String extra = "";

    public Account(String id) {
        this.id = id;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public Mono<Boolean> transfer(String assetId, long volume) {
        if (!assets.containsKey(assetId)) {
            assets.put(assetId, new AtomicLong(0L));
            reserved.put(assetId, new AtomicLong(0L));
        }
        AtomicLong value = assets.get(assetId);
        value.addAndGet(volume);
        return Mono.just(true);
    }

    public Mono<Boolean> reserve(String assetId, long volume) {
        if (!assets.containsKey(assetId)) {
            assets.put(assetId, new AtomicLong(0L));
            reserved.put(assetId, new AtomicLong(0L));
        }
        AtomicLong value = reserved.get(assetId);
        value.addAndGet(volume);
        return Mono.just(true);
    }

    public String getId() {
        return id;
    }

    public long getVolume(String assetId) {
        if (!assets.containsKey(assetId)) return 0L;
        return assets.get(assetId).get();
    }

    public Mono<Boolean> hasEnough(String assetId, long required) {
        if (!assets.containsKey(assetId)) return Mono.just(false);
        AtomicLong reserve = reserved.get(assetId);
        return Mono.just(assets.get(assetId).get() - reserve.get() >= required);
    }

    public Map<String, Long> getPublicAssets() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : assets.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }
}
