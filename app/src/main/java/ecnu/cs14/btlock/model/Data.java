package ecnu.cs14.btlock.model;

import java.util.ArrayList;
import java.util.Collection;

public class Data extends ArrayList<Byte> {

    public static final int SIZE = 16;

    /**
     * Constructs a new {@code ArrayList} instance with zero initial capacity.
     */
    public Data() {
        super(SIZE);
    }

    /**
     * Constructs a new instance of {@code ArrayList} containing the elements of
     * the specified collection.
     *
     * @param collection the collection of elements to add.
     */
    public Data(Collection<? extends Byte> collection) throws Exception {
        super(collection);
        if(collection.size()!=SIZE)
        {
            throw new Exception("Wrong size.");
        }
    }

    public <T extends Byte> Data(T[] array) throws Exception
    {
        super(SIZE);
        if (array.length != SIZE){
            throw new Exception("Wrong size.");
        }
        for (int i = 0; i < SIZE; i++) {
            set(i, array[i]);
        }
    }

    public Data(byte[] array) throws Exception
    {
        super(SIZE);
        if (array.length != SIZE){
            throw new Exception("Wrong size.");
        }
        for (int i = 0; i < SIZE; i++) {
            set(i, array[i]);
        }
    }

    /**
     * Constructs a new instance of {@code ArrayList} with the specified
     * initial capacity.
     *
     * @param capacity the initial capacity of this {@code ArrayList}.
     */
    public Data(int capacity) throws Exception {
        super(capacity);
        if(capacity!=SIZE)
        {
            throw new Exception("Wrong size.");
        }
    }

}
