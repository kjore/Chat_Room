import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class user implements Serializable {
    private String name;
    private String password;
    private boolean status = false; // 用户状态，默认不在线
    public static List<user> userList = new ArrayList<>();

    public static List<user> onlineUserList = new ArrayList<>();
    public user(String name, String password, boolean status) {
        this.name = name;
        this.password = password;
        this.status = status;
    }
    public boolean isStatus() {
        return status;
    }


    public void setStatus(boolean status) {
        this.status = status;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static List<user> getOnlineUserList() {
        return onlineUserList;
    }

    public boolean checkPassword(String name, String password) {
        for (user u : userList) {
            if (u.getName().equals(name) && u.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addUser(user u) {
        userList.add(u);
        return true; // 注册成功
    }
    @Override
    public String toString() {
        return name + "," + password + "," + status;
    }
    public static void saveUsersToFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (user u : userList) {
                writer.write(u.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 根据您的错误处理策略，您可能希望在此处抛出异常或记录错误
        }
    }

    // 从文本文件加载用户数据
    public static void loadUsersFromFile(String fileName) {
        userList.clear(); // 清空现有列表，以避免重复加载
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) { // 跳过空行
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String name = parts[0].trim();
                    String password = parts[1].trim();
                    boolean status = Boolean.parseBoolean(parts[2].trim());
                    userList.add(new user(name, password, status));
                } else {
                    System.err.println("警告: 文件 '" + fileName + "' 中发现格式不正确的行: " + line);
                    // 您可以选择记录此错误或采取其他操作
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 根据您的错误处理策略，您可能希望在此处抛出异常或记录错误
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("警告: 文件 '" + fileName + "' 中有行数据不足。");
            e.printStackTrace();
        }
    }
}
