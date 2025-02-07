package com.daexsys.grappl.client.commands;

import com.daexsys.grappl.GrapplGlobal;
import com.daexsys.grappl.GrapplServerState;
import com.daexsys.grappl.client.Client;
import com.daexsys.grappl.client.ClientLog;
import com.daexsys.grappl.client.ConsoleWindow;
import com.daexsys.grappl.client.GrapplClientState;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

public class CommandHandler {
    public static String send = "";

    public static void handleCommand(String command, DataInputStream dataInputStream, DataOutputStream dataOutputStream, int port) {
        String ip = GrapplGlobal.DOMAIN;

        String[] spl = command.split("\\s+");
        try {

            if (spl[0].equalsIgnoreCase("ipban")) {

                if (Client.isLoggedIn) {
                    String ipToBAN = spl[1];

                    dataOutputStream.writeByte(5);
                    PrintStream printStream = new PrintStream(dataOutputStream);
                    printStream.println(ipToBAN);

                    ClientLog.log("Banned ip: " + ipToBAN);
                } else {
                    ClientLog.log("You must be logged in to ban IPs.");
                }
            }

            else if (spl[0].equalsIgnoreCase("login")) {
                String username = spl[1];
                String password = spl[2];

                dataOutputStream.writeByte(0);

                PrintStream printStream = new PrintStream(dataOutputStream);
                printStream.println(username);
                printStream.println(password);

                boolean success = dataInputStream.readBoolean();
                boolean alpha = dataInputStream.readBoolean();
                int thePort = dataInputStream.readInt();
                Client.isAlphaTester = alpha;
                Client.isLoggedIn = success;

                if (success) {
                    ClientLog.log("Logged in as " + username);
                    ClientLog.log("Alpha tester: " + alpha);
                    ClientLog.log("Static port: " + thePort);
                    Client.username = username;
                } else {
                    JOptionPane.showMessageDialog(Client.grapplGUI.getjFrame(), "Login failed!");
                }
            } else if (spl[0].equalsIgnoreCase("whoami")) {
                if (Client.isLoggedIn) {
                    ClientLog.log(Client.username);
                } else {
                    ClientLog.log("You aren't logged in, so you are anonymous.");
                }
            }

            else if(spl[0].equalsIgnoreCase("relay")) {
                ClientLog.log(Client.relayServerIP);
            }

            else if(spl[0].equalsIgnoreCase("version")) {
                ClientLog.log(GrapplGlobal.APP_NAME + " version " + GrapplClientState.VERSION);
            }

            else if(spl[0].equalsIgnoreCase("listadd")) {
                ClientLog.log("Adding to server list");
                dataOutputStream.writeByte(6);
                String game = spl[1];
                PrintStream printStream = new PrintStream(dataOutputStream);
                send = game + " - " + Client.relayServerIP + ":"+ Client.publicPort;
                printStream.println(send);
            }

            else if(spl[0].equalsIgnoreCase("listremove")) {
                ClientLog.log("Removing from server list");
                dataOutputStream.writeByte(7);
                PrintStream printStream = new PrintStream(dataOutputStream);
                printStream.println(send);
            }

            else if(spl[0].equalsIgnoreCase("help")) {
                ClientLog.log("Commands: init, login [username] [password], setport [port], listadd [gamename], listremovem, whoami, version, relay, ipban [ip]");
            }

            else if (spl[0].equalsIgnoreCase("setport")) {
                if (Client.isLoggedIn) {
                    if (Client.isAlphaTester) {
                        dataOutputStream.writeByte(2);
                        dataOutputStream.writeInt(Integer.parseInt(spl[1]));
                        ClientLog.log("Your port was set to: " + Integer.parseInt(spl[1]));
                    } else {
                        ClientLog.log("You are not an alpha tester, so you can't set static ports.");
                    }
                } else {
                    ClientLog.log("You are not logged in.");
                }
            } else if (spl[0].equalsIgnoreCase("init")) {
                ClientLog.log("Starting...");
                Client.initToRelay(ip, port);

            } else if (spl[0].equalsIgnoreCase("quit")) {
                System.exit(0);
            } else {
                ClientLog.log("Unknown command");
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createCommandThread() {
        Thread commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream dataInputStream = null;
                DataOutputStream dataOutputStream = null;

                try {
                    dataInputStream = new DataInputStream(Client.authSocket.getInputStream());
                    dataOutputStream = new DataOutputStream(Client.authSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ClientLog.log(GrapplGlobal.APP_NAME + " Command Line");

                Scanner scanner = new Scanner(System.in);

                while(true) {
                    try {
                        String line = scanner.nextLine();
                        CommandHandler.handleCommand(line, dataInputStream, dataOutputStream, Client.localPort);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });

        commandThread.setName("Grappl Command Thread");
        commandThread.start();
    }
}
