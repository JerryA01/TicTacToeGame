package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicTacToeClient extends JFrame implements Runnable {

    // Two clients play concurrently — Runnable allows asynchronous game logic

    private JTextField idField; //textField to display players Mark
    private JTextArea displayArea; //JTextarea to display output
    private JPanel boardPanel; //panel for tic-tac-toe board
    private JPanel containerPanel; //panel to hold board
    private Square[][] board; //tic-tac-toe board
    private Square currentSquare; //current square
    private Socket connection; //creates a TCP connection to the server
    private Scanner input; //input from server
    private Formatter output; //output to Server
    private String ticTacToeHost; //host name for Server
    private String myMark; //this clients Mark
    private boolean myTurn; //determines which client's turn it is
    private final String X_MARK = "X"; //mark for first client
    private final String O_MARK = "O"; //mark for second client


    //set up user-interface and board
    public TicTacToeClient(String host) {

        ticTacToeHost = host; //set name of Server
        displayArea = new JTextArea(4, 30); //set up JTextArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        boardPanel = new JPanel(); //set up panel for squares in board
        boardPanel.setLayout(new GridLayout(3, 3, 0, 0));

        // === GUI Setup: Build Tic-Tac-Toe Board ===
        board = new Square[3][3]; //board with 3 rows and 3 columns.

        for (int row = 0; row < board.length; row++) {
            //loop over the columns in the board
            for (int column = 0; column < board[row].length; column++) {
                //Each board has its own Square object and each square has a mark, a location and a mouse listener, each square is initialized by an empty mark
                //each Square has a location from 0-8 as we have a 3x3 board. so each square will hold one of our locations in the 3x3 board.
                //so when we click a square on our 3x3 board, we know its location, and we can set it to X or O
                board[row][column] = new Square(" ", row * 3 + column);

                //we add our board to boardPanel which is our 3x3 GridLayout so it looks like a tic-tac-toe board
                boardPanel.add(board[row][column]); //add square to that cell
            }
        }

        idField = new JTextField(); //set up textField
        idField.setEditable(false);
        add(idField, BorderLayout.NORTH);

        containerPanel = new JPanel(); //set up panel to contain boardPanel
        containerPanel.add(boardPanel, BorderLayout.CENTER); //add board panel
        add(containerPanel, BorderLayout.CENTER); //add container panel

        setSize(300, 225);  //set size of window
        setVisible(true); //show window

        startClient();
    }


    public void startClient() {
        try
        {
            //make connection to server
            //the server address is running on the same machine as the client, so InetAddress.getByName(ticTacHoeHost) is basically localhost/127.0.0.1
            connection = new Socket(InetAddress.getByName(ticTacToeHost), 12345);

            //get streams for input and output using our TCP Socket connection
            input = new Scanner(connection.getInputStream());
            output = new Formatter(connection.getOutputStream());

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        // Create a dedicated thread for this client using ExecutorService.
        // Submitting 'this' triggers run() — launching communication and game logic concurrently.
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(this); //execute client
    }


    public void run() {

        //our server sends us the clients mark here which can either be X or O
        myMark = input.nextLine(); //get player's mark (X or O)

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //display players mark using EDT thread
                idField.setText("You are player \"" + myMark + "\"");
            }
        });

        myTurn = (myMark.equals(X_MARK));

        //receive messages sent to client and output them
        //infinite loop whilst the server and client has a connection, so we'll forever listen to messages from server
        while (true) {
            //this is how we as the client determine what to do, whatever the message the server sends us will allow us to decide what to do
            //we manage the "what to do" in processMessage
            if (input.hasNextLine())
                //send that as a parameter to our method processMessage
                processMessage(input.nextLine());
        }

    }

    // === Server Message Handling ===
    private void processMessage(String message) {
        if (handleValidMove(message)) return;
        if (handleInvalidMove(message)) return;
        if (handleOpponentMove(message)) return;
        if (handleWinMessage(message)) return;
        if (handleTieMessage(message)) return;
        if (handleOpponentWin(message)) return;

        //fallback: display any other message
        displayMessage(message + "\n");

    }

    // === Server Message Handling Methods  ===

    public boolean handleValidMove(String message) {
        if (message.equals("Valid move.")) {
            displayMessage("valid move, please wait. \n");
            //this client listens for server messages @input.hasNextLine, and the board is only updated if the server sends a "Valid Move" message. This acts as a safeguard
            //against marking the board prematurely
            setMark(currentSquare, myMark); //set mark in square
            return true;
        }
        return false;
    }

    public boolean handleInvalidMove(String message){
        if (message.equals("Invalid move, try again")) {
            displayMessage(message + "\n"); //display invalid move
            myTurn = true; //still this Clients turn
            return true;
        }
        return false;
    }

    public boolean handleOpponentMove(String message){
         if (message.equals("Opponent moved")) {
            //get the other clients location on the board
            int location = input.nextInt(); //get move location
            input.nextLine(); //skip newLine after int location
            int row = location / 3; //calculate row
            int column = location % 3; //calculate column

            //update our clients board with the user's move.
            setMark(board[row][column], (myMark.equals(X_MARK) ? O_MARK : X_MARK));
            // mark move
            displayMessage("Opponent moved. Your turn.\n");
            myTurn = true; //now this client's turn
            return true;
         }
         return false;
    }

    public boolean handleWinMessage(String message){
        if(message.startsWith("Congratulations you have won")){
            String[] parts = message.split(" ");
            if(parts.length >= 5) {
                int location = Integer.parseInt(parts[4]);
                int row = location / 3; //calculate row
                int column = location % 3; //calculate column

                //update our clients board with the user's move.
                setMark(board[row][column], (myMark.equals(X_MARK) ? X_MARK : O_MARK));

                displayMessage("Congratulations you have won!" + "\n");
                myTurn = false;

                handleGameOver();
            }
            return true;
        }
        return false;
    }

    public boolean handleTieMessage(String message){
        if(message.contains("tie")){

            int[] emptyPos = getFirstEmptySquarePosition();
            int row = emptyPos[0];
            int column = emptyPos[1];

            //update our clients board with the user's move.
            setMark(board[row][column], (myMark.equals(X_MARK) ? O_MARK : X_MARK));

            displayMessage(message + "\n");
            myTurn = false;

            handleGameOver();

            return true;
        }
        return false;
    }

    public boolean handleOpponentWin(String message)
    {
        if(message.startsWith("Opponent won"))
        {
            System.out.println("Client received opponent win notification");
            String[] parts = message.split(" ");
            int location = Integer.parseInt(parts[2]);
            int row = location / 3; //calculate row
            int column = location % 3; //calculate column

            //update our clients board with the user's move.
            setMark(board[row][column], (myMark.equals(X_MARK) ? O_MARK : X_MARK));

            displayMessage("Opponent won!" + "\n");
            myTurn = false;

            handleGameOver();
            return true;
        }
        return false;
    }

    public void handleGameOver() {
        // Delay on a background thread to avoid freezing UI
        new Thread(() -> {
            try {
                Thread.sleep(2000); // wait 2 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Show simple dialog on the Swing EDT
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        TicTacToeClient.this,
                        "Game Over",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
        }).start();
    }

    // === UI Utilities ===

    //manipulate displayArea in event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayArea.append(messageToDisplay); //updates output
            }
        });
    }

    //utility method to set mark on board in event-dispatch thread
    private void setMark(final Square squareToMark, final String mark) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                squareToMark.setMark(mark); //set mark in Square
            }

        });
    }

    public void setCurrentSquare(Square square) {
        currentSquare = square; //set current square to argument
    }


    // === Board Click Logic ===

    //when its our turn and our thread isn't locked, when we click a square we sent that location to the server for processing
    //the server will then send us back a message saying if it was valid, false etc. if valid our processMessage method updates our board with the square we clicked
    public void sendClickedSquare(int location) {
        //this myTurn here is what stops the client that is waiting from sending moves to the server
        if (myTurn) {
            System.out.println("is it clicking");
            System.out.println("the location send to server " + location);
            if(myTurn)
                System.out.println("its my turn too");

            if(connection != null && connection.isConnected() && !connection.isClosed())
                System.out.println("ok connection not closed");

            output.format("%d\n", location); //send location to server
            output.flush();
            myTurn = false; //not my turn anymore
        }
    }


    // === Square Class (Client-Side UI Representation) ===

    //private inner class for the square on the board
    private class Square extends JPanel {
        private String mark; //mark to be drawn in this square
        private int location; //location of square

        public Square(String squareMark, int squareLocation) {
            mark = squareMark; //set mark for this square
            location = squareLocation; //set location of this square


            //when we click a Square we set our instance variable currentSquare to the square we clicked
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setCurrentSquare(Square.this); //set Current square
                    System.out.println("CLICKED! And i am player " + myMark);
                    // Send clicked location to server
                    sendClickedSquare(getSquaredLocation());
                }
            });
        }

        //return preferred size of square
        public Dimension getPreferredSize() {
            return new Dimension(30, 30);
        }

        //return minimum size of Square
        public Dimension getMinimumSize() {
            return getPreferredSize(); //return preferred size
        }

        //set mark for Square
        public void setMark(String newMark) {
            mark =  newMark; //set mark of square
            //repaint so we can see the updated board with our mark set
            repaint();
        }

        //get mark for square
        public String getMark() {
            return mark;
        }

        //return Square location
        public int getSquaredLocation() {
            return location; //return location of square
        }

        //draw Square
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            //this draws the square right here and we put our mark in the square
            g.drawRect(0, 0, 29, 29);
            g.drawString(mark, 11, 20); //draw mark
        }
    }

    public int[] getFirstEmptySquarePosition() {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (board[row][col].getMark().equals(" ")) {
                    return new int[]{row, col};
                }
            }
        }
        return new int[]{-1, -1}; // no empty square found
    }


}




