package online;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class HiloCliente extends Thread {

    private DatagramSocket socket;
    private int serverPort = 5555;
    private String ipServerStr = "255.255.255.255";
    private InetAddress ipServer;
    private boolean end = false;
    private ControladorJuego controladorJuego;

    public HiloCliente(ControladorJuego controladorJuego) {
        try {
            this.controladorJuego = controladorJuego;
            ipServer = InetAddress.getByName(ipServerStr);
            socket = new DatagramSocket();
        } catch (SocketException | UnknownHostException e) {
        }
    }

    @Override
    public void run() {
        do {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(packet);
                processMessage(packet);
            } catch (IOException e) {
                // throw new RuntimeException(e);
            }
        } while(!end);
    }

    private void processMessage(DatagramPacket packet) {
        String message = (new String(packet.getData())).trim();
        String[] parts = message.split(":");
        System.out.println("Mensaje recibido: " + message);
        switch(parts[0]){
        }
    }

    public void sendMessage(String message) {
        byte[] byteMessage = message.getBytes();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, ipServer, serverPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminate() {
        this.end = true;
        socket.close();
        this.interrupt();
    }
}
