package network;

import java.io.*;
import java.net.Socket;

public class ClientSocket {

    private static ClientSocket instance;

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private ClientSocket() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            System.out.println("Đã kết nối thành công đến Server!");
        } catch (IOException e) {
            System.err.println("Không thể kết nối Server: " + e.getMessage());
        }
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    public synchronized String sendRequest(String cmd) {
        try {
            dos.writeUTF(cmd);   // 👈 GỬI UTF
            dos.flush();

            return dis.readUTF(); // 👈 NHẬN UTF
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public void closeConnection() {
        try {
            if (socket != null) socket.close();
            instance = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
