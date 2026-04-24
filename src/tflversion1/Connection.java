package tflversion1;

public class Connection {

    private String startStation;
    private String endStation;
    private double travelTime;
    private String line;
    private String direction;
    private boolean closed;
    private double delay;

    public Connection(String startStation, String endStation, double travelTime,
            String line, String direction) {
        this.startStation = startStation;
        this.endStation = endStation;
        this.travelTime = travelTime;
        this.line = line;
        this.direction = direction;
        this.closed = false;
        this.delay = 0;
    }

    // Returns infinity if closed, otherwise travel time plus any active delay
    public double getTotalTime() {
        if (closed) {
            return Double.POSITIVE_INFINITY;
        }
        return travelTime + delay;
    }

    public void close() {
        closed = true;
    }

    public void open() {
        closed = false;
    }

    public void setDelay(double minutes) {
        this.delay = minutes;
    }

    public void removeDelay() {
        this.delay = 0;
    }

    public String getStartStation() {
        return startStation;
    }

    public String getEndStation() {
        return endStation;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public String getLine() {
        return line;
    }

    public String getDirection() {
        return direction;
    }

    public boolean isClosed() {
        return closed;
    }

    public double getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        String status = "";
        if (closed) {
            status = " [CLOSED]";
        } else if (delay > 0) {
            status = " [DELAY: +" + delay + " min]";
        }
        return line + " (" + direction + "): " + startStation + " -> "
                + endStation + " " + travelTime + " min" + status;
    }
}
