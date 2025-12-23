/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package voice;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class VoiceCall {

    private static final AudioFormat FORMAT =
            new AudioFormat(8000.0f, 16, 1, true, false);

    private TargetDataLine mic;
    private SourceDataLine speaker;
    private Socket socket;
    private ServerSocket server;
    private boolean running = false;

    // ====== RECEIVER (BỊ GỌI) ======
    public void startReceiver(int port) throws Exception {
        server = new ServerSocket(port);
        socket = server.accept();

        speaker = AudioSystem.getSourceDataLine(FORMAT);
        speaker.open(FORMAT);
        speaker.start();

        running = true;

        new Thread(() -> {
            try (InputStream in = socket.getInputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while (running && (len = in.read(buffer)) > 0) {
                    speaker.write(buffer, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ====== CALLER (GỌI) ======
    public void startCaller(String ip, int port) throws Exception {
        socket = new Socket(ip, port);

        mic = AudioSystem.getTargetDataLine(FORMAT);
        mic.open(FORMAT);
        mic.start();

        running = true;

        new Thread(() -> {
            try (OutputStream out = socket.getOutputStream()) {
                byte[] buffer = new byte[1024];
                while (running) {
                    int count = mic.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        out.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (mic != null) mic.close();
            if (speaker != null) speaker.close();
            if (socket != null) socket.close();
            if (server != null) server.close();
        } catch (Exception ignored) {}
    }
}

