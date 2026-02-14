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

import java.util.Random;

public class PantallaJuego extends ScreenAdapter {

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

    private static final int COLUMNAS = 10;
    private static final int FILAS = 20;
    private static final int TAM_BLOQUE = 24;
    private static final int Y_TABLERO = 20;
    private static final int TABLERO_SEPARACION = 140;
    private static final int X_TABLERO_1 = 80;
    private static final int X_TABLERO_2 = X_TABLERO_1 + COLUMNAS * TAM_BLOQUE + TABLERO_SEPARACION;

    private final TableroTetris jugador1 = new TableroTetris("P1", X_TABLERO_1);
    private final TableroTetris jugador2 = new TableroTetris("P2", X_TABLERO_2);
    private boolean partidaTerminada = false;

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

        if (juego.getMusica() != null && !juego.getMusica().isPlaying()) {
            juego.getMusica().play();
        }

        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        partidaTerminada = false;
    }

    @Override
    public void render(float delta) {
        procesarEntrada();

        jugador1.actualizar(delta);
        jugador2.actualizar(delta);

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

        figura.end();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        jugador1.dibujarUI(batch, fuente);
        jugador2.dibujarUI(batch, fuente);
        batch.end();
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

    private class TableroTetris {
        private final String nombre;
        private final int xTablero;
        private final int[][] tablero = new int[FILAS][COLUMNAS];

        private int[][] piezaActual;
        private int piezaColor;
        private int pivoteX;
        private int pivoteY;

        private float tiempoCaida = 0f;
        private float intervaloCaida = 0.5f;

        private int puntaje = 0;
        private int lineasEliminadas = 0;
        private boolean pausado = false;
        private boolean gameOver = false;

        private TableroTetris(String nombre, int xTablero) {
            this.nombre = nombre;
            this.xTablero = xTablero;
        }

        private void reiniciarPartida() {
            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    tablero[y][x] = 0;
                }
            }
            puntaje = 0;
            lineasEliminadas = 0;
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

        private void actualizar(float delta) {
            if (!pausado && !gameOver) {
                tiempoCaida += delta;
                if (tiempoCaida >= intervaloCaida) {
                    tiempoCaida = 0f;
                    if (!mover(0, -1)) {
                        fijarPieza();
                    }
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
            lineasEliminadas += lineas;
            intervaloCaida = Math.max(0.12f, 0.5f - (puntaje / 1500f));

            generarPiezaNueva();
        }

        private void hardDrop() {
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
            figura.setColor(0.15f, 0.15f, 0.2f, 1f);
            figura.rect(xTablero - 4, Y_TABLERO - 4, COLUMNAS * TAM_BLOQUE + 8, FILAS * TAM_BLOQUE + 8);

            for (int y = 0; y < FILAS; y++) {
                for (int x = 0; x < COLUMNAS; x++) {
                    int celda = tablero[y][x];
                    if (celda != 0) {
                        figura.setColor(colorPieza(celda));
                        figura.rect(xTablero + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                    }
                }
            }

            if (!gameOver) {
                figura.setColor(colorPieza(piezaColor));
                for (int i = 0; i < piezaActual.length; i++) {
                    int x = pivoteX + piezaActual[i][0];
                    int y = pivoteY + piezaActual[i][1];
                    if (y >= 0 && y < FILAS) {
                        figura.rect(xTablero + x * TAM_BLOQUE, Y_TABLERO + y * TAM_BLOQUE, TAM_BLOQUE - 1, TAM_BLOQUE - 1);
                    }
                }
            }
        }

        private void dibujarUI(SpriteBatch batch, BitmapFont fuente) {
            int uiX = xTablero + COLUMNAS * TAM_BLOQUE + 10;
            int uiY = 430;
            fuente.draw(batch, nombre, xTablero, 465);
            fuente.draw(batch, "PUNTAJE: " + puntaje, uiX, uiY);
            fuente.draw(batch, "LINEAS: " + lineasEliminadas, uiX, uiY - 30);
            fuente.draw(batch, "ESC: MENU", uiX, uiY - 60);
            fuente.draw(batch, (nombre.equals("P1") ? "P: PAUSA" : "O: PAUSA"), uiX, uiY - 90);

            if (pausado) {
                fuente.draw(batch, "PAUSADO", uiX, uiY - 120);
            }

            if (gameOver) {
                fuente.draw(batch, "GAME OVER", uiX, uiY - 150);
                fuente.draw(batch, "ENTER: REINICIAR", uiX, uiY - 180);
            }
        }

        private int getPuntaje() {
            return puntaje;
        }

        private int getLineasEliminadas() {
            return lineasEliminadas;
        }

        private boolean isGameOver() {
            return gameOver;
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
}
