package tflversion1;

import java.io.BufferedReader;
import java.io.FileReader;

public class Graph {

    private DynamicArray<Station>     stations;
    private DynamicArray<Connection>  connections;
    private DynamicArray<Interchange> interchanges;

    public Graph() {
        stations     = new DynamicArray<>();
        connections  = new DynamicArray<>();
        interchanges = new DynamicArray<>();
    }

    // =========================================================================
    // PHASE 3 — Data loading
    // =========================================================================

    public void loadConnections(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        reader.readLine(); // skip header row

        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length < 5) {
                System.out.println("Skipping malformed line " + lineNumber + " in " + filename);
                continue;
            }

            String start     = parts[0].trim();
            String end       = parts[1].trim();
            double time      = Double.parseDouble(parts[2].trim());
            String lineName  = parts[3].trim();
            String direction = parts[4].trim();

            connections.add(new Connection(start, end, time, lineName, direction));
            registerStationLine(start, lineName);
            registerStationLine(end,   lineName);
        }

        reader.close();
        System.out.println("Loaded " + connections.size() + " connections.");
    }

    public void loadInterchanges(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        reader.readLine(); // skip header row

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length < 4) {
                continue;
            }

            String station  = parts[0].trim();
            String fromLine = parts[1].trim();
            String toLine   = parts[2].trim();
            double time     = Double.parseDouble(parts[3].trim());

            interchanges.add(new Interchange(station, fromLine, toLine, time));
        }

        reader.close();
        System.out.println("Loaded " + interchanges.size() + " interchanges.");
    }

    // Adds the line to an existing station entry, or creates a new one
    private void registerStationLine(String name, String lineName) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).getName().equalsIgnoreCase(name)) {
                stations.get(i).addLine(lineName);
                return;
            }
        }
        Station s = new Station(name);
        s.addLine(lineName);
        stations.add(s);
    }

    // =========================================================================
    // PHASE 4 — Engineer operations
    // =========================================================================

    /*
     * For close/open/delay methods, pass null or "" for line/direction to
     * apply the operation to every connection between start and end regardless
     * of line or direction (i.e. both directions at once).
     */

    public boolean closeTrack(String start, String end,
                              String line, String direction) {
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (matchesConnection(c, start, end, line, direction)) {
                c.close();
                found = true;
                System.out.println("Closed: " + formatConnection(c));
            }
        }
        if (!found) {
            System.out.println("No matching track section found.");
        }
        return found;
    }

    public boolean openTrack(String start, String end,
                             String line, String direction) {
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (matchesConnection(c, start, end, line, direction)) {
                c.open();
                found = true;
                System.out.println("Opened: " + formatConnection(c));
            }
        }
        if (!found) {
            System.out.println("No matching track section found.");
        }
        return found;
    }

    public boolean addDelay(String start, String end,
                            String line, String direction, double minutes) {
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (matchesConnection(c, start, end, line, direction)) {
                c.setDelay(minutes);
                found = true;
                System.out.println("Delay of " + minutes + " min added to: "
                        + formatConnection(c));
            }
        }
        if (!found) {
            System.out.println("No matching track section found.");
        }
        return found;
    }

    public boolean removeDelay(String start, String end,
                               String line, String direction) {
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (matchesConnection(c, start, end, line, direction)) {
                c.removeDelay();
                found = true;
                System.out.println("Delay removed from: " + formatConnection(c));
            }
        }
        if (!found) {
            System.out.println("No matching track section found.");
        }
        return found;
    }

    // Prints all closed track sections in the required spec format
    public void printClosedTracks() {
        System.out.println("\n--- Closed Track Sections ---");
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.isClosed()) {
                System.out.println(c.getLine() + " (" + c.getDirection() + "): "
                        + c.getStartStation() + " - " + c.getEndStation() + " - closed");
                found = true;
            }
        }
        if (!found) {
            System.out.println("No closed track sections.");
        }
    }

    // Prints all delayed track sections showing normal time and delayed time
    public void printDelayedTracks() {
        System.out.println("\n--- Delayed Track Sections ---");
        boolean found = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.getDelay() > 0) {
                System.out.printf("%s (%s): %s to %s - %.2f min delay"
                        + " (normal: %.2f min, delayed: %.2f min)%n",
                        c.getLine(), c.getDirection(),
                        c.getStartStation(), c.getEndStation(),
                        c.getDelay(), c.getTravelTime(), c.getTotalTime());
                found = true;
            }
        }
        if (!found) {
            System.out.println("No delayed track sections.");
        }
    }

    // =========================================================================
    // Utility queries (used by Phase 6 customer menu)
    // =========================================================================

    // Returns a DynamicArray of unique line names across all connections
    public DynamicArray<String> getAllLines() {
        DynamicArray<String> lines = new DynamicArray<>();
        for (int i = 0; i < connections.size(); i++) {
            String name = connections.get(i).getLine();
            if (!lines.contains(name)) {
                lines.add(name);
            }
        }
        return lines;
    }

    // Returns the Station for a given name (case-insensitive), or null
    public Station findStation(String name) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).getName().equalsIgnoreCase(name.trim())) {
                return stations.get(i);
            }
        }
        return null;
    }

    // Prints TfL-style information about a station
    public void printStationInfo(String name) {
        Station s = findStation(name);
        if (s == null) {
            System.out.println("Station not found: " + name);
            return;
        }

        System.out.println("\n--- Station: " + s.getName() + " ---");

        System.out.print("Lines served: ");
        for (int i = 0; i < s.getLines().size(); i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(s.getLines().get(i));
        }
        System.out.println();

        System.out.println("Connections:");
        boolean hasConn = false;
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.getStartStation().equalsIgnoreCase(name.trim())) {
                String status = "";
                if (c.isClosed()) {
                    status = " [CLOSED]";
                } else if (c.getDelay() > 0) {
                    status = " [DELAY: +" + c.getDelay() + " min]";
                }
                System.out.printf("  %s (%s): to %-30s %.2f min%s%n",
                        c.getLine(), c.getDirection(),
                        c.getEndStation(), c.getTravelTime(), status);
                hasConn = true;
            }
        }
        if (!hasConn) {
            System.out.println("  None");
        }

        System.out.println("Interchanges:");
        boolean hasIc = false;
        for (int i = 0; i < interchanges.size(); i++) {
            Interchange ic = interchanges.get(i);
            if (ic.getStation().equalsIgnoreCase(name.trim())) {
                System.out.printf("  Change: %-20s -> %s (+%.2f min)%n",
                        ic.getFromLine(), ic.getToLine(), ic.getTransferTime());
                hasIc = true;
            }
        }
        if (!hasIc) {
            System.out.println("  None");
        }
    }

    // Prints all stations that appear on a given line
    public void printStationsByLine(String lineName) {
        System.out.println("\n--- Stations on " + lineName + " ---");
        DynamicArray<String> seen = new DynamicArray<>();
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.getLine().equalsIgnoreCase(lineName.trim())
                    && !seen.contains(c.getStartStation())) {
                seen.add(c.getStartStation());
                System.out.println("  " + c.getStartStation());
            }
        }
        if (seen.isEmpty()) {
            System.out.println("  No stations found for line: " + lineName);
        }
    }

    // Returns all connections departing from a named station
    public DynamicArray<Connection> getConnectionsFromStation(String name) {
        DynamicArray<Connection> result = new DynamicArray<>();
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.getStartStation().equalsIgnoreCase(name.trim())) {
                result.add(c);
            }
        }
        return result;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public DynamicArray<Station>     getStations()     { return stations; }
    public DynamicArray<Connection>  getConnections()  { return connections; }
    public DynamicArray<Interchange> getInterchanges() { return interchanges; }

    // =========================================================================
    // Private helpers
    // =========================================================================

    // True if c matches start+end, and (if provided) line and direction
    private boolean matchesConnection(Connection c,
                                      String start, String end,
                                      String line,  String direction) {
        if (!c.getStartStation().equalsIgnoreCase(start.trim())) return false;
        if (!c.getEndStation().equalsIgnoreCase(end.trim()))     return false;
        if (line != null && !line.isEmpty()
                && !c.getLine().equalsIgnoreCase(line.trim()))           return false;
        if (direction != null && !direction.isEmpty()
                && !c.getDirection().equalsIgnoreCase(direction.trim())) return false;
        return true;
    }

    private String formatConnection(Connection c) {
        return c.getLine() + " (" + c.getDirection() + "): "
                + c.getStartStation() + " -> " + c.getEndStation();
    }
}
