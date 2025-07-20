# Multiplayer Tic Tac Toe (Java • Threads • Networking)

A real-time, synchronized multiplayer Tic Tac Toe game built entirely with Java's core libraries — no frameworks, no shortcuts. This project demonstrates networking fundamentals, thread coordination, and GUI design through a fully functional client-server architecture. Built for portfolio impact and technical precision.

## Table of Contents
- [Overview](#Overview)
- [Video walkthrough of the application](#Walkthrough)
- [Video walkthrough on how to install and use this on your local machine](#HowToUseVideo)
- [Important Info](#ImportantInfo)
- [Installation & Usage](#installation&Usage)


## Overview

This project showcases a fully functional multiplayer Tic Tac Toe game implemented in Java — built from scratch using only the Java Standard Library.

It combines key concepts in networking, concurrency, and thread synchronization, with a clean separation between server and client architecture. The server manages two-player connections, handles turn-based gameplay, and enforces synchronization using `Lock` and `Condition` constructs to ensure smooth interaction between threads. Each client runs independently and communicates with the server via socket-based input/output streams.

The game interface is built using Java Swing, providing a simple but functional GUI on both server and client sides. No external libraries or frameworks were used — the focus was on mastering core Java capabilities and demonstrating full-stack understanding of a synchronized networked application.

This is an ongoing personal project aimed at strengthening my grasp of low-level networking and threading while also creating polished portfolio-ready code. A client outside the server’s LAN can connect if port forwarding is configured and firewalls allow traffic on the server's designated port.



## Walkthrough

..

## installation&Usage

### Requirements
- Java 8 or higher installed
- Any IDE with Java support (e.g., IntelliJ IDEA, Eclipse, NetBeans)
- If running the server and clients on the same local machine or LAN, internet is not required.
- If running on different networks, you'll need:
   - internet access
   - The server's rouuter to support port forwading
   - Firewall rules to allow traffic on port 12345
   - The client to know the server's public IP address

### Steps to run Locally
- clone or download the repository  
- Import into your IDE
  - Open your IDE and import the project as a standard Java application 
- Start the Server
  - Open TicTacToeServer.java
  - Run the main method to launch the server GUI
- Run two clients
  - Open two IDE windows or tabs
  - Run TicTacToeClient.java twice
  - Each client will connect and be assigned either "X" or "O" 
- Play Locally
  - All componenets communicate over localhost:12345
  - For remote play, port forwading and firewall permissions must be configured 

### Testing Tips
- You can simulate two clients on one machine by launching two seperate instances of your IDE or using terminal-based launches
- Debugging messages are printed to the server and client windows for live tracking
- Feel free to customize client UI or refactor gameplay messages for personaliation.





  


