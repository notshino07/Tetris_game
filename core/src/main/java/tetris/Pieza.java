package tetris;

public class Pieza {
    private final int[][] bloques;
    private final int colorId;

    public Pieza(int[][] bloques, int colorId) {
        this.bloques = bloques;
        this.colorId = colorId;
    }

    public int[][] getBloques() {
        return bloques;
    }

    public int getColorId() {
        return colorId;
    }
}
