package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/6/13.
 */
public class Triple<T, S, R> {
    private T first;
    private S second;
    private R third;

    public Triple(T first, S second, R third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public R getThird() {
        return third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple triple = (Triple) o;

        if (first != null ? !first.equals(triple.first) : triple.first != null) return false;
        if (second != null ? !second.equals(triple.second) : triple.second != null) return false;
        if (third != null ? !third.equals(triple.third) : triple.third != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        return result;
    }

    public static <T, S, R> Triple<T, S, R> make(T first, S second, R third) {
        return new Triple<T, S, R>(first, second, third);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %s]", first, second, third);
    }
}
