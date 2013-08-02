package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 6/27/13.
 */
public interface Function<I, O> {
    public O evaluate(I x); // TODO: Cache function values to save time.
}
