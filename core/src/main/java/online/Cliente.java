package online;

import java.net.InetAddress;

public class Cliente {

    private final String id;
    private final int num;
    private final InetAddress ip;
    private final int port;

    public Cliente(int num, InetAddress ip, int port) {
        this.num = num;
        this.ip = ip;
        this.port = port;
        this.id = ip.toString() + ":" + port;
    }

    public String getId() {
        return id;
    }

    public int getNum() {
        return num;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
