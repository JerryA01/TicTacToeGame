package Launcher;

import Client.TicTacToeClient;

import javax.swing.*;

public class ClientTwoLauncher {

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
            application = new TicTacToeClient(args[0]); // Use IP argument from command line
        }

        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
