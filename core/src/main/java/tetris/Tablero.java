package tetris;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import jugador.ControlJugador;
public class Tablero {
    private static final int COLUMNAS = 10;
    private static final int FILAS = 20;
    private static final int TAM_BLOQUE = 24;
    private static final int Y_TABLERO = 20;

    private final String nombre;
    private final int xTablero;
    private final int[][] tablero = new int[FILAS][COLUMNAS];
    private final GeneradorPiezas generador;

    private Pieza piezaActual;
    private int pivoteX;
    private int pivoteY;

    private float tiempoCaida = 0f;
    private float intervaloCaida = 0.5f;
    private boolean pausado = false;
    private boolean gameOver = false;

    private int puntaje;
    private int lineasTotales;
    private int nivel;
    private int velocidadCaida;

    private boolean mostrandoNivel = false;
    private float tiempoMostrarNivel = 0f;
    private int nivelActual = 1;
    private final GlyphLayout layoutNivel = new GlyphLayout();

    public Tablero(String nombre, int xTablero, GeneradorPiezas generador) {
        this.nombre = nombre;
        this.xTablero = xTablero;
        this.generador = generador;
    }

    public void reiniciarPartida() {
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
        tiempoCaida = 0f;
        mostrandoNivel = false;
        tiempoMostrarNivel = 0f;
        nivelActual = nivel;
        generarPiezaNueva();
    }

    private void generarPiezaNueva() {
        piezaActual = generador.nuevaPieza();
        pivoteX = COLUMNAS / 2;
        pivoteY = FILAS - 2;

        if (!puedeMover(0, 0, piezaActual)) {
            gameOver = true;
        }
    }

    public void actualizar(float delta) {
        if (!pausado && !gameOver) {
            tiempoCaida += delta;
            if (tiempoCaida >= intervaloCaida) {
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

    public void procesarEntrada(ControlJugador control) {
        if (gameOver) {
            if (control.presionoReiniciar()) {
                reiniciarPartida();
            }
            return;
        }
        if (control.presionoPausa()) {
            pausado = !pausado;
            return;
        }
        if (pausado) {
            return;
        }
        if (control.presionoIzquierda()) {
            mover(-1, 0);
        }
        if (control.presionoDerecha()) {
            mover(1, 0);
        }
        if (control.presionoAbajo()) {
            if (!mover(0, -1)) {
                fijarPieza();
            }
        }
        if (control.presionoRotarDerecha()) {
            rotarDerecha();
        }
        if (control.presionoRotarIzquierda()) {
            rotarIzquierda();
        }
        if (control.presionoHardDrop()) {
            hardDrop();
        }
    }

    public void hardDrop() {
        while (puedeMover(0, -1, piezaActual)) {
            pivoteY -= 1;
        }
        fijarPieza();
        tiempoCaida = 0f;
    }

    private boolean mover(int dx, int dy) {
        if (puedeMover(dx, dy, piezaActual)) {
            pivoteX += dx;
            pivoteY += dy;
            return true;
        }
        return false;
    }

    private boolean puedeMover(int dx, int dy, Pieza pieza) {
        int[][] forma = pieza.getBloques();
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
        int[][] rotada = rotar(piezaActual.getBloques(), true);
        Pieza nueva = new Pieza(rotada, piezaActual.getColorId());
        if (puedeMover(0, 0, nueva)) {
            piezaActual = nueva;
        }
    }

    private void rotarIzquierda() {
        int[][] rotada = rotar(piezaActual.getBloques(), false);
        Pieza nueva = new Pieza(rotada, piezaActual.getColorId());
        if (puedeMover(0, 0, nueva)) {
            piezaActual = nueva;
        }
    }

    private int[][] rotar(int[][] forma, boolean derecha) {
        int[][] rotada = new int[forma.length][2];
        for (int i = 0; i < forma.length; i++) {
            int x = forma[i][0];
            int y = forma[i][1];
            if (derecha) {
                rotada[i][0] = -y;
                rotada[i][1] = x;
            } else {
                rotada[i][0] = y;
                rotada[i][1] = -x;
            }
        }
        return rotada;
    }

    private void actualizarVelocidadCaida() {
        velocidadCaida = Math.max(80, 800 - (nivel - 1) * 70);
        intervaloCaida = velocidadCaida / 1000f;
    }

    private void fijarPieza() {
        int[][] forma = piezaActual.getBloques();
        for (int i = 0; i < forma.length; i++) {
            int x = pivoteX + forma[i][0];
            int y = pivoteY + forma[i][1];
            if (y >= 0 && y < FILAS && x >= 0 && x < COLUMNAS) {
                tablero[y][x] = piezaActual.getColorId();
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
        }

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

    public void dibujar(ShapeRenderer figura) {
        if (mostrandoNivel) {
            float alpha = Math.min(1f, tiempoMostrarNivel / 0.3f) * 0.6f;
            figura.setColor(1f, 1f, 1f, alpha);
            figura.rect(0, 0, 960, 480);
        }

        figura.setColor(0.15f, 0.15f, 0.2f, 1f);
        figura.rect(
            xTablero - 4,
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
                        xTablero + x * TAM_BLOQUE,
                        Y_TABLERO + y * TAM_BLOQUE,
                        TAM_BLOQUE - 1,
                        TAM_BLOQUE - 1
                    );
                }
            }
        }

        if (!gameOver && piezaActual != null) {
            figura.setColor(colorPieza(piezaActual.getColorId()));
            int[][] forma = piezaActual.getBloques();
            for (int i = 0; i < forma.length; i++) {
                int x = pivoteX + forma[i][0];
                int y = pivoteY + forma[i][1];
                if (y >= 0 && y < FILAS) {
                    figura.rect(
                        xTablero + x * TAM_BLOQUE,
                        Y_TABLERO + y * TAM_BLOQUE,
                        TAM_BLOQUE - 1,
                        TAM_BLOQUE - 1
                    );
                }
            }
        }
    }

    public void dibujarUI(SpriteBatch batch, BitmapFont fuente) {
        int uiX = xTablero + COLUMNAS * TAM_BLOQUE + 10;
        int uiY = 430;
        fuente.draw(batch, nombre, xTablero, 465);
        fuente.draw(batch, "NIVEL: " + nivel, uiX, uiY);
        fuente.draw(batch, "LINEAS: " + lineasTotales, uiX, uiY - 30);
        fuente.draw(batch, "PUNTAJE: " + puntaje, uiX, uiY - 60);
        fuente.draw(batch, "ESC: MENU", uiX, uiY - 90);
        fuente.draw(batch, (nombre.equals("P1") ? "P: PAUSA" : "O: PAUSA"), uiX, uiY - 120);

        if (pausado) {
            fuente.draw(batch, "PAUSADO", uiX, uiY - 150);
        }

        if (gameOver) {
            fuente.draw(batch, "GAME OVER", uiX, uiY - 180);
            fuente.draw(batch, "ENTER: REINICIAR", uiX, uiY - 210);
        }

        if (mostrandoNivel) {
            float prevX = fuente.getData().scaleX;
            float prevY = fuente.getData().scaleY;
            fuente.getData().setScale(2.6f);
            String texto = "NIVEL " + nivelActual;
            layoutNivel.setText(fuente, texto);
            float ancho = Gdx.graphics.getWidth();
            float alto = Gdx.graphics.getHeight();
            float x = (ancho - layoutNivel.width) / 2f;
            float y = (alto + layoutNivel.height) / 2f;
            fuente.draw(batch, texto, x, y);
            fuente.getData().setScale(prevX, prevY);
        }
    }

    private void mostrarSubidaNivel(int nuevoNivel) {
        nivelActual = nuevoNivel;
        mostrandoNivel = true;
        tiempoMostrarNivel = 1f;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public int getLineasEliminadas() {
        return lineasTotales;
    }

    public boolean isGameOver() {
        return gameOver;
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
