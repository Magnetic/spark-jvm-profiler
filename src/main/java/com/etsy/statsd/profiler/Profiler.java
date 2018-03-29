package com.etsy.statsd.profiler;

import com.etsy.statsd.profiler.reporter.Reporter;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Interface for profilers
 *
 * @author Andrew Johnson
 */
public abstract class Profiler {
    public static final Class<?>[] CONSTRUCTOR_PARAM_TYPES = new Class<?>[]{Reporter.class, Arguments.class};

    private final Reporter<?> reporter;
    // CONTAINER_ID=container_e273_1510780065801_7737_01_000042
    private String[] tags = null;

    private long recordedStats = 0;
    public Profiler(Reporter reporter, Arguments arguments) {
        Preconditions.checkNotNull(reporter);
        this.reporter = reporter;
        handleArguments(arguments);

        Collection<String> tagsList = new ArrayList<>();
        String containerId = System.getenv("CONTAINER_ID");
        if (containerId != null) {
            String applicationId = getApplicationId(containerId);
            tagsList.add("container_id:" + containerId);
            tagsList.add("application_id:" + applicationId);
        }

        String luigiTaskFamily = arguments.remainingArgs.get("luigi_task_family");
        if (luigiTaskFamily != null) {
            tagsList.add("luigi_task_family:" + luigiTaskFamily);
        }

        String luigiTaskDate = arguments.remainingArgs.get("luigi_task_date");
        if (luigiTaskDate != null) {
            tagsList.add("luigi_task_date:" + luigiTaskDate);
        }

        String luigiTaskHour = arguments.remainingArgs.get("luigi_task_hour");
        if (luigiTaskHour != null) {
            tagsList.add("luigi_task_hour:" + luigiTaskHour);
        }

        String sparkApplicationName = arguments.remainingArgs.get("spark_application_name");
        if (sparkApplicationName != null) {
            tagsList.add("spark_application_name:" + sparkApplicationName);
        }

        String sparkNumExecutors = arguments.remainingArgs.get("spark_num_executors");
        if (sparkApplicationName != null) {
            tagsList.add("spark_num_executors:" + sparkNumExecutors);
        }

        String sparkExecutorCores = arguments.remainingArgs.get("spark_executor_cores");
        if (sparkApplicationName != null) {
            tagsList.add("spark_executor_cores:" + sparkExecutorCores);
        }

        String[] tagsTemp = new String[tagsList.size()];
        tags = tagsList.toArray(tagsTemp);
    }

    public static String getApplicationId(String containerId) {
        String[] parts = containerId.split("_");
        return "application_" + parts[2] + "_" + parts[3];
    }

    /**
     * Perform profiling
     */
    public abstract void profile();

    /**
     * Hook to flush any remaining data cached by the profiler at JVM shutdown
     */
    public abstract void flushData();

    /**
     * Get the period to use for this profiler in the ScheduledExecutorService
     *
     * @return The ScheduledExecutorThread period for this profiler
     */
    public abstract long getPeriod();

    /**
     * Get the unit of time that corresponds to the period for this profiler
     *
     * @return A TimeUnit corresponding the the period for this profiler
     */
    public abstract TimeUnit getTimeUnit();

    /**
     * CPUTracingProfiler can emit some metrics that indicate the upper and lower bound on the length of stack traces
     * This is helpful for querying this data for some backends (such as Graphite) that do not have rich query languages
     * Reporters can override this to disable these metrics
     *
     * @return true if the bounds metrics should be emitted, false otherwise
     */
    protected boolean emitBounds() {
        return reporter.emitBounds();
    }

    /**
     * Handle any additional arguments necessary for this profiler
     *
     * @param arguments The arguments given to the profiler
     */
    protected abstract void handleArguments(Arguments arguments);

    /**
     * Record a gauge value
     *
     * @param key The key for the gauge
     * @param value The value of the gauge
     */
    protected void recordGaugeValue(String key, long value) {
        recordedStats++;
        reporter.recordGaugeValue(key, value);
    }

    /**
     * @see #recordGaugeValue(String, long)
     */
    protected void recordGaugeValue(String key, double value) {
        recordedStats++;
        reporter.recordGaugeValue(key, value);
    }

    /**
     * Record multiple gauge values
     * This is useful for reporters that can send points in batch
     *
     * @param gauges A map of gauge names to values
     */
    protected void recordGaugeValues(Map<String, ? extends Number> gauges) {
        recordedStats++;
        reporter.recordGaugeValues(gauges, tags);
    }

    public long getRecordedStats() { return recordedStats; }
}
