package game.powerup;

import game.GamePanel;

public final class Lazer {
    private Lazer() {}
    public static void activate(GamePanel gp) {
        gp.fireLaser();
    }
}
