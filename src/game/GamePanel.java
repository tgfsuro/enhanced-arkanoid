package game;

import game.objects.Ball;
import game.objects.Brick;
import game.objects.Paddle;
import game.play.DropManager;
import game.play.Pickup;
import game.play.ProjectileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {

    private final int WIDTH, HEIGHT;
    private final Timer timer = new Timer(1000 / 60, this);

    private enum State { MENU, PLAY, PAUSE, SETTINGS, GAMEOVER, WIN }
    private State state = State.MENU;

    // === KÍCH THƯỚC PADDLE MỚI (nhỏ hơn) ===
    private static final int PADDLE_W_DEFAULT = 96 ;
    private static final int PADDLE_H_DEFAULT = 12;

    private final LevelManager levels;
    private final MusicHub music = new MusicHub();
    private final DropManager dropMgr = new DropManager();
    private final ProjectileManager projMgr = new ProjectileManager();

    private final List<Ball> balls = new ArrayList<>();
    private final Paddle paddle;

    // serving sway
    private boolean serving = true;
    private double serveOffset = 0;
    private int serveDir = +1;
    private double serveSpeed = 1.8;

    // HUD/state
    private int score = 0, lives = 3, levelIndex = 0;
    private final int TOTAL_LEVELS = 5;

    // buttons
    private final Rectangle pauseBtn = new Rectangle();
    private final Rectangle homeBtn  = new Rectangle();
    private final Rectangle settingsBtn = new Rectangle();
    private final int btnW = 26, btnH = 26, btnPad = 10;
    private Image pauseIcon, homeIcon, gearIcon, mainBg;

    // settings overlay
    private final Rectangle btnMusicToggle = new Rectangle();
    private final Rectangle btnMainMenu    = new Rectangle();
    private final Rectangle btnBack        = new Rectangle();

    // expand timer
    private long expandUntil = 0;
    private Integer paddleOrigW = null;

    // input
    private boolean left, right;

    // ==== wrappers cho powerup ====
    public void enableGun(int ms) { projMgr.enableGun(ms); }                  // gun: bắn từ 2 mép
    public void fireLaser() { projMgr.fireLaser(paddleCenterX(), levels, dropMgr, this::addScore); } // laser: giữa
    public void addLife(int d) { lives = Math.min(9, lives + Math.max(0, d)); }

    public GamePanel(int w, int h) {
        this.WIDTH = w; this.HEIGHT = h;

        setPreferredSize(new Dimension(w, h));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        levels = new LevelManager(w, h);

        paddle = new Paddle(
                WIDTH / 2.0 - PADDLE_W_DEFAULT / 2.0,
                HEIGHT - 60,
                PADDLE_W_DEFAULT,
                PADDLE_H_DEFAULT,
                5.8
        );
        // dùng skin đang lưu trong thư mục của bạn
        paddle.setSkinPath("src/game/mainhall/paddle/paddle1.png");

        balls.add(new Ball(WIDTH / 2.0, HEIGHT - 60, 8, 0, 0));

        try { pauseIcon  = AssetLoader.scaled("images/pause.png",  btnW, btnH); } catch (Exception ignored) {}
        try { homeIcon   = AssetLoader.scaled("images/home.png",   btnW, btnH); } catch (Exception ignored) {}
        try { gearIcon   = AssetLoader.scaled("images/gear.png",   btnW, btnH); } catch (Exception ignored) {}
        try { mainBg     = AssetLoader.scaled("backgrounds/mainbackground.jpg", w, h); } catch (Exception ignored) {}

        levels.load(levelIndex);
        music.playMenu();
        timer.start();
    }

    public void prepareLevel1FromHall() {
        music.stop();
        levelIndex = 0;
        levels.load(levelIndex);
        resetBallPaddle();
        serving = true;
        state = State.PLAY;
        music.playLevel(levelIndex);
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (state == State.PLAY) update();
        repaint();
    }

    private void update() {
        if (left)  paddle.move(-1, WIDTH);
        if (right) paddle.move( 1, WIDTH);

        if (serving) {
            double maxOffset = paddle.w / 2.0 - 8;
            serveOffset += serveDir * serveSpeed;
            if (serveOffset >  maxOffset) { serveOffset =  maxOffset; serveDir = -1; }
            if (serveOffset < -maxOffset) { serveOffset = -maxOffset; serveDir = +1; }

            Ball b = balls.get(0);
            b.x = paddle.x + paddle.w / 2.0 + serveOffset;
            b.y = paddle.y - b.r - 1;

            tickExpandTimer();
            dropMgr.update(paddle, HEIGHT, this::applyDrop);
            projMgr.update(System.currentTimeMillis(), paddle, HEIGHT, levels, dropMgr, this::addScore);
            return;
        }

        for (int i = 0; i < balls.size(); i++) {
            Ball b = balls.get(i);
            b.update();
            if (b.x - b.r < 0)      { b.x = b.r;         b.vx = -b.vx; }
            if (b.x + b.r > WIDTH)  { b.x = WIDTH-b.r;   b.vx = -b.vx; }
            if (b.y - b.r < 0)      { b.y = b.r;         b.vy = -b.vy; }
            if (b.y - b.r > HEIGHT) { balls.remove(i--); continue; }

            if (b.getRect().intersects(paddle.getRect()) && b.vy > 0) {
                b.y = paddle.y - b.r - 1;
                double center = paddle.x + paddle.w / 2.0;
                double t = (b.x - center) / (paddle.w / 2.0);
                t = Math.max(-1, Math.min(1, t));
                double speed = 6.0;
                b.vx = t * 5.0;
                b.vy = -Math.sqrt(Math.max(1, speed*speed - b.vx*b.vx));
            }

            Rectangle br = b.getRect();
            for (int j = 0; j < levels.bricks.length; j++) {
                Brick brick = levels.bricks[j];
                if (brick == null) continue;
                Rectangle r = brick.getRect();
                if (!br.intersects(r)) continue;

                int leftO   = (int)(br.x + br.width) - r.x;
                int rightO  = (int)(r.x + r.width) - br.x;
                int topO    = (int)(br.y + br.height) - r.y;
                int bottomO = (int)(r.y + r.height) - br.y;
                if (Math.min(leftO, rightO) < Math.min(topO, bottomO)) b.vx = -b.vx; else b.vy = -b.vy;

                boolean broken = brick.onHit();
                if (broken) {
                    if (brick.getPickup() != null) {
                        dropMgr.spawn(brick.getPickup(), brick.x + brick.w/2.0, brick.y + brick.h/2.0);
                    }
                    levels.bricks[j] = null;
                    addScore(10);
                } else addScore(2);
                break;
            }
        }

        if (balls.isEmpty()) {
            lives--;
            if (lives <= 0) { state = State.GAMEOVER; music.stop(); return; }
            enterServingMode();
        }

        tickExpandTimer();
        dropMgr.update(paddle, HEIGHT, this::applyDrop);
        projMgr.update(System.currentTimeMillis(), paddle, HEIGHT, levels, dropMgr, this::addScore);

        if (levels.cleared()) {
            if (levelIndex + 1 < TOTAL_LEVELS) {
                levelIndex++;
                levels.load(levelIndex);
                resetBallPaddle();
                enterServingMode();
                music.playLevel(levelIndex);
            } else { state = State.WIN; music.stop(); }
        }
    }

    private void tickExpandTimer(){
        long now = System.currentTimeMillis();
        if (expandUntil > 0 && now >= expandUntil) {
            if (paddleOrigW != null) paddle.w = paddleOrigW;
            expandUntil = 0; paddleOrigW = null;
        }
    }

    private void enterServingMode() {
        balls.clear();
        balls.add(new Ball(paddle.x + paddle.w/2.0, paddle.y - 9, 8, 0, 0));
        serving = true; serveOffset = 0; serveDir = +1;
    }

    private void addScore(int d){ score += d; }
    private int paddleCenterX() { return (int)(paddle.x + paddle.w / 2.0); }

    private void applyDrop(Pickup.Type t) {
        switch (t) {
            case EXPAND       -> activateExpand(10_000);
            case BONUS_BALLS  -> spawnBonusBalls();
            case LAZER        -> fireLaser();
            case GUN          -> enableGun(5_000);
            case HEART        -> addLife(1);
        }
    }

    public void activateExpand(long ms) {
        if (paddleOrigW == null) paddleOrigW = paddle.w;
        paddle.w = (int)Math.round(paddle.w * 1.5);   // thu bớt hệ số nở
        if (paddle.x + paddle.w > WIDTH - 10) paddle.x = WIDTH - 10 - paddle.w;
        expandUntil = System.currentTimeMillis() + ms;
    }

    /** x3 mỗi bóng hiện có (bonus chuỗi). */
    public void spawnBonusBalls() {
        if (serving || balls.isEmpty()) return;
        int n = balls.size();
        List<Ball> extra = new ArrayList<>(n * 2);
        for (int i = 0; i < n; i++) {
            Ball ref = balls.get(i);
            extra.add(new Ball(ref.x, ref.y, ref.r, ref.vx - 1.8, ref.vy - 0.6));
            extra.add(new Ball(ref.x, ref.y, ref.r, ref.vx + 1.8, ref.vy - 0.6));
        }
        balls.addAll(extra);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == State.MENU && mainBg != null) g2.drawImage(mainBg, 0, 0, null);
        else if (levels.background() != null)      g2.drawImage(levels.background(), 0, 0, null);
        else {
            g2.setPaint(new GradientPaint(0,0,new Color(10,10,20), 0,getHeight(), Color.BLACK));
            g2.fillRect(0,0,getWidth(),getHeight());
        }

        if (state != State.MENU) {
            for (Brick b : levels.bricks) if (b != null) b.draw(g2);
            paddle.draw(g2);
            for (Ball b : balls) b.draw(g2);

            dropMgr.render(g2);
            projMgr.render(g2, paddle);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("Score: " + score, 12, 20);
            g2.drawString("Lives: " + Math.max(0, lives), 120, 20);
            g2.drawString("Level: " + (levelIndex + 1) + "/5", 200, 20);

            int px = getWidth() - btnW - btnPad, py = 8;
            if (pauseIcon != null) g2.drawImage(pauseIcon, px, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); g2.fillRect(px+4,py+3,6,20); g2.fillRect(px+16,py+3,6,20); }
            pauseBtn.setBounds(px, py, btnW, btnH);

            int hx = px - btnW - 8;
            if (homeIcon != null) g2.drawImage(homeIcon, hx, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); int[] xs={hx+3,hx+13,hx+23,hx+23,hx+3}; int[] ys={py+14,py+4,py+14,py+24,py+24}; g2.fillPolygon(xs,ys,5); }
            homeBtn.setBounds(hx, py, btnW, btnH);

            int sx = hx - btnW - 8;
            if (gearIcon != null) g2.drawImage(gearIcon, sx, py, null);
            else { g2.setColor(Color.LIGHT_GRAY); g2.drawOval(sx+4,py+4,18,18); g2.drawLine(sx+13,py+4,sx+13,py+22); g2.drawLine(sx+4,py+13,sx+22,py+13); }
            settingsBtn.setBounds(sx, py, btnW, btnH);
        }

        drawOverlay(g2);
    }

    private void drawOverlay(Graphics2D g2) {
        boolean needOverlay =
                state == State.SETTINGS || state == State.GAMEOVER || state == State.WIN ||
                        (state == State.PAUSE && !serving);
        if (!needOverlay) return;

        g2.setColor(new Color(0,0,0,110));
        g2.fillRect(0,0,getWidth(),getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        String title = switch (state) {
            case PAUSE -> "PAUSED";
            case SETTINGS -> "SETTINGS";
            case GAMEOVER -> "GAME OVER - Press R to Retry";
            case WIN -> (levelIndex + 1 < 5) ? "LEVEL CLEARED - Press N for Next"
                    : "ALL LEVELS CLEARED - Press R to Restart";
            default -> "";
        };
        if (!title.isEmpty()) {
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (getWidth()-tw)/2, getHeight()/3);
        }
        if (state == State.SETTINGS) {
            int bw = 260, bh = 36, gap = 14, cx = (getWidth()-bw)/2, cy = getHeight()/2;
            btnMusicToggle.setBounds(cx, cy, bw, bh);
            drawBtn(g2, btnMusicToggle, music.isEnabled() ? "Music: ON (pause/resume)" : "Music: OFF");
            btnMainMenu.setBounds(cx, cy + bh + gap, bw, bh);
            drawBtn(g2, btnMainMenu, "Return to MAIN MENU");
            btnBack.setBounds(cx, cy + 2*(bh+gap), bw, bh);
            drawBtn(g2, btnBack, "Back");
        }
    }
    private void drawBtn(Graphics2D g2, Rectangle r, String text) {
        g2.setColor(new Color(255,255,255,30));
        g2.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
        int tw = g2.getFontMetrics().stringWidth(text), th = g2.getFontMetrics().getAscent();
        g2.drawString(text, r.x + (r.width - tw)/2, r.y + (r.height + th)/2 - 3);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT || e.getKeyCode()==KeyEvent.VK_A)  left = true;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT|| e.getKeyCode()==KeyEvent.VK_D) right = true;
        if (e.getKeyCode()==KeyEvent.VK_SPACE) {
            if (state == State.MENU) { prepareLevel1FromHall(); return; }
            if (serving) { launchFromServe(); state = State.PLAY; music.resume(); return; }
        }
        if (e.getKeyCode()==KeyEvent.VK_P && state!=State.MENU && state!=State.SETTINGS) {
            if (state==State.PLAY && !serving) { state=State.PAUSE; music.pause(); }
            else if (state==State.PAUSE) { state=State.PLAY; music.resume(); }
        }
        if (e.getKeyCode()==KeyEvent.VK_N && state==State.WIN) {
            if (levelIndex + 1 < TOTAL_LEVELS) {
                levelIndex++; levels.load(levelIndex); resetBallPaddle();
                enterServingMode(); music.playLevel(levelIndex);
            } else { state = State.MENU; resetAll(); music.playMenu(); }
        }
        if (e.getKeyCode()==KeyEvent.VK_R && (state==State.GAMEOVER || state==State.WIN)) {
            resetAll(); enterServingMode(); music.playLevel(levelIndex);
        }
    }
    private void launchFromServe() {
        Ball b = balls.get(0);
        double center = paddle.x + paddle.w / 2.0;
        double t = (b.x - center) / (paddle.w / 2.0);
        t = Math.max(-1, Math.min(1, t));
        double speed = 6.0;
        b.vx = t * 5.0;
        b.vy = -Math.sqrt(Math.max(1, speed*speed - b.vx*b.vx));
        serving = false;
    }
    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT || e.getKeyCode()==KeyEvent.VK_A)  left = false;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT|| e.getKeyCode()==KeyEvent.VK_D) right = false;
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (settingsBtn.contains(p) && state!=State.MENU) { state=State.SETTINGS; repaint(); return; }
        if (pauseBtn.contains(p) && state!=State.MENU) {
            if (state==State.PLAY && !serving) { state=State.PAUSE; music.pause(); }
            else if (state==State.PAUSE) { state=State.PLAY; music.resume(); }
            repaint(); return;
        }
        if (homeBtn.contains(p)) { goToMainMenu(); return; }
        if (state == State.SETTINGS) {
            if (btnMusicToggle.contains(p)) {
                music.setEnabled(!music.isEnabled(), state==State.MENU, levelIndex);
                repaint(); return;
            } else if (btnMainMenu.contains(p)) { goToMainMenu(); return; }
            else if (btnBack.contains(p)) { state=State.PAUSE; repaint(); return; }
        }
    }
    private void goToMainMenu() {
        try { music.stop(); } catch (Exception ignored) {}
        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (win != null) win.dispose();
        javax.swing.SwingUtilities.invokeLater(() -> new game.mainhall.MainHall());
    }

    private void resetBallPaddle() {
        paddle.x = WIDTH / 2.0 - paddle.w / 2.0;
        balls.clear(); balls.add(new Ball(WIDTH/2.0, HEIGHT - 60, 8, 0, 0));
        dropMgr.clear(); projMgr.clear();
        expandUntil = 0; paddleOrigW = null;
        serving = true; serveOffset = 0; serveDir = +1;
    }
    private void resetAll() {
        score = 0; lives = 3; levelIndex = 0;
        levels.load(levelIndex);
        resetBallPaddle();
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
