package de.uni_bremen.comnets.maniac.graphs;

import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public class Connection {
    private Device first;
    private Device second;
    private Float cost;

    public Connection(Device first, Device second, Float cost) {
        this.first = first;
        this.second = second;
        this.cost = cost;
    }

    public Device getFirst() {
        return first;
    }

    public Device getSecond() {
        return second;
    }

    public Float getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return String.format("{[%s] -> [%s]}", first, second);
    }
}
