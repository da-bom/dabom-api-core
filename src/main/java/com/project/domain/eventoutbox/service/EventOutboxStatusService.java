package com.project.domain.eventoutbox.service;

import com.project.domain.eventoutbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventOutboxStatusService {

    private final EventOutboxRepository eventOutboxRepository;

    /** 커밋 이후에도 안전하게 상태를 바꾸기 위해 항상 새 트랜잭션에서 SENT 전이를 수행한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long outboxId) {
        eventOutboxRepository.markSentIfPending(outboxId);
    }
}
