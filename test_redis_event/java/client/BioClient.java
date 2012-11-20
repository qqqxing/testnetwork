package com.apusic.cache.nioperformancetest.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class BioClient {

    public static void main(String[] args) {

        try {
            int clients = 10;
            int count = 100000;
            int bufLen = 100;
            Socket[] socket = new Socket[clients];
            for (int i = 0; i < socket.length; i++) {
                socket[i] = new Socket("127.0.0.1", 10001);
                socket[i].setKeepAlive(true);
                socket[i].setTcpNoDelay(true);
            }

            R[] ts = new R[clients];
            for (int i = 0; i < ts.length; i++) {
                ts[i] = new R(socket[i], count, bufLen);
            }
            long start = System.currentTimeMillis();
            for (int i = 0; i < ts.length; i++) {
                ts[i].run();
            }

            for (int i = 0; i < ts.length; i++) {
                ts[i].join();
            }

            long end = System.currentTimeMillis();

            for (int i = 0; i < socket.length; i++) {
                socket[i].close();
            }

            long time = end - start;
            long total = count * clients;
            System.out.println(total + " requests in " + time + " ms.");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}

class R extends Thread {
    final Socket s;
    final int count;
    final DataOutputStream w;
    final DataInputStream r;
    final int bufLen;
    final byte[] buf;
    byte[] rbuf;

    public R(Socket s, int count, int bufLen) throws IOException {
        this.s = s;
        this.count = count;

        w = new DataOutputStream(s.getOutputStream());
        r = new DataInputStream(s.getInputStream());
        this.bufLen = bufLen;

        this.buf = new byte[bufLen + 4];
        
        this.buf[0] = (byte) ((bufLen >> 24) & 0xFF);
        this.buf[1] = (byte) ((bufLen >> 16) & 0xFF);
        this.buf[2] = (byte) ((bufLen >> 8) & 0xFF);
        this.buf[3] = (byte) ((bufLen ) & 0xFF);
        
        for (int i = 4; i < buf.length; i++) {
            buf[i] = (byte) (i % 128);
        }
    }
    
    public int readHead(DataInputStream r) throws IOException{
        byte[] headByte = new byte[4];
        int n = 0;
        int off = 0;
        while(off < 4){
            n = r.read(headByte, off, 4 - off);
            if(n == -1){
                return -1;
            }
            off += n;
        }
        
        int len = ((headByte[0] & 0xFF) << 24) | ((headByte[1] & 0xFF) << 16)
        | ((headByte[2] & 0xFF) << 8) | (headByte[3] & 0xFF);
        return len;
    }
    
    public boolean readBody(DataInputStream r,byte[] body) throws IOException{
        int n = 0;
        int off = 0;
        
        while(off < body.length){
            n = r.read(body, off, body.length - off);
            if( n == -1){
                return false;
            }
            off += n;
        }
        return true;
    }

    public void run() {
        try {

            for (int i = 0; i < this.count; i++) {
                w.write(buf);
                
                int len = readHead(r);
                if(len == -1){
                    return ;
                }
                this.rbuf = null;
                this.rbuf = new byte[len];
                this.readBody(r, rbuf);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
