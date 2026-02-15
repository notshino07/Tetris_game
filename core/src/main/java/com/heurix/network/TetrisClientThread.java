package com.heurix.network;

import com.heurix.interfaces.GameController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TetrisClientThread extends Thread {

    private DatagramSocket socket;
    private int serverPort = 5555;
    private String ipServerStr = "255.255.255.255";
    private InetAddress ipServer;
    private boolean end = false;
    private GameController gameController;

    public TetrisClientThread(GameController gameController) {
        try {
            this.gameController = gameController;
            ipServer = InetAddress.getByName(ipServerStr);
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        sendMessage("Connect");
        if (gameController != null) {
            gameController.attachClientThread(this);
        }
        do {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(packet);
                processMessage(packet);
            } catch (IOException e) {
                if (!end) {
                    throw new RuntimeException(e);
                }
            }
        } while(!end);
    }

    private void processMessage(DatagramPacket packet) {
        String message = (new String(packet.getData())).trim();
        String[] parts = message.split(":");
        System.out.println("[Cliente] Mensaje recibido: " + message);
        switch(parts[0]) {
            case "AlreadyConnected":
                System.out.println("[Cliente] Ya estás conectado");
                break;
            case "Connected":
                if (gameController != null && parts.length > 1) {
                    gameController.connect(Integer.parseInt(parts[1]));
                }
                break;
            case "Full":
                System.out.println("[Cliente] Servidor lleno");
                terminate();
                break;
            case "Start":
                if (gameController != null) {
                    gameController.start();
                }
                break;
            case "PIEZA":
                if (parts.length > 1 && gameController != null) {
                    String tipo = parts[1].trim();
                    System.out.println("[Cliente] Pieza recibida: " + tipo);
                    gameController.onPieceReceived(tipo);
                }
                break;
            case "TABLERO":
                if (parts.length > 1 && gameController != null) {
                    int playerNum = Integer.parseInt(parts[1]);
                    String data = message.substring(message.indexOf(":", message.indexOf(":") + 1) + 1);
                    gameController.onBoardUpdate(playerNum, data);
                }
                break;
            case "BASURA":
                if (parts.length > 1 && gameController != null) {
                    int cantidad = Integer.parseInt(parts[1].trim());
                    gameController.onGarbage(cantidad);
                }
                break;
            case "GANASTE":
                if (gameController != null) {
                    gameController.onWin();
                }
                break;
            case "Disconnect":
                if (gameController != null) {
                    gameController.attachClientThread(null);
                }
                terminate();
                break;
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

    public void sendMove(Move move) {
        sendMessage("Move:" + move.name());
    }

    public void terminate() {
        this.end = true;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        this.interrupt();
    }

    public enum Move {
        LEFT,
        RIGHT,
        ROTATE,
        DROP,
        SOFT_DROP
    }
}
