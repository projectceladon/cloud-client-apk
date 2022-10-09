package com.intel.gamepad.bean;

import android.util.Log;

import com.intel.gamepad.utils.FormatUtil;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

public class ConnStatsBean {
    private final String TAG = "ConnStatsBean";
    private final Object reportLock = new Object();
    // analyzed stats
    private final statsEntity statNetworkRtt;
    private final statsEntity statAvailBps;
    private final statsEntity statDecoderQp;
    private final statsEntity statDecoderFps;
    private final statsEntity statDecoderDelay;
    private final statsEntity statReceiverBps;
    private final statsEntity statSquareFrameDelay;
    private final statsEntity statReceiverPacketReceive;
    private final statsEntity statReceiverPacketLost;
    private final AspVideoQosReport videoQosReport;
    private RTCStatsReport rtcStatsReport;

    public ConnStatsBean() {
        this.statNetworkRtt = new statsEntity();
        this.statAvailBps = new statsEntity();
        this.statDecoderQp = new statsEntity();
        this.statDecoderFps = new statsEntity();
        this.statDecoderDelay = new statsEntity();
        this.statReceiverBps = new statsEntity();
        this.statSquareFrameDelay = new statsEntity();
        this.statReceiverPacketReceive = new statsEntity();
        this.statReceiverPacketLost = new statsEntity();

        this.videoQosReport = new AspVideoQosReport();
    }

    public String parseReport(RTCStatsReport rtcStatsReport) {
        this.rtcStatsReport = rtcStatsReport;
        return parseData();
    }

    private String parseData() {
        if (rtcStatsReport != null) {
            double tsUs = rtcStatsReport.getTimestampUs();
            double tsSec = tsUs * (1e-6);
            synchronized (reportLock) {
                videoQosReport.tsSec = tsSec;
            }

            // analyze the stats
            Map<String, RTCStats> statsMap = rtcStatsReport.getStatsMap();
            for (RTCStats stat : statsMap.values()) {
                String type = stat.getType();
                boolean isVideoStats = isVideoStats(stat);
                if ("candidate-pair".equals(type) && "succeeded".equals(findStatsMemberByKey(stat, "state"))) {

                    Log.v(TAG, "item candidate-pair:" + stat.toString());

                    // network-rtt
                    double rtt = (double) Objects.requireNonNull(findStatsMemberByKey(stat, "totalRoundTripTime"));//累计的往返时间  0.035
                    long responses = ((BigInteger) Objects.requireNonNull(findStatsMemberByKey(stat, "responsesReceived"))).longValue();//接收到的响应数(累计值)   68
                    statNetworkRtt.updateAccumulatedValue(rtt * 1000, responses);
                    Log.v(TAG, "statNetworkRtt:" + statNetworkRtt.value);

                    // available-bps
                    Object obj = findStatsMemberByKey(stat, "availableOutgoingBitrate");
                    if(obj!=null){
                        double bps = (double)obj;
                        statAvailBps.updateValue(bps);
                        Log.v(TAG, "statAvailBps:" + statAvailBps.value);
                    }

                    // receiver-bps
                    long bytesReceived = ((BigInteger) Objects.requireNonNull(findStatsMemberByKey(stat, "bytesReceived"))).longValue();
                    statReceiverBps.updateAccumulatedValue(bytesReceived * 8, tsSec);
                    Log.v(TAG, "statReceivedBps:" + statReceiverBps.value);

                    //videoQosReport
                    synchronized (reportLock) {
                        videoQosReport.availBps = statAvailBps.getValue();
                        videoQosReport.receiverBps = statReceiverBps.getValue();
                    }

                } else if (isVideoStats && "inbound-rtp".equals(type)) {

                    Log.v(TAG, "item inbound-rtp:" + stat.toString());

                    // ssrc
                    long ssrc = (long) Objects.requireNonNull(findStatsMemberByKey(stat, "ssrc"));
                    Log.v(TAG, "ssrc:" + ssrc);

                    // decoder-fps
                    long framesDecoded = (long) Objects.requireNonNull(findStatsMemberByKey(stat, "framesDecoded"));//RTP正确解码的帧总数 ，即没有丢帧的总数
                    statDecoderFps.updateAccumulatedValue(framesDecoded, tsSec);
                    Log.v(TAG, "statDecoderFps:" + statDecoderFps.value);

                    if (statDecoderFps.value > 0) {
                        // decoder-delay
                        double totalDecodeTime = (double) Objects.requireNonNull(findStatsMemberByKey(stat, "totalDecodeTime"));
                        statDecoderDelay.updateAccumulatedValue(totalDecodeTime * 1000, framesDecoded);
                        Log.v(TAG, "statDecoderDelay:" + statDecoderDelay.value);

                        // decoder-qp
                        long qpSum = ((BigInteger) Objects.requireNonNull(findStatsMemberByKey(stat, "qpSum"))).longValue(); //接收器解码的帧的总和，帧数以framesDecoded为单位
                        statDecoderQp.updateAccumulatedValue(qpSum, framesDecoded);
                        Log.v(TAG, "QpSum and FrameDeCoded:" + qpSum + ";" + framesDecoded);
                        Log.v(TAG, "statDecoderQp:" + statDecoderQp.value);
                    }

                    //square-frame-delay
                    double totalSquaredInterFrameDelay = (double) Objects.requireNonNull(findStatsMemberByKey(stat, "totalSquaredInterFrameDelay"));
                    double totalInterFrameDelay = (double) Objects.requireNonNull(findStatsMemberByKey(stat, "totalInterFrameDelay"));
                    statSquareFrameDelay.updateDelayValue(totalSquaredInterFrameDelay, totalInterFrameDelay, framesDecoded);
                    Log.v(TAG, "statSquareFrameDelay:" + statSquareFrameDelay.value);


                    //receive-packet
                    long packetsReceived = (long) Objects.requireNonNull(findStatsMemberByKey(stat, "packetsReceived"));
                    statReceiverPacketReceive.updateAccumulatedValue(packetsReceived, 0);
                    Log.v(TAG, "statReceiverPacketReceive:" + statReceiverPacketReceive.value);


                    // send-lost
                    int packetsLost = (int) Objects.requireNonNull(findStatsMemberByKey(stat, "packetsLost"));
                    statReceiverPacketLost.updateAccumulatedValue(packetsLost, 0);
                    Log.v(TAG, "packetsLost:" + statReceiverPacketLost.value);

                    //videoQosReport
                    synchronized (reportLock) {
                        videoQosReport.packetsReceive = statReceiverPacketReceive.value;
                        videoQosReport.fps = statDecoderFps.value;
                        videoQosReport.qp = statDecoderQp.value;
                        videoQosReport.squareFrameDelay = statSquareFrameDelay.value;
                        videoQosReport.tsSec = tsSec;
                    }

                }


            }
            return updateTxt();
        }
        return null;
    }

    private String updateTxt() {
        Log.v(TAG, "tsSec:" + videoQosReport.tsSec +
                "\navailBps:" + videoQosReport.availBps +
                "\nreceiverBps:" + videoQosReport.receiverBps +
                "\npacketsReceive:" + videoQosReport.packetsReceive +
                "\nfps:" + videoQosReport.fps +
                "\nqp:" + videoQosReport.qp +
                "\nsquareFrameDelay:" + videoQosReport.squareFrameDelay);
        return "availRate:" + FormatUtil.transferSize(videoQosReport.availBps, 2) + "ps" +
                "\nreceiverRate:" + FormatUtil.transferSize(videoQosReport.receiverBps, 2) + "ps" +
                "\npacketsReceive:" + FormatUtil.formatValue(videoQosReport.packetsReceive, 1) +
                "\nfps:" + FormatUtil.formatValue(videoQosReport.fps, 2) +
                "\nqp:" + FormatUtil.formatValue(videoQosReport.qp, 1);
    }

    private boolean isVideoStats(RTCStats stat) {
        Map<String, Object> mem = stat.getMembers();
        return mem.containsKey("kind") && Objects.equals(mem.get("kind"), "video");
    }

    private Object findStatsMemberByKey(RTCStats stats, String key) {
        Map<String, Object> mem = stats.getMembers();
        return mem.containsKey(key) ? mem.get(key) : null;
    }


    private boolean isStatsKeyEqual(String a, String b) {
        return a != null && b != null && b.startsWith(a);
    }


    static class statsEntity {

        private double value = 0.0;
        private double totalValue = 0.0;
        private double totalValue2 = 0.0;
        private double totalDenominator = 1;

        private void updateAccumulatedValue(double accValue, double accDenominator) {
            if (accDenominator == 0) {
                value = accValue - totalValue;
            } else if (accDenominator > totalDenominator) {
                value = (accValue - totalValue) / (accDenominator - totalDenominator);
            }
            totalDenominator = accDenominator;
            totalValue = accValue;
        }

        private void updateDelayValue(double accValue, double accValue2, double accDenominator) {
            if (accDenominator != 0 && (accDenominator - totalDenominator)!=0 ) {
                BigDecimal a = new BigDecimal(accDenominator - totalDenominator);
                BigDecimal b = new BigDecimal(accValue2 - totalValue2);
                BigDecimal c = new BigDecimal(accValue - totalValue);
                BigDecimal d = c.subtract(b.pow(2).divide(a, BigDecimal.ROUND_CEILING)).divide(a, BigDecimal.ROUND_CEILING);
                totalValue = accValue;
                totalValue2 = accValue2;
                totalDenominator = accDenominator;
                value = d.doubleValue();
            }
        }


        private void updateValue(double curValue) {
            value = curValue;
        }

        private double getValue() {
            return value;
        }


    }


    static class AspVideoQosReport {
        private double tsSec;

        private double availBps;
        private double receiverBps;

        private double packetsReceive;
        private double fps;
        private double qp;

        private double squareFrameDelay;
    }

}
