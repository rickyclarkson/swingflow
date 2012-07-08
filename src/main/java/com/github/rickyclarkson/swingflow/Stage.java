package com.github.rickyclarkson.swingflow;

public interface Stage {
    StageProgress progress();
    void start();
    String name();
}