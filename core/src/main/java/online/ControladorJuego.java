package online;

public interface ControladorJuego {
    void conectar(int numeroJugador);

    void empezar();

    void actualizarTableroRemoto(int numeroJugador, String tablero, int puntaje, int lineas, boolean gameOver, boolean ko);

    void clienteDesconectado(int numeroJugador);

    void servidorLleno();

    void servidorCerrado();

    void volverAlMenu();

    void recibirBasura(int cantidad);

    void recibirBasuraDirigida(int objetivo, int cantidad);
}
