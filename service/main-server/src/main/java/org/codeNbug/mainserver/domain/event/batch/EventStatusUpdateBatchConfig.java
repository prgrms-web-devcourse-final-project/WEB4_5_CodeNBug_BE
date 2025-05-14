package org.codeNbug.mainserver.domain.event.batch;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.Collections;

@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class EventStatusUpdateBatchConfig {

    private final JpaCommonEventRepository eventRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job eventStatusUpdateJob(JobRepository jobRepository,
                                    Step eventStatusUpdateStep) {
        return new JobBuilder("eventStatusUpdateJob", jobRepository)
                .start(eventStatusUpdateStep)
                .build();
    }

    @Bean
    public Step eventStatusUpdateStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("eventStatusUpdateStep", jobRepository)
                .<Event, Event>chunk(100, transactionManager)
                .reader(eventReader())
                .processor(eventProcessor())
                .writer(eventWriter())
                .faultTolerant()
                .skip(IllegalArgumentException.class)   // 특정 잘못된 Event → 무시하고 계속
                .skipLimit(10)                          // 최대 10건 까지만 skip
                .retry(TransientDataAccessException.class)  // DB 일시 오류 재시도
                .retryLimit(3)                          // 최대 3회 재시도
                .build();
    }

    @Bean
    public RepositoryItemReader<Event> eventReader() {
        return new RepositoryItemReaderBuilder<Event>()
                .name("eventReader")
                .repository(eventRepository)
                .methodName("findSliceByIsDeletedFalse")
                .arguments(Collections.emptyList())
                .sorts(Collections.singletonMap("eventId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Event, Event> eventProcessor() {
        return event -> {
            LocalDateTime now = LocalDateTime.now();
            if (event.getStatus() != EventStatusEnum.CANCELLED) {
                if (event.getBookingStart() != null && event.getBookingEnd() != null) {
                    if (!now.isBefore(event.getBookingStart()) && !now.isAfter(event.getBookingEnd())) {
                        event.setStatus(EventStatusEnum.OPEN);
                    } else {
                        event.setStatus(EventStatusEnum.CLOSED);
                    }
                } else {
                    event.setStatus(EventStatusEnum.CLOSED);
                }
            }
            return event;
        };
    }

    @Bean
    public JpaItemWriter<Event> eventWriter() {
        JpaItemWriter<Event> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
