package tflversion1;

/*
 * Hand-coded generic resizable array.
 * Replaces java.util.ArrayList for Version 1.
 * Backed by a plain Object[] that doubles in capacity when full.
 */
public class DynamicArray<T> {

    private static final int INITIAL_CAPACITY = 8;

    private Object[] data;
    private int size;

    public DynamicArray() {
        data = new Object[INITIAL_CAPACITY];
        size = 0;
    }

    // Appends item to the end, growing the backing array if needed
    public void add(T item) {
        if (size == data.length) {
            grow();
        }
        data[size] = item;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    public void set(int index, T item) {
        checkIndex(index);
        data[index] = item;
    }

    // Removes element at index, shifting subsequent elements left
    public void remove(int index) {
        checkIndex(index);
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        size--;
        data[size] = null;
    }

    // Linear scan using equals(); returns false for null item
    public boolean contains(T item) {
        if (item == null) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (item.equals(data[i])) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // Doubles backing array capacity
    private void grow() {
        Object[] bigger = new Object[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            bigger[i] = data[i];
        }
        data = bigger;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + size);
        }
    }
}
