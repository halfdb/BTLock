package ecnu.cs14.btlock.model;

import java.util.ArrayList;
import java.util.Collection;

public class Password extends ArrayList<Byte> implements Cloneable {
    public static final int SIZE = 15;
    
    /**
     * Constructs a new {@code ArrayList} instance with zero initial capacity.
     */
    public Password() {
        super(SIZE);
    }

    public Password(String s) {
        super(SIZE);
        String[] strings = s.split(" ");
        for (int i = 0; i < SIZE; i++) {
            add(i, Byte.valueOf(strings[i]));
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(get(0));
        for (int i = 1; i < SIZE; i++) {
            stringBuilder.append(' ');
            stringBuilder.append(get(i));
        }
        return stringBuilder.toString();
    }

    /**
     * Constructs a new instance of {@code ArrayList} containing the elements of
     * the specified collection.
     *
     * @param collection the collection of elements to add.
     */
    public Password(Collection<? extends Byte> collection) throws Exception {
        super(collection);
        if(collection.size()!=SIZE)
        {
            throw new Exception("Wrong size.");
        }
    }

    public <T extends Byte> Password(T[] array) throws Exception
    {
        super(SIZE);
        if (array.length != SIZE){
            throw new Exception("Wrong size.");
        }
        for (int i = 0; i < SIZE; i++) {
            add(i, array[i]);
        }
    }

    public Password(byte[] array) throws Exception
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
            return new Password(this.byteArray());
        } catch (Exception e) {
            // impossible
            return super.clone();
        }
    }

    public static Password extractFromData(Data data) {
        try {
            byte[] bytes = new byte[SIZE];
            int i = Data.SIZE - SIZE, i0 = i;
            for ( ;   i < Data.SIZE ;  i++) {
                bytes[i - i0] = data.get(i);
            }
            return new Password(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
