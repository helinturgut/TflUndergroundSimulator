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
     * Pass null or "" for line/direction to apply the operation to every
     * connection between start and end regardless of line or direction.
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

    // Prints all closed track sections in the spec-required format
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

    // Prints all delayed track sections with normal and delayed times
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
    // PHASE 5 — Dijkstra fastest-route finder
    // =========================================================================

    /*
     * Each graph node is identified by the string "stationName|line|direction".
     * This captures not just where a passenger is, but which line and direction
     * they are currently on — essential for modelling interchanges correctly.
     *
     * The algorithm uses a hand-coded MinHeap (Phase 2) as the priority queue
     * and a DynamicArray of NodeState objects instead of a HashMap for O(n)
     * distance lookups (acceptable for this network size).
     *
     * Stale heap entries are discarded via a visited flag (lazy deletion).
     */

    public void findRoute(String startName, String endName) {
        if (findStation(startName) == null) {
            System.out.println("Start station not found: " + startName);
            return;
        }
        if (findStation(endName) == null) {
            System.out.println("End station not found: " + endName);
            return;
        }
        if (startName.equalsIgnoreCase(endName)) {
            System.out.println("Start and end station are the same.");
            return;
        }

        DynamicArray<NodeState> nodes = buildNodeTable();

        // Set every node at the start station to distance 0
        for (int i = 0; i < nodes.size(); i++) {
            NodeState ns = nodes.get(i);
            if (ns.id.split("\\|")[0].equalsIgnoreCase(startName.trim())) {
                ns.dist = 0.0;
            }
        }

        runDijkstra(nodes, endName);

        // Find the end node with the smallest distance
        String bestEndId  = null;
        double bestDist   = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nodes.size(); i++) {
            NodeState ns = nodes.get(i);
            String station = ns.id.split("\\|")[0];
            if (station.equalsIgnoreCase(endName.trim()) && ns.dist < bestDist) {
                bestDist  = ns.dist;
                bestEndId = ns.id;
            }
        }

        if (bestEndId == null || bestDist == Double.POSITIVE_INFINITY) {
            System.out.println("No route found between " + startName + " and " + endName
                    + ". All paths may be closed.");
            return;
        }

        printPath(nodes, bestEndId, startName, endName, bestDist);
    }

    // Builds one NodeState per unique "station|line|direction" combination
    private DynamicArray<NodeState> buildNodeTable() {
        DynamicArray<NodeState> nodes = new DynamicArray<>();
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            String fromId = c.getStartStation() + "|" + c.getLine() + "|" + c.getDirection();
            String toId   = c.getEndStation()   + "|" + c.getLine() + "|" + c.getDirection();
            if (findNodeIndex(nodes, fromId) < 0) {
                nodes.add(new NodeState(fromId));
            }
            if (findNodeIndex(nodes, toId) < 0) {
                nodes.add(new NodeState(toId));
            }
        }
        return nodes;
    }

    private void runDijkstra(DynamicArray<NodeState> nodes, String endName) {
        MinHeap heap = new MinHeap();

        // Seed the heap with all zero-distance start nodes
        for (int i = 0; i < nodes.size(); i++) {
            NodeState ns = nodes.get(i);
            if (ns.dist == 0.0) {
                heap.insert(0.0, ns.id);
            }
        }

        while (!heap.isEmpty()) {
            String currentId    = heap.extractMin();
            NodeState currentNS = getNodeState(nodes, currentId);

            if (currentNS == null || currentNS.visited) {
                continue; // stale heap entry — skip
            }
            currentNS.visited = true;

            String[] parts     = currentId.split("\\|");
            String curStation  = parts[0];
            String curLine     = parts[1];
            String curDir      = parts[2];
            double curDist     = currentNS.dist;

            // Expand travel connections along the same line and direction
            for (int i = 0; i < connections.size(); i++) {
                Connection c = connections.get(i);
                if (c.getStartStation().equalsIgnoreCase(curStation)
                        && c.getLine().equalsIgnoreCase(curLine)
                        && c.getDirection().equalsIgnoreCase(curDir)) {

                    String neighbourId = c.getEndStation() + "|" + c.getLine() + "|" + c.getDirection();
                    double newDist     = curDist + c.getTotalTime();
                    NodeState neighbour = getNodeState(nodes, neighbourId);

                    if (neighbour != null && !neighbour.visited && newDist < neighbour.dist) {
                        neighbour.dist = newDist;
                        neighbour.prev = currentId;
                        heap.insert(newDist, neighbourId);
                    }
                }
            }

            // Expand interchanges (line changes at the current station)
            for (int i = 0; i < interchanges.size(); i++) {
                Interchange ic = interchanges.get(i);
                if (ic.getStation().equalsIgnoreCase(curStation)
                        && ic.getFromLine().equalsIgnoreCase(curLine)) {

                    // An interchange can board any direction of the target line
                    for (int j = 0; j < connections.size(); j++) {
                        Connection c = connections.get(j);
                        if (c.getStartStation().equalsIgnoreCase(curStation)
                                && c.getLine().equalsIgnoreCase(ic.getToLine())) {

                            String neighbourId  = ic.getStation() + "|"
                                    + ic.getToLine() + "|" + c.getDirection();
                            double newDist      = curDist + ic.getTransferTime();
                            NodeState neighbour = getNodeState(nodes, neighbourId);

                            if (neighbour != null && !neighbour.visited
                                    && newDist < neighbour.dist) {
                                neighbour.dist = newDist;
                                neighbour.prev = currentId;
                                heap.insert(newDist, neighbourId);
                            }
                        }
                    }
                }
            }
        }
    }

    // Reconstructs the path from prev pointers, then prints it in spec format
    private void printPath(DynamicArray<NodeState> nodes, String bestEndId,
                           String startName, String endName, double totalTime) {

        // Walk back through prev pointers to build the reversed path
        DynamicArray<String> reversed = new DynamicArray<>();
        String cursor = bestEndId;
        while (cursor != null) {
            reversed.add(cursor);
            NodeState ns = getNodeState(nodes, cursor);
            cursor = (ns != null) ? ns.prev : null;
        }

        // Reverse into forward order
        DynamicArray<String> path = new DynamicArray<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }

        System.out.println("\nRoute: " + startName + " to " + endName + ":");

        int stepNum = 1;

        // Step 1 — Start label
        String[] first = path.get(0).split("\\|");
        System.out.println("(" + stepNum++ + ") Start: "
                + first[0] + ", " + first[1] + " (" + first[2] + ")");

        // Steps 2..N-1 — travel or interchange
        for (int i = 1; i < path.size(); i++) {
            String[] cur = path.get(i).split("\\|");
            String[] prv = path.get(i - 1).split("\\|");

            if (prv[0].equalsIgnoreCase(cur[0])) {
                // Same station — interchange step
                double timeTaken = getNodeState(nodes, path.get(i)).dist
                                 - getNodeState(nodes, path.get(i - 1)).dist;
                System.out.printf("(%d) Change: %s %s (%s) to %s (%s) %.2fmin%n",
                        stepNum++, cur[0], prv[1], prv[2], cur[1], cur[2], timeTaken);
            } else {
                // Different station — travel step
                double legTime = lookupTravelTime(prv[0], cur[0], cur[1], cur[2]);
                System.out.printf("(%d) %s (%s): %s to %s %.2fmin%n",
                        stepNum++, cur[1], cur[2], prv[0], cur[0], legTime);
            }
        }

        // Final step — End label
        String[] last = path.get(path.size() - 1).split("\\|");
        System.out.println("(" + stepNum + ") End: "
                + last[0] + ", " + last[1] + " (" + last[2] + ")");
        System.out.printf("Total Journey Time: %.2f minutes%n", totalTime);
    }

    // =========================================================================
    // PHASE 6 — Customer queries (station info, line listings)
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

    // Returns the Station object for a name (case-insensitive), or null
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

    // Prints all stations served by a given line
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

    // True if c matches start+end and (if non-empty) line and direction
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

    // Linear scan to find the index of a node by ID; returns -1 if absent
    private int findNodeIndex(DynamicArray<NodeState> nodes, String id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // Returns the NodeState for an ID, or null if not found
    private NodeState getNodeState(DynamicArray<NodeState> nodes, String id) {
        int idx = findNodeIndex(nodes, id);
        return (idx >= 0) ? nodes.get(idx) : null;
    }

    // Looks up the actual (total) travel time for a specific connection leg
    private double lookupTravelTime(String from, String to,
                                    String line, String direction) {
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            if (c.getStartStation().equalsIgnoreCase(from)
                    && c.getEndStation().equalsIgnoreCase(to)
                    && c.getLine().equalsIgnoreCase(line)
                    && c.getDirection().equalsIgnoreCase(direction)) {
                return c.getTotalTime();
            }
        }
        return 0.0;
    }

    // =========================================================================
    // NodeState — private inner class for Dijkstra
    // =========================================================================

    /*
     * Holds the Dijkstra state for one node ("stationName|line|direction").
     *   dist    — best known cumulative journey time from the start
     *   prev    — ID of the node we arrived from (null at the start nodes)
     *   visited — true once this node is settled (extracted from the heap)
     */
    private static class NodeState {
        String  id;
        double  dist;
        String  prev;
        boolean visited;

        NodeState(String id) {
            this.id      = id;
            this.dist    = Double.POSITIVE_INFINITY;
            this.prev    = null;
            this.visited = false;
        }
    }
}
