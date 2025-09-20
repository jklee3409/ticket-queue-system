package com.mycom.myapp.scheduler;

import com.mycom.myapp.constant.RedisConstant;
import com.mycom.myapp.service.EntryQueueService;
import com.mycom.myapp.service.QueueManagerService;
import com.mycom.myapp.service.SseService;
import com.mycom.myapp.service.WaitingQueueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    @Value("${queue.settings.max-active-users}")
    private Long MAX_ACTIVE_USERS;

    private final QueueManagerService queueManagerService;
    private final WaitingQueueService waitingQueueService;
    private final EntryQueueService entryQueueService;
    private final SseService sseService;

    @Scheduled(fixedDelay = 3000)
    public void processQueue() {
        entryQueueService.popRandomActiveUsers();

        Long currentActiveCount = entryQueueService.getActiveUserCount();
        if (currentActiveCount == null) {
            log.error("Active user set (key: {}) does not exist in Redis.", RedisConstant.ACTIVE_USERS_KEY);
            return;
        }
        long possibleActiveCount = MAX_ACTIVE_USERS - currentActiveCount;
        if (possibleActiveCount <= 0) return;

        List<String> movedUsers = queueManagerService.moveTopNUsersToEntryQueue(possibleActiveCount);

        if (movedUsers != null && !movedUsers.isEmpty()) {
            movedUsers.forEach(userId -> {
                sseService.sentToClient(
                        userId,
                        "ACTIVE_TURN",
                        "The wait is over."
                );
            });

            List<String> waitingUsers = waitingQueueService.getWaitingUsersInOrder();

            if (waitingUsers == null || waitingUsers.isEmpty()) return;

            for (int i = 0; i < waitingUsers.size(); i++) {
                String userId = waitingUsers.get(i);
                long currentRank = i + 1;

                sseService.sentToClient(
                        userId,
                        "RANK_UPDATE",
                        currentRank
                );
            }
        }
    }
}
