package tflversion1;

public class Interchange {

    private String station;
    private String fromLine;
    private String toLine;
    private double transferTime;

    public Interchange(String station, String fromLine, String toLine, double transferTime) {
        this.station = station;
        this.fromLine = fromLine;
        this.toLine = toLine;
        this.transferTime = transferTime;
    }

    public String getStation() {
        return station;
    }

    public String getFromLine() {
        return fromLine;
    }

    public String getToLine() {
        return toLine;
    }

    public double getTransferTime() {
        return transferTime;
    }

    @Override
    public String toString() {
        return station + ": " + fromLine + " -> " + toLine
                + " (+" + transferTime + " min)";
    }
}
