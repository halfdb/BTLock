package ecnu.cs14.btlock.model;

import java.util.ArrayList;
import java.util.Collection;

public class Data extends ArrayList<Byte> implements Cloneable {

    public static final int SIZE = 16;

    /**
     * Constructs a new {@code ArrayList} instance with zero initial capacity.
     */
    public Data() {
        super(SIZE);
        while (size()<SIZE) {
            add((byte)0);
        }
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
            add(i, array[i]);
        }
    }

    public Data(byte[] array) throws Exception
    {
        super(SIZE);
        if (array.length != SIZE){
            throw new Exception("Wrong size.");
        }
        for (int i = 0; i < SIZE; i++) {
            add(i, array[i]);
        }
    }

    public byte[] byteArray() {
        byte[] ret = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            ret[i] = get(i);
        }
        return ret;
    }

    @Override
    public Object clone() {
        try {
            return new Data(this.byteArray());
        } catch (Exception e) {
            // impossible
            return super.clone();
        }
    }

    public static Data extendPassword(Password password) {
        try {
            byte[] bytes = new byte[SIZE];
            int i0 = SIZE - Password.SIZE;
            for (int i = i0; i < SIZE; i++) {
                bytes[i] = password.get(i - i0);
            }
            return new Data(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
