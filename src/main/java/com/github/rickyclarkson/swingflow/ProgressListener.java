package com.github.rickyclarkson.swingflow;

import java.util.List;

interface ProgressListener<T> {
    void process(List<Progress<T>> chunks);
}
