package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import juego.JuegoTetris;
import online.ControladorJuego;
import online.HiloServidor;

public class PantallaServidor extends ScreenAdapter implements ControladorJuego {

    private static final int VENTANA_ANCHO = 960;
    private static final int VENTANA_ALTO = 480;
    private static final int MAX_JUGADORES = 2;

    private final JuegoTetris juego;
    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camara;
    private Viewport viewport;
    private GlyphLayout layout;
    private HiloServidor servidor;
    private int conectados = 0;

    public PantallaServidor(JuegoTetris juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        camara = new OrthographicCamera();
        viewport = new ScreenViewport(camara);
        viewport.update(VENTANA_ANCHO, VENTANA_ALTO, true);
        layout = new GlyphLayout();

        servidor = new HiloServidor(this);
        servidor.start();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camara.update();
        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        String titulo = "Servidor en ejecucion";
        layout.setText(font, titulo);
        font.draw(batch, titulo, (VENTANA_ANCHO - layout.width) / 2f, VENTANA_ALTO - 40);

        String estado = "Conectados: " + conectados + "/" + MAX_JUGADORES;
        layout.setText(font, estado);
        font.draw(batch, estado, (VENTANA_ANCHO - layout.width) / 2f, VENTANA_ALTO - 80);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (servidor != null) {
            servidor.terminate();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }

    @Override
    public void conexion(int jugador) {
        if (servidor != null) {
            conectados = servidor.getClientesConectados();
        } else {
            conectados++;
        }
    }

    @Override
    public void desconectado(int jugador) {
        if (servidor != null) {
            conectados = servidor.getClientesConectados();
        } else {
            conectados = Math.max(0, conectados - 1);
        }
    }
}
