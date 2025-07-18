package Launcher;

import Client.TicTacToeClient;

import javax.swing.*;

public class ClientOneLauncher {

    public static void main(String[] args)
    {
        TicTacToeClient application;


        if(args.length == 0)
        {
            // Use default localhost if no IP is passed
            application = new TicTacToeClient("127.0.0.1");
        }
        else
        {
            // Use IP argument from command line
            application = new TicTacToeClient(args[0]); //use args
        }

        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
