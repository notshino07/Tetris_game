package com.heurix.network;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class TetrisServerThread extends Thread {

    private DatagramSocket socket;
    private int serverPort = 5555;
    private boolean end = false;
    private final int MAX_CLIENTS = 2;
    private int connectedClients = 0;
    private ArrayList<Client> clients = new ArrayList<Client>();
    private ArrayList<PlayerState> playerStates = new ArrayList<PlayerState>();
    private final ArrayDeque<String> colaPiezas = new ArrayDeque<String>();
    private final Random random = new Random();
    private final List<String> bolsa = Arrays.asList("I", "O", "T", "S", "Z", "J", "L");
    private long lastTick = System.currentTimeMillis();
    private static final int FILAS = 20;
    private static final int COLUMNAS = 10;
    private static final int TICK_MS = 500;

    public TetrisServerThread() {
        try {
            socket = new DatagramSocket(serverPort);
            socket.setSoTimeout(100);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        do {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(packet);
                processMessage(packet);
            } catch (SocketTimeoutException timeout) {
                // se usa para mantener el tick
            } catch (IOException e) {
                if (!end) {
                    throw new RuntimeException(e);
                }
            }
            long now = System.currentTimeMillis();
            if (now - lastTick >= TICK_MS) {
                lastTick = now;
                tick();
            }
        } while(!end);
    }

    private void processMessage(DatagramPacket packet) {
        String message = (new String(packet.getData())).trim();
        String[] parts = message.split(":");
        System.out.println("[Servidor] Mensaje recibido: " + message);
        int index = findClientIndex(packet);

        if (parts[0].equals("Connect")) {
            if (index != -1) {
                sendMessage("AlreadyConnected", packet.getAddress(), packet.getPort());
                return;
            }
            if (connectedClients >= MAX_CLIENTS) {
                sendMessage("Full", packet.getAddress(), packet.getPort());
                return;
            }
            connectedClients++;
            Client newClient = new Client(connectedClients, packet.getAddress(), packet.getPort());
            clients.add(newClient);
            playerStates.add(new PlayerState(connectedClients));
            sendMessage("Connected:" + connectedClients, packet.getAddress(), packet.getPort());
            if (connectedClients == MAX_CLIENTS) {
                sendMessageToAll("Start");
                iniciarSecuencia();
            }
            return;
        }

        if (index == -1) {
            sendMessage("NotConnected", packet.getAddress(), packet.getPort());
            return;
        }

        Client client = clients.get(index);
        PlayerState state = playerStates.get(index);

        switch(parts[0]) {
            case "Move":
                if (parts.length > 1) {
                    applyMove(state, client, parts[1].trim());
                }
                break;
        }
    }

    private void iniciarSecuencia() {
        rellenarBolsa();
        broadcastNextPiece();
        spawnPendingPieces();
        broadcastBoards();
    }

    private void tick() {
        for (int i = 0; i < playerStates.size(); i++) {
            PlayerState state = playerStates.get(i);
            if (state.gameOver) {
                continue;
            }
            if (state.currentPiece == null) {
                spawnPieceFromQueue(state);
            }
            if (state.currentPiece != null && !move(state, 0, -1)) {
                lockPiece(state);
            }
            if (state.gameOver) {
                handleGameOver(state, i);
            }
        }
        broadcastBoards();
    }

    private void handleGameOver(PlayerState state, int playerIndex) {
        if (!state.gameOver) {
            return;
        }
        int otherIndex = getOtherClientIndex(playerIndex);
        if (otherIndex >= 0) {
            sendMessage("GANASTE", clients.get(otherIndex).getIp(), clients.get(otherIndex).getPort());
        }
    }

    private void applyMove(PlayerState state, Client client, String accion) {
        if (state.gameOver || state.currentPiece == null) {
            return;
        }
        switch(accion) {
            case "LEFT":
                move(state, -1, 0);
                break;
            case "RIGHT":
                move(state, 1, 0);
                break;
            case "ROTATE":
                rotate(state);
                break;
            case "DROP":
                hardDrop(state);
                break;
            case "SOFT_DROP":
                move(state, 0, -1);
                break;
        }
        broadcastBoards();
    }

    private boolean move(PlayerState state, int dx, int dy) {
        if (canMove(state, dx, dy)) {
            state.pieceX += dx;
            state.pieceY += dy;
            return true;
        }
        return false;
    }

    private void rotate(PlayerState state) {
        int[][] rotada = rotateMatrix(state.currentPiece.shape);
        if (canMove(state, 0, 0, rotada)) {
            state.currentPiece = new TetrisPiece(state.currentPiece.tipo, rotada);
        }
    }

    private void hardDrop(PlayerState state) {
        while(move(state, 0, -1)) {
            // drop
        }
        lockPiece(state);
    }

    private void lockPiece(PlayerState state) {
        int[][] shape = state.currentPiece.shape;
        for (int i = 0; i < shape.length; i++) {
            int x = state.pieceX + shape[i][0];
            int y = state.pieceY + shape[i][1];
            if (x >= 0 && x < COLUMNAS && y >= 0 && y < FILAS) {
                state.grid[y][x] = state.currentPiece.color;
            }
        }
        int lineas = clearLines(state);
        if (lineas > 0) {
            state.score += lineas * 100;
            enviarBasura(lineas, state);
        }
        state.currentPiece = null;
        broadcastNextPiece();
        spawnPieceFromQueue(state);
        state.gameOver = hasBlocksAbove(state);
    }

    private void spawnPieceFromQueue(PlayerState state) {
        if (state.currentPiece != null) {
            return;
        }
        String tipo = state.pendingPieces.pollFirst();
        if (tipo == null) {
            return;
        }
        state.currentPiece = TetrisPiece.ofTipo(tipo);
        state.pieceX = COLUMNAS / 2;
        state.pieceY = FILAS - 2;
    }

    private boolean hasBlocksAbove(PlayerState state) {
        for (int y = FILAS - 1; y >= FILAS - 2; y--) {
            for (int x = 0; x < COLUMNAS; x++) {
                if (state.grid[y][x] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int clearLines(PlayerState state) {
        int cleared = 0;
        for (int y = 0; y < FILAS; y++) {
            boolean llena = true;
            for (int x = 0; x < COLUMNAS; x++) {
                if (state.grid[y][x] == 0) {
                    llena = false;
                    break;
                }
            }
            if (llena) {
                cleared++;
                for (int row = y; row < FILAS - 1; row++) {
                    state.grid[row] = Arrays.copyOf(state.grid[row + 1], COLUMNAS);
                }
                Arrays.fill(state.grid[FILAS - 1], 0);
                y--;
            }
        }
        return cleared;
    }

    private void enviarBasura(int lineas, PlayerState emitter) {
        int amount = computeBasura(lineas);
        if (amount <= 0) {
            return;
        }
        for (PlayerState state : playerStates) {
            if (state != emitter) {
                applyGarbage(state, amount);
                sendMessageToAll("BASURA:" + amount);
            }
        }
    }

    private void applyGarbage(PlayerState state, int amount) {
        for (int i = 0; i < amount; i++) {
            for (int y = 0; y < FILAS - 1; y++) {
                state.grid[y] = Arrays.copyOf(state.grid[y + 1], COLUMNAS);
            }
            int hole = random.nextInt(COLUMNAS);
            for (int x = 0; x < COLUMNAS; x++) {
                state.grid[FILAS - 1][x] = (x == hole) ? 0 : 7;
            }
        }
        state.gameOver = hasBlocksAbove(state);
    }

    private int computeBasura(int lineas) {
        switch(lineas) {
            case 2: return 1;
            case 3: return 2;
            case 4: return 4;
            default: return 0;
        }
    }

    private void broadcastNextPiece() {
        if (colaPiezas.isEmpty()) {
            rellenarBolsa();
        }
        String tipo = colaPiezas.pollFirst();
        System.out.println("[Servidor] Enviando pieza: " + tipo);
        for (PlayerState state : playerStates) {
            state.pendingPieces.addLast(tipo);
        }
        sendMessageToAll("PIEZA:" + tipo);
        sendMessageToAll("NUEVA_PIEZA:" + tipo);
    }

    private void rellenarBolsa() {
        ArrayList<String> copia = new ArrayList<>(bolsa);
        Collections.shuffle(copia, random);
        colaPiezas.addAll(copia);
    }

    private void spawnPendingPieces() {
        for (PlayerState state : playerStates) {
            spawnPieceFromQueue(state);
        }
    }

    private void broadcastBoards() {
        for (PlayerState state : playerStates) {
            String payload = serializeBoard(state);
            sendMessageToAll("TABLERO:" + state.playerNum + ":" + payload);
        }
    }

    private String serializeBoard(PlayerState state) {
        int[][] snapshot = copyGrid(state.grid);
        if (state.currentPiece != null) {
            int[][] shape = state.currentPiece.shape;
            for (int i = 0; i < shape.length; i++) {
                int x = state.pieceX + shape[i][0];
                int y = state.pieceY + shape[i][1];
                if (x >= 0 && x < COLUMNAS && y >= 0 && y < FILAS) {
                    snapshot[y][x] = state.currentPiece.color;
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int y = FILAS - 1; y >= 0; y--) {
            for (int x = 0; x < COLUMNAS; x++) {
                builder.append(snapshot[y][x]);
            }
            if (y > 0) {
                builder.append("|");
            }
        }
        return builder.toString();
    }

    private int[][] copyGrid(int[][] grid) {
        int[][] copia = new int[FILAS][COLUMNAS];
        for (int y = 0; y < FILAS; y++) {
            copia[y] = Arrays.copyOf(grid[y], COLUMNAS);
        }
        return copia;
    }

    private boolean canMove(PlayerState state, int dx, int dy) {
        return canMove(state, dx, dy, state.currentPiece.shape);
    }

    private boolean canMove(PlayerState state, int dx, int dy, int[][] forma) {
        for (int i = 0; i < forma.length; i++) {
            int x = state.pieceX + forma[i][0] + dx;
            int y = state.pieceY + forma[i][1] + dy;
            if (x < 0 || x >= COLUMNAS || y < 0 || y >= FILAS) {
                return false;
            }
            if (y >= 0 && y < FILAS && state.grid[y][x] != 0) {
                return false;
            }
        }
        return true;
    }

    private int getOtherClientIndex(int index) {
        if (clients.size() < 2) {
            return -1;
        }
        return index == 0 ? 1 : 0;
    }

    private void sendMessage(String message, InetAddress clientIp, int clientPort) {
        byte[] byteMessage = message.getBytes();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, clientIp, clientPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessageToAll(String message) {
        for (Client client : clients) {
            sendMessage(message, client.getIp(), client.getPort());
        }
    }

    private int[][] rotateMatrix(int[][] forma) {
        int[][] rotada = new int[forma.length][2];
        for (int i = 0; i < forma.length; i++) {
            int x = forma[i][0];
            int y = forma[i][1];
            rotada[i][0] = -y;
            rotada[i][1] = x;
        }
        return rotada;
    }

    private class PlayerState {
        final int playerNum;
        final int[][] grid = new int[FILAS][COLUMNAS];
        final ArrayDeque<String> pendingPieces = new ArrayDeque<>();
        TetrisPiece currentPiece;
        int pieceX;
        int pieceY;
        boolean gameOver;
        int score;

        PlayerState(int playerNum) {
            this.playerNum = playerNum;
        }
    }

    private static class TetrisPiece {
        final String tipo;
        final int[][] shape;
        final int color;

        TetrisPiece(String tipo, int[][] shape, int color) {
            this.tipo = tipo;
            this.shape = shape;
            this.color = color;
        }

        static TetrisPiece ofTipo(String tipo) {
            switch(tipo) {
                case "I":
                    return new TetrisPiece("I", new int[][] {{-1,0},{0,0},{1,0},{2,0}}, 1);
                case "O":
                    return new TetrisPiece("O", new int[][] {{0,0},{1,0},{0,1},{1,1}}, 4);
                case "T":
                    return new TetrisPiece("T", new int[][] {{-1,0},{0,0},{1,0},{0,1}}, 7);
                case "S":
                    return new TetrisPiece("S", new int[][] {{-1,0},{0,0},{0,1},{1,1}}, 5);
                case "Z":
                    return new TetrisPiece("Z", new int[][] {{-1,1},{0,1},{0,0},{1,0}}, 6);
                case "J":
                    return new TetrisPiece("J", new int[][] {{-1,0},{0,0},{1,0},{1,1}}, 3);
                case "L":
                default:
                    return new TetrisPiece("L", new int[][] {{-1,0},{0,0},{1,0},{-1,1}}, 2);
            }
        }
    }

    public void terminate() {
        this.end = true;
        socket.close();
        this.interrupt();
    }
}
