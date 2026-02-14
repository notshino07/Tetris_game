package tetris;

import java.util.Random;

public class GeneradorPiezas {
    private final Random random = new Random();

    private final int[][][] piezasBase = {
        { {-1, 0}, {0, 0}, {1, 0}, {2, 0} },   // I
        { {-1, 0}, {0, 0}, {1, 0}, {1, 1} },   // L
        { {-1, 1}, {-1, 0}, {0, 0}, {1, 0} },  // J
        { {0, 0}, {1, 0}, {0, 1}, {1, 1} },    // O
        { {-1, 0}, {0, 0}, {0, 1}, {1, 1} },   // S
        { {-1, 1}, {0, 1}, {0, 0}, {1, 0} },   // Z
        { {-1, 0}, {0, 0}, {1, 0}, {0, 1} }    // T
    };

    public Pieza nuevaPieza() {
        int id = random.nextInt(piezasBase.length);
        int[][] copia = copiarPieza(piezasBase[id]);
        return new Pieza(copia, id + 1);
    }

    private int[][] copiarPieza(int[][] origen) {
        int[][] copia = new int[origen.length][2];
        for (int i = 0; i < origen.length; i++) {
            copia[i][0] = origen[i][0];
            copia[i][1] = origen[i][1];
        }
        return copia;
    }
}
