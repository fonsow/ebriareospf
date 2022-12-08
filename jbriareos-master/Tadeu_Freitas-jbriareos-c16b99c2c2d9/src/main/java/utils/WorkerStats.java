package utils;

import java.util.Date;

public class WorkerStats {
    public double cpuUsage;
    public double memUsage;
    public long revision;

    public WorkerStats(double cpuUsage, double memUsage) {
        this.cpuUsage = cpuUsage;
        this.memUsage = memUsage;
        this.revision = new Date().getTime();
    }
}
