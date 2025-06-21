import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * 多人聊天服务器（私聊 + 群聊 + 单点在线）
 * 依赖类：user、Group、ClientHandler
 */
public class ChatServer {

    private static final int PORT = 8070;                       // 监听端口
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    /** ★ 在线用户表：用户名 -> 该用户当前唯一连接 */
    private static final Map<String, ClientHandler> ONLINE = new ConcurrentHashMap<>();
    // 离线消息队列：用户名 → 待投递消息列表
    public final Map<String, List<String>> offlineQueue = new ConcurrentHashMap<>();


    /* --- 如果你需要服务器保存群对象可继续使用 --- */
    private final List<Group> groups = new ArrayList<>();

    /* ============================================================= */
    public void start() {
        new Thread(new FileServer()).start();
        System.out.println("聊天服务器已启动 (支持群聊和私聊)，正在监听端口: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新客户端连接: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("服务器错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("正在加载用户数据...");
        user.loadUsersFromFile("users.txt");

        System.out.println("正在加载群组数据...");
        Group.loadGroupsFromFile("groups.txt");

        System.out.println("启动聊天服务器...");
        new ChatServer().start();
    }
    /**
     * 处理用户需要保持在线状态的请求
     * 当同一账号在新地点登录时，原客户端调用此方法保持自己的在线状态
     * @param username 需要保持在线的用户名
     */

    /* =============================================================
       用户登录：加入 ONLINE，如果已有旧连接则踢下线
       ============================================================= */
    public synchronized void userLogin(String username, ClientHandler handler)
    {
        /* ★★★ 单点登录核心逻辑 ★★★ */
        ClientHandler old = ONLINE.put(username, handler);   // 原子替换并取旧值
        if (old != null && old != handler) {
            old.sendMessage("ACCOUNT_CONFLICT");             // 告知旧客户端
            // old.close();                                     // 关闭旧 socket
        }

        System.out.println("用户 " + username + " 已登录 (当前在线数: " + ONLINE.size() + ")");

        // 更新用户列表状态
        for (user u : user.userList) {
            if (u.getName().equals(username)) {
                u.setStatus(true); break;
            }
        }
        user.saveUsersToFile("users.txt");

        System.out.println("登陆时进行发送旧的信息");

        List<String> pending = offlineQueue.remove(username);
        if (pending != null && !pending.isEmpty()) {
            handler.sendMessage("SYSTEM: 您有 " + pending.size() + " 条离线消息，正在推送…");

            for (String raw : pending) {
                handler.sendMessage(raw);           // ★ 直接下发，无需再 split/forward
            }
            handler.sendMessage("SYSTEM: 离线消息推送完毕。");
        }

        sendFullUserList(handler);
        sendAllGroups(handler);
        broadcastToAll("STATUSON: " + username);
    }

    /* =============================================================
       仅把完整用户和群信息发给某个刚连上的客户端
       ============================================================= */
    public void userToLogin(ClientHandler handler) {
        sendFullUserList(handler);
        sendAllGroups(handler);
    }

    private void sendFullUserList(ClientHandler handler) {
        StringBuilder sb = new StringBuilder("USERLIST:");
        for (user u : user.userList) {
            sb.append(u.getName()).append(',')
                    .append(u.getPassword()).append(',')
                    .append(u.isStatus()).append(';');
        }
        handler.sendMessage(sb.toString());
    }

    private void sendAllGroups(ClientHandler handler) {
        for (Group g : Group.groupList) {
            handler.sendMessage("ADDGROUP|" + g.getId() + "|" + g.getGroupName() + "|" +
                    g.getCreatorUsername() + "|" + String.join(",", g.getMembers()));
        }
    }

    /* =============================================================
       用户登出
       ============================================================= */
    public synchronized void userLogout(String username) {

        //判断用户是被顶号还是登出，只有username开头为_no时才表示顶号，此时不除去用户在线
        //取出username后三个字符与_no比较
        if (username.endsWith("_no")) {
            username = username.substring(0, username.length() - 3);
            System.out.println("用户 " + username + " 其他设备登陆");
            return ;}
        ONLINE.remove(username);
        System.out.println("用户 " + username + " 已登出");

        for (user u : user.userList) {
            if (u.getName().equals(username)) {
                u.setStatus(false); break;
            }
        }
        user.saveUsersToFile("users.txt");
        broadcastToAll("STATUSOFF: " + username);
    }

    /* =============================================================
       注册新用户
       ============================================================= */
    public synchronized void registerNewUser(String username, String password) {
        user.userList.add(new user(username, password, false));
        user.saveUsersToFile("users.txt");
        broadcastUserListUpdate();
        System.out.println("新用户注册成功: " + username);
    }

    /* =============================================================
       消息转发
       ============================================================= */
    public void forwardMessage(String sender, String recipient, String content)
    {
        System.out.println("第三步");
        System.out.println("forwardMessage: sender=" + sender + " recipient=" + recipient +
                " online?"+ONLINE.containsKey(recipient));
        String msg = "MSG:" + sender + ":" + recipient + ":" + content;
        ClientHandler recH = ONLINE.get(recipient);
        if (recH != null)
        {
            System.out.println(ONLINE);
            recH.sendMessage(msg);
        }
        else
        {
            // 对方不在线，入队
            offlineQueue
                    .computeIfAbsent(recipient, k->new ArrayList<>())
                    .add(msg);
            System.out.println("→ 已缓存离线消息给 " + recipient + " : " + msg);
        }
    }

    public void forwardGroupMessage(String sender, String groupId, String content) {
        Group g = Group.findGroupById(groupId);
        if (g == null || !g.isMember(sender)) return;

        String msg = "GROUP_MSG|" + sender + "|" + groupId + "|" + g.getGroupName() + "|" + content;
        for (String member : g.getMembers()) {
            ClientHandler h = ONLINE.get(member);
            if (h != null) h.sendMessage(msg);
        }
    }

    /* =============================================================
       群组相关辅助（保持你原来的实现）
       ============================================================= */
    public void addGroup(ClientHandler clientHandler) {
        try {
            // 获取最后一条发送的消息内容
            String message = clientHandler.getLastMessage();

            if (message != null && message.startsWith("CREATE_GROUP|")) {
                // 解析群组信息: CREATE_GROUP|id|groupName|creatorName|members
                String[] parts = message.split("\\|");
                if (parts.length >= 5) {
                    String groupId = parts[1];
                    String groupName = parts[2];
                    String creatorName = parts[3];
                    String[] memberList = parts[4].split(",");

                    // 在服务器端创建群组
                    Group newGroup = new Group(groupId, groupName, creatorName, memberList);
                    Group.addGroup(newGroup);

                    // 保存群组到文件中
                    Group.saveGroupsToFile("groups.txt");

                    System.out.println("新群聊创建成功: " + groupName + " (ID: " + groupId + "), 创建者: " + creatorName);

                    // 通知所有在线的群成员
                    for (String member : newGroup.getMembers()) {
                        ClientHandler memberHandler = ONLINE.get(member);

                        if (memberHandler != null) {
                            // 向群成员发送加入通知
                            memberHandler.sendMessage("GROUP_JOINED|" + groupId + "|" + groupName);
                        }
                    }

                    for (ClientHandler handler3 : ONLINE.values()) {
                        handler3.sendMessage("ADDGROUP|" + groupId + "|" + groupName +"|"+ creatorName +"|"+ String.join(",", memberList));
                    }
                } else {
                    // 数据格式不正确，通知客户端
                    clientHandler.sendMessage("ERROR|群组数据格式不正确");
                }
            }
        } catch (Exception e) {
            System.err.println("处理群组创建请求时出错: " + e.getMessage());
            e.printStackTrace();
            clientHandler.sendMessage("ERROR|创建群组时发生服务器错误");
        }
    }

    public synchronized void addUserToGroup(String username, String groupId, ClientHandler handler) {
        Group g = Group.findGroupById(groupId);
        if (g == null) { handler.sendMessage("ERROR|群组不存在"); return; }
        if (g.isMember(username)) { handler.sendMessage("INFO|已在群中"); return; }

        g.addMember(username); Group.saveGroupsToFile("groups.txt");
        broadcastToAll("ADDGROUP|" + g.getId() + "|" + g.getGroupName() + "|" +
                g.getCreatorUsername() + "|" + String.join(",", g.getMembers()));

        // 通知群成员
        broadcastToMembers(g, "GROUP_MEMBER_JOINED|" + groupId + "|" + g.getGroupName() + "|" + username);
    }

    private void broadcastToMembers(Group g, String msg) {
        for (String m : g.getMembers()) {
            ClientHandler h = ONLINE.get(m);
            if (h != null) h.sendMessage(msg);
        }
    }




    /* =============================================================
       工具：向所有在线客户端广播
       ============================================================= */
    private void broadcastToAll(String msg) {
        for (ClientHandler h : ONLINE.values()) h.sendMessage(msg);
    }

    private void broadcastUserListUpdate() {
        broadcastToAll("USERLIST_UPDATED");
    }

    public void forwardToUser(String username, String msg)//处理文件
    {
        ClientHandler h = ONLINE.get(username);
        if(h!=null)
        {
            System.out.println("对方在线，可以发送");
            h.sendMessage(msg);
        }

    }

    /**
     * 列出某群的所有文件，并通过 handler 发送回客户端
     */
    public synchronized void listGroupFiles(String groupId, ClientHandler handler) {
        File dir = new File("ServerFiles/Groups", groupId);
        String[] files = dir.exists() && dir.isDirectory()
                ? dir.list()
                : new String[0];
        // 格式: GROUP_FILE_LIST|群ID|file1,file2,...
        String joined = String.join(",", files);
        handler.sendMessage("GROUP_FILE_LIST|" + groupId + "|" + joined);
    }


    public Map<String, List<String>> getOfflineBox()
    {
        return offlineQueue;
    }


}
