


//实现了一个功能相对完整的聊天客户端核心逻辑：
//连接管理: 建立和断开与服务器的 TCP 连接。
//用户认证: 发送登录和（通过命令格式暗示的）登出请求。还提供了注册新用户的接口。
//消息收发:
//通过一个独立线程异步接收来自服务器的消息。
//通过 sendCommand 方法向服务器发送格式化的命令（包括聊天消息、登录、登出、注册等）。
//消息处理与分发: 解析服务器下发的不同类型的消息（聊天内容、用户状态、用户列表更新），并通过回调机制通知外部组件（如UI）。
//编码: 使用 UTF-8 编码处理输入输出，支持多语言。
//错误处理: 对网络IO异常进行了基本的捕获和处理，并更新连接状态
import chatclient.logger.MessageLogger;
import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatClient {
    private Socket socket; // 用于与服务器进行网络通信的套接字
    private BufferedReader in; // 从服务器读取数据的缓冲读取器
    private BufferedWriter out; // 向服务器写入数据的缓冲写入器
    private String username; // 当前客户端的用户名
    private boolean isConnected = false; //标记客户端是否已连接到服务器，默认为 false
    private static final String SERVER_ADDRESS = "127.0.0.1"; // 服务器的 IP 地址 (本地回环地址)   此地址在不同设备登陆时需要改为实际服务器地址  总共有两个需要改
    private static final int SERVER_PORT = 8070; // 服务器监听的端口号


    private static volatile String pendingPeer   = null;   // 发送端：对方
    private static volatile File   pendingFile   = null;   // 发送端：文件
    private static volatile String incomingPeer  = null;   // 接收端：发送方
    private static volatile File   incomingSave  = null;   // 接收端：保存路径


    // 监听服务器消息的线程
    private Thread messageListener;

    // 消息回调接口，用于通知UI更新这个回调机制用于将从服务器接收到的信息（如新消息、用户状态改变）通知给用户界面 (UI) 或其他需要这些信息的组件，从而实现解耦。
    private MessageCallback messageCallback;

    // 在 ChatClient 的 MessageCallback 接口中添加
    public interface MessageCallback {
        void onMessageReceived(String from, String content);
        void onGroupMessageReceived(String from, String groupId, String content);
        void onUserStatusChanged();
        void onUserListUpdated();
        void onGroupMemberJoined(String groupId, String groupName, String username);
        void onGroupFileListReceived(String groupId, java.util.List<String> files);

    }

    public ChatClient(String username) {
        this.username = username;
    }

    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    //连接服务器

    public boolean connect(int mode) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            if(mode==1) {
                sendCommand("TOLOGIN");
            }

            startMessageListener();

            isConnected = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();   // 看 stackTrace
            System.err.println("目标 IP/端口 = " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }

    }
    //断开与服务器的连接
    public void disconnect(int mode) {
        if (isConnected) {
            try {
                if(mode==1)
                {sendCommand("LOGOUT:" + username);
                }
                isConnected = false;
                if (messageListener != null) {
                    messageListener.interrupt();
                }

                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("断开连接时发生错误: " + e.getMessage());
            }
        }
    }
    private void startMessageListener() {
        messageListener = new Thread(() -> {
            try {
                String message;
                while (isConnected && (message = in.readLine()) != null) {
                    processServerMessage(message);
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("接收消息时发生错误: " + e.getMessage());
                    isConnected = false;
                }
            }
        });
        messageListener.start();
    }

    private void processServerMessage(String message) {
        // ChatClient.processServerMessage

        if ("ACCOUNT_CONFLICT".equals(message)) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "账号已在另一处登录，您被迫下线！", "提示",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            try {
                System.exit(0);
            } catch (Exception ignore) {}

            // 不直接调用System.exit(0)，而是通过回调通知UI层处理关闭逻辑
            return;            // 别再往下解析
        }
        System.out.println("您已接收到一个消息"+message);
        if (message.startsWith("MSG:")) {
            // 格式: MSG:sender:receiver:content
            String[] parts = message.split(":", 4);
            if (parts.length >= 4 && messageCallback != null) {

                String sender   = parts[1];
                String receiver = parts[2];
                String content  = parts[3];
                if (sender.equalsIgnoreCase(this.username)) {
                    // 这条消息是自己发的，UI 已经展示过，直接忽略
                    return;
                }
                //使按钮闪烁
                /* ★ 关键：谁是“对话对象”？ */
                // 如果 sender 就是自己 ⇒ peer = receiver
                // 否则 peer = sender          （正常收到别人发来的私聊）
                String peer = sender.equalsIgnoreCase(this.username)
                        ? receiver
                        : sender;
                /* —— 日志保持不变，存到以 peer 命名的会话目录 —— */
                MessageLogger.write(this.username,  // 当前登录人目录
                        peer,           // 会话 ID          (★)
                        sender,         // 真实发送者
                        content);       // 文本内容

                /* —— 只把 peer 传回 UI —— */
                messageCallback.onMessageReceived(peer, content);
            }
        }

        else if (message.startsWith("GROUP_MSG|")) {
            // 群聊消息格式: GROUP_MSG|发送者|群组ID|群组名|内容
            String[] parts = message.split("\\|", 5);
            if (parts.length >= 5 && messageCallback != null) {
                String sender = parts[1];
                String groupId = parts[2];
                String groupName = parts[3];
                String content = parts[4];

                // 为这种消息类型添加一个新的回调方法
                // 或者使用现有的 onMessageReceived，但标明是群消息
                // 例如：messageCallback.onGroupMessageReceived(sender, groupId, groupName, content);

                MessageLogger.write(this.username,
                        "GROUP_" + groupId,
                        sender,
                        content);

                // 如果选择使用现有回调，可以在消息前添加标识
                messageCallback.onGroupMessageReceived(sender, groupId, content);
            }
        }
        // 在 ChatClient 的消息处理中添加
        else if (message.equals("ACCOUNT_CONFLICT")) {
            System.err.println("账号在其他地方登录，已被迫下线！");
            try { socket.close(); } catch (IOException ex) {}
            isConnected = false;
        }
        else if (message.startsWith("GROUP_MEMBER_JOINED|")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 4) {
                String groupId = parts[1];
                String groupName = parts[2];
                String username = parts[3];

                if (messageCallback != null) {
                    messageCallback.onGroupMemberJoined(groupId, groupName, username);
                }
            }
        }else if (message.startsWith("STATUSOFF:")) {
            String[] parts = message.split(" ", 2);
            if (parts.length >= 2 && messageCallback != null) {
                for (user u : user.userList) {
                    if (u.getName().equals(parts[1])) {
                        u.setStatus(false);
                        System.out.println("用户 " + parts[1] + " 已下线");
                    }
                }
                user.saveUsersToFile("userstmp.txt");
                messageCallback.onUserStatusChanged();
            }
        } else if(message.startsWith("STATUSON:")) {
            String[] parts = message.split(" ", 2);
            if (parts.length >= 2 && messageCallback != null) {
                int ifcunzai=0;
                for (user u : user.userList) {
                    if (u.getName().equals(parts[1])) {
                        u.setStatus(true);
                        System.out.println("用户 " + parts[1] + " 已上线");
                        ifcunzai++;
                    }
                }
                if(ifcunzai==0)
                {
                    user u=new user(parts[1],"***",true);
                    user.userList.add(u);
                }
                user.saveUsersToFile("userstmp.txt");
                messageCallback.onUserStatusChanged();
            }
        } else if (message.equals("USERLIST")) {
            // 用户列表请求
            sendCommand("USERLIST:" + username);
        }
        else if (message.equals("USERLIST_UPDATED")) {
            // 用户列表已更新
            if (messageCallback != null) {
                messageCallback.onUserListUpdated();
            }
        }
        else if (message.startsWith("ADDGROUP|")) {
            // 解析群组信息: ADDGROUP|groupId|groupName|creatorName|members
            String[] parts = message.split("\\|");
            if (parts.length >= 5) {
                String groupId = parts[1];
                String groupName = parts[2];
                String creatorName = parts[3];
                String[] memberArray = parts[4].split(",");

                // 检查这个群组是否已经存在
                boolean groupExists = false;
                for (Group group : Group.GroupList) {
                    if (group.getId().equals(groupId)) {
                        groupExists = true;
                        break;
                    }
                }

                // 如果群组不存在，则添加到本地群组列表中
                if (!groupExists) {
                    // 创建成员列表
                    List<String> memberList = new ArrayList<>();
                    for (String member : memberArray) {
                        if (!member.trim().isEmpty()) {
                            memberList.add(member.trim());
                        }
                    }

                    // 创建新群组
                    Group newGroup = new Group(groupName, creatorName, memberList);

                    // 设置正确的群组ID（因为构造函数会生成新ID）
                    try {
                        java.lang.reflect.Field idField = Group.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(newGroup, groupId);
                    } catch (Exception e) {
                        System.err.println("无法设置群聊ID: " + e.getMessage());
                    }

                    // 添加到群组列表并保存到文件
                    Group.GroupList.add(newGroup);
                    Group.saveGroupsToFile("groupstmp.txt");

                    System.out.println("已添加新群组: " + groupName + " (ID: " + groupId + ")");

                    // 如果当前用户是群组成员，通知UI更新群组列表
                    if (messageCallback != null && newGroup.isMember(username)) {
                        messageCallback.onUserListUpdated();
                    }
                }
            } else {
                System.err.println("收到格式不正确的群组信息: " + message);
            }
        }
        else if (message.startsWith("USERLIST:")) {
            // 解析用户列表信息
            user.userList.clear(); // 清空现有列表避免重复
            String userListData = message.substring(9); // 去掉"USERLIST:"前缀
            System.out.println("测试客户端是否收到用户信息："+userListData);
            String[] userEntries = userListData.split(";");
            for (String userEntry : userEntries) {
                if (userEntry.isEmpty()) continue;

                String[] userData = userEntry.split(",");
                if (userData.length >= 3) {
                    String name = userData[0];
                    String password = userData[1];
                    boolean status = Boolean.parseBoolean(userData[2]);

                    user u = new user(name, password,status);
                    user.userList.add(u);
                }
            }

            user.saveUsersToFile("userstmp.txt");

            System.out.println("测试是否将用户加入");
            for(user u : user.userList) {
                System.out.println("用户名: " + u.getName() + ", 密码: " + u.getPassword() + ", 在线状态: " + u.isStatus());
            }
        }

        else if (message.startsWith("GROUP_FILE_LIST|")) {
            // 格式: GROUP_FILE_LIST|<groupId>|file1,file2,...
            String[] parts = message.split("\\|", 3);
            if (parts.length == 3 && messageCallback != null) {
                String groupId = parts[1];
                List<String> files = parts[2].isEmpty()
                        ? Collections.emptyList()
                        : Arrays.asList(parts[2].split(","));
                messageCallback.onGroupFileListReceived(groupId, files);
            }
        }

        /* A. 收到文件邀请 → 只弹一次“保存到哪儿” */
        else if (message.startsWith("FILE_OFFER ")) {
            if (incomingSave != null)
            { sendCommand("FILE_DENY " + username); return; }

            String[] p = message.split(" ", 5);
            String to   = p[1];
            String from = p[2];
            String FileName = p[3];
            String size = p[4];

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(from + " 想发送文件 (" + size + " bytes)");
            fc.setSelectedFile(new File(FileName));

            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                incomingPeer = from;           // 发送者昵称
                incomingSave = fc.getSelectedFile();
                // ★ ACK 填发送者昵称
                sendCommand("FILE_ACCEPT " + from);
            } else {
                sendCommand("FILE_DENY " + from);
            }
        }


        /* B. 我（发送端）收到对方的 ACCEPT */
        else if (message.startsWith("FILE_ACCEPT ")) {

            if (pendingPeer == null || pendingFile == null) {
                System.out.println("Peer="+pendingPeer+" File="+pendingFile);
                System.err.println("⚠️ 没有待发送文件，却收到了 FILE_ACCEPT");
                return;
            }

            try {
                int    port = FileTransferManager.serveFileOnce(pendingFile);
                String ip   = InetAddress.getLocalHost().getHostAddress();

                /* ★★★ 在第 2 段加上自己的昵称 (myName / username) ★★★ */
                String cmd = "FILE_CONNECT "
                        + pendingPeer      // 字段 1: 目标 (toUser)
                        + " " + username     // 字段 2: 发送者 (fromUser)
                        + " " + ip
                        + " " + port
                        + " " + pendingFile.getName();

                System.out.println("SEND>> " + cmd);
                sendCommand(cmd);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "文件发送失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                pendingPeer = null;
                pendingFile = null;
            }
        }


        /* C. 对方拒绝 */
        else if (message.startsWith("FILE_DENY ")) {
            // 直接清空发送端状态
            pendingPeer = null;
            pendingFile = null;
            JOptionPane.showMessageDialog(null, "对方拒绝接收文件");
        }

        /* D. 我（接收端）收到连接信息 → 开始下载 */
        /* D. 收到连接信息 → 开始下载 */
        else if (message.startsWith("FILE_CONNECT ")) {
            // FILE_CONNECT <toUser> <fromUser> <ip> <port> <fileName>
            String[] p = message.split(" ", 6);
            String toUser   = p[1];          // lty
            String fromUser = p[2];          // ltytest
            String ip       = p[3];
            int    port     = Integer.parseInt(p[4]);

            /* ★ 校验：fromUser 必须等于我们之前记下的 incomingPeer */
            if (!fromUser.equals(incomingPeer) || incomingSave == null) {
                System.out.println("拦截: "+fromUser+" vs "+incomingPeer);
                return;
            }

            new Thread(() -> {
                try {
                    System.out.println("[DEBUG] try connect "+ip+":"+port);
                    FileTransferManager.receiveFile(ip, port, incomingSave);
                    JOptionPane.showMessageDialog(null,
                            "文件接收完成，已保存到:\n" + incomingSave.getAbsolutePath());
                } catch (Exception ex) {
                    if (incomingSave.exists()) incomingSave.delete();
                    JOptionPane.showMessageDialog(null,
                            "文件接收失败: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } finally {
                    incomingPeer = null;
                    incomingSave = null;
                }
            }).start();
        }



    }
    //向服务器发送聊天消息


    public void sendMessage(String recipient, String content) {
        if (isConnected) {
            // 消息格式: SEND:接收者:内容
            sendCommand("SEND:" + recipient + ":" + content);
            MessageLogger.write(this.username, recipient, this.username, content);
            System.out.println("1");
        }
    }

    // 生成 6 位友好 ID 的小工具
    private static String randomGroupId() {
        return String.format("%06d", (int)(Math.random() * 1_000_000));
    }

    /** 客户端建群：内部拼好指令并发送 */
    public void createGroup(String groupName, String creator) {
        String id = randomGroupId();
        String cmd = "CREATE_GROUP|" + id + "|" + groupName + "|" + creator + "|" + creator;
        sendCommand(cmd);
    }

    /** 客户端加群 */
    public void joinGroup(String groupId, String username) {
        sendCommand("JOIN_GROUP|" + groupId + "|" + username);
    }


    //public void LoginUser(String username) {sendCommand("LOGIN:" + username);}

    public void registerNewUser(String username, String password)
    {
        System.out.println("ChatClient (" + this.username + "): 准备注册新用户: " + username);
        sendCommand("REGISTER:" + username + ":" + password);
    }

    //这是一个私有的辅助方法，用于将格式化后的命令字符串发送到服务器
    public void sendCommand(String command) {
        try {
            out.write(command);   // 写入缓冲区
            out.newLine();    // ① *一定* 要有行尾分隔，否则 readLine() 永远阻塞
            out.flush();      // ② *一定* flush，别等缓冲区自己满
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }
// 修改原有的 sendCommand 方法，使其返回 boolean 并增加日志

    public boolean isConnected() {
        return isConnected;
    }
    // ChatClient.java


    public List<String> listGroupFiles(String groupId) {
        sendCommand("GROUP_FILE_LIST_REQUEST|" + groupId);  // 同步/异步由你决定
        try {
            String resp = in.readLine();       // 阻塞读一行
            if (resp != null && resp.startsWith("FILES|")) {
                String csv = resp.substring(6);   // 去掉 "FILES|"
                if (csv.isEmpty()) return Collections.emptyList();
                return Arrays.asList(csv.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }



    /* === 向群上传文件 === */
    public void uploadGroupFile(String gid, File f) {
        sendCommand("GROUP_FILE_UPLOAD "+gid+" "+f.getName()+" "+f.length());
        // 等服务器返回 "FILE_PORT ..."
        new Thread(() -> {
            try { FileTransferManager.uploadGroup(gid, f); }
            catch(Exception ex){ ex.printStackTrace(); }
        }).start();
    }

    /* === 私聊点对点发文件 === */
    public void sendFileToUser(String toUser, File f) {
        if (pendingFile != null) {
            JOptionPane.showMessageDialog(null, "已有文件发送中，稍后再试"); return;
        }
        pendingPeer = toUser.trim();   // 对方
        pendingFile = f;

        // ★ 把自己的昵称 (myName) 作为第 2 段附带
        sendCommand("FILE_OFFER " + toUser + " " + username + " " + f.getName() + " " + f.length());
    }


}


