package juego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.audio.Music;
import pantallas.PantallaMenu;
import utiles.Recursos;

public class JuegoTetris extends Game {
    private final Recursos recursos = new Recursos();

    @Override
    public void create() {
        recursos.cargarMusicaFondo();
        recursos.reproducirMusica();
        setScreen(new PantallaMenu(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        recursos.dispose();
        super.dispose();
    }

    public Music getMusica() {
        return recursos.getMusica();
    }

    public void setVolumen(float volumen) {
        recursos.setVolumen(volumen);
    }

    public float getVolumen() {
        return recursos.getVolumen();
    }

    public Recursos getRecursos() {
        return recursos;
    }
}
