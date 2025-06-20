import java.io.*;
import java.net.*;

public class FileTransferManager {
    private static final int FILE_PORT = 9000;          // 群共享文件端口

    /* ===== 群文件上传 ===== */
    public static void uploadGroup(String gid, File f) throws IOException {
        try(Socket s=new Socket("127.0.0.1", FILE_PORT);
            DataOutputStream out=new DataOutputStream(s.getOutputStream());
            FileInputStream  in =new FileInputStream(f)){
            out.writeUTF("UPLOAD "+gid+" "+f.getName()+" "+f.length());
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1) out.write(buf,0,n);
        }
    }

    /* ===== 群文件下载 ===== */
    public static void downloadGroup(String gid,String name,File save) throws IOException{
        try(Socket s=new Socket("127.0.0.1", FILE_PORT);
            DataOutputStream out=new DataOutputStream(s.getOutputStream());
            DataInputStream  in =new DataInputStream(s.getInputStream());
            FileOutputStream fos=new FileOutputStream(save)){
            out.writeUTF("DOWNLOAD "+gid+" "+name);
            long len=in.readLong(), r=0; byte[] buf=new byte[8192];
            while(r<len){ int n=in.read(buf); fos.write(buf,0,n); r+=n; }
        }
    }

    /* ====== 私聊：发起端监听并发送文件 ====== */
    public static int serveFileOnce(File f) throws IOException {
        ServerSocket ss = new ServerSocket(0);          // 随机端口
        new Thread(() -> {                              // 后台传输
            try(Socket peer=ss.accept();
                FileInputStream fis=new FileInputStream(f);
                OutputStream os=peer.getOutputStream()){
                byte[] buf=new byte[8192]; int n;
                while((n=fis.read(buf))!=-1) os.write(buf,0,n);
            } catch(IOException ignore){}
        }).start();
        return ss.getLocalPort();
    }

    /* ====== 私聊：接收端连接并保存 ====== */
    public static void receiveFile(String ip,int port,File save) throws IOException{
        try(Socket s=new Socket(ip,port);
            InputStream in=s.getInputStream();
            FileOutputStream fos=new FileOutputStream(save)){
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1) fos.write(buf,0,n);
        }
    }
}
