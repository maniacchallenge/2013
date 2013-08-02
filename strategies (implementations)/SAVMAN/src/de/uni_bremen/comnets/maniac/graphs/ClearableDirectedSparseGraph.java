package de.uni_bremen.comnets.maniac.graphs;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public class ClearableDirectedSparseGraph<V, E> extends DirectedSparseGraph<V, E> {
    public void clear() {
        edges.clear();
        vertices.clear();
    }
}
