package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import juego.JuegoTetris;
import jugador.ControlJugador;
import tetris.GeneradorPiezas;
import tetris.Tablero;
public class PantallaJuego extends ScreenAdapter {
    private static final int COLUMNAS = 10;
    private static final int TAM_BLOQUE = 24;
    private static final int TABLERO_SEPARACION = 140;
    private static final int X_TABLERO_1 = 80;
    private static final int X_TABLERO_2 = X_TABLERO_1 + COLUMNAS * TAM_BLOQUE + TABLERO_SEPARACION;
    private static final int VENTANA_ANCHO = 960;
    private static final int VENTANA_ALTO = 480;

    private final JuegoTetris juego;

    private ShapeRenderer figura;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private OrthographicCamera camara;

    private final GeneradorPiezas generador = new GeneradorPiezas();
    private final Tablero jugador1 = new Tablero("P1", X_TABLERO_1, generador);
    private final Tablero jugador2 = new Tablero("P2", X_TABLERO_2, generador);
    private final ControlJugador controlP1 = ControlJugador.crearParaJugador1();
    private final ControlJugador controlP2 = ControlJugador.crearParaJugador2();
    private boolean partidaTerminada = false;

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

        jugador1.procesarEntrada(controlP1);
        jugador2.procesarEntrada(controlP2);
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
}
