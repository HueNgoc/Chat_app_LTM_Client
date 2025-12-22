/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/Application.java to edit this template
 */
package view;

import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import network.ClientSocket;
import view.BubblePanel;

/**
 *
 * @author Admin
 */
public class Dashboard extends javax.swing.JFrame {

    private String myEmail;
    private String myName;
    private int currentGroupId = -1;
    private int currentFriendId = -1; // id của friend đang chat
    private String currentFriendName = ""; // tên friend đang chat

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Dashboard.class.getName());

    /**
     * Creates new form Dashboard
     */
    public Dashboard(String email, String name) {
        initComponents();
        this.myEmail = email;
        this.myName = name;

        this.setTitle("Chat App - " + name);
        lblAvt.setText(name);
        listGroup.setModel(new DefaultListModel<>());

        loadGroups();
        loadFriends();
        pnBody.setLayout(new BoxLayout(pnBody, BoxLayout.Y_AXIS));
        pnBody.setOpaque(true);
        pnBody.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        pnBody.setAutoscrolls(true);

    }

    public void addMessage(String text, boolean isMe) {
        JPanel pnRow = new JPanel();
        pnRow.setBackground(Color.WHITE);
        pnRow.setLayout(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 5));

        // Cho row fill full width
        //pnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnRow.getPreferredSize().height));
        pnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        BubblePanel pnBubble = new BubblePanel(isMe ? new Color(0, 197, 255) : Color.LIGHT_GRAY);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (isEmoji(codePoint)) {
                if (sb.length() > 0) {
                    pnBubble.add(new JLabel(sb.toString()));
                    sb.setLength(0);
                }

                try {
                    String hex = Integer.toHexString(codePoint);
                    String url = "https://twemoji.maxcdn.com/v/latest/72x72/" + hex + ".png";

                    ImageIcon icon = new ImageIcon(new java.net.URL(url));
                    Image img = icon.getImage().getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH);
                    ImageIcon smallIcon = new ImageIcon(img);
                    //pnBubble.add(new JLabel(icon));
                    JLabel lbl = new JLabel(smallIcon);
                    lbl.setPreferredSize(new Dimension(24, 24));
                    lbl.setMaximumSize(new Dimension(24, 24));
                    lbl.setMinimumSize(new Dimension(24, 24));
                    pnBubble.add(lbl);
                } catch (Exception e) {
                    pnBubble.add(new JLabel(new String(Character.toChars(codePoint))));
                }

            } else {
                sb.append(Character.toChars(codePoint));
            }
        }

        if (sb.length() > 0) {
            pnBubble.add(new JLabel(sb.toString()));
        }

        pnRow.add(pnBubble);
        pnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnRow.getPreferredSize().height));
        pnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnBody.add(pnRow);

        pnBody.revalidate();
        pnBody.repaint();

        scrollToBottom();
    }

    private boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1F6FF)
                || (codePoint >= 0x1F600 && codePoint <= 0x1F64F)
                || (codePoint >= 0x1F680 && codePoint <= 0x1F6FF)
                || (codePoint >= 0x1F900 && codePoint <= 0x1F9FF);
    }

    private void scrollToBottom() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            jScrollPane3.getVerticalScrollBar().setValue(
                    jScrollPane3.getVerticalScrollBar().getMaximum()
            );
        });
    }

    private void loadGroups() {
        String res = ClientSocket.getInstance()
                .sendRequest("GET_GROUPS;" + myEmail);

        if (res.startsWith("GROUP_LIST")) {
            DefaultListModel<String> model = new DefaultListModel<>();
            String[] arr = res.split(";");

            for (int i = 1; i < arr.length; i++) {
                model.addElement(arr[i]); // "id:name"
            }
            listGroup.setModel(model);
        }
    }

    private void loadNotJoinedGroups() {
        String res = ClientSocket.getInstance()
                .sendRequest("GET_NOT_JOINED_GROUPS;" + myEmail);

        if (!res.startsWith("GROUP_LIST")) {
            JOptionPane.showMessageDialog(this, "Không lấy được danh sách group");
            return;
        }

        String[] arr = res.split(";");
        DefaultListModel<String> model = new DefaultListModel<>();

        for (int i = 1; i < arr.length; i++) {
            model.addElement(arr[i]); // id:name
        }

        listGroup.setModel(model);
        lblName.setText("Danh sách group");
        currentGroupId = -1;
    }

    private void loadFriendRequests() {
        String res = ClientSocket.getInstance()
                .sendRequest("GET_FRIEND_REQUESTS;" + myEmail);

        if (!res.startsWith("FRIEND_REQUESTS")) {
            return;
        }

        DefaultListModel<String> model = new DefaultListModel<>();
        String[] arr = res.split(";");

        for (int i = 1; i < arr.length; i++) {
            model.addElement(arr[i]); // id:name
        }

        listRequest.setModel(model);
    }

    private void loadFriends() {

        DefaultListModel<String> model = new DefaultListModel<>();

        String res = ClientSocket.getInstance()
                .sendRequest("GET_FRIENDS;" + myEmail);

        if (res == null || !res.startsWith("FRIEND_LIST")) {
            return;
        }

        String[] parts = res.split(";");

        for (int i = 1; i < parts.length; i++) {
            // format server: id:name:email:avatar
            String[] f = parts[i].split(":");

            if (f.length >= 2) {
                String id = f[0];
                String name = f[1];

                // 👇 HIỂN THỊ CÓ ID
                model.addElement(id + " - " + name);
            }
        }

        listFriend.setModel(model);
    }

    private void loadBlockedFriends() {
        String res = ClientSocket.getInstance()
                .sendRequest("GET_BLOCKED_FRIENDS;" + myEmail);

        if (!res.startsWith("BLOCK_LIST")) {
            return;
        }

        DefaultListModel<String> model = new DefaultListModel<>();
        String[] arr = res.split(";");

        for (int i = 1; i < arr.length; i++) {
            model.addElement(arr[i]); // id:name hoặc name
        }

        listBlock.setModel(model);
    }

    // file
    private void addFileMessage(String sender, String fileName,
            String filePath, boolean isMe) {

        JPanel pnRow = new JPanel(
                new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 5)
        );
        pnRow.setBackground(Color.WHITE);
        pnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        BubblePanel bubble = new BubblePanel(
                isMe ? new Color(0, 197, 255) : Color.LIGHT_GRAY
        );

        JLabel lblFile = new JLabel("📎 " + sender + ": " + fileName);
        lblFile.setForeground(Color.BLUE);
        lblFile.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        lblFile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                downloadFile(filePath, fileName);
            }
        });

        bubble.add(lblFile);
        pnRow.add(bubble);
        pnBody.add(pnRow);

        pnBody.revalidate();
        pnBody.repaint();
        scrollToBottom();
    }

    private void downloadFile(String serverPath, String fileName) {

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveFile = chooser.getSelectedFile();

        new Thread(() -> {
            try {
                ClientSocket cs = ClientSocket.getInstance();

                cs.sendRequest("DOWNLOAD_FILE;" + serverPath);

                DataInputStream dis = new DataInputStream(
                        cs.getSocket().getInputStream()
                );

                long size = dis.readLong();

                FileOutputStream fos = new FileOutputStream(saveFile);
                byte[] buffer = new byte[4096];
                int read;
                long remaining = size;

                while (remaining > 0
                        && (read = dis.read(buffer, 0,
                                (int) Math.min(buffer.length, remaining))) != -1) {

                    fos.write(buffer, 0, read);
                    remaining -= read;
                }

                fos.close();

                SwingUtilities.invokeLater(()
                        -> JOptionPane.showMessageDialog(this, "Tải file thành công")
                );

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenuFriends = new javax.swing.JPopupMenu();
        btnBlock = new javax.swing.JMenuItem();
        btnView = new javax.swing.JMenuItem();
        btnHuyKB = new javax.swing.JMenuItem();
        jPopupMenuGroups = new javax.swing.JPopupMenu();
        btnLeaveGroup = new javax.swing.JMenuItem();
        btnViewGroup = new javax.swing.JMenuItem();
        jPopupMenuRequest = new javax.swing.JPopupMenu();
        btnAccept = new javax.swing.JMenuItem();
        btnReject = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        lblAvt = new javax.swing.JLabel();
        btnCreateGroup = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listFriend = new javax.swing.JList<>();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listGroup = new javax.swing.JList<>();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        listRequest = new javax.swing.JList<>();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        listBlock = new javax.swing.JList<>();
        btnListUser = new javax.swing.JButton();
        btnListGroup = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        pnBody = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        btnSendFile = new javax.swing.JButton();
        txtMessage = new javax.swing.JTextField();
        btnEmoij = new javax.swing.JButton();
        btnSend = new javax.swing.JButton();
        lblName = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();

        btnBlock.setText("Block");
        btnBlock.setActionCommand("");
        btnBlock.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnBlockMouseReleased(evt);
            }
        });
        btnBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBlockActionPerformed(evt);
            }
        });
        jPopupMenuFriends.add(btnBlock);

        btnView.setText("View Profile");
        btnView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewActionPerformed(evt);
            }
        });
        jPopupMenuFriends.add(btnView);

        btnHuyKB.setText("Reject");
        btnHuyKB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHuyKBActionPerformed(evt);
            }
        });
        jPopupMenuFriends.add(btnHuyKB);

        btnLeaveGroup.setText("Leave Group");
        btnLeaveGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLeaveGroupActionPerformed(evt);
            }
        });
        jPopupMenuGroups.add(btnLeaveGroup);

        btnViewGroup.setText("View Group");
        btnViewGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewGroupActionPerformed(evt);
            }
        });
        jPopupMenuGroups.add(btnViewGroup);

        btnAccept.setText("Accept");
        btnAccept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptActionPerformed(evt);
            }
        });
        jPopupMenuRequest.add(btnAccept);

        btnReject.setText("Reject");
        btnReject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRejectActionPerformed(evt);
            }
        });
        jPopupMenuRequest.add(btnReject);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(250);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(2000, 596));

        jPanel2.setPreferredSize(new java.awt.Dimension(500, 475));

        lblAvt.setText("Avt");

        btnCreateGroup.setText("Create Group");
        btnCreateGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateGroupActionPerformed(evt);
            }
        });

        jTabbedPane1.setToolTipText("");
        jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseClicked(evt);
            }
        });

        listFriend.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listFriendMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                listFriendMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(listFriend);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Friends", jPanel1);

        listGroup.setComponentPopupMenu(jPopupMenuGroups);
        listGroup.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listGroupMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                listGroupMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                listGroupMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(listGroup);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Groups", jPanel4);

        listRequest.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        listRequest.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                listRequestMouseReleased(evt);
            }
        });
        jScrollPane4.setViewportView(listRequest);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Friend Request", jPanel6);

        jScrollPane5.setViewportView(listBlock);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Friends Block", jPanel8);

        btnListUser.setText("List User");
        btnListUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnListUserActionPerformed(evt);
            }
        });

        btnListGroup.setText("List Group");
        btnListGroup.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListGroupMouseClicked(evt);
            }
        });
        btnListGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnListGroupActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(lblAvt, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnCreateGroup))
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(btnListUser)
                        .addGap(49, 49, 49)
                        .addComponent(btnListGroup)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblAvt)
                    .addComponent(btnCreateGroup))
                .addGap(18, 18, 18)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 479, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnListGroup)
                    .addComponent(btnListUser))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("");

        jSplitPane1.setLeftComponent(jPanel2);

        jPanel3.setPreferredSize(new java.awt.Dimension(1000, 596));

        pnBody.setLayout(new javax.swing.BoxLayout(pnBody, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane3.setViewportView(pnBody);

        btnSendFile.setText("Send File");
        btnSendFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendFileActionPerformed(evt);
            }
        });

        txtMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMessageActionPerformed(evt);
            }
        });

        btnEmoij.setText("Emoji");
        btnEmoij.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEmoijActionPerformed(evt);
            }
        });

        btnSend.setText("Send");
        btnSend.setActionCommand("");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(btnSendFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEmoij))
                    .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnSend)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSend))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSendFile)
                    .addComponent(btnEmoij))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        lblName.setText("Name/ Group");

        jButton2.setText("jButton2");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addGap(21, 21, 21)))
                .addContainerGap(43, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addGap(44, 44, 44)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 370, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 751, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewActionPerformed
        // TODO add your handling code here:
        String selected = listFriend.getSelectedValue();
        if (selected == null) {
            return;
        }

// selected = "5 - Minh Hau"
        String idStr = selected.split("-")[0].trim(); // "5"

        int friendId = Integer.parseInt(idStr);

        ViewProfileFriend dialog
                = new ViewProfileFriend(this, true, friendId);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);


    }//GEN-LAST:event_btnViewActionPerformed

    private void btnBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBlockActionPerformed
        // TODO add your handling code here:
        // Lấy tên người đang bị chọn trong List
        String selected = listFriend.getSelectedValue();
        if (selected == null) {
            return;
        }

        String[] parts = selected.split(" - ");
        if (parts.length < 2) {
            return;
        }

        int friendId;
        try {
            friendId = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            return;
        }

        String res = ClientSocket.getInstance()
                .sendRequest("BLOCK_FRIEND;" + myEmail + ";" + friendId);

        if ("BLOCK_SUCCESS".equals(res)) {
            JOptionPane.showMessageDialog(this, "Đã chặn " + parts[1]);
            loadFriends();
            loadBlockedFriends();
        } else {
            JOptionPane.showMessageDialog(this, "Chặn thất bại");
        }
    }//GEN-LAST:event_btnBlockActionPerformed

    private void btnBlockMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBlockMouseReleased

    }//GEN-LAST:event_btnBlockMouseReleased

    private void listFriendMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listFriendMouseReleased
        // TODO add your handling code here:
        // TODO add your handling code here:
        // Kiểm tra xem có phải là click chuột phải không (Right Click)
        if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {

            // 1. Lấy vị trí con chuột đang trỏ vào dòng nào trong List
            int index = listFriend.locationToIndex(evt.getPoint());

            // 2. Chọn dòng đó (bôi đen) để người dùng biết mình đang click vào ai
            listFriend.setSelectedIndex(index);

            // 3. Nếu trỏ trúng vào một dòng hợp lệ (không phải vùng trắng)
            if (index >= 0 && listFriend.isSelectedIndex(index)) {
                // Hiện cái Menu (jPopupMenu1) ngay tại vị trí con chuột
                jPopupMenuFriends.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_listFriendMouseReleased

    private void txtMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMessageActionPerformed
        // TODO add your handling code here:
        btnSend.doClick();
    }//GEN-LAST:event_txtMessageActionPerformed

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        // TODO add your handling code here:
        String text = txtMessage.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        if (currentFriendId != -1) { // chat riêng
            String res = ClientSocket.getInstance()
                    .sendRequest("SEND_PRIVATE;" + myEmail + ";" + currentFriendId + ";" + text);

            if (res.startsWith("SEND_PRIVATE_SUCCESS")) {
                addMessage("Me: " + text, true);
                txtMessage.setText("");
            }
        } else if (currentGroupId != -1) { // chat group
            String res = ClientSocket.getInstance()
                    .sendRequest("SEND_GROUP;" + myEmail + ";" + currentGroupId + ";" + text);

            if (res.startsWith("SEND_GROUP_SUCCESS")) {
                addMessage("Me: " + text, true);
                txtMessage.setText("");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Chưa chọn friend hoặc group để chat");
        }
    }//GEN-LAST:event_btnSendActionPerformed

    private void btnEmoijActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEmoijActionPerformed
        // TODO add your handling code here:
        // Danh sách emoji Unicode (có thể thêm nhiều)
        String[] emojis = {"😄", "😂", "😍", "👍", "🎉", "❤️", "😢", "😎"};

        // Hiển thị lựa chọn emoji
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Chọn Emoji:",
                "Emoji Picker",
                JOptionPane.PLAIN_MESSAGE,
                null,
                emojis,
                emojis[0]
        );

        if (selected != null) {
            // Chèn emoji vào ô chat hiện tại
            txtMessage.setText(txtMessage.getText() + selected);
            txtMessage.requestFocus();
        }
    }//GEN-LAST:event_btnEmoijActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btnCreateGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateGroupActionPerformed
        // TODO add your handling code here:
        String groupName = JOptionPane.showInputDialog(this, "Tên nhóm:");
        if (groupName != null) {
            String res = ClientSocket.getInstance()
                    .sendRequest("CREATE_GROUP;" + myEmail + ";" + groupName);

            if (res.startsWith("CREATE_GROUP_SUCCESS")) {
                loadGroups();
            }
        }

    }//GEN-LAST:event_btnCreateGroupActionPerformed

    private void btnLeaveGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLeaveGroupActionPerformed
        // TODO add your handling code here:
        String selected = listGroup.getSelectedValue();
        if (selected == null) {
            return;
        }

        int groupId = Integer.parseInt(selected.split(":")[0]);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn rời nhóm?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String res = ClientSocket.getInstance()
                .sendRequest("LEAVE_GROUP;" + myEmail + ";" + groupId);

        if (res.equals("LEAVE_GROUP_SUCCESS")) {
            loadGroups();
        } else {
            JOptionPane.showMessageDialog(this, "Rời nhóm thất bại");
        }
    }//GEN-LAST:event_btnLeaveGroupActionPerformed

    private void listGroupMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listGroupMouseReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_listGroupMouseReleased

    private void listGroupMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listGroupMousePressed
        // TODO add your handling code here:
        int index = listGroup.locationToIndex(evt.getPoint());
        if (index >= 0) {
            listGroup.setSelectedIndex(index);
        }
    }//GEN-LAST:event_listGroupMousePressed

    private void jTabbedPane1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTabbedPane1MouseClicked
        // TODO add your handling code here:
        int index = jTabbedPane1.getSelectedIndex();
        String title = jTabbedPane1.getTitleAt(index);

        if (title.equals("Friend Request")) {
            loadFriendRequests();
        }

    }//GEN-LAST:event_jTabbedPane1MouseClicked

    private void listGroupMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listGroupMouseClicked
        // TODO add your handling code here:
        String selected = listGroup.getSelectedValue();
        if (selected != null) {
            String[] parts = selected.split(":");
            if (parts.length < 2) {
                System.err.println("Lỗi: dữ liệu group không hợp lệ: " + selected);
                return;
            }
            try {
                currentGroupId = Integer.parseInt(parts[0]);
                currentFriendId = -1;
                lblName.setText(parts[1]); // Hiển thị tên group
            } catch (NumberFormatException e) {
                System.err.println("Lỗi parse groupId: " + parts[0]);
                return;
            }

            // Clear pnBody để load lại message
            pnBody.removeAll();
            pnBody.revalidate();
            pnBody.repaint();

            // Lấy lịch sử tin nhắn từ server
            String res = ClientSocket.getInstance()
                    .sendRequest("GET_GROUP_MESSAGES;" + currentGroupId);

            if (res.startsWith("GROUP_MESSAGES")) {
                String[] msgs = res.split(";");
                for (int i = 1; i < msgs.length; i++) {
//                String[] msgParts = msgs[i].split(":", 3); // email, fullname, content
//                if (msgParts.length < 3) continue; // tránh lỗi nếu msg không đủ
//                String senderEmail = msgParts[0];
//                String senderName = msgParts[1];
//                String content = msgParts[2];
//
//                String displayName = senderEmail.equals(myEmail) ? "Me" : senderName;
//                addMessage(displayName + ": " + content, senderEmail.equals(myEmail));

// coment từ chỗ này để chode file
//                    String[] msgParts = msgs[i].split("\\|", 4); // id | email | fullname | content
//                    if (msgParts.length < 4) {
//                        continue;
//                    }
//
//                    String msgId = msgParts[0];       // có thể bỏ qua nếu không cần
//                    String senderEmail = msgParts[1];
//                    String senderName = msgParts[2];
//                    String content = msgParts[3];
//
//                    String displayName = senderEmail.equals(myEmail) ? "Me" : senderName;
//                    addMessage(displayName + ": " + content, senderEmail.equals(myEmail));
                    String[] msgParts = msgs[i].split("\\|");
                    if (msgParts.length < 4) {
                        continue; // tối thiểu phải có id|email|name|type
                    }
                    String senderEmail = msgParts[1];
                    String senderName = msgParts[2];
                    String msgType = msgParts[3];

                    boolean isMe = senderEmail.equals(myEmail);
                    String displayName = isMe ? "Me" : senderName;

                    if ("File".equals(msgType)) {
                        if (msgParts.length < 6) {
                            continue; // cần fileName + filePath
                        }
                        String fileName = msgParts[4];
                        String filePath = msgParts[5];
                        addFileMessage(displayName, fileName, filePath, isMe);
                    } else {
                        if (msgParts.length < 5) {
                            continue; // cần content
                        }
                        String content = msgParts[4];
                        addMessage(displayName + ": " + content, isMe);
                    }

                }
            }
        }
    }//GEN-LAST:event_listGroupMouseClicked

    private void btnListGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnListGroupActionPerformed
        // TODO add your handling code here:
        JoinGroupDialog dialog = new JoinGroupDialog(this, myEmail);
        dialog.setVisible(true);

        // Sau khi dialog đóng → reload group chính
        loadGroups();

    }//GEN-LAST:event_btnListGroupActionPerformed

    private void btnListGroupMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListGroupMouseClicked
        // TODO add your handling code here:

    }//GEN-LAST:event_btnListGroupMouseClicked

    private void btnListUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnListUserActionPerformed
        // TODO add your handling code here:
        FriendDialog dlg
                = new FriendDialog(this, myEmail);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }//GEN-LAST:event_btnListUserActionPerformed

    private void listRequestMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listRequestMouseReleased
        // TODO add your handling code here:
        if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {

            int idx = listRequest.locationToIndex(evt.getPoint());

            if (idx >= 0) {
                listRequest.setSelectedIndex(idx);

                // hiện popup menu tại vị trí chuột
                jPopupMenuRequest.show(
                        listRequest,
                        evt.getX(),
                        evt.getY()
                );
            }
        }
    }//GEN-LAST:event_listRequestMouseReleased

    private void btnAcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptActionPerformed
        // TODO add your handling code here:
        String selected = listRequest.getSelectedValue();
        if (selected == null) {
            return;
        }

        int friendId = Integer.parseInt(selected.split(":")[0]);

        String res = ClientSocket.getInstance()
                .sendRequest("ACCEPT_FRIEND;" + myEmail + ";" + friendId);

        if (res.equals("ACCEPT_SUCCESS")) {
            JOptionPane.showMessageDialog(this, "Đã chấp nhận");
            loadFriendRequests();
            loadFriends();
        }
    }//GEN-LAST:event_btnAcceptActionPerformed

    private void btnRejectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRejectActionPerformed
        // TODO add your handling code here:
        String selected = listRequest.getSelectedValue();
        if (selected == null) {
            return;
        }

        int friendId = Integer.parseInt(selected.split(":")[0]);

        String res = ClientSocket.getInstance()
                .sendRequest("REJECT_FRIEND;" + myEmail + ";" + friendId);

        if (res.equals("REJECT_SUCCESS")) {
            JOptionPane.showMessageDialog(this, "Đã từ chối");
            loadFriendRequests();
        }
    }//GEN-LAST:event_btnRejectActionPerformed

    private void btnSendFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendFileActionPerformed
//        if (currentGroupId == -1) {
//            JOptionPane.showMessageDialog(this, "Chưa chọn friend hoặc group");
//            return;
//        }
//
//        JFileChooser chooser = new JFileChooser();
//        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
//            return;
//        }
//
//        File file = chooser.getSelectedFile();
//
//        new Thread(() -> {
//            ClientSocket socket = ClientSocket.getInstance();
//
//            String res = socket.sendFileGroup(
//                    myEmail,
//                    currentGroupId,
//                    file
//            );
//
////            
//// Trong hàm btnSendFileActionPerformed
//            if ("SEND_FILE_SUCCESS".equals(res)) {
//
//                // reload message sau khi gửi file
//                String msg = socket.sendRequest("GET_GROUP_MESSAGES;" + currentGroupId);
//
//                SwingUtilities.invokeLater(() -> {
//                    pnBody.removeAll();
//
//                    if (msg.startsWith("GROUP_MESSAGES")) {
//                        String[] msgs = msg.split(";");
//
//                        for (int i = 1; i < msgs.length; i++) {
//                            // SỬA TỪ ĐÂY: Dùng logic split và check type giống hệt hàm click group
//                            String[] parts = msgs[i].split("\\|"); // Bỏ giới hạn số 4 đi để cắt hết
//
//                            if (parts.length < 4) {
//                                continue;
//                            }
//
//                            String senderEmail = parts[1];
//                            String senderName = parts[2];
//                            String msgType = parts[3]; // Lấy loại tin nhắn (Text hay File)
//
//                            boolean isMe = senderEmail.equals(myEmail);
//                            String displayName = isMe ? "Me" : senderName;
//
//                            // Kiểm tra loại tin nhắn để gọi hàm hiển thị đúng
//                            if ("File".equals(msgType)) {
//                                if (parts.length < 6) {
//                                    continue;
//                                }
//                                String fileName = parts[4];
//                                String filePath = parts[5];
//                                // Gọi hàm hiện file
//                                addFileMessage(displayName, fileName, filePath, isMe);
//                            } else {
//                                if (parts.length < 5) {
//                                    continue;
//                                }
//                                String content = parts[4];
//                                // Gọi hàm hiện text
//                                addMessage(displayName + ": " + content, isMe);
//                            }
//                        }
//                    }
//
//                    pnBody.revalidate();
//                    pnBody.repaint();
//                    // Cuộn xuống dưới cùng sau khi load xong
//                    scrollToBottom();
//                });
//            } else {
//                SwingUtilities.invokeLater(()
//                        -> JOptionPane.showMessageDialog(this, "Gửi file thất bại")
//                );
//            }
//        }).start();
if (currentGroupId == -1 && currentFriendId == -1) {
        JOptionPane.showMessageDialog(this, "Chưa chọn friend hoặc group");
        return;
    }

    JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
        return;
    }

    File file = chooser.getSelectedFile();

    new Thread(() -> {
        ClientSocket socket = ClientSocket.getInstance();
        String res = "";

        try {
            if (currentFriendId != -1) {
                // Gửi file cho friend riêng
                res = socket.sendFilePrivate(myEmail, currentFriendId, file);
            } else if (currentGroupId != -1) {
                // Gửi file cho group
                res = socket.sendFileGroup(myEmail, currentGroupId, file);
            }

            if ("SEND_FILE_SUCCESS".equals(res)) {
              
                // Reload chat sau khi gửi file
                SwingUtilities.invokeLater(() -> {
                    pnBody.removeAll();
                    String msg = "";

                    if (currentFriendId != -1) {
                        msg = socket.sendRequest("GET_PRIVATE_MESSAGES;" + myEmail + ";" + currentFriendId);
                        if (msg.startsWith("PRIVATE_MESSAGES")) {
                            String[] msgs = msg.split(";");
                            for (int i = 1; i < msgs.length; i++) {
                                String[] parts = msgs[i].split("\\|"); // id|email|name|type|content/fileName
                                if (parts.length < 4) continue;
                                String senderEmail = parts[1];
                                String senderName = parts[2];
                                String msgType = parts[3];
                                boolean isMe = senderEmail.equals(myEmail);
                                String displayName = isMe ? "Me" : senderName;

                                if ("File".equals(msgType) && parts.length >= 6) {
                                    String fileName = parts[4];
                                    String filePath = parts[5];
                                    addFileMessage(displayName, fileName, filePath, isMe);
                                } else if (parts.length >= 5) {
                                    String content = parts[4];
                                    addMessage(displayName + ": " + content, isMe);
                                }
                            }   
                        }
                    } else if (currentGroupId != -1) {
                        msg = socket.sendRequest("GET_GROUP_MESSAGES;" + currentGroupId);
                        // Logic giống như trước
                        if (msg.startsWith("GROUP_MESSAGES")) {
                            String[] msgs = msg.split(";");
                            for (int i = 1; i < msgs.length; i++) {
                                String[] parts = msgs[i].split("\\|");
                                if (parts.length < 4) continue;
                                String senderEmail = parts[1];
                                String senderName = parts[2];
                                String msgType = parts[3];
                                boolean isMe = senderEmail.equals(myEmail);
                                String displayName = isMe ? "Me" : senderName;

                                if ("File".equals(msgType) && parts.length >= 6) {
                                    String fileName = parts[4];
                                    String filePath = parts[5];
                                    addFileMessage(displayName, fileName, filePath, isMe);
                                } else if (parts.length >= 5) {
                                    String content = parts[4];
                                    addMessage(displayName + ": " + content, isMe);
                                }
                            }
                        }
                    }

                    pnBody.revalidate();
                    pnBody.repaint();
                    scrollToBottom();
                });
            } else {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Gửi file thất bại")
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Gửi file thất bại")
            );
        }
    }).start();

    }//GEN-LAST:event_btnSendFileActionPerformed

    private void listFriendMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listFriendMouseClicked
        // TODO add your handling code here:
        String selected = listFriend.getSelectedValue();
        if (selected == null) {
            return;
        }

        // selected = "3 - Huệ"
        String[] parts = selected.split(" - ");
        if (parts.length < 2) {
            return;
        }

        try {
            currentFriendId = Integer.parseInt(parts[0].trim());
            currentGroupId = -1;
            currentFriendName = parts[1].trim();
        } catch (NumberFormatException e) {
            return;
        }

        lblName.setText(currentFriendName);

        // Clear chat
        pnBody.removeAll();
        pnBody.revalidate();
        pnBody.repaint();

        // Load lịch sử chat
        String res = ClientSocket.getInstance()
                .sendRequest("GET_PRIVATE_MESSAGES;" + myEmail + ";" + currentFriendId);

        if (res.startsWith("PRIVATE_MESSAGES")) {
            String[] msgs = res.split(";");
            for (int i = 1; i < msgs.length; i++) {
                String[] msgParts = msgs[i].split("\\|");
                if (msgParts.length < 4) {
                    continue;
                }

                String senderEmail = msgParts[1];
                String senderName = msgParts[2];
                String msgType = msgParts[3];

                boolean isMe = senderEmail.equals(myEmail);
                String displayName = isMe ? "Me" : senderName;

                if ("File".equals(msgType)) {
                    if (msgParts.length >= 6) {
                        String fileName = msgParts[4];
                        String filePath = msgParts[5];
                        addFileMessage(displayName, fileName, filePath, isMe);
                    }
                } else {
                    if (msgParts.length >= 5) {
                        String content = msgParts[4];
                        addMessage(displayName + ": " + content, isMe);
                    }
                }

            }
        }
    }//GEN-LAST:event_listFriendMouseClicked

    private void btnHuyKBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHuyKBActionPerformed
        // TODO add your handling code here:

        String selected = listFriend.getSelectedValue();
        if (selected == null) {
            return;
        }

        // selected = "3 - Huệ"
        String[] parts = selected.split(" - ");

        if (parts.length < 1) {
            JOptionPane.showMessageDialog(this, "Dữ liệu không hợp lệ");
            return;
        }

        int friendId;
        try {
            friendId = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Không lấy được ID bạn bè");
            return;
        }

        String res = ClientSocket.getInstance()
                .sendRequest("REMOVE_FRIEND;" + myEmail + ";" + friendId);

        if ("REMOVE_FRIEND_SUCCESS".equals(res)) {
            JOptionPane.showMessageDialog(this, "Đã hủy kết bạn");
            loadFriends();
        } else {
            JOptionPane.showMessageDialog(this, "Hủy kết bạn thất bại");
        }

    }//GEN-LAST:event_btnHuyKBActionPerformed

    private void btnViewGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewGroupActionPerformed
        // TODO add your handling code here:
        String selected = listGroup.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn group");
            return;
        }

        // selected = "3:Java Group"
        String[] parts = selected.split(":");
        if (parts.length < 2) {
            JOptionPane.showMessageDialog(this, "Dữ liệu group không hợp lệ");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Không lấy được groupId");
            return;
        }

        // 👉 MỞ DIALOG VIEW GROUP
        ViewGroupInfo dialog
                = new ViewGroupInfo(this, true, groupId);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }//GEN-LAST:event_btnViewGroupActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem btnAccept;
    private javax.swing.JMenuItem btnBlock;
    private javax.swing.JButton btnCreateGroup;
    private javax.swing.JButton btnEmoij;
    private javax.swing.JMenuItem btnHuyKB;
    private javax.swing.JMenuItem btnLeaveGroup;
    private javax.swing.JButton btnListGroup;
    private javax.swing.JButton btnListUser;
    private javax.swing.JMenuItem btnReject;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btnSendFile;
    private javax.swing.JMenuItem btnView;
    private javax.swing.JMenuItem btnViewGroup;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPopupMenu jPopupMenuFriends;
    private javax.swing.JPopupMenu jPopupMenuGroups;
    private javax.swing.JPopupMenu jPopupMenuRequest;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblAvt;
    private javax.swing.JLabel lblName;
    private javax.swing.JList<String> listBlock;
    private javax.swing.JList<String> listFriend;
    private javax.swing.JList<String> listGroup;
    private javax.swing.JList<String> listRequest;
    private javax.swing.JPanel pnBody;
    private javax.swing.JTextField txtMessage;
    // End of variables declaration//GEN-END:variables

}
