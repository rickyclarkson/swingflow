package com.github.rickyclarkson.swingflow;

import fj.P2;

public interface Stage {
    int estimateSecondsLeft();
    P2<String, String> briefAndDetailedStatuses();
    void start();
    String title();
}
