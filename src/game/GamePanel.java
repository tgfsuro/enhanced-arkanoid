package game;

import game.objects.Ball;
import game.objects.Brick;
import game.objects.Paddle;

import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;

/** Arkanoid với 5 level; mỗi level có ảnh nền riêng. */
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
    private enum State { MENU, PLAY, PAUSE, GAMEOVER, WIN }
    private State state = State.MENU;

    private int score = 0;
    private int lives = 3;
    private int levelIndex = 0;          // 0-based
    private final int TOTAL_LEVELS = 5;

    // ====== buttons ======
    private final Rectangle pauseBtn = new Rectangle();
    private final Rectangle homeBtn  = new Rectangle();
    private final int btnW = 26, btnH = 26, btnPad = 10;
    private Image pauseIcon, homeIcon;

    // ====== background ======
    private Image levelBg;
    private float bgDim = 0.10f;         // dim overlay 0..1

    // ====== âm thanh ======
    private Clip bgm;

    // fallback nếu thiếu file level
    private static final String[][] DEFAULT_LEVELS = {
            {"1010101010","0101010101","1111111111","0101010101","1010101010"},
            {"1111111111","1111111111","1111111111","0000000000","0000000000"},
            {"1110011110","1100000011","1001111001","1100000011","0111111110"},
            {"1111111111","1000000001","1011111101","1010000101","1111111111"},
            {"0001111000","0011111100","0111111110","0011111100","0001111000"}
    };

    public GamePanel(int w, int h) {
        this.WIDTH = w; this.HEIGHT = h;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        paddle = new Paddle(WIDTH/2.0 - 50, HEIGHT - 40, 100, 12, 6);
        ball   = new Ball(WIDTH/2.0, HEIGHT - 60, 8, 4, -4);

        try { pauseIcon = AssetLoader.scaled("images/pause.png", btnW, btnH); } catch (Exception ignored) {}
        try { homeIcon  = AssetLoader.scaled("images/home.png",  btnW, btnH); } catch (Exception ignored) {}
        try { bgm = AssetLoader.loop("sounds/music.wav"); } catch (Exception ignored) {}

        loadLevel(levelIndex);

        timer = new Timer(1000/60, this);
        timer.start();
    }

    // ---------------- Levels ----------------
    private void loadLevel(int index) {
        List<String> lines;
        String path = "levels/level" + (index + 1) + ".txt";
        try {
            lines = AssetLoader.readLines(path);
        } catch (Exception ex) {
            lines = Arrays.asList(DEFAULT_LEVELS[index % DEFAULT_LEVELS.length]);
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
            if (row.length() != cols) throw new IllegalStateException("All lines in level must have same length");
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
                "backgrounds/Map"   + n + ".png",   // tên bạn đang dùng
                "backgrounds/Map"   + n + ".jpg",
                "backgrounds/Map"   + n + ".jpeg"
        };
        for (String p : candidates) {
            try {
                Image img = AssetLoader.scaled(p, WIDTH, HEIGHT);
                if (img != null) { levelBg = img; return; }
            } catch (Exception ignored) {}
        }
    }

    private boolean noBricksLeft() {
        for (Brick b : bricks) if (b != null) return false;
        return true;
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
            if (lives <= 0) { state = State.GAMEOVER; pauseBgm(); }
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
            AssetLoader.playOnce("sounds/hit.wav");
            break;
        }

        if (noBricksLeft()) state = State.WIN;
    }

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

        // background
        if (levelBg != null) {
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

        for (Brick b : bricks) if (b != null) b.draw(g2);
        paddle.draw(g2);
        ball.draw(g2);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString("Score: " + score, 12, 20);
        g2.drawString("Lives: " + "❤".repeat(Math.max(0, lives)), 100, 20);
        g2.drawString("Level: " + (levelIndex+1) + "/" + TOTAL_LEVELS, 200, 20);

        // buttons
        int px = getWidth() - btnW - btnPad, py = 8;
        if (pauseIcon != null) g2.drawImage(pauseIcon, px, py, null);
        else { g2.setColor(Color.LIGHT_GRAY); g2.fillRect(px+4,py+3,6,20); g2.fillRect(px+16,py+3,6,20); }
        pauseBtn.setBounds(px, py, btnW, btnH);

        int hx = px - btnW - 8;
        if (homeIcon != null) g2.drawImage(homeIcon, hx, py, null);
        else {
            g2.setColor(Color.LIGHT_GRAY);
            int[] xs = {hx+3,hx+13,hx+23,hx+23,hx+3};
            int[] ys = {py+14,py+4, py+14, py+24, py+24};
            g2.fillPolygon(xs, ys, xs.length);
        }
        homeBtn.setBounds(hx, py, btnW, btnH);

        if (state != State.PLAY) drawOverlay(g2);
    }

    private void drawOverlay(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,120));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        String title = switch (state) {
            case MENU     -> "Press SPACE to Start";
            case PAUSE    -> "PAUSED - SPACE to Resume";
            case GAMEOVER -> "GAME OVER - Press R to Retry";
            case WIN      -> (levelIndex+1 < TOTAL_LEVELS) ? "LEVEL CLEARED - Press N for Next"
                    : "ALL LEVELS CLEARED - Press R to Restart";
            default -> "";
        };
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (getWidth()-tw)/2, getHeight()/3);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString("←/→: move paddle", 20, getHeight()-40);
        g2.drawString("Click  ⌂  for Home,  ||  for Pause", 20, getHeight()-20);
    }

    // ---------------- Input ----------------
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  left = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (state == State.MENU) { state = State.PLAY; resumeBgm(); }
            else if (state == State.PLAY) { state = State.PAUSE; pauseBgm(); }
            else if (state == State.PAUSE) { state = State.PLAY; resumeBgm(); }
        }

        if (e.getKeyCode() == KeyEvent.VK_N && state == State.WIN) {
            if (levelIndex + 1 < TOTAL_LEVELS) {
                levelIndex++;
                loadLevel(levelIndex);
                resetBallAndPaddle();
                state = State.PLAY; resumeBgm();
            } else {
                resetForMenu(); state = State.MENU;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_R && (state == State.GAMEOVER || state == State.WIN)) {
            resetForMenu(); state = State.PLAY; resumeBgm();
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  left = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = false;
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (pauseBtn.contains(p)) {
            if (state == State.PLAY) { state = State.PAUSE; pauseBgm(); }
            else { state = State.PLAY; resumeBgm(); }
            AssetLoader.playOnce("sounds/hit.wav");
        } else if (homeBtn.contains(p)) {
            state = State.MENU; resetForMenu(); pauseBgm();
            AssetLoader.playOnce("sounds/hit.wav");
        }
        repaint();
    }

    private void resetForMenu() {
        resetBallAndPaddle();
        score = 0; lives = 3; levelIndex = 0;
        loadLevel(levelIndex);
    }

    private void pauseBgm()  { if (bgm != null && bgm.isRunning()) bgm.stop(); }
    private void resumeBgm() { if (bgm != null && !bgm.isRunning()) bgm.loop(Clip.LOOP_CONTINUOUSLY); }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
