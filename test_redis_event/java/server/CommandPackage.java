package com.apusic.cache.nioperformancetest.server;

public class CommandPackage {

    int length = -1;
    byte[] data;
    byte[] lengthBytes = new byte[4];
    
    int writePos = 0;
    
    public CommandPackage(){
        
    }
    
    
    
}
