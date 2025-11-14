package game.powerup;

import game.GamePanel;

/** Power-up: thêm mạng (1UP). */
public final class Heart {
    private Heart() {}

    /** Tăng thêm 1 mạng. Có giới hạn trên do GamePanel quyết định. */
    public static void activate(GamePanel gp) {
        gp.addLife(1);
    }
}
