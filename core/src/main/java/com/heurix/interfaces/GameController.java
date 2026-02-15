package com.heurix.interfaces;

import com.heurix.network.TetrisClientThread;

public interface GameController {
    void connect(int playerNum);
    void start();
    void onPieceReceived(String tipo);
    void onBoardUpdate(int playerNum, String rawBoard);
    void onGarbage(int cantidad);
    void onWin();
    void attachClientThread(TetrisClientThread clientThread);
}
