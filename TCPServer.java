/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.IOException;
import java.net.*; 

class TCPServer { 
	
	    public static void main(String argv[]) throws Exception { 
		
		try (ServerSocket welcomeSocket = new ServerSocket(6789))
		{
			System.out.println("Server started"); 
  
			while(true) { 

			System.out.println("Waiting for a client ..."); 
			Socket connectionSocket = welcomeSocket.accept(); 
			System.out.println("Client accepted"); 
			
			// Do something
			SftpServer sftp = new SftpServer();
			
			sftp.run(connectionSocket);
			} 
		} 
		catch(IOException e) {
			System.out.println(e);
		}

	} 

} 

