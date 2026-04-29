package com.acooldog.toolbox.sensortest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GpxNmeaReplayEngineTest {
    @Test
    public void gpxReplayCreatesWatermarkedNmeaStream() {
        NmeaReplayReport report = new GpxNmeaReplayEngine().generate(sampleGpx(), NmeaAnomalyMode.SPEED_JUMP);

        assertEquals(3, report.getPoints().size());
        assertTrue(report.toNmeaStream().contains(NmeaReplayReport.WATERMARK));
        assertTrue(report.toNmeaStream().contains("$GPRMC"));
        assertTrue(report.getFindings().size() >= 1);
    }

    private String sampleGpx() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gpx version=\"1.1\" creator=\"test\">"
                + "<trk><trkseg>"
                + "<trkpt lat=\"36.667662\" lon=\"117.027707\"><ele>55</ele><time>2024-01-01T00:00:00Z</time></trkpt>"
                + "<trkpt lat=\"36.667900\" lon=\"117.028100\"><ele>56</ele><time>2024-01-01T00:00:05Z</time></trkpt>"
                + "<trkpt lat=\"36.668200\" lon=\"117.028500\"><ele>57</ele><time>2024-01-01T00:00:10Z</time></trkpt>"
                + "</trkseg></trk>"
                + "</gpx>";
    }
}
