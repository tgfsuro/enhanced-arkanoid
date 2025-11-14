package game.mainhall;

import game.AssetLoader;
import game.GameWindow;
import game.MusicHub;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainHall extends JFrame {

    private Image bg;
    private final MusicHub music = new MusicHub();

    public MainHall() {
        setTitle("Main Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Panel nền + layout ở giữa
        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, null);
                else { g.setColor(Color.BLACK); g.fillRect(0,0,getWidth(),getHeight()); }
            }
        };
        root.setPreferredSize(new Dimension(800, 600));
        root.setLayout(new GridBagLayout()); // căn giữa

        try { bg = AssetLoader.scaled("backgrounds/mainbackground.jpg", 800, 600); } catch (Exception ignored) {}

        // Cột nút ở giữa
        JPanel btnCol = new JPanel();
        btnCol.setOpaque(false);
        btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));

        Font f = new Font("Monospaced", Font.BOLD, 16);

        JButton btnPlay = new JButton("PLAY");
        btnPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPlay.setFont(f);
        btnPlay.setPreferredSize(new Dimension(180, 42));
        btnPlay.setMaximumSize(new Dimension(180, 42));
        btnPlay.addActionListener((ActionEvent e) -> {
            new GameWindow(true); // vào thẳng level 1: bóng serving, SPACE để bắn
            music.stop();
            dispose();
        });

        // ======= NEW: nút chọn Paddle (skin) =======
        JButton btnPaddle = new JButton("PADDLE");
        btnPaddle.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPaddle.setFont(f);
        btnPaddle.setPreferredSize(new Dimension(180, 42));
        btnPaddle.setMaximumSize(new Dimension(180, 42));
        btnPaddle.addActionListener(e -> new PaddleSelectWindow(this).setVisible(true));
        // ===========================================

        JButton btnSettings = new JButton("SETTINGS");
        btnSettings.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSettings.setFont(f);
        btnSettings.setPreferredSize(new Dimension(180, 42));
        btnSettings.setMaximumSize(new Dimension(180, 42));
        btnSettings.addActionListener(e -> {
            boolean on = music.isEnabled();
            int ans = JOptionPane.showConfirmDialog(
                    this,
                    "Music is " + (on ? "ON" : "OFF") + ". Toggle?",
                    "Settings",
                    JOptionPane.YES_NO_OPTION
            );
            if (ans == JOptionPane.YES_OPTION) {
                music.setEnabled(!on, true, 0); // áp dụng cho sảnh
                if (music.isEnabled()) music.playMenu(); else music.stop();
            }
        });

        JButton btnQuit = new JButton("QUIT");
        btnQuit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnQuit.setFont(f);
        btnQuit.setPreferredSize(new Dimension(180, 42));
        btnQuit.setMaximumSize(new Dimension(180, 42));
        btnQuit.addActionListener(e -> { music.stop(); System.exit(0); });

        // sắp xếp nút
        btnCol.add(btnPlay);
        btnCol.add(Box.createVerticalStrut(14));
        btnCol.add(btnPaddle);   // <— nút mới
        btnCol.add(Box.createVerticalStrut(14));
        btnCol.add(btnSettings);
        btnCol.add(Box.createVerticalStrut(14));
        btnCol.add(btnQuit);

        root.add(btnCol, new GridBagConstraints());
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        music.playMenu();
    }
}
