package pantalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.tetrisonline.game.TetrisGame;

import online.ControladorJuego;
import online.HiloCliente;

import java.util.ArrayDeque;
import java.util.Random;

public class PantallaJuego extends ScreenAdapter implements ControladorJuego {

    private final TetrisGame juego;

    private final Random random = new Random();

    private final int[][][] piezasBase = {
        { {-1, 0}, {0, 0}, {1, 0}, {2, 0} },   // I
        { {-1, 0}, {0, 0}, {1, 0}, {1, 1} },   // L
        { {-1, 1}, {-1, 0}, {0, 0}, {1, 0} },  // J
        { {0, 0}, {1, 0}, {0, 1}, {1, 1} },    // O
        { {-1, 0}, {0, 0}, {0, 1}, {1, 1} },   // S
        { {-1, 1}, {0, 1}, {0, 0}, {1, 0} },   // Z
        { {-1, 0}, {0, 0}, {1, 0}, {0, 1} }    // T
    };

    private ShapeRenderer figura;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private OrthographicCamera camara;
    private Texture overlayKo;
    private HiloCliente hiloCliente;
    private final Object colaLock = new Object();
    private final ArrayDeque<String> colaPiezasP1 = new ArrayDeque<String>();
    private final ArrayDeque<String> colaPiezasP2 = new ArrayDeque<String>();

    private static final int COLUMNAS = 10;
    private static final int FILAS = 20;
    private static final int TAM_BLOQUE = 24;
    private static final int Y_TABLERO = 20;
    private static final int TABLERO_SEPARACION = 140;
    private static final int X_TABLERO_1 = 80;
    private static final int X_TABLERO_2 = X_TABLERO_1 + COLUMNAS * TAM_BLOQUE + TABLERO_SEPARACION;

    private final TableroTetris jugador1 = new TableroTetris("P1", X_TABLERO_1, colaPiezasP1);
    private final TableroTetris jugador2 = new TableroTetris("P2", X_TABLERO_2, colaPiezasP2);
    private boolean partidaTerminada = false;
    private float barraVentajaOffset = 0f;
    private float barraVentajaSuave = 0f;
    private float barraVentajaParpadeo = 0f;
    private float temblorP1 = 0f;
    private float temblorP2 = 0f;

    public PantallaJuego(TetrisGame juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        figura = new ShapeRenderer();
        batch = new SpriteBatch();
        fuente = new BitmapFont();
        camara = new OrthographicCamera();
        camara.setToOrtho(false, 960, 480);
        overlayKo = crearOverlayKo();

        if (juego.getMusica() != null && !juego.getMusica().isPlaying()) {
            juego.getMusica().play();
        }

        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        partidaTerminada = false;
        hiloCliente = new HiloCliente(this);
        hiloCliente.start();
        hiloCliente.sendMessage("Conectado");
        hiloCliente.sendMessage("hola estoy conectado papi");
    }

    @Override
    public void render(float delta) {
        procesarEntrada();

        jugador1.actualizar(delta);
        jugador2.actualizar(delta);
        actualizarBarraVentaja(delta);
        actualizarTemblor(delta);

        if (!partidaTerminada) {
            boolean p1Over = jugador1.isGameOver();
            boolean p2Over = jugador2.isGameOver();
            if (p1Over || p2Over) {
                int ganador = determinarGanador(p1Over, p2Over);
                partidaTerminada = true;
                juego.setScreen(new PantallaGanador(
                    juego,
                    ganador,
                    jugador1.getPuntaje(),
                    jugador2.getPuntaje(),
                    jugador1.getLineasEliminadas(),
                    jugador2.getLineasEliminadas()
                ));
                return;
            }
        }

        dibujar();
    }

    private void procesarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            juego.setScreen(new PantallaMenu(juego));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.ALT_RIGHT)) {
            jugador1.hardDrop();
            return;
        }

        jugador1.procesarEntradaJugador1();
        jugador2.procesarEntradaJugador2();
    }

    private void dibujar() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        figura.setProjectionMatrix(camara.combined);
        figura.begin(ShapeRenderer.ShapeType.Filled);

        jugador1.dibujar(figura);
        jugador2.dibujar(figura);
        dibujarBarraVentaja(figura);

        figura.end();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        jugador1.dibujarUI(batch, fuente);
        jugador2.dibujarUI(batch, fuente);
        batch.end();
    }

    private void actualizarBarraVentaja(float delta) {
        int ventaja = jugador1.getLineasEliminadas() - jugador2.getLineasEliminadas();
        float target = Math.max(-80f, Math.min(80f, ventaja * 6f));
        barraVentajaOffset = target;
        float suavizado = Math.min(1f, delta * 6f);
        barraVentajaSuave = barraVentajaSuave + (barraVentajaOffset - barraVentajaSuave) * suavizado;
        barraVentajaParpadeo += delta;
    }

    private void actualizarTemblor(float delta) {
        boolean p1Peligro = jugador1.estaEnPeligro();
        boolean p2Peligro = jugador2.estaEnPeligro();

        temblorP1 = p1Peligro ? temblorP1 + delta : 0f;
        temblorP2 = p2Peligro ? temblorP2 + delta : 0f;

        float offsetP1 = p1Peligro ? (float) Math.sin(temblorP1 * 40f) * 3f : 0f;
        float offsetP2 = p2Peligro ? (float) Math.sin(temblorP2 * 40f) * 3f : 0f;
        jugador1.setShakeOffsetX(offsetP1);
        jugador2.setShakeOffsetX(offsetP2);
    }


    private void dibujarBarraVentaja(ShapeRenderer sr) {
        float barWidth = 16f;
        float barHeight = 360f;
        float baseX = 960f - barWidth - 12f;
        float x = baseX + barraVentajaSuave;
        x = Math.max(12f, Math.min(960f - barWidth - 12f, x));
        float y = 60f;
        float alpha = 0.85f;
        int ventaja = jugador1.getLineasEliminadas() - jugador2.getLineasEliminadas();
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
        if (overlayKo != null) {
            overlayKo.dispose();
        }
    }

    private Texture crearOverlayKo() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0f, 0f, 1f);
        pixmap.fill();
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    public void onStart() {
        synchronized (colaLock) {
            colaPiezasP1.clear();
            colaPiezasP2.clear();
        }
    }

    public void onNuevaPieza(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return;
        }
        synchronized (colaLock) {
            colaPiezasP1.addLast(tipo.trim());
            colaPiezasP2.addLast(tipo.trim());
        }
        jugador1.intentarTomarPieza();
        jugador2.intentarTomarPieza();
    }

    private String obtenerSiguienteTipo(ArrayDeque<String> cola) {
        synchronized (colaLock) {
            return cola.pollFirst();
        }
    }

    private int mapTipoToId(String tipo) {
        switch (tipo) {
            case "I":
                return 0;
            case "L":
                return 1;
            case "J":
                return 2;
            case "O":
                return 3;
            case "S":
                return 4;
            case "Z":
                return 5;
            case "T":
                return 6;
            default:
                return -1;
        }
    }

    private void enviarAccionPiezaColocada() {
        if (hiloCliente != null) {
            hiloCliente.sendMessage("LISTO");
        }
    }

    private class TableroTetris {
        private final String nombre;
        private final int xTablero;
        private final int[][] tablero = new int[FILAS][COLUMNAS];
        private final ArrayDeque<String> colaPiezas;

        private int[][] piezaActual;
        private int piezaColor;
        private int pivoteX;
        private int pivoteY;
        private float shakeOffsetX = 0f;

        private float tiempoCaida = 0f;
        private float intervaloCaida = 0.5f;

        private int puntaje = 0;
        private int lineasTotales = 0;
        private int nivel = 1;
        private int velocidadCaida = 800;
        private boolean pausado = false;
        private boolean gameOver = false;
        private boolean perdioPorBasura = false;

        private boolean mostrandoNivel = false;
        private float tiempoMostrarNivel = 0f;
        private int nivelActual = 1;
        private final GlyphLayout layoutNivel = new GlyphLayout();

        private TableroTetris(String nombre, int xTablero, ArrayDeque<String> colaPiezas) {
            this.nombre = nombre;
            this.xTablero = xTablero;
            this.colaPiezas = colaPiezas;
        }

        private void reiniciarPartida() {
            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    tablero[y][x] = 0;
                }
            }
            puntaje = 0;
            lineasTotales = 0;
            nivel = 1;
            actualizarVelocidadCaida();
            pausado = false;
            gameOver = false;
            perdioPorBasura = false;
            tiempoCaida = 0f;
            mostrandoNivel = false;
            tiempoMostrarNivel = 0f;
            nivelActual = nivel;
            generarPiezaNueva();
        }

        private void generarPiezaNueva() {
            String tipo = obtenerSiguienteTipo(colaPiezas);
            if (tipo == null) {
                piezaActual = null;
                piezaColor = 0;
                return;
            }
            int id = mapTipoToId(tipo);
            if (id < 0 || id >= piezasBase.length) {
                piezaActual = null;
                piezaColor = 0;
                return;
            }
            piezaActual = copiarPieza(piezasBase[id]);
            piezaColor = id + 1;
            pivoteX = COLUMNAS / 2;
            pivoteY = FILAS - 2;

            if (!puedeMover(0, 0, piezaActual)) {
                gameOver = true;
            }
        }

        private int[][] copiarPieza(int[][] origen) {
            int[][] copia = new int[origen.length][2];
            for (int i = 0; i < origen.length; i++) {
                copia[i][0] = origen[i][0];
                copia[i][1] = origen[i][1];
            }
            return copia;
        }

        private void actualizar(float delta) {
            if (!pausado && !gameOver) {
                if (piezaActual == null) {
                    generarPiezaNueva();
                }
                tiempoCaida += delta;
                if (piezaActual != null && tiempoCaida >= intervaloCaida) {
                    tiempoCaida = 0f;
                    if (!mover(0, -1)) {
                        fijarPieza();
                    }
                }
            }

            if (mostrandoNivel) {
                tiempoMostrarNivel -= delta;
                if (tiempoMostrarNivel <= 0f) {
                    tiempoMostrarNivel = 0f;
                    mostrandoNivel = false;
                }
            }
        }

        private void procesarEntradaJugador1() {
            if (gameOver) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    reiniciarPartida();
                }
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                pausado = !pausado;
                return;
            }
            if (pausado) {
                return;
            }
            if (piezaActual == null) {
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                mover(-1, 0);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                mover(1, 0);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                if (!mover(0, -1)) {
                    fijarPieza();
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                rotarDerecha();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                rotarIzquierda();
            }
            // hard drop se maneja en PantallaJuego con ALT
        }

        private void procesarEntradaJugador2() {
            if (gameOver) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    reiniciarPartida();
                }
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
                pausado = !pausado;
                return;
            }
            if (pausado) {
                return;
            }
            if (piezaActual == null) {
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                mover(-1, 0);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                mover(1, 0);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                if (!mover(0, -1)) {
                    fijarPieza();
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                rotarDerecha();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                rotarIzquierda();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
                hardDrop();
            }
        }

        private boolean mover(int dx, int dy) {
            if (piezaActual == null) {
                return false;
            }
            if (puedeMover(dx, dy, piezaActual)) {
                pivoteX += dx;
                pivoteY += dy;
                return true;
            }
            return false;
        }

        private boolean puedeMover(int dx, int dy, int[][] forma) {
            for (int i = 0; i < forma.length; i++) {
                int x = pivoteX + forma[i][0] + dx;
                int y = pivoteY + forma[i][1] + dy;

                if (x < 0 || x >= COLUMNAS || y < 0) {
                    return false;
                }

                if (y < FILAS && tablero[y][x] != 0) {
                    return false;
                }
            }
            return true;
        }

        private void rotarDerecha() {
            if (piezaActual == null) {
                return;
            }
            int[][] rotada = new int[piezaActual.length][2];
            for (int i = 0; i < piezaActual.length; i++) {
                rotada[i][0] = -piezaActual[i][1];
                rotada[i][1] = piezaActual[i][0];
            }

            if (puedeMover(0, 0, rotada)) {
                piezaActual = rotada;
            }
        }

        private void rotarIzquierda() {
            if (piezaActual == null) {
                return;
            }
            int[][] rotada = new int[piezaActual.length][2];
            for (int i = 0; i < piezaActual.length; i++) {
                rotada[i][0] = piezaActual[i][1];
                rotada[i][1] = -piezaActual[i][0];
            }

            if (puedeMover(0, 0, rotada)) {
                piezaActual = rotada;
            }
        }

        private void actualizarVelocidadCaida() {
            velocidadCaida = Math.max(80, 800 - (nivel - 1) * 70);
            intervaloCaida = velocidadCaida / 1000f;
        }

        private void fijarPieza() {
            if (piezaActual == null) {
                return;
            }
            for (int i = 0; i < piezaActual.length; i++) {
                int x = pivoteX + piezaActual[i][0];
                int y = pivoteY + piezaActual[i][1];
                if (y >= 0 && y < FILAS && x >= 0 && x < COLUMNAS) {
                    tablero[y][x] = piezaColor;
                    float px = xTablero + shakeOffsetX + x * TAM_BLOQUE + TAM_BLOQUE * 0.5f;
                    float py = Y_TABLERO + y * TAM_BLOQUE + TAM_BLOQUE * 0.5f;
                }
            }

            int lineas = limpiarLineas();
            if (lineas > 0) {
                int lineasAntes = lineasTotales;
                lineasTotales += lineas;
                int nivelesSubir = (lineasTotales / 3) - (lineasAntes / 3);
                if (nivelesSubir > 0) {
                    nivel += nivelesSubir;
                    actualizarVelocidadCaida();
                    mostrarSubidaNivel(nivel);
                }
                puntaje += lineas * 100 * nivel;
                enviarBasuraAlRival(lineas);
            }

            generarPiezaNueva();
            enviarAccionPiezaColocada();
        }

        private void hardDrop() {
            if (piezaActual == null) {
                return;
            }
            while (puedeMover(0, -1, piezaActual)) {
                pivoteY -= 1;
            }
            fijarPieza();
            tiempoCaida = 0f;
        }

        private int limpiarLineas() {
            int lineasLimpiadas = 0;

            for (int y = 0; y < FILAS; y++) {
                boolean completa = true;
                for (int x = 0; x < COLUMNAS; x++) {
                    if (tablero[y][x] == 0) {
                        completa = false;
                        break;
                    }
                }

                if (completa) {
                    lineasLimpiadas++;
                    for (int x = 0; x < COLUMNAS; x++) {
                        int celda = tablero[y][x];
                        if (celda != 0) {
                            float px = xTablero + shakeOffsetX + x * TAM_BLOQUE + TAM_BLOQUE * 0.5f;
                            float py = Y_TABLERO + y * TAM_BLOQUE + TAM_BLOQUE * 0.5f;
                        }
                    }

                    for (int fila = y; fila < FILAS - 1; fila++) {
                        for (int x = 0; x < COLUMNAS; x++) {
                            tablero[fila][x] = tablero[fila + 1][x];
                        }
                    }

                    for (int x = 0; x < COLUMNAS; x++) {
                        tablero[FILAS - 1][x] = 0;
                    }

                    y--;
                }
            }

            return lineasLimpiadas;
        }

        private void dibujar(ShapeRenderer figura) {
            if (mostrandoNivel) {
                float alpha = Math.min(1f, tiempoMostrarNivel / 0.3f) * 0.6f;
                figura.setColor(1f, 1f, 1f, alpha);
                figura.rect(0, 0, 960, 480);
            }
            figura.setColor(0.15f, 0.15f, 0.2f, 1f);
            figura.rect(xTablero + shakeOffsetX - 4, Y_TABLERO - 4, COLUMNAS * TAM_BLOQUE + 8, FILAS * TAM_BLOQUE + 8);

            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    int celda = tablero[y][x];
                    if (celda != 0) {
                        figura.setColor(colorPieza(celda));
                        figura.rect(xTablero + shakeOffsetX + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                    }
                }
            }

            if (!gameOver && piezaActual != null) {
                figura.setColor(colorPieza(piezaColor));
                for (int i = 0; i < piezaActual.length; i++) {
                    int x = pivoteX + piezaActual[i][0];
                    int y = pivoteY + piezaActual[i][1];
                    if (y >= 0 && y < FILAS) {
                        figura.rect(xTablero + shakeOffsetX + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                    }
                }
            }
        }

        private void dibujarUI(SpriteBatch batch, BitmapFont fuente) {
            int uiX = xTablero + COLUMNAS * TAM_BLOQUE + 10;
            int uiY = 430;
            fuente.draw(batch, nombre, xTablero + shakeOffsetX, 465);
            fuente.draw(batch, "NIVEL: " + nivel, uiX, uiY);
            fuente.draw(batch, "LINEAS: " + lineasTotales, uiX, uiY - 30);
            fuente.draw(batch, "PUNTAJE: " + puntaje, uiX, uiY - 60);
            fuente.draw(batch, "ESC: MENU", uiX, uiY - 90);
            fuente.draw(batch, (nombre.equals("P1") ? "P: PAUSA" : "O: PAUSA"), uiX, uiY - 120);

            if (estaEnPeligro()) {
                float avisoX = xTablero + shakeOffsetX;
                float avisoY = Y_TABLERO + FILAS * TAM_BLOQUE + 18;
                fuente.draw(batch, "EN PELIGRO", avisoX, avisoY);
            }

            if (pausado) {
                fuente.draw(batch, "PAUSADO", uiX, uiY - 150);
            }

            if (gameOver) {
                fuente.draw(batch, perdioPorBasura ? "KO" : "GAME OVER", uiX, uiY - 180);
                fuente.draw(batch, "ENTER: REINICIAR", uiX, uiY - 210);
            }

            if (mostrandoNivel) {
                float prevX = fuente.getData().scaleX;
                float prevY = fuente.getData().scaleY;
                float t = 1f - Math.min(1f, tiempoMostrarNivel / 1f);
                float escala = 3.2f + (2.2f - 3.2f) * t;
                fuente.getData().setScale(escala);
                String texto = "NIVEL " + nivelActual;
                layoutNivel.setText(fuente, texto);
                float x = (960f - layoutNivel.width) / 2f;
                float y = (480f + layoutNivel.height) / 2f;
                fuente.draw(batch, texto, x, y);
                fuente.getData().setScale(prevX, prevY);
            }

            if (gameOver && perdioPorBasura) {
                Color prev = batch.getColor();
                batch.setColor(1f, 0.1f, 0.1f, 0.35f);
                batch.draw(overlayKo, 0, 0, 960, 480);
                batch.setColor(prev);

                float prevX = fuente.getData().scaleX;
                float prevY = fuente.getData().scaleY;
                fuente.getData().setScale(3.0f);
                String textoKo = "KO";
                layoutNivel.setText(fuente, textoKo);
                float x = (960f - layoutNivel.width) / 2f;
                float y = (480f + layoutNivel.height) / 2f;
                fuente.draw(batch, textoKo, x, y);
                fuente.getData().setScale(prevX, prevY);
            }
        }

        private void mostrarSubidaNivel(int nuevoNivel) {
            nivelActual = nuevoNivel;
            mostrandoNivel = true;
            tiempoMostrarNivel = 1f;
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
            if (basura <= 0) {
                return;
            }
            TableroTetris rival = (this == jugador1) ? jugador2 : jugador1;
            rival.recibirBasura(basura);
        }

        private void recibirBasura(int cantidad) {
            if (cantidad > 0) {
                perdioPorBasura = true;
            }
            for (int i = 0; i < cantidad; i++) {
                if (hayBloquesEnFilaSuperior()) {
                    gameOver = true;
                    return;
                }
                for (int y = FILAS - 1; y > 0; y--) {
                    for (int x = 0; x < COLUMNAS; x++) {
                        tablero[y][x] = tablero[y - 1][x];
                    }
                }
                int hueco = random.nextInt(COLUMNAS);
                for (int x = 0; x < COLUMNAS; x++) {
                    tablero[0][x] = (x == hueco) ? 0 : 7;
                }
            }
        }

        private boolean hayBloquesEnFilaSuperior() {
            for (int x = 0; x < COLUMNAS; x++) {
                if (tablero[FILAS - 1][x] != 0) {
                    return true;
                }
            }
            return false;
        }

        private int getPuntaje() {
            return puntaje;
        }

        private int getLineasEliminadas() {
            return lineasTotales;
        }

        private boolean isGameOver() {
            return gameOver;
        }

        private boolean estaEnPeligro() {
            return !gameOver && hayBloquesCercaDelTope();
        }

        private void setShakeOffsetX(float offset) {
            this.shakeOffsetX = offset;
        }

        private boolean hayBloquesCercaDelTope() {
            for (int x = 0; x < COLUMNAS; x++) {
                if (tablero[FILAS - 1][x] != 0 || tablero[FILAS - 2][x] != 0 || tablero[FILAS - 3][x] != 0) {
                    return true;
                }
            }
            return false;
        }

        private void intentarTomarPieza() {
            if (piezaActual == null && !gameOver) {
                generarPiezaNueva();
            }
        }
    }

    private int determinarGanador(boolean p1Over, boolean p2Over) {
        if (p1Over && !p2Over) {
            return 2;
        }
        if (p2Over && !p1Over) {
            return 1;
        }
        if (jugador1.getPuntaje() >= jugador2.getPuntaje()) {
            return 1;
        }
        return 2;
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

    private static class PantallaGanador extends ScreenAdapter {

        private final TetrisGame juego;
        private final int ganador;
        private final int puntajeP1;
        private final int puntajeP2;
        private final int lineasP1;
        private final int lineasP2;

        private SpriteBatch batch;
        private BitmapFont font;
        private BitmapFont titleFont;
        private Texture overlay;
        private Stage stage;
        private Skin skin;
        private TextButton revanchaButton;
        private TextButton menuButton;
        private Actor[] focusOrder;
        private int focusIndex = 0;
        private Sound victoriaSound;
        private final GlyphLayout layout = new GlyphLayout();

        private PantallaGanador(TetrisGame juego, int ganador, int puntajeP1, int puntajeP2, int lineasP1, int lineasP2) {
            this.juego = juego;
            this.ganador = ganador;
            this.puntajeP1 = puntajeP1;
            this.puntajeP2 = puntajeP2;
            this.lineasP1 = lineasP1;
            this.lineasP2 = lineasP2;
        }

        @Override
        public void show() {
            batch = new SpriteBatch();
            font = new BitmapFont();
            font.getData().setScale(1.4f);
            titleFont = new BitmapFont();
            titleFont.getData().setScale(2.2f);

            overlay = createOverlay();

            skin = createSkin(font);
            stage = new Stage(new ScreenViewport());
            Gdx.input.setInputProcessor(stage);

            revanchaButton = new TextButton("REVANCHA", skin);
            menuButton = new TextButton("MENU", skin);

            Table root = new Table();
            root.setFillParent(true);
            root.center();
            stage.addActor(root);

            root.add(revanchaButton).width(260).height(64).padBottom(12).row();
            root.add(menuButton).width(260).height(64);

            revanchaButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (juego.getMusica() != null && !juego.getMusica().isPlaying()) {
                        juego.getMusica().play();
                    }
                    juego.setScreen(new PantallaJuego(juego));
                }
            });

            menuButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    juego.setScreen(new PantallaMenu(juego));
                }
            });

            focusOrder = new Actor[] { revanchaButton, menuButton };
            setFocus(0);
            addFocusOnMouse(revanchaButton);
            addFocusOnMouse(menuButton);
            stage.addListener(new GanadorInputListener());

            if (juego.getMusica() != null && juego.getMusica().isPlaying()) {
                juego.getMusica().stop();
            }
            victoriaSound = Gdx.audio.newSound(Gdx.files.internal("victoria.wav"));
            victoriaSound.play(1f);
        }

        @Override
        public void render(float delta) {
            stage.act(delta);

            batch.begin();
            batch.draw(overlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            String titulo = "GANADOR: JUGADOR " + ganador;
            titleFont.draw(batch, titulo, centerX(titleFont, titulo), Gdx.graphics.getHeight() - 120);

            String p1 = "P1  PUNTAJE: " + puntajeP1 + "  LINEAS: " + lineasP1;
            String p2 = "P2  PUNTAJE: " + puntajeP2 + "  LINEAS: " + lineasP2;
            font.draw(batch, p1, centerX(font, p1), Gdx.graphics.getHeight() - 180);
            font.draw(batch, p2, centerX(font, p2), Gdx.graphics.getHeight() - 210);
            batch.end();

            stage.draw();
        }

        @Override
        public void resize(int width, int height) {
            if (stage != null) {
                stage.getViewport().update(width, height, true);
            }
        }

        @Override
        public void dispose() {
            if (batch != null) {
                batch.dispose();
            }
            if (font != null) {
                font.dispose();
            }
            if (titleFont != null) {
                titleFont.dispose();
            }
            if (overlay != null) {
                overlay.dispose();
            }
            if (stage != null) {
                stage.dispose();
            }
            if (skin != null) {
                skin.dispose();
            }
            if (victoriaSound != null) {
                victoriaSound.dispose();
            }
        }

        private Texture createOverlay() {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(new Color(0f, 0f, 0f, 0.75f));
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            pixmap.dispose();
            return tex;
        }

        private Skin createSkin(BitmapFont font) {
            Skin s = new Skin();
            s.add("default-font", font);

            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            Texture whiteTex = new Texture(pixmap);
            pixmap.dispose();
            s.add("white", whiteTex);

            TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
            buttonStyle.up = s.newDrawable("white", new Color(0.15f, 0.15f, 0.2f, 0.9f));
            buttonStyle.down = s.newDrawable("white", new Color(0.1f, 0.1f, 0.14f, 1f));
            buttonStyle.over = s.newDrawable("white", new Color(0.2f, 0.2f, 0.28f, 1f));
            buttonStyle.font = font;
            buttonStyle.fontColor = Color.WHITE;
            s.add("default", buttonStyle);

            return s;
        }

        private float centerX(BitmapFont f, String text) {
            layout.setText(f, text);
            return (Gdx.graphics.getWidth() - layout.width) / 2f;
        }

        private void setFocus(int index) {
            focusIndex = Math.max(0, Math.min(index, focusOrder.length - 1));
            stage.setKeyboardFocus(focusOrder[focusIndex]);
            updateSelection();
        }

        private void updateSelection() {
            revanchaButton.setText((focusIndex == 0 ? "- " : "  ") + "REVANCHA");
            menuButton.setText((focusIndex == 1 ? "- " : "  ") + "MENU");
        }

        private void addFocusOnMouse(Actor actor) {
            actor.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (actor == revanchaButton) {
                        setFocus(0);
                    } else if (actor == menuButton) {
                        setFocus(1);
                    }
                    return false;
                }
            });
        }

        private class GanadorInputListener extends InputListener {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.UP || keycode == com.badlogic.gdx.Input.Keys.LEFT) {
                    setFocus((focusIndex - 1 + focusOrder.length) % focusOrder.length);
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.DOWN || keycode == com.badlogic.gdx.Input.Keys.RIGHT) {
                    setFocus((focusIndex + 1) % focusOrder.length);
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                    Actor focused = stage.getKeyboardFocus();
                    if (focused == revanchaButton) {
                        revanchaButton.fire(new ChangeListener.ChangeEvent());
                        return true;
                    }
                    if (focused == menuButton) {
                        menuButton.fire(new ChangeListener.ChangeEvent());
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override
    public void conectar(int numeroJugador) {
        // No-op en esta pantalla alternativa.
    }

    @Override
    public void empezar() {
        onStart();
    }

    @Override
    public void actualizarTableroRemoto(int numeroJugador, String tablero, int puntaje, int lineas, boolean gameOver, boolean ko) {
        // No-op en esta pantalla alternativa.
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
        juego.setScreen(new PantallaMenu(juego));
    }

    @Override
    public void recibirBasura(int cantidad) {
        // No-op en esta pantalla alternativa.
    }

    @Override
    public void recibirBasuraDirigida(int objetivo, int cantidad) {
        // No-op en esta pantalla alternativa.
    }
}
