package jugador;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class ControlJugador {
    private final int[] izquierda;
    private final int[] derecha;
    private final int[] abajo;
    private final int[] rotarDerecha;
    private final int[] rotarIzquierda;
    private final int[] pausa;
    private final int[] hardDrop;
    private final int[] reiniciar;

    public ControlJugador(
        int[] izquierda,
        int[] derecha,
        int[] abajo,
        int[] rotarDerecha,
        int[] rotarIzquierda,
        int[] pausa,
        int[] hardDrop,
        int[] reiniciar
    ) {
        this.izquierda = izquierda;
        this.derecha = derecha;
        this.abajo = abajo;
        this.rotarDerecha = rotarDerecha;
        this.rotarIzquierda = rotarIzquierda;
        this.pausa = pausa;
        this.hardDrop = hardDrop;
        this.reiniciar = reiniciar;
    }

    public static ControlJugador crearParaJugador1() {
        return new ControlJugador(
            new int[] { Input.Keys.LEFT },
            new int[] { Input.Keys.RIGHT },
            new int[] { Input.Keys.DOWN },
            new int[] { Input.Keys.UP, Input.Keys.X },
            new int[] { Input.Keys.Z },
            new int[] { Input.Keys.P },
            new int[] { Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT },
            new int[] { Input.Keys.ENTER }
        );
    }

    public static ControlJugador crearParaJugador2() {
        return new ControlJugador(
            new int[] { Input.Keys.A },
            new int[] { Input.Keys.D },
            new int[] { Input.Keys.S },
            new int[] { Input.Keys.W, Input.Keys.R },
            new int[] { Input.Keys.Q },
            new int[] { Input.Keys.O },
            new int[] { Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT },
            new int[] { Input.Keys.ENTER }
        );
    }

    public boolean presionoIzquierda() {
        return presiono(izquierda);
    }

    public boolean presionoDerecha() {
        return presiono(derecha);
    }

    public boolean presionoAbajo() {
        return presiono(abajo);
    }

    public boolean presionoRotarDerecha() {
        return presiono(rotarDerecha);
    }

    public boolean presionoRotarIzquierda() {
        return presiono(rotarIzquierda);
    }

    public boolean presionoPausa() {
        return presiono(pausa);
    }

    public boolean presionoHardDrop() {
        return presiono(hardDrop);
    }

    public boolean presionoReiniciar() {
        return presiono(reiniciar);
    }

    private boolean presiono(int[] teclas) {
        if (teclas == null) {
            return false;
        }
        for (int tecla : teclas) {
            if (Gdx.input.isKeyJustPressed(tecla)) {
                return true;
            }
        }
        return false;
    }
}
