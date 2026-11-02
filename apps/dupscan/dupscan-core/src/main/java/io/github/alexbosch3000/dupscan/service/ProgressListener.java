package io.github.alexbosch3000.dupscan.service;

/** Optional progress callbacks so a front-end (CLI/API) can render status. */
public interface ProgressListener {

    void onPhase(String phase);

    void onHashProgress(long processed, long total);

    static ProgressListener noop() {
        return new ProgressListener() {
            @Override
            public void onPhase(String phase) {
            }

            @Override
            public void onHashProgress(long processed, long total) {
            }
        };
    }
}
