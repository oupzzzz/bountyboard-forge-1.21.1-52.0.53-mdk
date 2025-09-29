package net.oupz.bountyboard.client;

import net.oupz.bountyboard.wanted.WantedSavedData;

import java.util.ArrayList;
import java.util.List;

public final class ClientWantedTop {
    private static final List<WantedSavedData.TopEntry> TOP = new ArrayList<>(3);

    private ClientWantedTop() {}

    public static synchronized void set(List<WantedSavedData.TopEntry> entries) {
        TOP.clear();
        if (entries != null) TOP.addAll(entries);
    }

    public static synchronized List<WantedSavedData.TopEntry> get() {
        return new ArrayList<>(TOP);
    }

    public static synchronized boolean isEmpty() {
        return TOP.isEmpty();
    }
}
