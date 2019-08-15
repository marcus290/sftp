import java.io.*; 
import java.net.*; 
import java.util.ArrayList;
import java.util.Arrays;

public class SftpServer {
	int openConn; 
	String command;
	ArrayList<String> command_arr;

	public SftpServer(){
		this.openConn = 1;
		this.command_arr = new ArrayList<String>();
	}

    public void run(Socket connectionSocket) throws Exception {

		try (
			BufferedReader inFromClient = new BufferedReader(
				new InputStreamReader(connectionSocket.getInputStream())); 
			DataOutputStream  outToClient = 
				new DataOutputStream(connectionSocket.getOutputStream()); )
		{
			// Send the greeting
			outToClient.writeBytes("+MIT-XX SFTP Service\n");

			while (openConn == 1) {
				command = inFromClient.readLine(); 
				command_arr.clear();
				command_arr.addAll(
					Arrays.asList(command.split("\\s+"))
				);

				switch (command_arr.get(0)) {
					case "DONE":
						outToClient.writeBytes("+MIT-XX closing connection\n");
						openConn = 0;
						break;
					
	
				}
			}
		}
		catch(IOException e) {
			System.out.println("Reading from socket failed.");
			System.out.println(e);
		}

    }
}