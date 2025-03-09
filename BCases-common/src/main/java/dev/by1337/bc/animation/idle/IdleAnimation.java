package dev.by1337.bc.animation.idle;

import dev.by1337.bc.annotations.SyncOnly;

public interface IdleAnimation {
    @SyncOnly
    void play();

    @SyncOnly
    void pause();
}
