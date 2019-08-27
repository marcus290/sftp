package s;

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
			while(true) { 
				// System.out.println("SERVER: Waiting for a client ..."); 
				Socket connectionSocket = welcomeSocket.accept(); 
				
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

