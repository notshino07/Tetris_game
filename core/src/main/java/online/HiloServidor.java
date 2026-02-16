package online;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class HiloServidor extends Thread {

    private static final String MSG_CONECTADO = "Conectado";
    private static final String MSG_CONECTADO_OK = "CONECTADO";
    private static final String MSG_SERVIDOR_LLENO = "SERVIDOR_LLENO";
    private static final String MSG_EMPEZAR = "EMPEZAR";
    private static final String MSG_DESCONECTADO = "DESCONECTADO";
    private static final String MSG_CLIENTE_DESCONECTADO = "CLIENTEDESCONECTADO";
    private static final String MSG_SERVIDOR_CERRADO = "SERVIDORCERRADO";
    private static final String MSG_PING = "PING";
    private static final String MSG_PONG = "PONG";
    private static final String MSG_TABLERO = "TABLERO";
    private static final String MSG_BASURA = "BASURA";

    private DatagramSocket socket;
    private int serverPort = 5555;
    private boolean end = false;
    private final int MAX_CLIENTS = 2;
    private int connectedClients = 0;
    private ArrayList<Cliente> clients = new ArrayList<>();
    private ControladorJuego controladorJuego;
    private boolean partidaIniciada = false;

    public HiloServidor(ControladorJuego controladorJuego) {
        this.controladorJuego = controladorJuego;
        try {
            socket = new DatagramSocket(serverPort);
        } catch (SocketException e) {
            end = true;
        }
    }

    @Override
    public void run() {
        if (socket == null) {
            System.out.println("No se pudo iniciar el servidor: puerto ocupado.");
            return;
        }
        System.out.println("Servidor iniciado, esperando jugadores...");
        while (!end) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                procesarMensaje(packet);
            } catch (IOException e) {
                if (end) {
                    break;
                }
            }
        }
    }

    private void procesarMensaje(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();
        if (message.isEmpty()) return;
        String[] parts = message.split(":");
        int index = findClientIndex(packet);

        System.out.println("Mensaje recibido " + message);

        if (parts[0].equals(MSG_PING)) {
            sendMessage(MSG_PONG, packet.getAddress(), packet.getPort());
            return;
        }

        if (parts[0].equals(MSG_CONECTADO)) {
            if (index != -1) {
                Cliente existente = clients.get(index);
                sendMessage(MSG_CONECTADO_OK + ":" + existente.getNum(), packet.getAddress(), packet.getPort());
                return;
            }
            if (connectedClients >= MAX_CLIENTS && !partidaIniciada) {
                disconnectClients();
            }
            if (connectedClients >= MAX_CLIENTS) {
                sendMessage(MSG_SERVIDOR_LLENO, packet.getAddress(), packet.getPort());
                return;
            }

            connectedClients++;
            Cliente nuevo = new Cliente(connectedClients, packet.getAddress(), packet.getPort());
            clients.add(nuevo);
            sendMessage(MSG_CONECTADO_OK + ":" + connectedClients, packet.getAddress(), packet.getPort());
            if (controladorJuego != null) {
                controladorJuego.conexion(connectedClients);
            }
            if (connectedClients == MAX_CLIENTS) {
                partidaIniciada = true;
                sendMessageToAll(MSG_EMPEZAR);
            }
            return;
        }

        if (parts[0].equals(MSG_TABLERO)) {
            sendMessageToAll(message);
            return;
        }
        if (parts[0].equals(MSG_BASURA)) {
            sendMessageToAll(message);
            return;
        }

        if (parts[0].equals(MSG_DESCONECTADO)) {
            if (parts.length > 1) {
                sendMessageToAll(MSG_CLIENTE_DESCONECTADO + ":" + parts[1]);
            } else {
                sendMessageToAll(MSG_CLIENTE_DESCONECTADO);
            }
            if (index != -1) {
                clients.remove(index);
            } else {
                clients.clear();
            }
            connectedClients = clients.size();
            partidaIniciada = false;
            if (controladorJuego != null) {
                controladorJuego.desconectado(index != -1 ? (index + 1) : -1);
            }
        }
    }

    private int findClientIndex(DatagramPacket packet) {
        int i = 0;
        int clientIndex = -1;
        while (i < clients.size() && clientIndex == -1) {
            Cliente client = clients.get(i);
            String id = packet.getAddress().toString()+":"+packet.getPort();
            if (id.equals(client.getId())) {
                clientIndex = i;
            }
            i++;

        }
        return clientIndex;
    }

    public void sendMessage(String message, InetAddress clientIp, int clientPort) {
        if (socket == null || socket.isClosed()) return;
        byte[] byteMessage = message.getBytes();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, clientIp, clientPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminate(){
        sendMessageToAll(MSG_SERVIDOR_CERRADO);
        this.end = true;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        this.interrupt();
    }

    public void sendMessageToAll(String message) {
        for (Cliente client : clients) {
            sendMessage(message, client.getIp(), client.getPort());
        }
    }

    public void disconnectClients() {
        for (Cliente client : clients) {
            sendMessage(MSG_DESCONECTADO, client.getIp(), client.getPort());
        }
        this.clients.clear();
        this.connectedClients = 0;
        this.partidaIniciada = false;
    }

    public int getClientesConectados() {
        return connectedClients;
    }

    public boolean isPartidaIniciada() {
        return partidaIniciada;
    }
}
