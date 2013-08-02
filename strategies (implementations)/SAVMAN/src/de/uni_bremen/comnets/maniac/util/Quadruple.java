package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/1/13.
 */
public class Quadruple<T, S, R, Q> {
    private T first;
    private S second;
    private R third;
    private Q fourth;

    public Quadruple(T first, S second, R third, Q fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
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

    public Q getFourth() {
        return fourth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quadruple quadruple = (Quadruple) o;

        if (first != null ? !first.equals(quadruple.first) : quadruple.first != null) return false;
        if (fourth != null ? !fourth.equals(quadruple.fourth) : quadruple.fourth != null)
            return false;
        if (second != null ? !second.equals(quadruple.second) : quadruple.second != null)
            return false;
        if (third != null ? !third.equals(quadruple.third) : quadruple.third != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        result = 31 * result + (fourth != null ? fourth.hashCode() : 0);
        return result;
    }

    public static <T, S, R, Q> Quadruple<T, S, R, Q> make(T first, S second, R third, Q fourth) {
        return new Quadruple<T, S, R, Q>(first, second, third, fourth);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %s, %s]", first, second, third, fourth);
    }
}
