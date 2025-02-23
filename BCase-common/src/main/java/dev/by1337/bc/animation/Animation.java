package dev.by1337.bc.animation;

import org.bukkit.entity.Player;

public interface Animation {
    void play();
    // принудительно завершаем анимацию
    void forceStop();
    boolean onClick(int intId, Player clicker);
}
