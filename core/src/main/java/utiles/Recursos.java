package utiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class Recursos {
    private Music musica;
    private float volumen = 1.0f;

    public void cargarMusicaFondo() {
        if (musica == null) {
            musica = Gdx.audio.newMusic(Gdx.files.internal("tetris.mp3"));
            musica.setLooping(true);
        }
        musica.setVolume(volumen);
    }

    public void reproducirMusica() {
        if (musica != null && !musica.isPlaying()) {
            musica.play();
        }
    }

    public void detenerMusica() {
        if (musica != null && musica.isPlaying()) {
            musica.stop();
        }
    }

    public void setVolumen(float volumen) {
        this.volumen = Math.max(0f, Math.min(1f, volumen));
        if (musica != null) {
            musica.setVolume(this.volumen);
        }
    }

    public float getVolumen() {
        return volumen;
    }

    public Music getMusica() {
        return musica;
    }

    public Sound cargarSonido(String ruta) {
        return Gdx.audio.newSound(Gdx.files.internal(ruta));
    }

    public void dispose() {
        if (musica != null) {
            musica.dispose();
        }
    }
}
