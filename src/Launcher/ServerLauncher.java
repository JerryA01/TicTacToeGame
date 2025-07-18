package Launcher;

import Server.TicTacToeServer;

import javax.swing.*;

public class ServerLauncher {

    public static void main(String[] args)
    {
        TicTacToeServer application = new TicTacToeServer();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        application.execute();
    }

}
