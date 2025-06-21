import java.io.*;
import java.net.*;

class FileServer implements Runnable {
    private static final int PORT = 9000;
    private final File ROOT = new File("ServerFiles/Groups");

    public void run() {
        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("FileServer 启动, 端口 9000");
            while (true) new Worker(ss.accept()).start();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private class Worker extends Thread {
        private final Socket s;
        Worker(Socket s){ this.s=s; }

        public void run() {
            try (DataInputStream in = new DataInputStream(s.getInputStream());
                 DataOutputStream out= new DataOutputStream(s.getOutputStream())) {

                String head = in.readUTF();                  // UPLOAD gid name size | DOWNLOAD gid name
                String[] p = head.split(" ");
                if ("UPLOAD".equals(p[0])) {
                    String gid=p[1], name=p[2]; long len=Long.parseLong(p[3]);
                    File dir = new File(ROOT,gid); dir.mkdirs();
                    try(FileOutputStream fos=new FileOutputStream(new File(dir,name))) {
                        byte[] buf=new byte[8192]; long r=0;
                        while(r<len){ int n=in.read(buf); fos.write(buf,0,n); r+=n; }
                    }
                } else {                                    // DOWNLOAD
                    File f = new File(new File(ROOT,p[1]), p[2]);
                    out.writeLong(f.length());
                    try(FileInputStream fis=new FileInputStream(f)){
                        byte[] buf=new byte[8192]; int n;
                        while((n=fis.read(buf))!=-1) out.write(buf,0,n);
                    }
                }
            } catch(IOException ignore){}
        }
    }
}

