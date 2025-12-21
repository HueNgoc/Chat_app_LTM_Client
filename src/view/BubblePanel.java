package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class BubblePanel extends JPanel {

    private Color backgroundColor;

    public BubblePanel(Color color) {
        this.backgroundColor = color;
        setOpaque(false); // trong suốt phần viền
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); // để thêm JLabel text/emoji theo hàng ngang
        setAlignmentX(LEFT_ALIGNMENT); // căn trái
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Khử răng cưa
        g2.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        // Vẽ background bo tròn
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

        super.paintComponent(g);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        d.height = 30; // giới hạn chiều cao tối đa của bubble
        return d;
    }

    @Override
    public Dimension getPreferredSize() {
        // Cho phép panel mở rộng ngang nhưng hạn chế chiều cao
        Dimension d = super.getPreferredSize();
        d.width = Math.min(400, d.width); // giới hạn chiều rộng tối đa
        d.height = Math.max(24, d.height); // ít nhất bằng icon size
        return d;
    }
}
