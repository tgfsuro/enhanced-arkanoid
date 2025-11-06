package game;

import game.objects.Ball;
import game.objects.Brick;
import game.objects.Paddle;
import game.AssetLoader.Music;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.Clip;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;

/** Arkanoid 5 level; Main Menu có nền + nhạc riêng; Settings bật/tắt nhạc. */
public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {

    // ====== cấu hình ======
    private final int WIDTH, HEIGHT;
    private final Timer timer;

    // ====== objects ======
    private Paddle paddle;
    private Ball ball;
    private Brick[] bricks;
    private int rows, cols;
    private final int brickGap = 4, brickTop = 60;

    private boolean left, right;

    // ====== state/HUD ======
    private enum State { MENU, PLAY, PAUSE, SETTINGS, GAMEOVER, WIN }
    private State state = State.MENU;

    private int score = 0;
    private int lives = 3;
    private int levelIndex = 0;          // 0-based
    private final int TOTAL_LEVELS = 5;

    // ====== buttons (UI) ======
    private final Rectangle pauseBtn = new Rectangle();
    private final Rectangle homeBtn  = new Rectangle();
    private final Rectangle settingsBtn = new Rectangle();
    private final int btnW = 26, btnH = 26, btnPad = 10;
    private Image pauseIcon, homeIcon, gearIcon;

    // ====== backgrounds ======
    private Image levelBg;
    private float bgDim = 0.10f;

    // Main Menu background + music (đường dẫn tuyệt đối bạn đưa)
    private static final String MAIN_BG_ABS =
            "C:\\Users\\Admin\\Downloads\\basic-arkanoid\\src\\resources\\backgrounds\\mainbackground.jpg";
    private static final String MAIN_MUSIC_ABS =
            "C:\\Users\\Admin\\Downloads\\basic-arkanoid\\src\\resources\\sounds\\soundhall.mp3";
    private Image mainBg;  // nền của màn menu

    // ====== âm thanh ======
    private Music bgm;                 // nhạc hiện tại (menu hoặc level)
    private boolean musicEnabled = true;

    // Nhạc theo level (đường tuyệt đối bạn đang dùng)
    private static final String LEVEL_MUSIC_DIR =
            "C:\\Users\\Admin\\Downloads\\basic-arkanoid\\src\\resources\\sounds\\soundsoflevel";

    // fallback level layout
    private static final String[][] DEFAULT_LEVELS = {
            {"1010101010","0101010101","1111111111","0101010101","1010101010"},
            {"1111111111","1111111111","1111111111","0000000000","0000000000"},
            {"1110011110","1100000011","1001111001","1100000011","0111111110"},
            {"1111111111","1000000001","1011111101","1010000101","1111111111"},
            {"0001111000","0011111100","0111111110","0011111100","0001111000"}
    };

    // ====== Settings overlay controls ======
    private final Rectangle btnMusicToggle = new Rectangle();
    private final Rectangle btnMainMenu    = new Rectangle();
    private final Rectangle btnBack        = new Rectangle();

    public GamePanel(int w, int h) {
        this.WIDTH = w; this.HEIGHT = h;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        paddle = new Paddle(WIDTH/2.0 - 50, HEIGHT - 40, 100, 12, 6);
        ball   = new Ball(WIDTH/2.0, HEIGHT - 60, 8, 4, -4);

        try { pauseIcon  = AssetLoader.scaled("images/pause.png",  btnW, btnH); } catch (Exception ignored) {}
        try { homeIcon   = AssetLoader.scaled("images/home.png",   btnW, btnH); } catch (Exception ignored) {}
        try { gearIcon   = AssetLoader.scaled("images/gear.png",   btnW, btnH); } catch (Exception ignored) {}

        // load assets
        loadMainBackground();
        loadLevel(levelIndex);            // chuẩn bị level 1 (chưa phát nhạc level)

        // ở MENU ngay từ đầu -> phát nhạc menu
        playMenuMusic();

        timer = new Timer(1000/60, this);
        timer.start();
    }

    // ---------- Main Menu assets ----------
    private void loadMainBackground() {
        try {
            java.io.File f = new java.io.File(MAIN_BG_ABS);
            if (f.exists()) {
                mainBg = javax.imageio.ImageIO.read(f).getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
                return;
            }
        } catch (Exception ignored) {}
        // fallback classpath
        try { mainBg = AssetLoader.scaled("backgrounds/mainbackground.jpg", WIDTH, HEIGHT); }
        catch (Exception ignored) {}
    }

    private void playMenuMusic() {
        stopAnyMusic();
        if (!musicEnabled) return;
        // ưu tiên file tuyệt đối
        bgm = AssetLoader.loopMusicFromFile(MAIN_MUSIC_ABS);
        if (bgm != null) { System.out.println("[MENU BGM] ABS"); return; }
        // fallback classpath
        bgm = AssetLoader.loopMusicFromResource("sounds/soundhall.mp3");
        if (bgm != null) { System.out.println("[MENU BGM] CP"); }
    }

    // ---------------- Levels ----------------
    private void loadLevel(int index) {
        java.util.List<String> lines;
        String path = "levels/level" + (index + 1) + ".txt";
        try {
            lines = AssetLoader.readLines(path);
        } catch (Exception ex) {
            lines = java.util.Arrays.asList(DEFAULT_LEVELS[index % DEFAULT_LEVELS.length]);
        }

        rows = lines.size();
        cols = lines.get(0).length();

        int avail = WIDTH - 2 * 16;
        int bw = (avail - (cols - 1) * brickGap) / cols;
        int bh = 20;
        int startX = (WIDTH - (cols * bw + (cols - 1) * brickGap)) / 2;

        bricks = new Brick[rows * cols];
        int k = 0;
        for (int r = 0; r < rows; r++) {
            String row = lines.get(r);
            int y = brickTop + r * (bh + brickGap);
            for (int c = 0; c < cols; c++) {
                int x = startX + c * (bw + brickGap);
                bricks[k++] = (row.charAt(c) == '1') ? new Brick(x, y, bw, bh) : null;
            }
        }

        loadBackgroundFor(index);
    }

    private void loadBackgroundFor(int index) {
        levelBg = null;
        int n = index + 1;
        String[] candidates = {
                "backgrounds/level" + n + ".png",
                "backgrounds/level" + n + ".jpg",
                "backgrounds/level" + n + ".jpeg",
                "backgrounds/Map"   + n + ".png",
                "backgrounds/Map"   + n + ".jpg",
                "backgrounds/Map"   + n + ".jpeg"
        };

        ClassLoader cl = getClass().getClassLoader();
        for (String p : candidates) {
            java.net.URL url = cl.getResource(p);
            if (url != null) {
                try { levelBg = AssetLoader.scaled(p, WIDTH, HEIGHT); return; }
                catch (Exception ignored) {}
            }
        }
        // Fallback: đọc từ FS
        String[] roots = { "src/resources/", "resources/", "src/main/resources/" };
        for (String root : roots) {
            for (String p : candidates) {
                java.io.File f = new java.io.File(root + p);
                if (f.exists()) {
                    try {
                        levelBg = javax.imageio.ImageIO.read(f).getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
                        return;
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // ---------- Music control ----------
    private void stopAnyMusic() { if (bgm != null) { bgm.stop(); bgm = null; } }

    private void restartLevelMusic(int index) {
        stopAnyMusic();
        if (!musicEnabled) return;

        int n = index + 1;
        String absMp3 = LEVEL_MUSIC_DIR + "\\music" + n + ".mp3";

        // 1) ưu tiên file tuyệt đối MP3/WAV (Clip nếu có mp3spi; fallback JLayer)
        bgm = AssetLoader.loopMusicFromFile(absMp3);
        if (bgm != null) { System.out.println("[BGM use ABS] " + absMp3); return; }

        // 2) classpath MP3
        String cpMp3 = "sounds/soundsoflevel/music" + n + ".mp3";
        bgm = AssetLoader.loopMusicFromResource(cpMp3);
        if (bgm != null) { System.out.println("[BGM use CP] " + cpMp3); return; }

        // 3) classpath WAV/AIFF/AU
        String[] pcm = {
                "sounds/soundsoflevel/music" + n + ".wav",
                "sounds/soundsoflevel/music" + n + ".aiff",
                "sounds/soundsoflevel/music" + n + ".au"
        };
        for (String p : pcm) {
            bgm = AssetLoader.loopMusicFromResource(p);
            if (bgm != null) { System.out.println("[BGM use CP] " + p); return; }
        }

        System.out.println("[BGM] Không phát được nhạc level " + n);
    }

    // ---------------- Loop ----------------
    @Override public void actionPerformed(ActionEvent e) {
        if (state == State.PLAY) updateGame();
        repaint();
    }

    private void updateGame() {
        if (left)  paddle.move(-1, WIDTH);
        if (right) paddle.move( 1, WIDTH);

        ball.update();

        if (ball.x - ball.r < 0)      { ball.x = ball.r;           ball.vx = -ball.vx; }
        if (ball.x + ball.r > WIDTH)  { ball.x = WIDTH - ball.r;   ball.vx = -ball.vx; }
        if (ball.y - ball.r < 0)      { ball.y = ball.r;           ball.vy = -ball.vy; }

        if (ball.y - ball.r > HEIGHT) {
            lives--;
            if (lives <= 0) { state = State.GAMEOVER; stopAnyMusic(); }
            else { resetBallAndPaddle(); state = State.PAUSE; }
            return;
        }

        if (ball.getRect().intersects(paddle.getRect()) && ball.vy > 0) {
            ball.y = paddle.y - ball.r - 1;
            ball.vy = -ball.vy;
            if (left && !right)  ball.vx -= 1;
            if (right && !left)  ball.vx += 1;
        }

        Rectangle br = ball.getRect();
        for (int i = 0; i < bricks.length; i++) {
            Brick b = bricks[i];
            if (b == null) continue;
            Rectangle r = b.getRect();
            if (!br.intersects(r)) continue;

            int leftO   = (int)(br.x + br.width) - r.x;
            int rightO  = (int)(r.x + r.width) - br.x;
            int topO    = (int)(br.y + br.height) - r.y;
            int bottomO = (int)(r.y + r.height) - br.y;
            int minX = Math.min(leftO, rightO);
            int minY = Math.min(topO, bottomO);
            if (minX < minY) ball.vx = -ball.vx; else ball.vy = -ball.vy;

            bricks[i] = null;
            score += 10;
            AssetLoader.loopMusicFromResource("sounds/hit.wav"); // nếu có âm va chạm WAV; bỏ nếu không cần
            break;
        }

        if (noBricksLeft()) state = State.WIN;
    }

    private boolean noBricksLeft() { for (Brick b : bricks) if (b != null) return false; return true; }

    private void resetBallAndPaddle() {
        paddle.x = WIDTH/2.0 - paddle.w/2.0;
        ball.x = WIDTH/2.0; ball.y = HEIGHT - 60;
        ball.vx = 4; ball.vy = -4;
    }

    // ---------------- Render ----------------
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == State.MENU && mainBg != null) {
            g2.drawImage(mainBg, 0, 0, null);
        } else if (levelBg != null) {
            g2.drawImage(levelBg, 0, 0, null);
            if (bgDim > 0f) {
                g2.setColor(new Color(0,0,0, Math.min(255, (int)(bgDim*255))));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        } else {
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(0,0,new Color(10,10,20),0,getHeight(),Color.BLACK));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.setPaint(old);
        }

        if (state != State.MENU) {
            for (Brick b : bricks) if (b != null) b.draw(g2);
            paddle.draw(g2);
            ball.draw(g2);

            // HUD
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("Score: " + score, 12, 20);
            g2.drawString("Lives: " + "❤".repeat(Math.max(0, lives)), 100, 20);
            g2.drawString("Level: " + (levelIndex+1) + "/5", 200, 20);

            // buttons
            int px = getWidth() - btnW - btnPad; int py = 8;
            if (pauseIcon != null) g2.drawImage(pauseIcon, px, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); g2.fillRect(px+4,py+3,6,20); g2.fillRect(px+16,py+3,6,20); }
            pauseBtn.setBounds(px, py, btnW, btnH);

            int hx = px - btnW - 8;
            if (homeIcon != null) g2.drawImage(homeIcon, hx, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); int[] xs={hx+3,hx+13,hx+23,hx+23,hx+3}; int[] ys={py+14,py+4,py+14,py+24,py+24}; g2.fillPolygon(xs,ys,5); }
            homeBtn.setBounds(hx, py, btnW, btnH);

            int sx = hx - btnW - 8;
            if (gearIcon != null) g2.drawImage(gearIcon, sx, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); g2.drawOval(sx+4,py+4,18,18); }
            settingsBtn.setBounds(sx, py, btnW, btnH);
        }

        drawOverlay(g2);
    }

    private void drawOverlay(Graphics2D g2) {
        g2.setColor(new Color(0,0,0, state==State.MENU ? 90 : 120));
        if (state != State.PLAY) g2.fillRect(0,0,getWidth(),getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));

        String title = switch (state) {
            case MENU     -> "Press SPACE to Start";
            case PAUSE    -> "PAUSED - SPACE to Resume";
            case SETTINGS -> "SETTINGS";
            case GAMEOVER -> "GAME OVER - Press R to Retry";
            case WIN      -> (levelIndex+1 < TOTAL_LEVELS) ? "LEVEL CLEARED - Press N for Next"
                    : "ALL LEVELS CLEARED - Press R to Restart";
            default -> "";
        };
        int tw = g2.getFontMetrics().stringWidth(title);
        if (state != State.PLAY) g2.drawString(title, (getWidth()-tw)/2, getHeight()/3);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        if (state == State.MENU) {
            g2.drawString("SPACE: Start   ⚙ Settings   ⌂ Main Menu", 20, getHeight()-20);
        } else if (state != State.PLAY) {
            g2.drawString("←/→: move paddle", 20, getHeight()-40);
            g2.drawString("Click ⚙ for Settings, ⌂ for Home, || for Pause", 20, getHeight()-20);
        }

        if (state == State.SETTINGS) {
            int bw = 240, bh = 36, gap = 14;
            int cx = (getWidth()-bw)/2; int cy = getHeight()/2;

            btnMusicToggle.setBounds(cx, cy, bw, bh);
            drawBtn(g2, btnMusicToggle, (musicEnabled ? "Music: ON (pause/resume)" : "Music: OFF"));

            btnMainMenu.setBounds(cx, cy + bh + gap, bw, bh);
            drawBtn(g2, btnMainMenu, "Return to MAIN MENU");

            btnBack.setBounds(cx, cy + 2*(bh+gap), bw, bh);
            drawBtn(g2, btnBack, "Back");
        }
    }

    private void drawBtn(Graphics2D g2, Rectangle r, String text) {
        g2.setColor(new Color(255,255,255,30));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        int tw = g2.getFontMetrics().stringWidth(text);
        int th = g2.getFontMetrics().getAscent();
        g2.drawString(text, r.x + (r.width - tw)/2, r.y + (r.height + th)/2 - 3);
    }

    // ---------------- Input ----------------
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  left = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (state == State.MENU) {
                state = State.PLAY;
                // chuyển từ menu sang play: đổi nhạc
                restartLevelMusic(levelIndex);
            } else if (state == State.PLAY) {
                state = State.PAUSE;
                if (bgm != null) bgm.pause();
            } else if (state == State.PAUSE) {
                state = State.PLAY;
                if (bgm != null) bgm.resume(); else restartLevelMusic(levelIndex);
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_N && state == State.WIN) {
            if (levelIndex + 1 < TOTAL_LEVELS) {
                levelIndex++;
                loadLevel(levelIndex);
                resetBallAndPaddle();
                state = State.PLAY;
                restartLevelMusic(levelIndex);
            } else {
                state = State.MENU;
                resetForMenu();
                playMenuMusic();
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_R && (state == State.GAMEOVER || state == State.WIN)) {
            resetForMenu(); state = State.PLAY; restartLevelMusic(levelIndex);
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && state == State.SETTINGS) {
            state = State.PAUSE;
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  left = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = false;
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();

        if (settingsBtn.contains(p) && state != State.MENU) {
            state = State.SETTINGS; repaint(); return;
        }
        if (pauseBtn.contains(p) && state != State.MENU) {
            if (state == State.PLAY) { state = State.PAUSE; if (bgm != null) bgm.pause(); }
            else if (state == State.PAUSE) { state = State.PLAY; if (bgm != null) bgm.resume(); else restartLevelMusic(levelIndex); }
            repaint(); return;
        }
        if (homeBtn.contains(p)) {
            state = State.MENU;
            resetForMenu();
            playMenuMusic();
            repaint(); return;
        }

        if (state == State.SETTINGS) {
            if (btnMusicToggle.contains(p)) {
                musicEnabled = !musicEnabled;
                if (!musicEnabled) { if (bgm != null) bgm.pause(); }
                else {
                    if (state == State.MENU) {
                        if (bgm != null) bgm.resume(); else playMenuMusic();
                    } else {
                        if (bgm != null) bgm.resume(); else restartLevelMusic(levelIndex);
                    }
                }
                repaint(); return;
            } else if (btnMainMenu.contains(p)) {
                state = State.MENU;
                resetForMenu();
                playMenuMusic();
                repaint(); return;
            } else if (btnBack.contains(p)) {
                state = State.PAUSE; repaint(); return;
            }
        }
    }

    private void resetForMenu() {
        resetBallAndPaddle();
        score = 0; lives = 3; levelIndex = 0;
        loadLevel(levelIndex);
        // không phát nhạc level; menu music sẽ được play ở nơi gọi
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
