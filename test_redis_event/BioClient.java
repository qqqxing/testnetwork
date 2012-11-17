package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class BioClient {

	public static void forceRW(Socket s) throws IOException {
		OutputStream out = s.getOutputStream();
		DataOutputStream w = new DataOutputStream(out);
		DataInputStream r = new DataInputStream(s.getInputStream());

		int buf_len = 50; // 1KB

		byte[] buf = new byte[buf_len];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte) (i % 128);
		}
		buf[buf.length - 1] = '\n';
		byte[] rbuf = new byte[buf_len];

		// 1MB ,35ms
		// 50MB read,50MB write,2100ms
		// 100MB read,100MB write 4145ms

		int count = 1024 * 100;

		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			w.write(buf);
			r.read(rbuf);
		}

		long end = System.currentTimeMillis();
		System.out.println("Time : " + (end - start));
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		s.close();
	}

	public static void main(String[] args) {

		try {
			int clients = 400;
			int count = 10000;
			int bufLen = 50;
			Socket[] socket = new Socket[clients];
			for(int i = 0; i < socket.length;i++){
				socket[i] = new Socket("127.0.0.1", 9000);
				socket[i].setKeepAlive(true);
				socket[i].setTcpNoDelay(true);
			}
			
			R[] ts = new R[clients];
			for(int i = 0;i < ts.length;i++){
				ts[i] = new R(socket[i], count, bufLen);
			}
			long start = System.currentTimeMillis();
			for(int i = 0;i < ts.length;i++){
				ts[i].run();
			}
			
			for(int i = 0;i < ts.length;i++){
				ts[i].join();
			}
			
			long end = System.currentTimeMillis();
			
			
			for(int i = 0;i < socket.length;i++){
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
	public R(Socket s, int count,int bufLen) throws IOException {
		this.s = s;
		this.count = count;

		w = new DataOutputStream(s.getOutputStream());
		r = new DataInputStream(s.getInputStream());
		this.bufLen = bufLen;
		
		this.buf = new byte[bufLen];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte) (i % 128);
		}
		this.rbuf = new byte[bufLen];
	}

	public void run() {
		try {

			for (int i = 0; i < this.count; i++) {
				w.write(buf);
				r.read(rbuf);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
