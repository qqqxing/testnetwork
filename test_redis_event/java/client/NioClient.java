package com.apusic.cache.nioperformancetest.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class NioClient {

    /* 标识数字 */
    // private static int flag = 0;
    /* 缓冲区大小 */
    private static int BLOCK = 5;
    /* 接受数据缓冲区 */
    private static ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK + 4);
    /* 发送数据缓冲区 */
    private static ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK + 4);
    /* 服务器端地址 */
    private final static InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 10001);

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws IOException {

        int clientCount = 50;

        int rwCount = 2000000;

        int currentReadCount = 0;
        int currentWriteCount = 0;

        // 打开选择器
        Selector selector = Selector.open();

        SocketChannel[] channels = new SocketChannel[clientCount];

        for (int i = 0; i < channels.length; i++) {
            channels[i] = SocketChannel.open();
            channels[i].configureBlocking(false);
            channels[i].register(selector, SelectionKey.OP_CONNECT);
            channels[i].connect(SERVER_ADDRESS);
            channels[i].socket().setKeepAlive(true);
            channels[i].socket().setTcpNoDelay(true);

        }

        Set<SelectionKey> selectionKeys;
        Iterator<SelectionKey> iterator;
        SelectionKey selectionKey;
        SocketChannel client;

        byte[] tempbuf = new byte[BLOCK + 4];

        tempbuf[0] = (byte) ((BLOCK >> 24) & 0xFF);
        tempbuf[1] = (byte) ((BLOCK >> 16) & 0xFF);
        tempbuf[2] = (byte) ((BLOCK >> 8) & 0xFF);
        tempbuf[3] = (byte) ((BLOCK) & 0xFF);

        for (int i = 4; i < tempbuf.length; i++) {
            tempbuf[i] = (byte) (i % 128);
        }

        sendbuffer.wrap(tempbuf);

        int count = 0;

        long start = -1;

        while (!(currentReadCount > rwCount && currentWriteCount > rwCount)) {
            // 选择一组键，其相应的通道已为 I/O 操作准备就绪。
            // 此方法执行处于阻塞模式的选择操作。
            selector.select();
            // 返回此选择器的已选择键集。
            selectionKeys = selector.selectedKeys();
            iterator = selectionKeys.iterator();
       
            while (iterator.hasNext()) {
                selectionKey = iterator.next();
                if (selectionKey.isConnectable()) {
                    System.out.println("client connect");
                    client = (SocketChannel) selectionKey.channel();
                    // 判断此通道上是否正在进行连接操作。
                    // 完成套接字通道的连接过程。
                    if (client.isConnectionPending()) {
                        client.finishConnect();
                    }
                    Endpoint endpoint = new Endpoint();
                    endpoint.need = 4;
                    client.register(selector, SelectionKey.OP_WRITE, endpoint);

                } else if (selectionKey.isWritable()) {
                    if (start < 0) {
                        start = System.currentTimeMillis();
                    }
                    client = (SocketChannel) selectionKey.channel();
                    // sendbuffer.flip();
                    while (sendbuffer.hasRemaining()) {
                        client.write(sendbuffer);
                    }
                    sendbuffer.flip();
                    client.register(selector, SelectionKey.OP_READ);
                    currentWriteCount++;

                } else if (selectionKey.isReadable()) {
                    client = (SocketChannel) selectionKey.channel();

                    receivebuffer.clear();
                    count = client.read(receivebuffer);
                    if (count == -1) {
                        selectionKey.cancel();
                    }
                    if (count > 0) {

                        client.register(selector, SelectionKey.OP_WRITE);
                        currentReadCount++;
                    }
                }
                iterator.remove();
            }
            // selectionKeys.clear();
        }

        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println(currentWriteCount + " writes and " + currentReadCount + " reads in " + time + " ms.");

        double req = (double) currentWriteCount;
        double rtime = (double) time;
        rtime = rtime / 1000;
        double p = (req) / rtime;
        
        System.out.println(p + " requests per sec.");
        
        for (int i = 0; i < channels.length; i++) {
            channels[i].close();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

class Endpoint {
    int need = 0;
}
