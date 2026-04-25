package tflversion1;


public class MinHeap {

    private static final int INITIAL_CAPACITY = 64;

    private double[] keys;
    private String[] values;
    private int size;

    public MinHeap() {
        keys   = new double[INITIAL_CAPACITY];
        values = new String[INITIAL_CAPACITY];
        size   = 0;
    }

    // Inserts a new key and value, restores the heap property
    public void insert(double key, String value) {
        if (size == keys.length) {
            grow();
        }
        keys[size]   = key;
        values[size] = value;
        size++;
        bubbleUp(size - 1);
    }

    // Removes and returns the value
    public String extractMin() {
        if (size == 0) {
            return null;
        }
        String min = values[0];

        // Move last element to root and restore heap
        size--;
        keys[0]   = keys[size];
        values[0] = values[size];
        keys[size]   = 0;
        values[size] = null;

        if (size > 0) {
            bubbleDown(0);
        }
        return min;
    }

    // Returns the smallest key without removing it
    public double peekMinKey() {
        if (size == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return keys[0];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // Moves element at index i upward until heap order is restored
    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (keys[parent] > keys[i]) {
                swap(parent, i);
                i = parent;
            } else {
                break;
            }
        }
    }

    // Moves element at index i downward 
    private void bubbleDown(int i) {
        while (true) {
            int left     = 2 * i + 1;
            int right    = 2 * i + 2;
            int smallest = i;

            if (left < size && keys[left] < keys[smallest]) {
                smallest = left;
            }
            if (right < size && keys[right] < keys[smallest]) {
                smallest = right;
            }
            if (smallest == i) {
                break;
            }
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        double tempKey = keys[a];
        keys[a] = keys[b];
        keys[b] = tempKey;

        String tempVal = values[a];
        values[a] = values[b];
        values[b] = tempVal;
    }

    // Doubles the capacity of both backing arrays
    private void grow() {
        int newCapacity  = keys.length * 2;
        double[] newKeys = new double[newCapacity];
        String[] newVals = new String[newCapacity];
        for (int i = 0; i < keys.length; i++) {
            newKeys[i] = keys[i];
            newVals[i] = values[i];
        }
        keys   = newKeys;
        values = newVals;
    }
}
