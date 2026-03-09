package com.hamradio.net;

import com.hamradio.dsp.graph.DSPGraph;

public class Station {

    public enum TxRxState { IDLE, TRANSMITTING, RECEIVING }

    private final String callsign;
    private final double latitude;
    private final double longitude;
    private double frequencyHz;
    private String mode;
    private boolean upperSideband;
    private TxRxState txRxState = TxRxState.IDLE;
    private DSPGraph txGraph;
    private DSPGraph rxGraph;

    public Station(String callsign, double latitude, double longitude,
                   double frequencyHz, String mode) {
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.frequencyHz = frequencyHz;
        this.mode = mode;
        this.upperSideband = true;
    }

    public double distanceTo(Station other) {
        double R = 6371000.0; // Earth radius in meters
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public String getCallsign() { return callsign; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getFrequencyHz() { return frequencyHz; }
    public void setFrequencyHz(double freq) { this.frequencyHz = freq; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isUpperSideband() { return upperSideband; }
    public void setUpperSideband(boolean usb) { this.upperSideband = usb; }
    public TxRxState getTxRxState() { return txRxState; }
    public void setTxRxState(TxRxState state) { this.txRxState = state; }
    public DSPGraph getTxGraph() { return txGraph; }
    public void setTxGraph(DSPGraph g) { this.txGraph = g; }
    public DSPGraph getRxGraph() { return rxGraph; }
    public void setRxGraph(DSPGraph g) { this.rxGraph = g; }
}
