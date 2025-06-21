public class Host extends user {
    private ChatClient chatClient;

    public Host(String name, String password,boolean status) {
        super(name, password, status);
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}

