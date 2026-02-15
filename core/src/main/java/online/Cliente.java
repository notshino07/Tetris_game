package online;

import java.net.InetAddress;

public class Cliente {

    private final InetAddress ip;
    private final int port;
    private final String id;

    public Cliente(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
        this.id = ip.toString() + ":" + port;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }
}
