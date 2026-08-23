package com.forge.job.application.handler;

import com.forge.job.domain.Job;
import com.forge.job.domain.JobType;
import org.springframework.stereotype.Component;

final class NoOpJobHandlers {
    private NoOpJobHandlers() {
    }

    @Component
    static class SendNotification implements JobHandler {
        public JobType supportedType() {
            return JobType.SEND_NOTIFICATION;
        }

        public void handle(Job job) {
        }
    }

    @Component
    static class ReconcilePayment implements JobHandler {
        public JobType supportedType() {
            return JobType.RECONCILE_PAYMENT;
        }

        public void handle(Job job) {
        }
    }

    @Component
    static class ExpireReservation implements JobHandler {
        public JobType supportedType() {
            return JobType.EXPIRE_RESERVATION;
        }

        public void handle(Job job) {
        }
    }

    @Component
    static class GenerateReport implements JobHandler {
        public JobType supportedType() {
            return JobType.GENERATE_REPORT;
        }

        public void handle(Job job) {
        }
    }
}