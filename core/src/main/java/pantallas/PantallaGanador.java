package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import juego.JuegoTetris;

public class PantallaGanador extends ScreenAdapter {

    private final JuegoTetris juego;
    private final int ganador;
    private final int jugadorLocal;
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
    private Sound derrotaSound;
    private final GlyphLayout layout = new GlyphLayout();

    public PantallaGanador(JuegoTetris juego, int ganador, int jugadorLocal, int puntajeP1, int puntajeP2, int lineasP1, int lineasP2) {
        this.juego = juego;
        this.ganador = ganador;
        this.jugadorLocal = jugadorLocal;
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
        if (ganador == jugadorLocal) {
            victoriaSound = juego.getRecursos().cargarSonido("victoria.wav");
            victoriaSound.play(1f);
        } else {
            derrotaSound = juego.getRecursos().cargarSonido("lose.wav");
            derrotaSound.play(1f);
        }
    }

    @Override
    public void render(float delta) {
        stage.act(delta);

        batch.begin();
        batch.draw(overlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        String titulo = (ganador == jugadorLocal) ? "GANASTE" : "PERDISTE";
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
        if (derrotaSound != null) {
            derrotaSound.dispose();
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
