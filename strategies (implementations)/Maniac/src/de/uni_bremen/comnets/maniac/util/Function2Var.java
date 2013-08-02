package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/1/13.
 */
public interface Function2Var<I1, I2, O> {
    public O evaluate(I1 x, I2 y);
}
