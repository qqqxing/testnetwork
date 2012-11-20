package com.apusic.cache.nioperformancetest.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface TCPProtocal {
    
    public void handleAccept(SelectionKey key) throws IOException;
    public void handleRead(SelectionKey key) throws IOException;
    public void handleWrite(SelectionKey key) throws IOException;


}
