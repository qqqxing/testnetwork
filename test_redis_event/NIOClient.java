package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOClient {

	/* 标识数字 */
	//private static int flag = 0;
	/* 缓冲区大小 */
	private static int BLOCK = 50;
	/* 接受数据缓冲区 */
	private static ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK);
	/* 发送数据缓冲区 */
	private static ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK);
	/* 服务器端地址 */
	private final static InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(
			"127.0.0.1", 9000);

	public static void main(String[] args) throws IOException {

		int clientCount = 400;

		int rwCount = 4000000;

		int currentReadCount = 0;
		int currentWriteCount = 0;

		// 打开选择器
		Selector selector = Selector.open();

		SocketChannel[] channels = new SocketChannel[clientCount];
		// int[] readCount = new int[clientCount];
		// int[] writeCount = new int[clientCount];

		for (int i = 0; i < channels.length; i++) {
			channels[i] = SocketChannel.open();
			channels[i].configureBlocking(false);
			channels[i].register(selector, SelectionKey.OP_CONNECT);
			channels[i].connect(SERVER_ADDRESS);
			channels[i].socket().setKeepAlive(true);
			channels[i].socket().setTcpNoDelay(true);
			
		}

//		// 打开socket通道
//		SocketChannel socketChannel = SocketChannel.open();
//		// 设置为非阻塞方式
//		socketChannel.configureBlocking(false);
//		// 注册连接服务端socket动作
//		socketChannel.register(selector, SelectionKey.OP_CONNECT);
//		// 连接
//		socketChannel.connect(SERVER_ADDRESS);
//		// 分配缓冲区大小内存
//
//		SocketChannel socketChannel2 = SocketChannel.open();
//		socketChannel2.configureBlocking(false);
//		socketChannel2.register(selector, SelectionKey.OP_CONNECT);
//		socketChannel2.connect(SERVER_ADDRESS);

		Set<SelectionKey> selectionKeys;
		Iterator<SelectionKey> iterator;
		SelectionKey selectionKey;
		SocketChannel client;

		byte[] tempbuf = new byte[BLOCK];
		for (int i = 0; i < tempbuf.length; i++) {
			tempbuf[i] = (byte) (i % 128);
		}
		sendbuffer.put(tempbuf);

		int count =0;
		
		long start = -1;
		
		while (!(currentReadCount > rwCount && currentWriteCount > rwCount)) {
			// 选择一组键，其相应的通道已为 I/O 操作准备就绪。
			// 此方法执行处于阻塞模式的选择操作。
			selector.select();
			// 返回此选择器的已选择键集。
			selectionKeys = selector.selectedKeys();
			// System.out.println(selectionKeys.size());
			iterator = selectionKeys.iterator();
			if(start < 0){
				start = System.currentTimeMillis();
			}
			while (iterator.hasNext()) {
				selectionKey = iterator.next();
				if (selectionKey.isConnectable()) {
					System.out.println("client connect");
					client = (SocketChannel) selectionKey.channel();
					// 判断此通道上是否正在进行连接操作。
					// 完成套接字通道的连接过程。
					if (client.isConnectionPending()) {
						client.finishConnect();
						// System.out.println("完成连接!");
						// sendbuffer.clear();
						// sendbuffer.put("Hello,Server".getBytes());
						// sendbuffer.flip();
						// client.write(sendbuffer);
					}
					client.register(selector, SelectionKey.OP_WRITE);
				} else if (selectionKey.isWritable()) {
					// sendbuffer.clear();
					client = (SocketChannel) selectionKey.channel();
					// sendText = "message from client--" + (flag++);
					// sendbuffer.put(sendText.getBytes());
					// 将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位
					sendbuffer.flip();
					client.write(sendbuffer);
					// System.out.println("客户端向服务器端发送数据--："+sendText);
					client.register(selector, SelectionKey.OP_READ);
					currentWriteCount++;
				} else if (selectionKey.isReadable()) {
					client = (SocketChannel) selectionKey.channel();
					// 将缓冲区清空以备下次读取
					receivebuffer.clear();
					// 读取服务器发送来的数据到缓冲区中
					count = client.read(receivebuffer);
					if (count == -1) {
						selectionKey.cancel();
					}
					if (count > 0) {
						// receiveText = new String(
						// receivebuffer.array(),0,count);
						// System.out.println("客户端接受服务器端数据--:"+receiveText);
						client.register(selector, SelectionKey.OP_WRITE);
						currentReadCount++;
					}

				}
			}
			selectionKeys.clear();
		}
		
		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(currentWriteCount + " writes and " + currentReadCount + " reads in " + time + " ms.");

		for (int i = 0; i < channels.length; i++) {
			channels[i].close();
		}

	}
}
