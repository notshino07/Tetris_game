package com.tetrisonline.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import pantalla.PantallaMenu;

public class TetrisGame extends Game {
    private Music musica;
    private float volumen = 1.0f;

    @Override
    public void create() {
        musica = Gdx.audio.newMusic(Gdx.files.internal("tetris.mp3"));
        musica.setLooping(true);
        musica.setVolume(volumen);
        musica.play();
        // Inicia el juego mostrando el menu principal
        setScreen(new PantallaMenu(this));
    }

    @Override
    public void render() {
        // Importante: permite que la pantalla actual se renderice
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void dispose() {
        if (musica != null) {
            musica.dispose();
        }
        super.dispose();
    }

    public Music getMusica() {
        return musica;
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
}
