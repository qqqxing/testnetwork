package com.apusic.cache.nioperformancetest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class NioServer {

    static final int port = 10001;

    static final int BUF_LEN = 1024;

    static final boolean LINGER = false;
    static final boolean NO_DELAY = true;

    static final boolean KEEP_ALIVE = true;

    static final int RECV_BUF_LEN = 1024 * 4;
    static final int SEND_BUF_LEN = 1024 * 4;
    
    static final long SELECT_TIMEOUT = 3000;    //ms

    public static void aeMain() throws IOException {
        System.out.println("Open selector...");
        Selector selector = Selector.open();
        
        System.out.println("Bind address...");
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        
        System.out.println("Register selector...");
        serverChannel.register(selector,SelectionKey.OP_ACCEPT);
        
        System.out.println("Create protocol...");
        TCPProtocal protocal = new RWProtocal(BUF_LEN);
        
        System.out.println("Going into event loop...");
        while(true){
            if(selector.select(SELECT_TIMEOUT) == 0){
                System.out.print(".");
                continue;
            }
            
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            
            while(keyIter.hasNext()){
                SelectionKey key = keyIter.next();
                
                if(key.isAcceptable()){
                    protocal.handleAccept(key);
                }
                
                if(key.isReadable()){
                    protocal.handleRead(key);
                }
                
                if(key.isValid() && key.isWritable()){
                    protocal.handleWrite(key);
                }
                
                keyIter.remove();
            }
            
        }

    }

    public static void main(String[] args) {

        System.out.println("Hello there!");
        try {
            aeMain();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}