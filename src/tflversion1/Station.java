package tflversion1;

public class Station {

    private String name;
    private DynamicArray<String> lines;

    public Station(String name) {
        this.name  = name;
        this.lines = new DynamicArray<>();
    }

    // Adds a line only if it is not already recorded for this station
    public void addLine(String line) {
        if (!lines.contains(line)) {
            lines.add(line);
        }
    }

    public String getName() {
        return name;
    }

    public DynamicArray<String> getLines() {
        return lines;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" (lines: ");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(lines.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}
