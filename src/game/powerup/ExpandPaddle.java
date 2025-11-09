package game.powerup;

import game.GamePanel;

public final class ExpandPaddle {
    private ExpandPaddle() {}
    public static void activate(GamePanel gp) {
        gp.activateExpand(10_000); // 10s
    }
}
