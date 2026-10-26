package dev.codeduel.judge;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "codeduel.worker-enabled", havingValue = "true")
public class JudgeWorker {
  private static final Logger LOG = LoggerFactory.getLogger(JudgeWorker.class);
  private final JudgeJobStore jobs;
  private final JudgeGateway gateway;
  private final MeterRegistry metrics;

  public JudgeWorker(JudgeJobStore jobs, JudgeGateway gateway, MeterRegistry metrics) {
    this.jobs = jobs;
    this.gateway = gateway;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelay = 250, initialDelay = 3000)
  public void tick() {
    jobs.claim().ifPresent(this::execute);
  }

  /** All network calls happen outside database transactions. */
  private void execute(JudgeJobStore.Job job) {
    long started = System.nanoTime();
    try {
      if (jobs.expired(job)) throw new JudgeUnavailableException();
      for (var test : jobs.remaining(job)) {
        var step =
            gateway.evaluate(
                new JudgeGateway.Request(
                    job.source(), test.input(), job.timeLimitMs(), job.memoryLimitMb(), job.function()),
                test.expected(),
                test.executorToken());
        if (test.executorToken() == null && step.executorToken() != null) {
          boolean saved =
              gateway.reusableCheckpoint()
                  ? jobs.saveProgram(job, step.executorToken())
                  : jobs.saveToken(job, test.id(), step.executorToken());
          if (!saved) {
            if (gateway.reusableCheckpoint()) gateway.release(step.executorToken());
            return;
          }
        }
        var result = step.result();
        if (result.verdict().terminal() && !jobs.saveResult(job, test.id(), result)) return;
      }
      if (jobs.finishOrReschedule(job, gateway.kind()) && gateway.reusableCheckpoint())
        gateway.release(job.programToken());
    } catch (RuntimeException ex) {
      LOG.warn("Judge job {} failed ({})", job.id(), ex.getClass().getSimpleName());
      metrics.counter("codeduel.judge.failures").increment();
      jobs.fail(job);
    } finally {
      metrics
          .timer("codeduel.judge.worker.step")
          .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
    }
  }
}
