package online;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


public class HiloCliente extends Thread {

    private static final String MSG_CONECTADO = "CONECTADO";
    private static final String MSG_EMPEZAR = "EMPEZAR";
    private static final String MSG_DESCONECTADO = "DESCONECTADO";
    private static final String MSG_TABLERO = "TABLERO";
    private static final String MSG_SERVIDOR_CERRADO = "SERVIDORCERRADO";
    private static final String MSG_CLIENTE_DESCONECTADO = "CLIENTEDESCONECTADO";
    private static final String MSG_SERVIDOR_LLENO = "SERVIDOR_LLENO";
    private static final String MSG_PING = "PING";
    private static final String MSG_PONG = "PONG";
    private static final String MSG_BASURA = "BASURA";

    private static final int SOCKET_TIMEOUT_MS = 1000;
    private static final long SERVER_TIMEOUT_MS = 7000L;
    private static final long PING_INTERVAL_MS = 1500L;

    private DatagramSocket socket;
    private int serverPort = 5555;
    private String ipServerStr = "255.255.255.255";
    private InetAddress ipServer;
    private boolean end = false;
    private ControladorJuego controladorJuego;
    private int id;
    private long ultimoMensajeServidorMs = System.currentTimeMillis();
    private long ultimoPingMs = 0L;
    private boolean desconexionNotificada = false;
    private long inicioIntentoMs = System.currentTimeMillis();
    private boolean conectado = false;

    public HiloCliente(ControladorJuego controladorJuego) {
        try {
            this.controladorJuego = controladorJuego;
            ipServer = InetAddress.getByName(ipServerStr);
            socket = new DatagramSocket();
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        } catch (SocketException | UnknownHostException e) {
//            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!end) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                ultimoMensajeServidorMs = System.currentTimeMillis();
                processMessage(packet);
            } catch (SocketTimeoutException e) {
                // Sin mensajes en este tick
            } catch (IOException e) {
                if (end) break;
            }
            long ahora = System.currentTimeMillis();
            if (!end) {
                enviarPingSiCorresponde(ahora);
                verificarServidor(ahora);
            }
        }
    }

    private void processMessage(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();
        if (message.isEmpty()) return;
        String[] parts = message.split(":", 7);

        System.out.println("Mensaje recibido: " + message);

        switch (parts[0]) {
            case MSG_CONECTADO:
                if (parts.length > 1 && controladorJuego != null) {
                    id = Integer.parseInt(parts[1]);
                    conectado = true;
                    controladorJuego.conectar(id);
                }
                break;
            case MSG_PONG:
                break;
            case MSG_EMPEZAR:
                if (controladorJuego != null) {
                    controladorJuego.empezar();
                }
                break;
            case MSG_TABLERO:
                if (parts.length >= 6 && controladorJuego != null) {
                    int jugador = Integer.parseInt(parts[1]);
                    String tablero = parts[2];
                    int puntaje = Integer.parseInt(parts[3]);
                    int lineas = Integer.parseInt(parts[4]);
                    boolean gameOver = Boolean.parseBoolean(parts[5]);
                    boolean ko = parts.length >= 7 && Boolean.parseBoolean(parts[6]);
                    controladorJuego.actualizarTableroRemoto(jugador, tablero, puntaje, lineas, gameOver, ko);
                }
                break;
            case MSG_SERVIDOR_CERRADO:
                if (controladorJuego != null) {
                    controladorJuego.servidorCerrado();
                }
                break;
            case MSG_SERVIDOR_LLENO:
                if (controladorJuego != null) {
                    controladorJuego.servidorLleno();
                }
                break;
            case MSG_CLIENTE_DESCONECTADO:
                if (controladorJuego != null) {
                    int jugador = -1;
                    if (parts.length > 1) {
                        jugador = Integer.parseInt(parts[1]);
                    }
                    controladorJuego.clienteDesconectado(jugador);
                }
                break;
            case MSG_BASURA:
                if (parts.length >= 3 && controladorJuego != null) {
                    int objetivo = Integer.parseInt(parts[1]);
                    int cantidad = Integer.parseInt(parts[2]);
                    controladorJuego.recibirBasuraDirigida(objetivo, cantidad);
                }
                break;
        }
    }

    public void sendMessage(String message) {
        if (socket == null || socket.isClosed()) return;
        byte[] byteMessage = message.getBytes();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, ipServer, serverPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminate() {
        if (end) return;
        sendMessage(MSG_DESCONECTADO + ":" + id);
        this.end = true;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        this.interrupt();
    }

    private void enviarPingSiCorresponde(long ahora) {
        if (ahora - ultimoPingMs >= PING_INTERVAL_MS) {
            sendMessage(MSG_PING);
            ultimoPingMs = ahora;
        }
    }

    private void verificarServidor(long ahora) {
        if (desconexionNotificada) return;
        if (!conectado && (ahora - inicioIntentoMs) >= 3000L) {
            desconexionNotificada = true;
            if (controladorJuego != null) {
                controladorJuego.volverAlMenu();
            }
            terminate();
            return;
        }
        if (ahora - ultimoMensajeServidorMs >= SERVER_TIMEOUT_MS) {
            desconexionNotificada = true;
            if (controladorJuego != null) {
                controladorJuego.volverAlMenu();
            }
            terminate();
        }
    }
}
