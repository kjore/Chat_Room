import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Group {
    private String id; // 群聊的唯一ID
    private String groupName; // 群聊名称
    private String creatorUsername; // 创建者用户名
    private List<String> members; // 成员用户名列表

    // 存储所有群组的列表
   public static List<Group> groupList = new ArrayList<>();


    /**
     * 构造方法 - 创建一个新的群聊
     * @param id 群组ID
     * @param groupName 群聊名称
     * @param creatorUsername 创建者用户名
     * @param memberArray 成员数组
     */
    public Group(String id, String groupName, String creatorUsername, String[] memberArray) {
        this.id = id;
        this.groupName = groupName;
        this.creatorUsername = creatorUsername;
        this.members = new ArrayList<>();

        // 确保创建者在成员列表中
        if (!members.contains(creatorUsername)) {
            members.add(creatorUsername);
        }

        // 添加其他成员
        for (String member : memberArray) {
            if (!members.contains(member) && !member.isEmpty()) {
                members.add(member);
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getGroupName() { return groupName; }
    public String getCreatorUsername() { return creatorUsername; }
    public List<String> getMembers() { return new ArrayList<>(members); }

    // 成员管理方法
    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public boolean removeMember(String username) {
        return members.remove(username);
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    // 保存所有群组到文件
    public static void saveGroupsToFile(String fileName) {
        try {
            // 确保文件目录存在
            File file = new File(fileName);
            if (!file.exists()) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
            }

            // 写入文件
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Group group : groupList) {
                    // 格式: ID,群名,创建者,成员1,成员2,...
                    StringBuilder line = new StringBuilder();
                    line.append(group.id).append(",")
                            .append(group.groupName).append(",")
                            .append(group.creatorUsername);

                    // 添加所有成员
                    for (String member : group.members) {
                        line.append(",").append(member);
                    }

                    writer.write(line.toString());
                    writer.newLine();
                }
                System.out.println("群组数据已保存到: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("保存群组数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 从文件加载所有群组
    public static void loadGroupsFromFile(String fileName) {
        groupList.clear(); // 清空现有列表

        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("群组文件不存在，将创建新文件: " + fileName);
            saveGroupsToFile(fileName); // 创建一个空文件
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length >= 3) { // 至少有ID、群名和创建者
                    String id = parts[0].trim();
                    String groupName = parts[1].trim();
                    String creatorName = parts[2].trim();

                    // 提取成员列表
                    String[] members = new String[parts.length - 3];
                    for (int i = 3; i < parts.length; i++) {
                        members[i - 3] = parts[i].trim();
                    }

                    Group group = new Group(id, groupName, creatorName, members);
                    groupList.add(group);
                }
            }
            System.out.println("已从文件加载 " + groupList.size() + " 个群聊");
        } catch (IOException e) {
            System.err.println("加载群组数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 根据ID查找群组
    public static Group findGroupById(String groupId) {
        for (Group group : groupList) {
            if (group.getId().equals(groupId)) {
                return group;
            }
        }
        return null;
    }

    // 根据名称查找群组
    public static Group findGroupByName(String groupName) {
        for (Group group : groupList) {
            if (group.getGroupName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    // 获取所有群组
    public static List<Group> getAllGroups() {
        return new ArrayList<>(groupList);
    }

    // 添加群组到列表
    public static void addGroup(Group group) {
        groupList.add(group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return groupName;
    }
}