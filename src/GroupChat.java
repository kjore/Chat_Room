import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupChat implements Serializable {
    private String groupName;
    private String owner; // 群主名称
    private Set<String> members; // 群成员名称集合
    private static List<GroupChat> groupList = new ArrayList<>();

    public GroupChat(String groupName, String owner, Set<String> members) {
        this.groupName = groupName;
        this.owner = owner;
        this.members = members;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getOwner() {
        return owner;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void addMember(String username) {
        this.members.add(username);
    }

    public void removeMember(String username) {
        this.members.remove(username);
    }

    public static List<GroupChat> getGroupList() {
        return groupList;
    }

    public static void addGroup(GroupChat group) {
        groupList.add(group);
    }

    public static List<GroupChat> getUserGroups(String username) {
        List<GroupChat> userGroups = new ArrayList<>();
        for (GroupChat group : groupList) {
            if (group.getMembers().contains(username)) {
                userGroups.add(group);
            }
        }
        return userGroups;
    }

    public static GroupChat getGroupByName(String groupName) {
        for (GroupChat group : groupList) {
            if (group.getGroupName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    // 保存群聊信息到文件
    public static void saveGroupsToFile(String fileName) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(groupList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从文件加载群聊信息
    @SuppressWarnings("unchecked")
    public static void loadGroupsFromFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            groupList = (List<GroupChat>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
