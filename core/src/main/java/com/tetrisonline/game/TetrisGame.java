package com.tetrisonline.game;

import com.badlogic.gdx.Game;
import pantalla.PantallaMenu;

public class TetrisGame extends Game {

    @Override
    public void create() {
        // Inicia el juego mostrando el menú principal
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
        super.dispose();
    }
}
