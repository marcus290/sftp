/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


class TCPClient { 
    
    public static void main(String argv[]) throws Exception 
    { 
        String[] cmd = {"DONE"}; 
        String command;
        ArrayList<String> command_arr = new ArrayList<String>();
        String replyMessage;
        int openConn = 0;
	
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

            if (Arrays.asList(cmd).contains(command_arr.get(0))) {
                outToServer.writeBytes(command + '\n');
            } else {
                System.out.println("Invalid command. Please reenter:");
                break;
            }

            switch (command_arr.get(0)) {
                case "DONE":
                    replyMessage = inFromServer.readLine();
                    if (replyMessage.charAt(0) == '+') {
                        System.out.println(replyMessage);
                        clientSocket.close(); 
                        openConn = 0;
                    }
                    break;
                

            }

        }



	
    } 
} 
