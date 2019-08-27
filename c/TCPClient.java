package c;
/**
 * Code is modified from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*; 
import java.util.ArrayList;
import java.util.Arrays;

class TCPClient { 
    static File curr_dir = new File("./c/stor/");

    static private void retrFile(File rf, long retr_size, char type, Socket clientSocket) {
        int current = 0;
		
        switch (type) {
            case 'A':
            try (
                FileWriter fw = new FileWriter(rf);
                BufferedWriter bw = new BufferedWriter(fw);
            ){
                BufferedReader asciiFromServer = new BufferedReader(new
                    InputStreamReader(clientSocket.getInputStream()));
                char[] cbuffer = new char[(int) retr_size];
                int charRead;

                do {
                    charRead = asciiFromServer.read(cbuffer, current, (int) retr_size - current);
                    if(charRead >= 0) current += charRead;
                    System.out.println(String.format("Read %d of %d characters from stream", current, retr_size));
                } while(retr_size - current > 0);

                bw.write(cbuffer, 0, (int) retr_size);
                bw.flush();
            } catch (Exception e) {
                System.out.println(e);
            }
            break;

            case 'B':
            case 'C': 
            /**There is no difference between binary and continuous modes for machines with 
             * word sizes which are multiples of 8. All architectures have been using multiples
             * of 8 since the 1980s, so binary and continuous modes are treated the same here. 
             */
            try (
                FileOutputStream fos = new FileOutputStream(rf);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
            ){
                DataInputStream bytesFromServer = new DataInputStream(clientSocket.getInputStream());
                byte[] bbuffer = new byte[(int) retr_size];
                int bytesRead;

                do {
                    bytesRead = bytesFromServer.read(bbuffer, current, (int) retr_size - current);
                    if(bytesRead >= 0) current += bytesRead;
                    System.out.println(String.format("Read %d of %d bytes from stream", current, retr_size));
                } while(retr_size - current > 0);

                bos.write(bbuffer, 0, (int) retr_size);
                bos.flush();
            } catch (Exception e) {
                System.out.println(e);
            }
            break;
        }
        System.out.println(String.format("Finished writing %s (%d bytes) to client", rf, retr_size));
    }
    
    static private void storFile(File tf, long retr_size, DataOutputStream outToServer) {
		try (
            FileInputStream fis = new FileInputStream(tf);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ){
            byte[] bbuffer = new byte[(int) retr_size];
            bis.read(bbuffer, 0, (int) retr_size);

            outToServer.write(bbuffer, 0, (int) retr_size);
            outToServer.flush();
            System.out.println("STOR: Finished sending " + tf + " (" + retr_size + 
                " bytes) from client to server");
        } catch (Exception e) {
            System.out.println(e);
        } 
	}

    
    public static void main(String argv[]) throws Exception 
    { 
        final String[] cmd = {
            "TYPE", "LIST", "CDIR", "KILL", "NAME", "RETR", "STOR", "DONE"
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

        char type = 'B';

        boolean to_rename = false;
        boolean to_send = false;
        boolean to_stor = false;

        long retr_size = 0;
        long stor_size = 0;
        File rf = new File("");
        File tf = new File("");
        
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

            // If STOR command, need to do local check for file on client side
            if (command_arr.get(0).equals("STOR")) {
                if (command_arr.size() < 3) {
                    System.out.println("STOR: Not enough arguments, please reenter");
                    continue;
                } else if (!(tf = new File(curr_dir, command_arr.get(2))).isFile()) {
                    System.out.println("STOR: File does not exist in local dir");
                    continue;
                }
            }

            // Check for valid commands depending on whether user is authenticated
            if ( 
                auth == REQ_USER && command_arr.get(0).equals("USER") || 
                auth == REQ_ACCT_PASS && command_arr.get(0).matches("ACCT|PASS") ||
                auth == REQ_ACCT && command_arr.get(0).equals("ACCT") ||
                auth == REQ_PASS && command_arr.get(0).equals("PASS") ||

                auth == AUTH_DONE && Arrays.asList(cmd).contains(command_arr.get(0)) &&
                (!to_rename) && (!to_send) && (!to_stor) ||
                to_rename && command_arr.get(0).equals("TOBE") ||
                to_send && command_arr.get(0).matches("SEND|STOP") ||
                to_stor && command_arr.get(0).equals("SIZE")
            ) {
                outToServer.writeBytes(command + "\0\n");
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
                if (replyMessage.charAt(0) == '+') {
                    if (command_arr.size() > 1) {
                        type = command_arr.get(1).charAt(0);
                    }
                } 
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
                    if (command_arr.size() > 1) {
                        rf = new File(curr_dir, command_arr.get(1));
                    }
                }
                break;

                case "SEND":
                if (replyMessage.charAt(0) == '+') {
                    to_send = false;
                }
                retrFile(rf, retr_size, type, clientSocket);
                break;

                case "STOP":
                if (replyMessage.charAt(0) == '+') {
                    to_send = false;
                }
                break;
                
                case "STOR":
                if (replyMessage.charAt(0) == '+' && command_arr.size() > 2) {
                    to_stor = true;
                    System.out.println("STOR: " + command_arr.get(2) + " (" 
                                        + tf.length() + " bytes)");
                }
                break;

                case "SIZE":
                if (replyMessage.charAt(0) == '+' && command_arr.size() > 1) {
                    stor_size = Long.parseLong(command_arr.get(1));
                    storFile(tf, stor_size, outToServer);
                    System.out.println(inFromServer.readLine());
                }
                to_stor = false;
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
