package Launcher;

import Client.TicTacToeClient;

import javax.swing.*;

public class ClientTwoLauncher {

    public static void main(String[] args)
    {
        TicTacToeClient application; //declare client application

        //if no command line args
        if(args.length == 0)
        {
            application = new TicTacToeClient("127.0.0.1");
        }
        else
        {
            application = new TicTacToeClient(args[0]); //use args
        }

        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
