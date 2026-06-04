package com.example.presenceservice.scheduler;

import com.example.presenceservice.repository.RedisPresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistorySnapshotScheduler {

    private final RedisPresenceRepository repository;

    @Scheduled(fixedRate = 60_000)
    public void snapshot() {
        repository.saveHistorySnapshot(repository.countInside());
    }
}
