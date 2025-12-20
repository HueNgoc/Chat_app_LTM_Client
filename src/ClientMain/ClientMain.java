/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientMain;

/**
 *
 * @author Admin
 */
import view.Login; // Import file giao diện Login của bạn

public class ClientMain {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Tạo mới form Login và cho nó hiện lên
                new Login().setVisible(true);
            }
        });
    }
}