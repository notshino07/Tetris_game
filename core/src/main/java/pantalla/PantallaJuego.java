package pantalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tetrisonline.game.TetrisGame;

import java.util.Random;

public class PantallaJuego extends ScreenAdapter {

    private final TetrisGame juego;

    private static final int COLUMNAS = 10;
    private static final int FILAS = 20;
    private static final int TAM_BLOQUE = 24;
    private static final int X_TABLERO = 120;
    private static final int Y_TABLERO = 20;

    private final int[][] tablero = new int[FILAS][COLUMNAS];
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

    private int[][] piezaActual;
    private int piezaColor;
    private int pivoteX;
    private int pivoteY;

    private float tiempoCaida = 0f;
    private float intervaloCaida = 0.5f;

    private int puntaje = 0;
    private boolean pausado = false;
    private boolean gameOver = false;

    private ShapeRenderer figura;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private OrthographicCamera camara;

    public PantallaJuego(TetrisGame juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        figura = new ShapeRenderer();
        batch = new SpriteBatch();
        fuente = new BitmapFont();
        camara = new OrthographicCamera();
        camara.setToOrtho(false, 640, 480);

        reiniciarPartida();
    }

    private void reiniciarPartida() {
        for (int y = 0; y < FILAS; y++) {
            for (int x = 0; x < COLUMNAS; x++) {
                tablero[y][x] = 0;
            }
        }
        puntaje = 0;
        pausado = false;
        gameOver = false;
        intervaloCaida = 0.5f;
        tiempoCaida = 0f;
        generarPiezaNueva();
    }

    private void generarPiezaNueva() {
        int id = random.nextInt(piezasBase.length);
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

    @Override
    public void render(float delta) {
        procesarEntrada();

        if (!pausado && !gameOver) {
            tiempoCaida += delta;
            if (tiempoCaida >= intervaloCaida) {
                tiempoCaida = 0f;
                if (!mover(0, -1)) {
                    fijarPieza();
                }
            }
        }

        dibujar();
    }

    private void procesarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            juego.setScreen(new PantallaMenu(juego));
            return;
        }

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            while (mover(0, -1)) {
                // baja hasta el fondo
            }
            fijarPieza();
        }
    }

    private boolean mover(int dx, int dy) {
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
        int[][] rotada = new int[piezaActual.length][2];
        for (int i = 0; i < piezaActual.length; i++) {
            rotada[i][0] = piezaActual[i][1];
            rotada[i][1] = -piezaActual[i][0];
        }

        if (puedeMover(0, 0, rotada)) {
            piezaActual = rotada;
        }
    }

    private void fijarPieza() {
        for (int i = 0; i < piezaActual.length; i++) {
            int x = pivoteX + piezaActual[i][0];
            int y = pivoteY + piezaActual[i][1];
            if (y >= 0 && y < FILAS && x >= 0 && x < COLUMNAS) {
                tablero[y][x] = piezaColor;
            }
        }

        int lineas = limpiarLineas();
        puntaje += lineas * 100;
        intervaloCaida = Math.max(0.12f, 0.5f - (puntaje / 1500f));

        generarPiezaNueva();
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

    private void dibujar() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        figura.setProjectionMatrix(camara.combined);
        figura.begin(ShapeRenderer.ShapeType.Filled);

        figura.setColor(0.15f, 0.15f, 0.2f, 1f);
        figura.rect(X_TABLERO - 4, Y_TABLERO - 4, COLUMNAS * TAM_BLOQUE + 8, FILAS * TAM_BLOQUE + 8);

        for (int y = 0; y < FILAS; y++) {
            for (int x = 0; x < COLUMNAS; x++) {
                int celda = tablero[y][x];
                if (celda != 0) {
                    figura.setColor(colorPieza(celda));
                    figura.rect(X_TABLERO + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                }
            }
        }

        if (!gameOver) {
            figura.setColor(colorPieza(piezaColor));
            for (int i = 0; i < piezaActual.length; i++) {
                int x = pivoteX + piezaActual[i][0];
                int y = pivoteY + piezaActual[i][1];
                if (y >= 0 && y < FILAS) {
                    figura.rect(X_TABLERO + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                }
            }
        }

        figura.end();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        fuente.draw(batch, "PUNTAJE: " + puntaje, 400, 430);
        fuente.draw(batch, "ESC: MENU", 400, 400);
        fuente.draw(batch, "P: PAUSA", 400, 370);

        if (pausado) {
            fuente.draw(batch, "PAUSADO", 400, 330);
        }

        if (gameOver) {
            fuente.draw(batch, "GAME OVER", 400, 300);
            fuente.draw(batch, "ENTER: REINICIAR", 400, 270);
        }
        batch.end();
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
    }
}
