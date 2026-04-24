package tflversion1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Graph {

    private List<Station> stations;
    private List<Connection> connections;
    private List<Interchange> interchanges;

    public Graph() {
        stations = new ArrayList<>();
        connections = new ArrayList<>();
        interchanges = new ArrayList<>();
    }

    // --- Data loading ---

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

            String start = parts[0].trim();
            String end = parts[1].trim();
            double time = Double.parseDouble(parts[2].trim());
            String lineName = parts[3].trim();
            String direction = parts[4].trim();

            connections.add(new Connection(start, end, time, lineName, direction));
            registerStationLine(start, lineName);
            registerStationLine(end, lineName);
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

            String station = parts[0].trim();
            String fromLine = parts[1].trim();
            String toLine = parts[2].trim();
            double time = Double.parseDouble(parts[3].trim());

            interchanges.add(new Interchange(station, fromLine, toLine, time));
        }

        reader.close();
        System.out.println("Loaded " + interchanges.size() + " interchanges.");
    }

    // Adds the line to an existing station, or creates a new station entry
    private void registerStationLine(String name, String lineName) {
        for (Station s : stations) {
            if (s.getName().equalsIgnoreCase(name)) {
                s.addLine(lineName);
                return;
            }
        }
        Station s = new Station(name);
        s.addLine(lineName);
        stations.add(s);
    }

    // --- Accessors ---

    public List<Station> getStations() {
        return stations;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public List<Interchange> getInterchanges() {
        return interchanges;
    }
}
