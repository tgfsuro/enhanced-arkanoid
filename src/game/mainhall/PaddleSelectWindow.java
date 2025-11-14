package game.mainhall;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PaddleSelectWindow extends JDialog {
    public PaddleSelectWindow(Frame owner) {
        super(owner, "Chọn Paddle", true);
        setSize(520, 320);
        setLocationRelativeTo(owner);

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);
        JScrollPane sp = new JScrollPane(grid);
        sp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < PaddleSkinStore.count(); i++) {
            int idx = i;

            Image img = (i == PaddleSkinStore.getIndex())
                    ? PaddleSkinStore.getImage() : null;
            if (img == null) {
                // tạm thời set index để load preview, rồi restore
                int bak = PaddleSkinStore.getIndex();
                PaddleSkinStore.setIndex(i);
                img = PaddleSkinStore.getImage();
                PaddleSkinStore.setIndex(bak);
            }

            if (img == null) img = new BufferedImage(160, 20, BufferedImage.TYPE_INT_ARGB);

            // preview ở 160px chiều ngang, giữ tỉ lệ
            int pw = 160;
            int ph = Math.max(20, img.getHeight(null) * pw / Math.max(1, img.getWidth(null)));
            ImageIcon icon = new ImageIcon(img.getScaledInstance(pw, ph, Image.SCALE_SMOOTH));

            JLabel pic = new JLabel(icon, SwingConstants.CENTER);
            JRadioButton rb = new JRadioButton(PaddleSkinStore.nameAt(i), i == PaddleSkinStore.getIndex());
            rb.setHorizontalAlignment(SwingConstants.CENTER);
            group.add(rb);

            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
            cell.add(pic, BorderLayout.CENTER);
            cell.add(rb, BorderLayout.SOUTH);

            rb.addActionListener(e -> PaddleSkinStore.setIndex(idx));

            grid.add(cell);
        }

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(ok);

        setLayout(new BorderLayout());
        add(sp, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }
}
