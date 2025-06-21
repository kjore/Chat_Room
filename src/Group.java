import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID; // 用于生成唯一ID，以防名称不唯一

/**
 * 代表一个群聊实体。
 */
public class Group {
    private String id; // 群聊的唯一ID
    private String groupName; // 群聊名称
    private Host creator; // 群聊创建者
    private  List<String> members; // 成员用户名列表
    private String creatorUsername; // 新字段：存储创建者的用户名
    public static List<Group> GroupList = new ArrayList<>();

    public Group(String groupName, String creatorUsername, List<String> initialMembers) {
        this.id = generateFriendlyId(); // 使用友好 ID 生成方法
        this.groupName = groupName;
        this.creatorUsername = creatorUsername;
        this.members = new ArrayList<>();
        if (initialMembers != null) {
            this.members.addAll(initialMembers);
        }
        if (!this.members.contains(creatorUsername)) {
            this.members.add(creatorUsername);
        }
    }
    private String generateFriendlyId() {
        // 生成 6 位数字组合的 ID
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        // 检查 ID 是否已存在
        String newId = sb.toString();
        for (Group group : GroupList) {
            if (group.getId().equals(newId)) {
                // 如果 ID 已存在，递归重新生成
                return generateFriendlyId();
            }
        }

        return newId;
    }

    // === PATCH ‬G-1 : 简易工具方法 ===

    /** 兼容旧代码：返回群聊 ID，等价于 getId() */
    public String getGroupId() {
        return id;
    }

    /** 通过群聊 ID 全局查找，没有则返回 null */
    public static Group findById(String searchId) {
        for (Group g : GroupList) {
            if (g.id.equals(searchId)) return g;
        }
        return null;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }
    public String getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public Host getCreator() {
        return creator;
    }

    /**
     * 获取成员列表的副本。
     * @return 成员用户名列表
     */
    public List<String> getMembers() {
        return new ArrayList<>(members); // 返回副本以防止外部修改
    }

    /**
     * 添加一个成员到群聊。
     * @param username 要添加的用户名
     */
    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }
    public void setId(String id) {
        this.id = id;
    }
    /**
     * 从群聊中移除一个成员。
     * @param username 要移除的用户名
     */
    public void removeMember(String username) {
        members.remove(username);
    }

    /**
     * 检查指定用户是否为群聊成员。
     * @param username 用户名
     * @return 如果是成员则返回 true, 否则 false
     */
    public boolean isMember(String username) {
        return members.contains(username);
    }

    @Override
    public String toString() {
        return groupName; // JList中显示的名称
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id); // 基于ID判断相等性
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    //存入群聊信息保存文件
    public static void saveGroupsToFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Group group : GroupList) {
                // 写入群聊基本信息
                writer.write(group.getId() + "," + group.getGroupName() + "," + group.getCreatorUsername());

                // 写入成员列表
                for (String member : group.getMembers()) {
                    writer.write("," + member);
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 根据错误处理策略，可以在此处抛出异常或记录错误
        }
    }
    public static void loadGroupsFromFile(String fileName) {
        GroupList.clear(); // 清空现有列表

        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("群组文件不存在，将创建新文件: " + fileName);
            try {
                // 确保目录存在
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // 创建空文件
                file.createNewFile();
                return;
            } catch (IOException e) {
                System.err.println("创建群组文件失败: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length >= 3) { // 至少有ID、群名和创建者
                    String id = parts[0].trim();
                    String groupName = parts[1].trim();
                    String creatorUsername = parts[2].trim();

                    // 收集成员列表（从索引3开始）
                    List<String> membersList = new ArrayList<>();
                    for (int i = 3; i < parts.length; i++) {
                        membersList.add(parts[i].trim());
                    }

                    // 创建群聊对象
                    Group group = new Group(groupName, creatorUsername, membersList);
                    // 设置正确的ID
                    try {
                        java.lang.reflect.Field idField = Group.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(group, id);
                    } catch (Exception e) {
                        System.err.println("无法设置群聊ID: " + e.getMessage());
                    }

                    GroupList.add(group);
                }
            }
            System.out.println("已从文件加载 " + GroupList.size() + " 个群组");
        } catch (IOException e) {
            System.err.println("加载群组数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
