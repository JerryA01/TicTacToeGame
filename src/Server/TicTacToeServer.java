package Server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Formatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;

public class TicTacToeServer extends JFrame {

    //Altogether We'll have four 5 threads when everything is connected..
    //Main Thread which sets up the connections
    //Thread 1 (Manages Client 1s connection and game state)
    //Thread 2 (Manages client 2s connection and game state)
    //Client Thread 1 (This is run separately on the client machines, interacting with Thread 1)
    //Client Thread 2 (This is run separately on the client machine, interacting with Thread 2)
    //Synchronization happens between Thread 1 and Thread 2 to ensure the game progresses turn by turn.This is done using Lock and Condition

    private String[] board = new String[9];  //tic-tac-toe board
    private JTextArea outputArea; //for outputting moves
    private Player[] players; //array of players
    private ServerSocket server; //server socket to connect with Clients.  Represents a TCP socket connection to the client.
    private int currentPlayer; //keep track of player with current move
    private final static int PLAYER_X = 0; //constant for first player
    private final static int PLAYER_O = 1; //constant for second player
    private final static String[] MARKS = {"X","O"}; //array of Marks
    private ExecutorService runGame; //will run players. As we know, we use ExecuorService to run our Runnables(these are
    //used to run client threads
    private Lock gameLock; //to lock game for synchronization between the 2 player threads
    private Condition otherPlayerConnected; //to wait for other player to connect
    private Condition otherPlayerTurn; //to wait for other players turn
    private Condition otherPlayerGo;
    private static volatile boolean gameOver = false;
    private volatile boolean wantsToPlayAgain = false;
    private Condition bothPlayersResponded; //both players responses to playing again
    private volatile boolean validResponse = false;
    private int rematchResponses;



    //set up tic-tac-toe server and GUI that displays messages
    public TicTacToeServer()
    {
        super("Tic-Tac-Toe-Server"); //set title of window

        //create ExecutorService with a thread for each player. This is how we create our threads
        runGame = Executors.newFixedThreadPool(2); //This specifies how many threads can run simultaneously at the same time we only need 2
        gameLock = new ReentrantLock(); //create lock for game so we can create conditions for each thread. One go, the other stop. The other go, one stop.

        //condition variable for both players being connected. We'll use this to alert both Thread 1 and Thread 2 that both players have connected, so game can start
        otherPlayerConnected = gameLock.newCondition();


        //condition variable for the other player's turn, We'll use this to alert each Thread when it's their go and when one Thread needs to be locked allowing the other to go.
        otherPlayerTurn = gameLock.newCondition();


        //Our client has a board that is from 0-8. We use this board to keep up to date with each client's moves. The client must first send their location to us the server,
        // we then validate it before sending it back off to the opposite client. We send it to the opposite client so their board also gets updated.
        for (int i = 0; i < 9; i++) {
            board[i] = "";
        }

        //Each player will represent a Thread and client communication
            players = new Player[2]; //puts 2 Player objects in our players array

        //to store the currentPlayer,
            currentPlayer = PLAYER_X; //set current player to first player

        try
        {
            // === Server Setup ===
            // Initialize server socket, thread pool, and game state
            server = new ServerSocket(12345,2); //set up ServerSocket
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            System.exit(1);
        }

        outputArea = new JTextArea(); //create JTextArea for output

        add(outputArea, BorderLayout.CENTER);
        outputArea.setText("Server awaiting Connections\n");

        setSize(300,300); //set size of window
        setVisible(true); //show window

    } //end of constructor


    public void execute()
    {
        //wait for each client to connect so the game can be played
        for (int i = 0; i < players.length; i++) {
            try  //wait for connection, create player, start Runnable
            {
                // === Player Thread Initialization ===
                // Main thread pauses for incoming client connection.
                // On connection, a Player object is created to manage communication.
                // Each Player runs concurrently using ExecutorService, invoking run() automatically.
                players[i] = new Player(server.accept(), i);
                runGame.execute(players[i]);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
                System.exit(1);
            }
        }

        //both clients have connected to the server
        gameLock.lock(); //Once both clients connect we lock

        try
        {
            //we currently have Thread1 waiting on a condition to check if the other player has connected, since they have we let them know and then unlock our Main thread
            players[PLAYER_X].setSuspended(false); //set suspended for player x false (this is our conditions guarded wait so the thread doesn't wake up unexpectedly.)
            otherPlayerConnected.signal(); //wake up Thread1 Player X
        }

        finally {
            //main thread is done, we now begin player X execution from the point where it was waiting otherPlayerConnected.await();
            gameLock.unlock();
        }
    }

    private void displayMessage(final String messageToDisplay)
    {
        //display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {  //updates outPutArea
                        outputArea.append(messageToDisplay);
                    }
                }
        );
    }

    public boolean validateAndMove(int location, int player)
    {
        //if a player thread tried to access this without being current player its told to wait
        // Ensures only the current player can access game logic at a time
        while(player != currentPlayer)
        {
            //lock whichever client thread currently moved
            gameLock.lock(); //we always lock before a condition to not allow for race conditions
            try
            {
                // === Turn-Based Synchronization ===
                // Wait until it's the current player's turn
                otherPlayerTurn.await();
            }
            catch (InterruptedException exception)
            {
                exception.printStackTrace();
            }
            finally {
                gameLock.unlock(); //unlock game after waiting, always call unlock after a condition so that other thread can gain access
            }
        }


        if(!isOccupied(location))
        {
            board[location] = MARKS[currentPlayer]; //set move on board

            //currentPlayer is now other player so that they can go now and not get stuck on while player!= current player
            currentPlayer = (currentPlayer + 1) % 2; //change player, this is a complicated way of doing it but its essentially just switching the player around

            //now that we've switched to other player let them know the first players move so they can update it on their board
            players[currentPlayer].otherPlayerMoved(location);

            gameLock.lock(); //we lock first as we must lock first before signaling another thread, also to ensure we don't get race conditions

            try
            {
                otherPlayerTurn.signal(); //tell the other player they can go now, so we've set current player to them and now we've also signalled them
            }
            finally {
                gameLock.unlock(); //unlock game after signaling
            }
            return true; //notify player that move was valid

        }
        else  //move was not valid
        {
            return false; //notify player that move was invalid
        }
    }

    //checks if a location a client has picked on the board has already been occupied
    public boolean isOccupied(int location)
    {
        if(board[location].equals(MARKS[PLAYER_X]) || board[location].equals(MARKS[PLAYER_O]))
        {
            return true; //location is occupied
        }
        return false;
    }


    private class Player implements Runnable
    {
        private Socket connection; //connection to client
        private BufferedReader input; //input from Client
        private Formatter output; //output to client
        private int playerNumber; //tracks which player this is
        private String mark; //mark for this player
        private boolean suspended = true; //whether thread is suspended;


        //This will represent Thread1 - Client1 communication
        //and Thread2 - Client2 communication.
        public Player(Socket socket, int number)
        {
            bothPlayersResponded = gameLock.newCondition();
            playerNumber = number;
            mark = MARKS[playerNumber];
            connection = socket;
            try
            {
                //we get the clients information here in an input stream
                input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                //we send the client information using the outputStream
                output = new Formatter(connection.getOutputStream());
            }

            catch (IOException ioException)
            {
                ioException.printStackTrace();
                System.exit(1);
            }
        }

        public void otherPlayerMoved(int location)
        {
            //send message to client
            output.format("Opponent moved\n");
            output.format("%d\n", location); //send location of move to client so they can mark it on their board
            //flush so it doesn't wait in the buffer
            output.flush();
        }

        public void otherPlayerWon(int location)
        {
            output.format("Opponent won %d\n", location);
            output.flush();
        }

        public void otherPlayerDrew()
        {
            output.format("It's a tie game\n");
            output.flush();
        }

        /**
         * Entry point for the Player thread.
         * Manages communication with the client, including receiving moves,
         * validating game state, and coordinating with the opponent thread.
         * Handles input parsing, win/draw detection, and thread synchronization.
         */
        public void run()
        {

            try {
                    //send the corresponding client their mark
                    displayMessage("Player " + mark + " connected\n");

                    output.format("%s\n", mark); //send player's mark so either "X" or "O"
                    output.flush(); //force it to the client so it receives it now

                /**
                 * Manages Player X's initial connection phase.
                 * Uses guarded wait to block execution until Player O connects.
                 * Prevents spurious wakeups and ensures synchronized game start.
                 */
                waitForSecondPlayer();

                        while (!gameOver) {
                            //debug
                            System.out.println("I am player " + playerNumber);

                            //after each player has their turn, it will restart from here for the new player

                            int location = 0; //initialize move location

                            //
                            String line = input.readLine();
                            if (line == null)
                                break;

                            //removes leading/trailing whitespace from the input
                            line = line.trim();
                            //get the clients move location by using our inputStream
                            //the program maintains board locations as numbers from 0 to 8 (0 - 2 for the first row, 3 - 5 for the second row and 6 - 8 for the third row)
                            try {
                                location = Integer.parseInt(line);
                                System.out.println(location);
                                //if the location sent from a client is not an integer we catch a NumberFormatException and handle it nicely on the server..
                                //..instead of terminating the whole program
                            } catch (NumberFormatException e) {
                                System.out.println("Server only accepts Integers, however you sent " + line);
                            }


                            //check's if the move made is a winning move or results in a tie game
                            if (!isOccupied(location)) //make sure the move is valid first
                            {
                                board[location] = MARKS[currentPlayer]; //set the move on board

                                if (checkWin(MARKS[currentPlayer])) {
                                    output.format("Congratulations you have won %d\n", location);
                                    output.flush();

                                    currentPlayer = (currentPlayer + 1) % 2; //switch to the other player
                                    //let other player know they lost
                                    players[currentPlayer].otherPlayerWon(location);

                                    gameOver = true;
                                    break;
                                } else if (isBoardFull()) {
                                    output.format("Its a tie!\n");
                                    output.flush();

                                    currentPlayer = (currentPlayer + 1) % 2; //switch to the other player
                                    //let other player know that it was a tie
                                    players[currentPlayer].otherPlayerDrew();

                                    gameOver = true;
                                    break;
                                } else {
                                    board[location] = ""; //clear the board
                                }
                            }

                            //so if the clients move was not a winning move nor was it a tie, we validate it (checking the move hasn't already been taken)
                            //if valid, we let the client know it was valid so they can update their board on their end
                            if (validateAndMove(location, playerNumber)) {
                                displayMessage("\nlocation: " + location); //displays the location to our main server application

                                // === Server to Client Communication ===
                                // Send move result, win/loss messages, or game state updates
                                output.format("Valid move.\n"); //notify client
                                output.flush();
                            } else {
                                output.format("Invalid move, try again\n");
                                output.flush();
                            }
                        }
            } catch (IOException e) {
                // Helps me recognize that this is a network I/O failure, likely due to client disconnection or stream interruption
                throw new RuntimeException(e);
            } finally {

                try {
                    System.out.println("Closing client connection...");
                    connection.close(); //close connection to client
                }

                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                    System.exit(1);
                }
            }
        }

        private void waitForSecondPlayer()
        {
            if (playerNumber == PLAYER_X) {
                output.format("%s\n%s", "Player X connected", "Waiting for another player\n");
                output.flush(); //flush output

                gameLock.lock(); //lock thread on client 1 player "X"

                try {
                    //lock conditions can sometimes wake up unexpectedly, even if no thread explicitly signals them, this is called a spurious wakeup.
                    // Guarded wait protects against spurious wakeups disrupting game flow
                    while (suspended) {
                        otherPlayerConnected.await(); //as client1 were pausing here and waiting on otherPlayerConnected.signify() or signifyAll() to be called to tell us the
                        // other player has connected, then we can resume execution.
                        //when client2 calls signifyAll or signify then this client1 will wake up again and resume execution. so it will go from after the finally block
                    }
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                } finally {
                    gameLock.unlock(); //go back to main thread waiting for player 2 to connect
                }

                // When Player O connects, the suspended Player X thread resumes and starts the game.
                output.format("Other player connected. Your move.\n");
                output.flush();
            } else
            {
                // Sent to Player O upon connection; informs them to wait until Player X starts.
                output.format("Player 0 connected, please wait\n");
                output.flush(); //flush output
            }

        }

        //set whether or not thread is suspended
        public void setSuspended(boolean status)
        {
            suspended = status;
        }


        // === Game End Checks ===

        //logic that checks whether a move on the board correlates to a win for that client
        private boolean checkWin(String mark) {
            // Rows and columns
            for (int i = 0; i < 3; i++) {
                if (board[i*3].equals(mark) &&
                        board[i*3+1].equals(mark) &&
                        board[i*3+2].equals(mark)) return true;
                if (board[i].equals(mark) &&
                        board[i+3].equals(mark) &&
                        board[i+6].equals(mark)) return true;
            }

            // Diagonals
            if (board[0].equals(mark) &&
                    board[4].equals(mark) &&
                    board[8].equals(mark)) return true;
            if (board[2].equals(mark) &&
                    board[4].equals(mark) &&
                    board[6].equals(mark)) return true;

            return false;
        }

        //checks for a tie game
        private boolean isBoardFull() {
            for (String s : board)
                if (s.equals("")) return false;
            return true;
        }

    }

}
