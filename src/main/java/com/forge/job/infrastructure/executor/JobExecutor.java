package com.forge.job.infrastructure.executor;

import com.forge.infrastructure.configuration.ForgeProperties;
import com.forge.job.application.handler.JobHandlerRegistry;
import com.forge.job.application.port.JobRepository;
import com.forge.job.domain.Job;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@Component
public class JobExecutor {
    private final JobRepository repository;
    private final JobHandlerRegistry handlers;
    private final ForgeProperties.Job.Executor properties;
    private final ThreadPoolExecutor workers;
    private final ScheduledExecutorService retryScheduler;
    private final LongAdder submitted = new LongAdder();
    private final LongAdder completed = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder retried = new LongAdder();
    private final LongAdder rejected = new LongAdder();

    public JobExecutor(JobRepository repository, JobHandlerRegistry handlers, ForgeProperties properties) {
        this.repository = repository;
        this.handlers = handlers;
        properties = properties == null ? new ForgeProperties() : properties;
        this.properties = properties.getJob().getExecutor();
        workers = new ThreadPoolExecutor(this.properties.getCorePoolSize(), this.properties.getMaxPoolSize(),
                this.properties.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(this.properties.getQueueCapacity()),
                new NamedThreadFactory("forge-job-worker-"), new ThreadPoolExecutor.AbortPolicy());
        retryScheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("forge-job-retry-"));
    }

    public void submit(Job job) {
        if (workers.isShutdown()) {
            rejected.increment();
            throw new RejectedExecutionException("Job executor is shut down");
        }
        try {
            workers.execute(() -> execute(job));
            submitted.increment();
        } catch (RejectedExecutionException exception) {
            rejected.increment();
            throw exception;
        }
    }

    public int queueDepth() {
        return workers.getQueue().size();
    }

    public long submittedCount() {
        return submitted.sum();
    }

    public long completedCount() {
        return completed.sum();
    }

    public long failedCount() {
        return failed.sum();
    }

    public long retriedCount() {
        return retried.sum();
    }

    public long rejectedCount() {
        return rejected.sum();
    }

    private void execute(Job job) {
        try {
            job.start();
            repository.save(job);
            handlers.handlerFor(job).handle(job);
            job.complete();
            completed.increment();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            handleFailure(job);
        } catch (Exception exception) {
            handleFailure(job);
        } finally {
            repository.save(job);
        }
    }

    private void handleFailure(Job job) {
        failed.increment();
        if (job.getStatus() == com.forge.job.domain.JobStatus.RUNNING) {
            job.fail();
        }
        if (job.getStatus() == com.forge.job.domain.JobStatus.FAILED) {
            job.retry();
            if (job.getStatus() == com.forge.job.domain.JobStatus.RETRYING) {
                retried.increment();
                long delay = retryDelayMillis(job.getRetryCount());
                retryScheduler.schedule(() -> submit(job), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private long retryDelayMillis(int attempt) {
        long exponential = properties.getBaseRetryDelay().toMillis() * (1L << Math.min(attempt, 30));
        return Math.min(properties.getMaxRetryDelay().toMillis(), exponential);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        workers.shutdown();
        retryScheduler.shutdown();
        if (!workers.awaitTermination(properties.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
            workers.shutdownNow();
        }
        if (!retryScheduler.awaitTermination(properties.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
            retryScheduler.shutdownNow();
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger();

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            return new Thread(task, prefix + sequence.incrementAndGet());
        }
    }
}