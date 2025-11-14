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

    public LevelManager(int w, int h) {
        this.WIDTH = w;
        this.HEIGHT = h;
    }

    /** Tải layout & gán power-up theo ký tự E/B/L(Z)/G/H, nếu map không có thì rải auto (không dồn 1 cột). */
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

        // độ khó tăng dần
        double hardRate  = 0.10 + levelIndex * 0.05; // 10% → 30%
        double unbRate   = 0.05 + levelIndex * 0.03; // 5%  → 17%
        hardRate = Math.min(0.35, hardRate);
        unbRate  = Math.min(0.20, unbRate);

        // - Nếu map có ký tự E/B/L/Z/G/H → dùng đúng ký tự.
        // - Nếu không có, dùng công thức hash theo (r, c) để rải đều (hạn chế dồn 1 cột).
        for (int r = 0; r < rows; r++) {
            String row = lines.get(r);
            int y = top + r * (bh + gap);

            for (int c = 0; c < cols; c++) {
                int x = startX + c * (bw + gap);
                char ch = row.charAt(c);
                if (ch == '0' || ch == ' ') { bricks[idx++] = null; continue; }

                // 1) power-up do map chỉ định (ưu tiên)
                Pickup.Type puFromMap = null;
                switch (ch) {
                    case 'E' -> puFromMap = Pickup.Type.EXPAND;
                    case 'B' -> puFromMap = Pickup.Type.BONUS_BALLS;
                    case 'L', 'Z' -> puFromMap = Pickup.Type.LAZER; // L/Z đều là laser
                    case 'G' -> puFromMap = Pickup.Type.GUN;
                    case 'H' -> puFromMap = Pickup.Type.HEART;      // Heart từ map
                    default  -> { /* ký tự khác: không phải power-up */ }
                }

                // 2) nếu map không chỉ định thì rải auto
                Pickup.Type pu = puFromMap;
                if (pu == null) {
                    // (31*r + 17*c + 13*levelIndex) % 9 == 0  → xác suất ~1/9
                    int sel = (31 * r + 17 * c + 13 * levelIndex) % 9;
                    if (sel == 0) {
                        int t = (r * 3 + c * 5 + levelIndex) % 5;
                        pu = switch (t) {
                            case 0 -> Pickup.Type.EXPAND;
                            case 1 -> Pickup.Type.BONUS_BALLS;
                            case 2 -> Pickup.Type.LAZER;
                            case 3 -> Pickup.Type.GUN;
                            default -> Pickup.Type.HEART; // thêm Heart vào rải auto
                        };
                    }
                }

                // 3) tạo brick (ưu tiên bất tử > cứng > thường)
                double rnd = Math.random();
                if (rnd < unbRate) {
                    bricks[idx++] = new UnbreakableBrick(x, y, bw, bh);
                } else if (rnd < unbRate + hardRate) {
                    bricks[idx++] = new HardBrick(x, y, bw, bh, pu);
                } else {
                    Color col = new Color(120, 170, 255);
                    bricks[idx++] = new Brick(x, y, bw, bh, false, col, pu, 1);
                }
            }
        }

        // nền
        background = tryBackgroundForLevel(levelIndex);
    }

    /** Level đã dọn sạch (chấp nhận còn gạch bất tử). */
    public boolean cleared() {
        for (Brick b : bricks) {
            if (b != null && !b.isUnbreakable() && !b.isDestroyed()) return false;
        }
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
            try {
                Image img = AssetLoader.scaled(p, WIDTH, HEIGHT);
                if (img != null) return img;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
