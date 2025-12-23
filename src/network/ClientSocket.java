package network;

import java.io.*;
import java.net.Socket;

public class ClientSocket {
private final Object socketLock = new Object(); // them file

    private static ClientSocket instance;

    private static final String SERVER_IP = "192.168.56.1";
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

//    public synchronized String sendRequest(String cmd) {
//        try {
//            dos.writeUTF(cmd);   // 👈 GỬI UTF
//            dos.flush();
//
//            return dis.readUTF(); // 👈 NHẬN UTF
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "ERROR";
//        }
//    }
    // chinh sendrequest
    public String sendRequest(String cmd) {
    synchronized (socketLock) {
        try {
            dos.writeUTF(cmd);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
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
    // file 
    public Socket getSocket() {
    return socket;
}

    
    public String sendFileGroup(String email, int groupId, File file) {
    synchronized (socketLock) {
        try {
            // 1️⃣ header
            dos.writeUTF(
                "SEND_FILE_GROUP;" + email + ";" +
                groupId + ";" + file.getName() + ";" + file.length()
            );
            dos.flush();

            // 2️⃣ bytes
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();
            fis.close();

            // 3️⃣ server response
            return dis.readUTF();

        } catch (Exception e) {
            e.printStackTrace();
            return "SEND_FILE_FAIL";
        }
    }
}
    
    public String sendFilePrivate(String email, int receiverId, File file) {
    synchronized (socketLock) {
        try {
            // 1️⃣ Gửi header: SEND_FILE_PRIVATE;email;receiverId;fileName;fileLength
            dos.writeUTF(
                "SEND_FILE_PRIVATE;" + email + ";" +
                receiverId + ";" + file.getName() + ";" + file.length()
            );
            dos.flush();

            // 2️⃣ Gửi bytes của file
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();
            fis.close();

            // 3️⃣ Nhận phản hồi từ server
            return dis.readUTF();

        } catch (Exception e) {
            e.printStackTrace();
            return "SEND_FILE_FAIL";
        }
    }
}




}
