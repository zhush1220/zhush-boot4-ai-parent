package com.zhush.job.core;

import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AbstractJob 基类单测（执行转发 + 异常包装）
 *
 * @author zhush
 */
class AbstractJobTest {

    static class SuccessJob extends AbstractJob {
        boolean executed;

        @Override
        protected void doExecute(JobExecutionContext context) {
            executed = true;
        }
    }

    static class FailureJob extends AbstractJob {
        @Override
        protected void doExecute(JobExecutionContext context) throws Exception {
            throw new IllegalStateException("业务异常");
        }
    }

    private JobExecutionContext contextOf(Class<? extends AbstractJob> jobClass) {
        JobDetail detail = JobBuilder.newJob(jobClass).withIdentity("job1", "G1").build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(detail);
        return context;
    }

    @Test
    void executeDelegatesToDoExecute() throws Exception {
        SuccessJob job = new SuccessJob();
        job.execute(contextOf(SuccessJob.class));
        assertThat(job.executed).isTrue();
    }

    @Test
    void businessExceptionWrappedAsJobExecutionException() {
        FailureJob job = new FailureJob();
        assertThatThrownBy(() -> job.execute(contextOf(FailureJob.class)))
                .isInstanceOf(JobExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("业务异常");
    }
}
