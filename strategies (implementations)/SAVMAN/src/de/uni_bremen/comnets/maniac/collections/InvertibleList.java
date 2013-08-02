package de.uni_bremen.comnets.maniac.collections;

import java.util.List;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public interface InvertibleList<E> extends List<E> {

    /**
     * Permits a negative index as low as -size().  In the case of a negative
     * index, this method will count back from the end of the list.
     * For example, get(-1) will return the last item in the list, and
     * get(-size()) will return the first item.
     * @param index The index from which to retrieve the item, optionally negative.
     * @return The item at the specified index, if it is positive, or the element
     * the specified length from the end of the list, if it is negative.
     */
    @Override
    public E get(int index);

    /**
     * Permits a negative index as low as -size().  In the case of a negative
     * index, this method will count back from the end of the list.
     * For example, set(-1) will set the last item in the list, and
     * set(-size()) will set the first item.
     * @param index The index at which to set the item, optionally negative.
     * @return The item that was previously at the specified index.
     */
    @Override
    public E set(int index, E item);
}
