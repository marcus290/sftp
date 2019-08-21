/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*; 
import java.util.ArrayList;
import java.util.Arrays;

class TCPClient { 
    
    public static void main(String argv[]) throws Exception 
    { 
        final String[] cmd = {
            "TYPE", "LIST", "CDIR", "KILL", "NAME", "RETR", "DONE"
        }; 
        String command;
        ArrayList<String> command_arr = new ArrayList<String>();
        String replyMessage;
        int openConn = 0;

        final int REQ_USER = 0;
        final int REQ_ACCT_PASS = 1;
        final int REQ_ACCT = 2;
        final int REQ_PASS = 3;
        final int AUTH_DONE = 4;
        int auth = REQ_USER;

        boolean to_rename = false;
        boolean to_send = false;

        long retr_size;
        
        BufferedReader inFromUser = 
	        new BufferedReader(new InputStreamReader(System.in)); 
	
        Socket clientSocket = new Socket("localhost", 6789); 
        openConn = 1;
	
        DataOutputStream outToServer = 
	        new DataOutputStream(clientSocket.getOutputStream()); 
	
        
	    BufferedReader inFromServer = 
	        new BufferedReader(new
		    InputStreamReader(clientSocket.getInputStream())); 
    
        // Print the greeting
        System.out.println(inFromServer.readLine());

    
        // Get user input
        while (openConn == 1) {
            command = inFromUser.readLine();
            command_arr.clear();
            command_arr.addAll(
                Arrays.asList(command.split("\\s+"))
            );

            // Check for valid commands depending on whether user is authenticated
            if ( 
                auth == REQ_USER && command_arr.get(0).equals("USER") || 
                auth == REQ_ACCT_PASS && command_arr.get(0).matches("ACCT|PASS") ||
                auth == REQ_ACCT && command_arr.get(0).equals("ACCT") ||
                auth == REQ_PASS && command_arr.get(0).equals("PASS") ||

                auth == AUTH_DONE && Arrays.asList(cmd).contains(command_arr.get(0)) &&
                (!to_rename) && (!to_send) ||
                to_rename && command_arr.get(0).equals("TOBE") ||
                to_send && command_arr.get(0).matches("SEND|STOP")
            ) {
                outToServer.writeBytes(command + '\0' + '\n');
            } else {
                System.out.println("Invalid command. Please reenter:");
                continue;
            }

            replyMessage = inFromServer.readLine();
            System.out.println(replyMessage);

            switch (command_arr.get(0)) {
                case "USER":
                if (replyMessage.charAt(0) == '!') {
                    auth = AUTH_DONE;
                } else if (replyMessage.charAt(0) == '+') {
                    auth = REQ_ACCT_PASS;
                } 
                break;

                case "ACCT":
                if (replyMessage.charAt(0) == '!') {
                    auth = AUTH_DONE;
                } else if (replyMessage.charAt(0) == '+') {
                    auth = REQ_PASS;
                } 
                break;

                case "PASS":
                if (replyMessage.charAt(0) == '!') {
                    auth = AUTH_DONE;
                } else if (replyMessage.charAt(0) == '+') {
                    auth = REQ_ACCT;
                } 
                break;

                case "TYPE":
                break;

                case "LIST":
                if (replyMessage.charAt(0) == '+') {
                    replyMessage = inFromServer.readLine();
                    while (replyMessage.charAt(0) != '\0') {
                        System.out.println(replyMessage);
                        replyMessage = inFromServer.readLine();
                    }
                }
                break;

                case "CDIR":
                if (replyMessage.charAt(0) == '+') {
                    auth = REQ_ACCT_PASS;
                }
                break;

                case "KILL":
                break;

                case "NAME":
                if (replyMessage.charAt(0) == '+') {
                    to_rename = true;
                }
                break;

                case "TOBE":
                if (replyMessage.charAt(0) == '+') {
                    to_rename = false;
                }
                break;

                case "RETR":
                if (replyMessage.matches("\\d+\0")) {
                    to_send = true;
                    retr_size = Long.parseLong(replyMessage.substring(0, replyMessage.length() - 1));
                }
                break;

                case "SEND":
                break;

                case "STOP":
                if (replyMessage.charAt(0) == '+') {
                    to_send = false;
                }
                break;

                case "DONE":
                if (replyMessage.charAt(0) == '+') {
                    clientSocket.close(); 
                    openConn = 0;
                }
                break;
            }

        }



	
    } 
} 
