/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocket {
    // 1. Biến tĩnh để giữ instance duy nhất (Singleton)
    private static ClientSocket instance;

    // Cấu hình Server (IP và Port)
    private static final String SERVER_IP = "127.0.0.1"; // Localhost
    private static final int SERVER_PORT = 12345;

    // Các biến xử lý kết nối
    private Socket socket;
    private PrintWriter out;      // Để gửi dữ liệu đi
    private BufferedReader in;    // Để đọc dữ liệu về

    // 2. Constructor là private để ngăn tạo mới từ bên ngoài
    private ClientSocket() {
        try {
            // Mở kết nối đến Server
            socket = new Socket(SERVER_IP, SERVER_PORT);
            
            // Khởi tạo luồng nhập/xuất
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("Đã kết nối thành công đến Server!");
        } catch (IOException e) {
            System.err.println("Không thể kết nối đến Server: " + e.getMessage());
        }
    }

    // 3. Phương thức tĩnh để lấy instance (Lazy Initialization)
    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    // 4. Phương thức gửi yêu cầu và nhận phản hồi
    public String sendRequest(String cmd) {
        String response = "";
        try {
            if (socket == null || socket.isClosed()) {
                return "Error: Socket chưa kết nối";
            }

            // Gửi lệnh (cmd) lên Server
            out.println(cmd);

            // Đợi và đọc phản hồi từ Server
            response = in.readLine();

        } catch (IOException e) {
            response = "Error: " + e.getMessage();
            e.printStackTrace();
        }
        return response;
    }

    // Phương thức đóng kết nối khi không dùng nữa
    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            instance = null; // Reset instance
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}