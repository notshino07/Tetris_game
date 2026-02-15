package com.heurix.network;

import java.net.InetAddress;

public class Client {
    private final String id;
    private final int num;
    private final InetAddress ip;
    private final int port;

    public Client(int num, InetAddress ip, int port) {
        this.num = num;
        this.ip = ip;
        this.port = port;
        this.id = ip.toString() + ":" + port;
    }

    public String getId() {
        return id;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getNum() {
        return num;
    }
}
