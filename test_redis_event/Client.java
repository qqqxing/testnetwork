package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

	public static void forceRW(Socket s) throws IOException {
		OutputStream out = s.getOutputStream();
		DataOutputStream w = new DataOutputStream(out);
		DataInputStream r = new DataInputStream(s.getInputStream());

		int buf_len = 1024; // 1KB

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
			Socket s = new Socket("127.0.0.1", 9000);
			forceRW(s);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
