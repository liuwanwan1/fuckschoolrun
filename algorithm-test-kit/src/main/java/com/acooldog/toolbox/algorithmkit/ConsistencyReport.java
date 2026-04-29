package com.acooldog.toolbox.algorithmkit;

import java.util.Locale;

public final class ConsistencyReport {
    private final int score;
    private final double distanceMeters;
    private final int steps;
    private final double strideMeters;
    private final double gpsAverageSpeed;
    private final double stepDerivedSpeed;
    private final String verdict;

    ConsistencyReport(
            int score,
            double distanceMeters,
            int steps,
            double strideMeters,
            double gpsAverageSpeed,
            double stepDerivedSpeed,
            String verdict
    ) {
        this.score = score;
        this.distanceMeters = distanceMeters;
        this.steps = steps;
        this.strideMeters = strideMeters;
        this.gpsAverageSpeed = gpsAverageSpeed;
        this.stepDerivedSpeed = stepDerivedSpeed;
        this.verdict = verdict;
    }

    public int getScore() {
        return score;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public int getSteps() {
        return steps;
    }

    public double getStrideMeters() {
        return strideMeters;
    }

    public double getGpsAverageSpeed() {
        return gpsAverageSpeed;
    }

    public double getStepDerivedSpeed() {
        return stepDerivedSpeed;
    }

    public String getVerdict() {
        return verdict;
    }

    public String toJson() {
        return String.format(
                Locale.US,
                "{\"type\":\"sensor_consistency\",\"score\":%d,\"distanceMeters\":%.3f,\"steps\":%d,\"strideMeters\":%.3f,\"gpsAverageSpeed\":%.3f,\"stepDerivedSpeed\":%.3f,\"verdict\":\"%s\"}",
                score,
                distanceMeters,
                steps,
                strideMeters,
                gpsAverageSpeed,
                stepDerivedSpeed,
                GeoMath.escapeJson(verdict)
        );
    }

    public String toCsv() {
        return "metric,value\n"
                + "score," + score + "\n"
                + String.format(Locale.US, "distance_meters,%.3f\n", distanceMeters)
                + "steps," + steps + "\n"
                + String.format(Locale.US, "stride_meters,%.3f\n", strideMeters)
                + String.format(Locale.US, "gps_average_speed_mps,%.3f\n", gpsAverageSpeed)
                + String.format(Locale.US, "step_derived_speed_mps,%.3f\n", stepDerivedSpeed)
                + "verdict," + verdict + "\n";
    }

    public String outputSummary() {
        return String.format(Locale.US, "一致性评分=%d, 步幅=%.2fm", score, strideMeters);
    }
}
