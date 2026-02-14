package pantalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.tetrisonline.game.TetrisGame;

public class PantallaMenu extends ScreenAdapter {

    private final TetrisGame juego;
    private Texture fondoMenu;
    private Stage stage;
    private Skin skin;
    private Slider volumenSlider;
    private Label volumenLabel;
    private Label controlesLabel;
    private Table panel;
    private Actor[] focusOrder;
    private int focusIndex = 0;
    private TextButton playButton;
    private TextButton controlesButton;
    private TextButton volumenButton;
    private TextButton backButton;
    private Label titulo;
    private boolean mostrandoControles = false;
    private boolean mostrandoVolumen = false;

    public PantallaMenu(TetrisGame juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondoMenu = new Texture("menufondo.png");
        skin = createSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (juego.getMusica() != null && !juego.getMusica().isPlaying()) {
            juego.getMusica().play();
        }

        Image background = new Image(new TextureRegionDrawable(new TextureRegion(fondoMenu)));
        background.setFillParent(true);
        stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        panel = new Table();
        panel.setBackground(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.55f)));

        titulo = new Label("TETRIS", skin, "title");
        titulo.setColor(new Color(1f, 0.9f, 0.6f, 1f));
        playButton = new TextButton("PLAY", skin);
        controlesButton = new TextButton("CONTROLES", skin);
        volumenButton = new TextButton("VOLUMEN", skin);
        backButton = new TextButton("BACK", skin);
        backButton.setVisible(false);

        volumenSlider = new Slider(0, 100, 1, false, skin);
        volumenSlider.setValue(juego.getVolumen() * 100f);
        volumenLabel = new Label("VOLUMEN: 100%", skin);
        volumenLabel.setColor(new Color(1f, 0.9f, 0.6f, 1f));

        controlesLabel = new Label(
            "P1:\nFLECHAS: MOVER / BAJAR\nARRIBA o X: ROTAR\nZ: ROTAR IZQ\nALT: CAIDA RAPIDA\nP: PAUSAR\n\n"
                + "P2:\nA/D: MOVER\nS: BAJAR\nW o R: ROTAR DER\nQ: ROTAR IZQ\nSHIFT: CAIDA RAPIDA\nO: PAUSAR",
            skin
        );
        controlesLabel.setVisible(false);

        root.add(panel).pad(16);

        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                juego.setScreen(new PantallaJuego(juego));
            }
        });

        controlesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                mostrarSoloControles(true);
            }
        });
        volumenButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                mostrarSoloVolumen(true);
            }
        });
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (mostrandoControles) {
                    mostrarSoloControles(false);
                } else if (mostrandoVolumen) {
                    mostrarSoloVolumen(false);
                }
            }
        });

        volumenSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = volumenSlider.getValue();
                juego.setVolumen(value / 100f);
                updateSelection();
            }
        });

        showMenuPanel();
        addFocusOnMouse(playButton);
        addFocusOnMouse(controlesButton);
        addFocusOnMouse(volumenButton);
        addFocusOnMouse(volumenSlider);
        addFocusOnMouse(backButton);
        stage.addListener(new MenuInputListener(playButton, controlesButton, volumenButton, backButton));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
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
        if (fondoMenu != null) {
            fondoMenu.dispose();
        }
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }

    private Skin createSkin() {
        Skin s = new Skin();
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.6f);
        s.add("default-font", font);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(2.2f);
        s.add("title-font", titleFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTex = new Texture(pixmap);
        pixmap.dispose();
        s.add("white", whiteTex);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        s.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;
        s.add("title", titleStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = s.newDrawable("white", new Color(0.15f, 0.15f, 0.2f, 0.9f));
        buttonStyle.down = s.newDrawable("white", new Color(0.1f, 0.1f, 0.14f, 1f));
        buttonStyle.over = s.newDrawable("white", new Color(0.2f, 0.2f, 0.28f, 1f));
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        s.add("default", buttonStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = s.newDrawable("white", new Color(0.12f, 0.12f, 0.18f, 0.9f));
        sliderStyle.knob = s.newDrawable("white", new Color(0.85f, 0.85f, 0.9f, 1f));
        sliderStyle.knobOver = s.newDrawable("white", new Color(1f, 1f, 1f, 1f));
        sliderStyle.knobDown = s.newDrawable("white", new Color(0.9f, 0.9f, 0.95f, 1f));
        s.add("default-horizontal", sliderStyle);

        return s;
    }

    private class MenuInputListener extends InputListener {
        private final TextButton playButton;
        private final TextButton controlesButton;
        private final TextButton volumenButton;
        private final TextButton backButton;

        private MenuInputListener(TextButton playButton, TextButton controlesButton, TextButton volumenButton, TextButton backButton) {
            this.playButton = playButton;
            this.controlesButton = controlesButton;
            this.volumenButton = volumenButton;
            this.backButton = backButton;
        }

        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == com.badlogic.gdx.Input.Keys.UP) {
                setFocus((focusIndex - 1 + focusOrder.length) % focusOrder.length);
                return true;
            }
            if (keycode == com.badlogic.gdx.Input.Keys.DOWN) {
                setFocus((focusIndex + 1) % focusOrder.length);
                return true;
            }
            if (keycode == com.badlogic.gdx.Input.Keys.LEFT || keycode == com.badlogic.gdx.Input.Keys.RIGHT) {
                if (stage.getKeyboardFocus() == volumenSlider) {
                    float delta = (keycode == com.badlogic.gdx.Input.Keys.RIGHT) ? 1f : -1f;
                    volumenSlider.setValue(volumenSlider.getValue() + delta);
                    return true;
                }
            }
            if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                Actor focused = stage.getKeyboardFocus();
                if (focused == playButton) {
                    playButton.fire(new ChangeListener.ChangeEvent());
                    return true;
                }
                if (focused == controlesButton) {
                    controlesButton.fire(new ChangeListener.ChangeEvent());
                    return true;
                }
                if (focused == volumenButton) {
                    volumenButton.fire(new ChangeListener.ChangeEvent());
                    return true;
                }
                if (focused == backButton) {
                    backButton.fire(new ChangeListener.ChangeEvent());
                    return true;
                }
            }
            if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                if (mostrandoControles) {
                    mostrarSoloControles(false);
                    return true;
                } else if (mostrandoVolumen) {
                    mostrarSoloVolumen(false);
                    return true;
                } else {
                    Gdx.app.exit();
                    return true;
                }
            }
            return false;
        }
    }

    private void setFocus(int index) {
        if (focusOrder == null || focusOrder.length == 0) {
            return;
        }
        focusIndex = Math.max(0, Math.min(index, focusOrder.length - 1));
        stage.setKeyboardFocus(focusOrder[focusIndex]);
        updateSelection();
    }

    private void setFocusOrder(Actor... actors) {
        focusOrder = actors;
        focusIndex = 0;
        if (focusOrder.length > 0) {
            stage.setKeyboardFocus(focusOrder[0]);
        }
        updateSelection();
    }

    private void updateSelection() {
        if (mostrandoControles) {
            backButton.setText((focusIndex == 0 ? "- " : "  ") + "BACK");
            return;
        }
        if (mostrandoVolumen) {
            String volPrefix = focusIndex == 0 ? "- " : "  ";
            volumenLabel.setText(volPrefix + "VOLUMEN: " + (int) volumenSlider.getValue() + "%");
            backButton.setText((focusIndex == 1 ? "- " : "  ") + "BACK");
            return;
        }
        playButton.setText((focusIndex == 0 ? "- " : "  ") + "PLAY");
        controlesButton.setText((focusIndex == 1 ? "- " : "  ") + "CONTROLES");
        volumenButton.setText((focusIndex == 2 ? "- " : "  ") + "VOLUMEN");
    }

    private void addFocusOnMouse(Actor actor) {
        actor.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int index = indexOfActor(actor);
                if (index >= 0) {
                    setFocus(index);
                }
                return false;
            }
        });
    }

    private int indexOfActor(Actor actor) {
        if (focusOrder == null) {
            return -1;
        }
        for (int i = 0; i < focusOrder.length; i++) {
            if (focusOrder[i] == actor) {
                return i;
            }
        }
        return -1;
    }

    private void mostrarSoloControles(boolean mostrar) {
        mostrandoControles = mostrar;
        mostrandoVolumen = false;
        if (mostrar) {
            showControlesPanel();
        } else {
            showMenuPanel();
        }
    }

    private void mostrarSoloVolumen(boolean mostrar) {
        mostrandoVolumen = mostrar;
        mostrandoControles = false;
        if (mostrar) {
            showVolumenPanel();
        } else {
            showMenuPanel();
        }
    }

    private void showMenuPanel() {
        panel.clearChildren();
        controlesLabel.setVisible(false);
        panel.add(titulo).padBottom(28).row();
        panel.add(playButton).width(320).height(74).padBottom(14).row();
        panel.add(controlesButton).width(320).height(74).padBottom(12).row();
        panel.add(volumenButton).width(320).height(74).padBottom(18).row();
        panel.row();
        panel.pack();
        setFocusOrder(playButton, controlesButton, volumenButton);
    }

    private void showControlesPanel() {
        panel.clearChildren();
        controlesLabel.setVisible(true);
        panel.add(controlesLabel).padTop(6).padBottom(16).row();
        panel.add(backButton).width(240).height(60).padTop(6);
        panel.pack();
        setFocusOrder(backButton);
    }

    private void showVolumenPanel() {
        panel.clearChildren();
        controlesLabel.setVisible(false);
        panel.add(volumenLabel).padBottom(10).row();
        panel.add(volumenSlider).width(420).height(36).padBottom(16).row();
        panel.add(backButton).width(240).height(60).padTop(6);
        panel.pack();
        setFocusOrder(volumenSlider, backButton);
    }
}
