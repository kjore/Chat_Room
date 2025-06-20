
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username = null;
    private ChatServer server;
    private String lastMessage;

    public String getLastMessage() {
        return lastMessage;
    }
    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            sendMessage("欢迎连接到服务器！");
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();                       // 去掉 \r
                System.out.println("收到原始行: " + line);
                processClientMessage(line);
            }
        } catch (IOException e) {
            String clientInfo = username != null ? "用户 [" + username + "]" :
                    "未登录客户端 [" + clientSocket.getInetAddress().getHostAddress() + "]";
            System.out.println(clientInfo + " 断开连接");
        } finally {
            if (username != null) {
                // 通知用户下线
                username+="_no";
                server.userLogout(username);
            }
            close();
        }
    }

    private void deliverOfflineMsgs(String username) {
        List<String> box = server.getOfflineBox().remove(username);
        System.out.println("deliverOfflineMsgs: box = " + box);// 取出并清空
        if (box != null && !box.isEmpty()) {
            sendMessage("SYSTEM: 您有 " + box.size() + " 条离线消息，正在推送…");
            for (String raw : box) {
                sendMessage(raw);           // raw 仍是原始 "MSG:" 格式
            }
            sendMessage("SYSTEM: 离线消息推送完毕。");
        }
    }

    public void processClientMessage(String message) {
        System.out.println("收到客户端消息：" + message);
        // 记录最后一条消息
        this.lastMessage = message;
        if (message.startsWith("LOGIN:")) {
            // 登录: LOGIN:用户名
            String[] parts = message.split(":", 2);
            if (parts.length >= 2) {
                username = parts[1].trim(); // 去除用户名中的空格
                server.userLogin(username, this);
            }
        }

        else if (message.startsWith("LOGOUT:")) {
            // 登出: LOGOUT:用户名
            String[] parts = message.split(":", 2);
            if (parts.length >= 2) {
                server.userLogout(parts[1]);
            }
        } else if (message.startsWith("SEND:")) {
            // 发送消息: SEND:接收者:内容
            System.out.println("第二步");
            String[] parts = message.split(":", 3);
            System.out.println(Arrays.toString(parts));
            System.out.println(parts.length);
            if (parts.length >= 3 && username != null) {
                server.forwardMessage(username, parts[1], parts[2]);
            }
        } else if (message.startsWith("REGISTER:")) {
            // 注册新用户: REGISTER:用户名:密码
            String[] parts = message.split(":", 3);
            if (parts.length >= 3) {
                server.registerNewUser(parts[1], parts[2]);
            }

        }
        else if(message.startsWith("TOLOGIN"))
        {
            server.userToLogin(this);
        }

        else if(message.startsWith("RETOLOGIN"))
        {
            System.out.println("注册成功返回登陆");
            server.userToLogin(this);
        }
        else if(message.startsWith("CREATE_GROUP|"))
        {
            System.out.println("有新群聊注册");
            server.addGroup(this);
        }
        else if (message.startsWith("JOIN_GROUP|")) {
            // 处理加入群聊请求: JOIN_GROUP|群组ID|用户名
            String[] parts = message.split("\\|", 3);
            if (parts.length >= 3) {
                String groupId = parts[1];
                String joiningUsername = parts[2];
                server.addUserToGroup(joiningUsername, groupId, this);
            }
        }
        /* ========== 私聊文件：发起端 ========== */
        else if (message.startsWith("FILE_OFFER ")) {
            // 格式: FILE_OFFER <toUser> <fileName> <size>
            String[] p = message.split(" ", 4);
            String to = p[1];
            server.forwardToUser(to, message);
        }
        else if (message.startsWith("FILE_ACCEPT ")) {
            // 对端同意接收: FILE_ACCEPT <fromUser>
            String to = message.split(" ")[1];
            server.forwardToUser(to, message);
        }
        else if (message.startsWith("FILE_CONNECT ")) {
            // 发起方告知接收方 IP+端口: FILE_CONNECT <toUser> <ip> <port>
            String to = message.split(" ")[1];
            server.forwardToUser(to, message);
        }
        /* ========== 群文件上传/下载命令 ========== */
        else if (message.startsWith("GROUP_FILE_UPLOAD ") || message.startsWith("GROUP_FILE_DOWNLOAD ")) {
            // 可在此做权限检查，然后直接回复 "OK" 告知客户端连 9000 端口
            sendMessage("FILE_PORT 9000");
        }

        // 列出所有群文件
        else if (message.startsWith("GROUP_FILE_LIST_REQUEST|")) {
            // 客户端发送: GROUP_FILE_LIST_REQUEST|<groupId>
            String[] parts = message.split("\\|", 2);
            if (parts.length == 2) {
                server.listGroupFiles(parts[1],this);
            }
        }

        else if (message.startsWith("GROUP_MSG|")) {
            // 群聊消息: GROUP_MSG|群组ID|消息内容
            String[] parts = message.split("\\|", 3);
            if (parts.length >= 3 && username != null) {
                String groupId = parts[1];
                String content = parts[2];
                System.out.println("测试服务器收到来自群聊的消息："+groupId + "|" + content);
                server.forwardGroupMessage(username, groupId, content);
            }
        }
        System.out.println("SERVER ← CLIENT(processcilentmessage): " + message);
    }

    public void sendMessage(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SERVER -> CLIENT(sendMessage): " + msg);
    }

    void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void forceLogout() {
        sendMessage("ACCOUNT_CONFLICT");
        close();
    }

}
