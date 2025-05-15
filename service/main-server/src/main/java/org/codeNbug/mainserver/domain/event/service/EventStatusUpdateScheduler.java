package org.codeNbug.mainserver.domain.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusUpdateScheduler {

    private final JobLauncher jobLauncher;
    private final Job eventStatusUpdateJob;

    @Scheduled(cron = "0 */1 * * * *") // 1분마다 실행
    public void runEventStatusUpdateJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 매 실행마다 unique하게
                    .toJobParameters();
            jobLauncher.run(eventStatusUpdateJob, params);
        } catch (Exception e) {
            log.error("Event Status Update Batch 실행 중 오류", e);
        }
    }
}
