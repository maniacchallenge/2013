package de.uni_bremen.comnets.maniac.collections;

import com.android.internal.util.Predicate;

import org.apache.commons.collections15.Transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public class Predicates {
    public static <E> boolean  any(Iterable<E> iterable, Predicate<E> predicate) {
        for (E e : iterable) {
            if (predicate.apply(e)) {
                return true;
            }
        }
        return false;
    }

    public static <E> boolean all(Iterable<E> iterable, Predicate<E> predicate) {
        for (E e : iterable) {
            if (!predicate.apply(e)) {
                return false;
            }
        }
        return true;
    }

    public static <I, O> Set<O> transformAll(Iterable<I> iterable, Transformer<I, O> transformer) {
        Set<O> result = new HashSet<O>();
        for (I i : iterable) {
            result.add(transformer.transform(i));
        }
        return result;
    }

    public static <E> List<E> findAll(Iterable<E> iterable, Predicate<E> predicate) {
        List<E> result = new ArrayList<E>();
        for (E e : iterable) {
            if (predicate.apply(e)) {
                result.add(e);
            }
        }
        return result;
    }

    public static <E> E findAny(Iterable<E> iterable, Predicate<E> predicate) {
        for (E e : iterable) {
            if (predicate.apply(e)) {
                return e;
            }
        }
        return null;
    }
}
