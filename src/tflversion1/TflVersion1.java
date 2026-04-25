package tflversion1;

import java.util.Scanner;

public class TflVersion1 {

    private Graph   graph;
    private Scanner sc;

    public static void main(String[] args) {
        try {
            new TflVersion1().run();
        } catch (Exception e) {
            System.out.println("Fatal error loading data: " + e.getMessage());
        }
    }

    public TflVersion1() throws Exception {
        graph = new Graph();
        sc    = new Scanner(System.in);
        System.out.println("=== TfL London Underground Route Finder ===");
        graph.loadConnections("data/Connections.csv");
        graph.loadInterchanges("data/Interchanges.csv");
        System.out.println("Data loaded successfully.\n");
    }


    // Main 
    private void run() {
        while (true) {
            printSeparator();
            System.out.println("MAIN MENU");
            System.out.println("1. Customer");
            System.out.println("2. Engineer");
            System.out.println("3. Exit");
            System.out.print("Select: ");

            switch (readInt()) {
                case 1: customerMenu();  break;
                case 2: engineerMenu();  break;
                case 3:
                    System.out.println("Goodbye.");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid option — enter 1, 2 or 3.");
            }
        }
    }

    // Customer menu
    private void customerMenu() {
        while (true) {
            printSeparator();
            System.out.println("CUSTOMER MENU");
            System.out.println("1. Journey Planner");
            System.out.println("2. Station Information");
            System.out.println("3. List Stations by Line");
            System.out.println("4. Back");
            System.out.print("Select: ");

            switch (readInt()) {
                case 1: journeyPlanner();  break;
                case 2: stationInfo();     break;
                case 3: listByLine();      break;
                case 4: return;
                default: System.out.println("Invalid option — enter 1 to 4.");
            }
        }
    }

    private void journeyPlanner() {
        printSeparator();
        System.out.println("JOURNEY PLANNER");

        String start = promptStation("Start station");
        if (start == null) return;

        String end = promptStation("End station");
        if (end == null) return;

        if (start.equalsIgnoreCase(end)) {
            System.out.println("Start and end station cannot be the same.");
            pressEnter();
            return;
        }

        graph.findRoute(start, end);
        pressEnter();
    }

    private void stationInfo() {
        printSeparator();
        System.out.println("STATION INFORMATION");

        String name = promptStation("Station name");
        if (name == null) return;

        graph.printStationInfo(name);
        pressEnter();
    }

    private void listByLine() {
        printSeparator();
        System.out.println("LIST STATIONS BY LINE");

        DynamicArray<String> lines = graph.getAllLines();
        System.out.println("Available lines:");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + lines.get(i));
        }
        System.out.print("Select line number (0 to cancel): ");
        int choice = readInt();

        if (choice == 0) return;
        if (choice < 1 || choice > lines.size()) {
            System.out.println("Invalid selection.");
            pressEnter();
            return;
        }
        graph.printStationsByLine(lines.get(choice - 1));
        pressEnter();
    }

    // Engineer menu
    private void engineerMenu() {
        while (true) {
            printSeparator();
            System.out.println("ENGINEER MENU");
            System.out.println("1. Close Track Section");
            System.out.println("2. Open Track Section");
            System.out.println("3. Add Delay");
            System.out.println("4. Remove Delay");
            System.out.println("5. View Closed Tracks");
            System.out.println("6. View Delayed Tracks");
            System.out.println("7. Back");
            System.out.print("Select: ");

            switch (readInt()) {
                case 1: manageTrack(true);   break;
                case 2: manageTrack(false);  break;
                case 3: manageDelay(true);   break;
                case 4: manageDelay(false);  break;
                case 5:
                    graph.printClosedTracks();
                    pressEnter();
                    break;
                case 6:
                    graph.printDelayedTracks();
                    pressEnter();
                    break;
                case 7: return;
                default: System.out.println("Invalid option — enter 1 to 7.");
            }
        }
    }

    private void manageTrack(boolean closing) {
        printSeparator();
        System.out.println(closing ? "CLOSE TRACK SECTION" : "OPEN TRACK SECTION");

        DynamicArray<Connection> conns = selectConnectionFromStation();
        if (conns == null) return;

        System.out.println("\nSelect connection (0 to cancel):");
        for (int i = 0; i < conns.size(); i++) {
            Connection c = conns.get(i);
            String status = c.isClosed() ? " [CLOSED]" : "";
            System.out.printf("  %d. %s (%s): to %s%s%n",
                    i + 1, c.getLine(), c.getDirection(), c.getEndStation(), status);
        }
        System.out.print("Select: ");
        int idx = readInt();
        if (idx == 0) return;
        if (idx < 1 || idx > conns.size()) {
            System.out.println("Invalid selection.");
            pressEnter();
            return;
        }

        Connection sel  = conns.get(idx - 1);
        String dirFilter = chooseDirFilter(sel.getDirection());

        if (closing) {
            graph.closeTrack(sel.getStartStation(), sel.getEndStation(),
                    sel.getLine(), dirFilter);
        } else {
            graph.openTrack(sel.getStartStation(), sel.getEndStation(),
                    sel.getLine(), dirFilter);
        }
        pressEnter();
    }

    private void manageDelay(boolean adding) {
        printSeparator();
        System.out.println(adding ? "ADD DELAY" : "REMOVE DELAY");

        DynamicArray<Connection> conns = selectConnectionFromStation();
        if (conns == null) return;

        System.out.println("\nSelect connection (0 to cancel):");
        for (int i = 0; i < conns.size(); i++) {
            Connection c = conns.get(i);
            String delay = c.getDelay() > 0 ? " [DELAY: +" + c.getDelay() + " min]" : "";
            System.out.printf("  %d. %s (%s): to %s%s%n",
                    i + 1, c.getLine(), c.getDirection(), c.getEndStation(), delay);
        }
        System.out.print("Select: ");
        int idx = readInt();
        if (idx == 0) return;
        if (idx < 1 || idx > conns.size()) {
            System.out.println("Invalid selection.");
            pressEnter();
            return;
        }

        Connection sel   = conns.get(idx - 1);
        String dirFilter = chooseDirFilter(sel.getDirection());

        if (adding) {
            System.out.print("Delay in minutes: ");
            double minutes = readDouble();
            if (minutes <= 0) {
                System.out.println("Delay must be greater than 0.");
                pressEnter();
                return;
            }
            graph.addDelay(sel.getStartStation(), sel.getEndStation(),
                    sel.getLine(), dirFilter, minutes);
        } else {
            graph.removeDelay(sel.getStartStation(), sel.getEndStation(),
                    sel.getLine(), dirFilter);
        }
        pressEnter();
    }

    // Shared helpers

    // Prompts for a station name, validates it exists, returns the canonical name or null
    private String promptStation(String prompt) {
        System.out.print(prompt + ": ");
        String input = sc.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Station name cannot be empty.");
            return null;
        }
        Station found = graph.findStation(input);
        if (found == null) {
            System.out.println("Station not found: \"" + input + "\". Check spelling and try again.");
            return null;
        }
        return found.getName(); 
    }

    // Prompts for a start station and returns all outgoing connections from it
    private DynamicArray<Connection> selectConnectionFromStation() {
        String name = promptStation("Start station");
        if (name == null) return null;

        DynamicArray<Connection> conns = graph.getConnectionsFromStation(name);
        if (conns.isEmpty()) {
            System.out.println("No outgoing connections found for: " + name);
            pressEnter();
            return null;
        }
        return conns;
    }

    // Asks whether to apply to one direction or both
    private String chooseDirFilter(String currentDirection) {
        System.out.println("Apply to:");
        System.out.println("  1. This direction only (" + currentDirection + ")");
        System.out.println("  2. Both directions");
        System.out.print("Select: ");
        int choice = readInt();
        return (choice == 1) ? currentDirection : "";
    }

    // Input helpers
    private int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    private double readDouble() {
        try {
            return Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return 0.0;
        }
    }

    private void pressEnter() {
        System.out.print("\nPress Enter to continue...");
        sc.nextLine();
    }

    private void printSeparator() {
        System.out.println("\n------------------------------------------");
    }
}
