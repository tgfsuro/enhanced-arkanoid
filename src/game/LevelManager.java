package game;

import game.objects.Brick;
import game.objects.HardBrick;
import game.objects.UnbreakableBrick;
import game.play.Pickup;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Quản lý layout level + nền + tạo mảng gạch. */
public class LevelManager {

    private final int WIDTH, HEIGHT;
    public Brick[] bricks;
    private Image background;

    // khoảng cách/căn lề
    private final int gap = 4;
    private final int top = 60;

    public LevelManager(int w, int h) { this.WIDTH = w; this.HEIGHT = h; }

    public void load(int levelIndex) {
        // 1) đọc layout từ resources/levels/levelN.txt (nếu có), fallback sang mặc định
        List<String> lines;
        String path = "levels/level" + (levelIndex + 1) + ".txt";
        try {
            lines = AssetLoader.readLines(path);
        } catch (Exception ex) {
            lines = defaultLayout(levelIndex); // fallback
        }

        int rows = lines.size();
        int cols = lines.get(0).length();

        int avail = WIDTH - 2 * 16;
        int bw = (avail - (cols - 1) * gap) / cols;
        int bh = 20;
        int startX = (WIDTH - (cols * bw + (cols - 1) * gap)) / 2;

        bricks = new Brick[rows * cols];
        int idx = 0;

        // thiết lập độ khó tăng dần: tỉ lệ gạch cứng & bất tử tăng theo level
        double hardRate  = 0.10 + levelIndex * 0.05; // 10% → 30%
        double unbRate   = 0.05 + levelIndex * 0.03; // 5%  → 17%
        hardRate = Math.min(0.35, hardRate);
        unbRate  = Math.min(0.20, unbRate);

        for (int r = 0; r < rows; r++) {
            String row = lines.get(r);
            int y = top + r * (bh + gap);

            for (int c = 0; c < cols; c++) {
                int x = startX + c * (bw + gap);
                char ch = row.charAt(c);
                if (ch == '0') { bricks[idx++] = null; continue; }

                // powerup logo gán vào 1 số gạch: cứ 5 ô đặt 1 lần (đơn giản)
                Pickup.Type pu = null;
                if ((r * cols + c + levelIndex) % 5 == 0) {
                    int pick = (r + c + levelIndex) % 4;
                    pu = switch (pick) {
                        case 0 -> Pickup.Type.EXPAND;
                        case 1 -> Pickup.Type.BONUS_BALLS;
                        case 2 -> Pickup.Type.LAZER;
                        default -> Pickup.Type.GUN;
                    };
                }

                // ưu tiên bất tử > cứng > thường
                double rnd = Math.random();
                if (rnd < unbRate) {
                    bricks[idx++] = new UnbreakableBrick(x, y, bw, bh);
                } else if (rnd < unbRate + hardRate) {
                    bricks[idx++] = new HardBrick(x, y, bw, bh, pu);
                } else {
                    // gạch thường (hp=1), màu nhẹ; có thể chứa powerup
                    Color col = new Color(120, 170, 255);
                    bricks[idx++] = new Brick(x, y, bw, bh, false, col, pu, 1);
                }
            }
        }

        // nền: backgrounds/MapN.(png|jpg|jpeg)
        background = tryBackgroundForLevel(levelIndex);
    }

    public boolean cleared() {
        for (Brick b : bricks) if (b != null && !b.isUnbreakable() && !b.isDestroyed()) return false;
        // chấp nhận còn gạch bất tử
        for (Brick b : bricks) if (b != null && !b.isUnbreakable()) return false;
        return true;
    }

    public Image background() { return background; }

    // ---------- helpers ----------
    private List<String> defaultLayout(int levelIndex) {
        // 5 layout mặc định, khó dần
        List<List<String>> presets = List.of(
                Arrays.asList("1010101010","0101010101","1111111111","0101010101","1010101010"),
                Arrays.asList("1111111111","1111111111","1111111111","0011111100","0000000000"),
                Arrays.asList("1110011110","1100000011","1001111001","1100000011","0111111110"),
                Arrays.asList("1111111111","1000000001","1011111101","1010000101","1111111111"),
                Arrays.asList("0001111000","0011111100","0111111110","0011111100","0001111000")
        );
        return new ArrayList<>(presets.get(levelIndex % presets.size()));
    }

    private Image tryBackgroundForLevel(int index) {
        String[] cands = {
                "backgrounds/Map" + (index+1) + ".png",
                "backgrounds/Map" + (index+1) + ".jpg",
                "backgrounds/Map" + (index+1) + ".jpeg",
                "backgrounds/level" + (index+1) + ".png",
                "backgrounds/level" + (index+1) + ".jpg",
                "backgrounds/level" + (index+1) + ".jpeg",
        };
        for (String p : cands) {
            try { Image img = AssetLoader.scaled(p, WIDTH, HEIGHT); if (img != null) return img; } catch (Exception ignored) {}
        }
        return null;
    }
}
