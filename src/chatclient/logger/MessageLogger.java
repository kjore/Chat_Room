package chatclient.logger;                       // ← 放 src/chatclient/logger/

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MessageLogger {
    private static final String ROOT =
            System.getProperty("user.home") + File.separator + "ChatLogs" + File.separator;
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    public static void write(String loginUser, String convoId,
                             String sender, String text) {
        try {
            File f = new File(ROOT + loginUser, convoId + ".txt");
            f.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(f, true),
                            StandardCharsets.UTF_8))) {
                bw.write("[" + sdf.format(new Date()) + "] "
                        + sender + " : " + text);
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    public static void replay(String loginUser, String convoId,
                              java.util.function.Consumer<String> printer) {
        File f = new File(ROOT + loginUser, convoId + ".txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) printer.accept(line);
        } catch (IOException ignored) {}
    }
}


