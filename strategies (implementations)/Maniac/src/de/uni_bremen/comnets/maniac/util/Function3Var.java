package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/1/13.
 */
public interface Function3Var<I1, I2, I3, O> {
    public O evaluate(I1 x, I2 y, I3 z);
}
