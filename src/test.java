import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;

//聊天窗口关闭后不能继续接收消息

public class test {
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient("127.0.0.1");//此地址在不同设备登陆时需要改为实际服务器地址  总共有两个需要改
        chatClient.connect(1);

        setGlobalFont(new FontUIResource(new Font("宋体", Font.PLAIN, 20)));
        // 创建登录窗口
        LoginFrame loginFrame = new LoginFrame();
    }
    // 设置全局字体
    public static void setGlobalFont(FontUIResource font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }
}