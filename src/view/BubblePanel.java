/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

/**
 *
 * @author Admin
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class BubblePanel extends JPanel {
    private Color backgroundColor;

    public BubblePanel(Color color) {
        this.backgroundColor = color;
        setOpaque(false); // Để trong suốt phần viền thừa ra
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Khử răng cưa cho đẹp
        g2.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        
        g2.setColor(backgroundColor);
        // Vẽ hình chữ nhật bo tròn (x, y, width, height, độ cong ngang, độ cong dọc)
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        
        super.paintComponent(g);
    }
}