import java.io.*;
import java.net.*;

public class FileTransferManager {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int FILE_PORT = 9000;          // 群共享文件端口

    /* ===== 群文件上传 ===== */
    public static void uploadGroup(String gid, File f) throws IOException {
        try(Socket s=new Socket(SERVER_ADDRESS, FILE_PORT);
            DataOutputStream out=new DataOutputStream(s.getOutputStream());
            FileInputStream  in =new FileInputStream(f)){
            out.writeUTF("UPLOAD "+gid+" "+f.getName()+" "+f.length());
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1) out.write(buf,0,n);
        }
    }


    /* ===== 群文件下载 ===== */
    public static void downloadGroup(String gid,String name,File save) throws IOException{
        try(Socket s=new Socket(SERVER_ADDRESS, FILE_PORT);
            DataOutputStream out=new DataOutputStream(s.getOutputStream());
            DataInputStream  in =new DataInputStream(s.getInputStream());
            FileOutputStream fos=new FileOutputStream(save)){
            out.writeUTF("DOWNLOAD "+gid+" "+name);
            long len=in.readLong(), r=0; byte[] buf=new byte[8192];
            while(r<len){ int n=in.read(buf); fos.write(buf,0,n); r+=n; }
        }
    }

    /* ====== 私聊：发起端监听并发送文件 ====== */
    /* 发送端：serveFileOnce */
    public static int serveFileOnce(File f) throws IOException {
        ServerSocket ss = new ServerSocket(0);
        System.out.println("[DEBUG] serveFileOnce listen port=" + ss.getLocalPort());

        new Thread(() -> {
            try (Socket peer = ss.accept();
                 FileInputStream fis = new FileInputStream(f);
                 OutputStream os = peer.getOutputStream()) {

                System.out.println("[DEBUG] accepted, start sending " + f.getName());
                byte[] buf = new byte[8192]; int n, total = 0;
                while ((n = fis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                    total += n;
                }
                System.out.println("[DEBUG] send finished, bytes=" + total);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "ServeFile").start();
        return ss.getLocalPort();
    }

    /* 接收端：receiveFile */
    public static void receiveFile(String ip, int port, File save) throws IOException {
        System.out.println("[DEBUG] try connect " + ip + ":" + port);
        try (Socket s = new Socket(ip, port);
             InputStream in = s.getInputStream();
             FileOutputStream fos = new FileOutputStream(save)) {

            byte[] buf = new byte[8192]; int n, total = 0;
            while ((n = in.read(buf)) != -1) {
                fos.write(buf, 0, n);
                total += n;
            }
            System.out.println("[DEBUG] recv finished, bytes=" + total);
        }
    }

}
