package de.uni_bremen.comnets.maniac.collections;

import java.util.ArrayList;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public class InvertibleArrayList<E> extends ArrayList<E> implements InvertibleList<E> {
    @Override
    public E get(int index) {
        if (index < -size()) {
            throw new IndexOutOfBoundsException(String.format("Inverted index (%d) was out of bounds of the array (size %d)", index, size()));
        }
        else if (index < 0) {
            return super.get(size() + index);
        }
        else {
            return super.get(index);
        }
    }

    @Override
    public E set(int index, E item) {
        if (index < -size()) {
            throw new IndexOutOfBoundsException(String.format("Inverted index (%d) was out of bounds of the array (size %d)", index, size()));
        }
        else if (index < 0) {
            return super.set(size() + index, item);
        }
        else {
            return super.set(index, item);
        }
    }
}
