package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/9/13.
 */
public class MutableInteger {
    private int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutableInteger that = (MutableInteger) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
