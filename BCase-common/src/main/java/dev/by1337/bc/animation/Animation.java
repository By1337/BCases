package dev.by1337.bc.animation;

public interface Animation {
    void play();
    // принудительно завершаем анимацию
    void forceStop();
}
