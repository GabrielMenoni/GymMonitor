package com.example.presenceservice.repository;

import com.example.presenceservice.dto.UserPresenceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisPresenceRepository {

    private static final String INSIDE_KEY = "gymmonitor:presence:inside";
    private static final String USER_KEY_PREFIX = "gymmonitor:presence:user:";
    private static final String EVENT_KEY_PREFIX = "gymmonitor:presence:event:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${presence.session.ttl-hours:6}")
    private long ttlHours;

    public boolean isEventProcessed(UUID eventId) {
        Boolean inserted = redisTemplate.opsForValue()
                .setIfAbsent(EVENT_KEY_PREFIX + eventId, "1", Duration.ofHours(1));
        return inserted == null || !inserted;
    }

    public void saveCheckin(UUID userId, String userType, String entradaEm,
                            UUID sessaoId, UUID eventId) {
        long epochMillis = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(INSIDE_KEY, userId.toString(), epochMillis);

        Map<String, String> hash = Map.of(
                "userType", userType,
                "entradaEm", entradaEm,
                "sessaoId", sessaoId.toString(),
                "lastEventId", eventId.toString()
        );
        redisTemplate.opsForHash().putAll(USER_KEY_PREFIX + userId, hash);
        redisTemplate.expire(USER_KEY_PREFIX + userId, ttlHours, TimeUnit.HOURS);
    }

    public void removeUser(UUID userId) {
        redisTemplate.opsForZSet().remove(INSIDE_KEY, userId.toString());
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    public long countInside() {
        evictExpiredSessions();
        Long count = redisTemplate.opsForZSet()
                .count(INSIDE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        return count == null ? 0L : count;
    }

    public List<UserPresenceInfo> listUsersInside() {
        evictExpiredSessions();
        Set<String> userIds = redisTemplate.opsForZSet().range(INSIDE_KEY, 0, -1);
        if (userIds == null) return List.of();

        List<UserPresenceInfo> result = new ArrayList<>();
        for (String userIdStr : userIds) {
            Map<Object, Object> hash = redisTemplate.opsForHash()
                    .entries(USER_KEY_PREFIX + userIdStr);
            if (hash.isEmpty()) continue;
            String userType = (String) hash.get("userType");
            String entradaEm = (String) hash.get("entradaEm");
            String sessaoIdStr = (String) hash.get("sessaoId");
            if (userType == null || entradaEm == null || sessaoIdStr == null) {
                continue;
            }
            result.add(new UserPresenceInfo(
                    UUID.fromString(userIdStr),
                    userType,
                    entradaEm,
                    UUID.fromString(sessaoIdStr)
            ));
        }
        return result;
    }

    private void evictExpiredSessions() {
        long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(ttlHours);
        redisTemplate.opsForZSet().removeRangeByScore(INSIDE_KEY, Double.NEGATIVE_INFINITY, cutoff);
    }
}
