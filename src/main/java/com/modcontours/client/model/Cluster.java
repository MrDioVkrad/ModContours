package com.modcontours.client.model;

import java.util.ArrayList;
import java.util.List;

public final class Cluster {
    private final String modId;
    private final List<SlotCell> cells = new ArrayList<>();

    public Cluster(String modId) {
        this.modId = modId;
    }

    public String modId() {
        return modId;
    }

    public List<SlotCell> cells() {
        return cells;
    }
}
