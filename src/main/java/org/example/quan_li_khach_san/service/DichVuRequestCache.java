package org.example.quan_li_khach_san.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DichVuRequestCache {
    public record Request(Long stayId, Long serviceId, int quantity, String username, Long usageId) { }
    private static final int LIMIT = 10000;
    private final LinkedHashMap<String, Request> requests = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Boolean> delivered = new LinkedHashMap<>();

    public synchronized Request find(String key) { return requests.get(key); }

    public synchronized boolean remember(String key, Request request) {
        if (requests.containsKey(key)) return false;
        requests.put(key, request);
        if (requests.size() > LIMIT) requests.remove(requests.keySet().iterator().next());
        return true;
    }

    public synchronized void forget(String key, Request request) { requests.remove(key, request); }
    public synchronized boolean wasDelivered(Long id) { return delivered.containsKey(id); }
    public synchronized Set<Long> deliveredIds() { return Set.copyOf(delivered.keySet()); }

    public synchronized void markDelivered(Long id) {
        delivered.put(id, true);
        if (delivered.size() > LIMIT) delivered.remove(delivered.keySet().iterator().next());
    }
}
