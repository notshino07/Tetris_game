package pantalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tetrisonline.game.TetrisGame;

public class PantallaMenu extends ScreenAdapter {

    private final TetrisGame juego;
    private SpriteBatch batch;
    private BitmapFont fuente;

    public PantallaMenu(TetrisGame juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        fuente = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            juego.setScreen(new PantallaJuego(juego));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        fuente.draw(batch, "TETRIS SIMPLE", 230, 380);
        fuente.draw(batch, "ENTER: JUGAR", 230, 330);
        fuente.draw(batch, "ESC: SALIR", 230, 300);
        fuente.draw(batch, "CONTROLES:", 230, 250);
        fuente.draw(batch, "FLECHAS: MOVER / BAJAR", 230, 220);
        fuente.draw(batch, "ARRIBA o X: ROTAR", 230, 190);
        fuente.draw(batch, "ESPACIO: CAIDA RAPIDA", 230, 160);
        fuente.draw(batch, "P: PAUSAR", 230, 130);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (fuente != null) {
            fuente.dispose();
        }
    }
}
