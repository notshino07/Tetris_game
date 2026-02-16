package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import juego.JuegoTetris;
import jugador.ControlJugador;
import online.ControladorJuego;
import online.HiloCliente;
import tetris.GeneradorPiezas;
import tetris.Tablero;

public class PantallaJuego extends ScreenAdapter implements ControladorJuego {
    private static final int COLUMNAS = 10;
    private static final int TAM_BLOQUE = 24;
    private static final int TABLERO_SEPARACION = 140;
    private static final int X_TABLERO_1 = 80;
    private static final int X_TABLERO_2 = X_TABLERO_1 + COLUMNAS * TAM_BLOQUE + TABLERO_SEPARACION;
    private static final int VENTANA_ANCHO = 960;
    private static final int VENTANA_ALTO = 480;
    private static final String MSG_CONECTADO = "Conectado";
    private static final float INTERVALO_ENVIO_ESTADO = 0.08f;

    private final JuegoTetris juego;

    private ShapeRenderer figura;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private OrthographicCamera camara;
    private GlyphLayout layout;

    private final GeneradorPiezas generador = new GeneradorPiezas();
    private Tablero tableroLocal;
    private TableroRemoto tableroRemoto;
    private ControlJugador controlLocal;
    private boolean partidaTerminada = false;
    private boolean juegoEmpezado = false;

    private HiloCliente cliente;
    private int numeroJugadorLocal = 0;
    private float tiempoEnvioEstado = 0f;
    private float temblorLocal = 0f;
    private float temblorRemoto = 0f;
    private float shakeLocal = 0f;
    private float shakeRemoto = 0f;
    private float barraVentajaOffset = 0f;
    private float barraVentajaSuave = 0f;
    private float barraVentajaParpadeo = 0f;
    private float tiempoDesdeInicio = 0f;

    public PantallaJuego(JuegoTetris juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        figura = new ShapeRenderer();
        batch = new SpriteBatch();
        fuente = new BitmapFont();
        camara = new OrthographicCamera();
        camara.setToOrtho(false, VENTANA_ANCHO, VENTANA_ALTO);
        layout = new GlyphLayout();

        if (juego.getMusica() != null && !juego.getMusica().isPlaying()) {
            juego.getMusica().play();
        }

        tableroLocal = null;
        tableroRemoto = null;
        controlLocal = null;
        partidaTerminada = false;
        juegoEmpezado = false;
        numeroJugadorLocal = 0;
        tiempoEnvioEstado = 0f;

        cliente = new HiloCliente(this);
        cliente.start();
        cliente.sendMessage(MSG_CONECTADO);
    }

    @Override
    public void render(float delta) {
        procesarEntrada();

        if (juegoEmpezado && tableroLocal != null) {
            tableroLocal.actualizar(delta);
            enviarEstadoLocal(delta);
            actualizarTemblor(delta);

            if (!partidaTerminada) {
                boolean localOver = tableroLocal.isGameOver();
                boolean remotoOver = tableroRemoto != null && tableroRemoto.isGameOver();
                if (localOver || remotoOver) {
                    int ganador = determinarGanador(localOver, remotoOver);
                    partidaTerminada = true;
                    cerrarCliente();
                juego.setScreen(new PantallaGanador(
                    juego,
                    ganador,
                    numeroJugadorLocal,
                    obtenerPuntajeP1(),
                    obtenerPuntajeP2(),
                    obtenerLineasP1(),
                        obtenerLineasP2()
                    ));
                    return;
                }
            }
        }

        dibujar();
    }

    private void procesarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverAlMenu();
            return;
        }
        if (!juegoEmpezado || tableroLocal == null || controlLocal == null) {
            return;
        }
        tableroLocal.procesarEntrada(controlLocal);
    }

    private void dibujar() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        figura.setProjectionMatrix(camara.combined);
        figura.begin(ShapeRenderer.ShapeType.Filled);
        if (tableroLocal != null) {
            tableroLocal.dibujar(figura, shakeLocal);
        }
        if (tableroRemoto != null) {
            tableroRemoto.dibujar(figura, shakeRemoto);
        }
        if (tableroLocal != null && tableroLocal.isGameOver() && tableroLocal.isPerdioPorBasura()) {
            figura.setColor(1f, 0.1f, 0.1f, 0.35f);
            figura.rect(0, 0, VENTANA_ANCHO, VENTANA_ALTO);
        }
        if (tableroRemoto != null && tableroRemoto.isGameOver() && tableroRemoto.isPerdioPorBasura()) {
            figura.setColor(1f, 0.1f, 0.1f, 0.35f);
            figura.rect(tableroRemoto.getXTablero(), tableroRemoto.getYTablero(),
                tableroRemoto.getAnchoTablero(), tableroRemoto.getAltoTablero());
        }
        dibujarBarraVentaja(figura);
        figura.end();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        if (tableroLocal != null) {
            tableroLocal.dibujarUI(batch, fuente, shakeLocal);
        }
        if (tableroRemoto != null) {
            tableroRemoto.dibujarUI(batch, fuente, shakeRemoto);
        }
        if (!juegoEmpezado) {
            String texto = "Esperando otro jugador...";
            layout.setText(fuente, texto);
            fuente.draw(batch, texto, (VENTANA_ANCHO - layout.width) / 2f, VENTANA_ALTO / 2f);
        }
        if (tableroLocal != null && tableroLocal.isGameOver() && tableroLocal.isPerdioPorBasura()) {
            String texto = "KO";
            layout.setText(fuente, texto);
            float x = (VENTANA_ANCHO - layout.width) / 2f;
            float y = (VENTANA_ALTO + layout.height) / 2f;
            fuente.draw(batch, texto, x, y);
        }
        if (tableroRemoto != null && tableroRemoto.isGameOver() && tableroRemoto.isPerdioPorBasura()) {
            String texto = "KO";
            layout.setText(fuente, texto);
            float x = tableroRemoto.getXTablero() + (tableroRemoto.getAnchoTablero() - layout.width) / 2f;
            float y = tableroRemoto.getYTablero() + (tableroRemoto.getAltoTablero() + layout.height) / 2f;
            fuente.draw(batch, texto, x, y);
        }
        batch.end();
    }

    private void dibujarBarraVentaja(ShapeRenderer sr) {
        float barWidth = 16f;
        float barHeight = 360f;
        float baseX = VENTANA_ANCHO - barWidth - 12f;
        float x = baseX + barraVentajaSuave;
        x = Math.max(12f, Math.min(VENTANA_ANCHO - barWidth - 12f, x));
        float y = 60f;
        float alpha = 0.85f;
        int lineasLocal = tableroLocal != null ? tableroLocal.getLineasEliminadas() : 0;
        int lineasRemoto = tableroRemoto != null ? tableroRemoto.getLineasTotales() : 0;
        int ventaja = (numeroJugadorLocal == 1) ? (lineasLocal - lineasRemoto) : (lineasRemoto - lineasLocal);
        if (Math.abs(ventaja) > 10) {
            alpha = 0.5f + 0.35f * (float) Math.sin(barraVentajaParpadeo * 8f);
        }
        sr.setColor(0.1f, 0.45f, 0.95f, alpha);
        sr.rect(x, y, barWidth / 2f, barHeight);
        sr.setColor(0.9f, 0.2f, 0.2f, alpha);
        sr.rect(x + barWidth / 2f, y, barWidth / 2f, barHeight);
    }

    @Override
    public void dispose() {
        if (figura != null) {
            figura.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (fuente != null) {
            fuente.dispose();
        }
        cerrarCliente();
    }

    @Override
    public void conectar(int numeroJugador) {
        numeroJugadorLocal = numeroJugador;
        Gdx.app.postRunnable(() -> configurarJugadorLocal(numeroJugadorLocal));
    }

    @Override
    public void empezar() {
        juegoEmpezado = true;
        partidaTerminada = false;
        tiempoDesdeInicio = 0f;
        if (tableroLocal != null) {
            tableroLocal.reiniciarPartida();
        }
        if (tableroRemoto != null) {
            tableroRemoto.reset();
        }
    }

    @Override
    public void actualizarTableroRemoto(int numeroJugador, String tablero, int puntaje, int lineas, boolean gameOver, boolean ko) {
        if (numeroJugador == numeroJugadorLocal) return;
        if (tableroRemoto == null) return;
        Gdx.app.postRunnable(() -> tableroRemoto.aplicarEstado(tablero, puntaje, lineas, gameOver, ko));
    }

    @Override
    public void clienteDesconectado(int numeroJugador) {
        volverAlMenu();
    }

    @Override
    public void servidorLleno() {
        volverAlMenu();
    }

    @Override
    public void servidorCerrado() {
        volverAlMenu();
    }

    @Override
    public void volverAlMenu() {
        cerrarCliente();
        Gdx.app.postRunnable(() -> juego.setScreen(new PantallaMenu(juego)));
    }

    @Override
    public void recibirBasura(int cantidad) {
        if (tableroLocal == null || cantidad <= 0) return;
        Gdx.app.postRunnable(() -> tableroLocal.recibirBasura(cantidad));
    }

    @Override
    public void recibirBasuraDirigida(int objetivo, int cantidad) {
        if (objetivo != numeroJugadorLocal) return;
        recibirBasura(cantidad);
    }

    private void configurarJugadorLocal(int numeroJugador) {
        if (numeroJugador == 1) {
            tableroLocal = new Tablero("P1", X_TABLERO_1, generador);
            tableroRemoto = new TableroRemoto("P2", X_TABLERO_2);
            controlLocal = ControlJugador.crearParaJugador1();
        } else {
            tableroLocal = new Tablero("P2", X_TABLERO_2, generador);
            tableroRemoto = new TableroRemoto("P1", X_TABLERO_1);
            controlLocal = ControlJugador.crearParaJugador2();
        }
        tableroLocal.reiniciarPartida();
        tableroRemoto.reset();
    }

    private void enviarEstadoLocal(float delta) {
        if (cliente == null || tableroLocal == null) return;
        tiempoEnvioEstado += delta;
        if (tiempoEnvioEstado < INTERVALO_ENVIO_ESTADO) return;
        tiempoEnvioEstado = 0f;
        int lineas = tableroLocal.consumirLineasLimpiadas();
        if (lineas > 0) {
            enviarBasuraAlRival(lineas);
        }
        String estado = tableroLocal.serializarTablero();
        String mensaje = "TABLERO:" + numeroJugadorLocal + ":" + estado + ":" +
            tableroLocal.getPuntaje() + ":" + tableroLocal.getLineasEliminadas() + ":" +
            tableroLocal.isGameOver() + ":" + tableroLocal.isPerdioPorBasura();
        cliente.sendMessage(mensaje);
    }

    private void actualizarTemblor(float delta) {
        tiempoDesdeInicio += delta;
        if (tiempoDesdeInicio < 2f) {
            shakeLocal = 0f;
            shakeRemoto = 0f;
            return;
        }
        boolean localPeligro = tableroLocal != null && tableroLocal.estaEnPeligro();
        boolean remotoPeligro = tableroRemoto != null && tableroRemoto.estaEnPeligro();

        temblorLocal = localPeligro ? temblorLocal + delta : 0f;
        temblorRemoto = remotoPeligro ? temblorRemoto + delta : 0f;

        shakeLocal = localPeligro ? (float) Math.sin(temblorLocal * 40f) * 3f : 0f;
        shakeRemoto = remotoPeligro ? (float) Math.sin(temblorRemoto * 40f) * 3f : 0f;
        actualizarBarraVentaja(delta);
    }

    private void actualizarBarraVentaja(float delta) {
        int lineasLocal = tableroLocal != null ? tableroLocal.getLineasEliminadas() : 0;
        int lineasRemoto = tableroRemoto != null ? tableroRemoto.getLineasTotales() : 0;
        int ventaja = (numeroJugadorLocal == 1) ? (lineasLocal - lineasRemoto) : (lineasRemoto - lineasLocal);
        float target = Math.max(-80f, Math.min(80f, ventaja * 6f));
        barraVentajaOffset = target;
        float suavizado = Math.min(1f, delta * 6f);
        barraVentajaSuave = barraVentajaSuave + (barraVentajaOffset - barraVentajaSuave) * suavizado;
        barraVentajaParpadeo += delta;
    }

    private void enviarBasuraAlRival(int lineasLimpiadas) {
        int basura = 0;
        if (lineasLimpiadas == 2) {
            basura = 1;
        } else if (lineasLimpiadas == 3) {
            basura = 2;
        } else if (lineasLimpiadas >= 4) {
            basura = 4;
        }
        if (basura <= 0) return;
        int objetivo = (numeroJugadorLocal == 1) ? 2 : 1;
        cliente.sendMessage("BASURA:" + objetivo + ":" + basura);
    }

    private void cerrarCliente() {
        if (cliente != null) {
            cliente.terminate();
            cliente = null;
        }
    }

    private int determinarGanador(boolean localOver, boolean remotoOver) {
        if (localOver && !remotoOver) {
            return numeroJugadorLocal == 1 ? 2 : 1;
        }
        if (remotoOver && !localOver) {
            return numeroJugadorLocal;
        }
        if (obtenerPuntajeP1() >= obtenerPuntajeP2()) {
            return 1;
        }
        return 2;
    }

    private int obtenerPuntajeP1() {
        if (numeroJugadorLocal == 1 && tableroLocal != null) {
            return tableroLocal.getPuntaje();
        }
        if (numeroJugadorLocal != 1 && tableroRemoto != null) {
            return tableroRemoto.getPuntaje();
        }
        return 0;
    }

    private int obtenerPuntajeP2() {
        if (numeroJugadorLocal == 2 && tableroLocal != null) {
            return tableroLocal.getPuntaje();
        }
        if (numeroJugadorLocal != 2 && tableroRemoto != null) {
            return tableroRemoto.getPuntaje();
        }
        return 0;
    }

    private int obtenerLineasP1() {
        if (numeroJugadorLocal == 1 && tableroLocal != null) {
            return tableroLocal.getLineasEliminadas();
        }
        if (numeroJugadorLocal != 1 && tableroRemoto != null) {
            return tableroRemoto.getLineasTotales();
        }
        return 0;
    }

    private int obtenerLineasP2() {
        if (numeroJugadorLocal == 2 && tableroLocal != null) {
            return tableroLocal.getLineasEliminadas();
        }
        if (numeroJugadorLocal != 2 && tableroRemoto != null) {
            return tableroRemoto.getLineasTotales();
        }
        return 0;
    }

    private static class TableroRemoto {
        private static final int COLUMNAS = 10;
        private static final int FILAS = 20;
        private static final int TAM_BLOQUE = 24;
        private static final int Y_TABLERO = 20;

        private final String nombre;
        private final int xTablero;
        private final int[][] tablero = new int[FILAS][COLUMNAS];
        private int puntaje;
        private int lineasTotales;
        private boolean gameOver;
        private boolean perdioPorBasura;

        private TableroRemoto(String nombre, int xTablero) {
            this.nombre = nombre;
            this.xTablero = xTablero;
            reset();
        }

        private void reset() {
            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    tablero[y][x] = 0;
                }
            }
            puntaje = 0;
            lineasTotales = 0;
            gameOver = false;
            perdioPorBasura = false;
        }

        private void aplicarEstado(String serializado, int puntaje, int lineas, boolean gameOver, boolean ko) {
            if (serializado == null) return;
            String[] filas = serializado.split("\\|");
            if (filas.length != FILAS) {
                return;
            }
            for (int y = 0; y < FILAS; y++) {
                String fila = filas[FILAS - 1 - y];
                if (fila.length() < COLUMNAS) {
                    return;
                }
                for (int x = 0; x < COLUMNAS; x++) {
                    char c = fila.charAt(x);
                    if (c >= '0' && c <= '9') {
                        tablero[y][x] = c - '0';
                    } else {
                        tablero[y][x] = 0;
                    }
                }
            }
            this.puntaje = puntaje;
            this.lineasTotales = lineas;
            this.gameOver = gameOver;
            this.perdioPorBasura = ko;
        }

        private void dibujar(ShapeRenderer figura, float offsetX) {
            figura.setColor(0.15f, 0.15f, 0.2f, 1f);
            figura.rect(
                xTablero + offsetX - 4,
                Y_TABLERO - 4,
                COLUMNAS * TAM_BLOQUE + 8,
                FILAS * TAM_BLOQUE + 8
            );
            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    int celda = tablero[y][x];
                    if (celda != 0) {
                        figura.setColor(colorPieza(celda));
                        figura.rect(
                            xTablero + offsetX + x * TAM_BLOQUE,
                            Y_TABLERO + y * TAM_BLOQUE,
                            TAM_BLOQUE - 1,
                            TAM_BLOQUE - 1
                        );
                    }
                }
            }
        }

        private void dibujarUI(SpriteBatch batch, BitmapFont fuente, float offsetX) {
            int uiX = xTablero + COLUMNAS * TAM_BLOQUE + 10;
            int uiY = 430;
            fuente.draw(batch, nombre, xTablero + offsetX, 465);
            fuente.draw(batch, "NIVEL: " + getNivel(), uiX, uiY);
            fuente.draw(batch, "LINEAS: " + lineasTotales, uiX, uiY - 30);
            fuente.draw(batch, "PUNTAJE: " + puntaje, uiX, uiY - 60);
            if (estaEnPeligro()) {
                fuente.draw(batch, "EN PELIGRO", xTablero + offsetX, Y_TABLERO + FILAS * TAM_BLOQUE + 18);
            }
            if (gameOver) {
                fuente.draw(batch, "GAME OVER", uiX, uiY - 90);
            }
        }

        private int getPuntaje() {
            return puntaje;
        }

        private int getLineasTotales() {
            return lineasTotales;
        }

        private boolean isGameOver() {
            return gameOver;
        }

        private boolean isPerdioPorBasura() {
            return perdioPorBasura;
        }

        private boolean estaEnPeligro() {
            if (gameOver) return false;
            for (int x = 0; x < COLUMNAS; x++) {
                if (tablero[FILAS - 1][x] != 0 || tablero[FILAS - 2][x] != 0 || tablero[FILAS - 3][x] != 0) {
                    return true;
                }
            }
            return false;
        }

        private int getNivel() {
            return Math.max(1, (lineasTotales / 3) + 1);
        }

        private float getXTablero() {
            return xTablero - 4f;
        }

        private float getYTablero() {
            return Y_TABLERO - 4f;
        }

        private float getAnchoTablero() {
            return COLUMNAS * TAM_BLOQUE + 8f;
        }

        private float getAltoTablero() {
            return FILAS * TAM_BLOQUE + 8f;
        }

        private Color colorPieza(int id) {
            switch (id) {
                case 1:
                    return Color.CYAN;
                case 2:
                    return Color.ORANGE;
                case 3:
                    return Color.BLUE;
                case 4:
                    return Color.YELLOW;
                case 5:
                    return Color.GREEN;
                case 6:
                    return Color.RED;
                default:
                    return Color.MAGENTA;
            }
        }
    }
}
