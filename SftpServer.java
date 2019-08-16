import java.io.*; 
import java.net.*; 
import java.util.ArrayList;
import java.util.Arrays;

public class SftpServer {
	final String[] cmd = {"DONE"}; 
	
	int openConn; 
	
	final int REQ_USER = 0;
	final int REQ_ACCT_PASS = 1;
	final int REQ_ACCT = 2;
	final int REQ_PASS = 3;
	final int AUTH_DONE = 4;
	int auth;
	
	Users users;

	public SftpServer() throws FileNotFoundException {
		this.openConn = 1;
		this.auth = REQ_USER;
		this.users = new Users();
		for (String key : this.users.userDB.keySet()) {
			System.out.println(key);
		}
	}

    public void run(Socket connectionSocket) throws Exception {
		String command;
		ArrayList<String> command_arr = new ArrayList<String>();;

		try (
			BufferedReader inFromClient = new BufferedReader(
				new InputStreamReader(connectionSocket.getInputStream())); 
			DataOutputStream  outToClient = 
				new DataOutputStream(connectionSocket.getOutputStream()); )
		{
			// Send the greeting
			outToClient.writeBytes("+CS725 SFTP Service\n");

			while (openConn == 1) {
				command = inFromClient.readLine(); 
				command_arr.clear();
				command_arr.addAll(
					Arrays.asList(command.split("\\s+"))
				);

				// Check for valid commands depending on whether user is authenticated
				if (!(
					auth == REQ_USER && command_arr.get(0).equals("USER") || 
					auth == REQ_ACCT_PASS && command_arr.get(0).matches("ACCT|PASS") ||
					auth == REQ_ACCT && command_arr.get(0).equals("ACCT") ||
					auth == REQ_PASS && command_arr.get(0).equals("PASS") ||
					auth == AUTH_DONE && Arrays.asList(this.cmd).contains(command_arr.get(0))
				)) {
					System.out.println("Invalid command received. Waiting for next command.");
				}

				switch (command_arr.get(0)) {
					case "USER":
					if (this.users.inUsers(command_arr.get(1))) {
						if (this.users.needAccPass(command_arr.get(1))) {
							outToClient.writeBytes(String.format("!%s logged in\n", command_arr.get(1)));
							auth = AUTH_DONE;
						} else {
							outToClient.writeBytes("+User-id valid, send account and password\n");
							auth = REQ_ACCT_PASS;
						}
					} else {
						outToClient.writeBytes("-Invalid user-id, try again\n");
					}
					break;

					case "DONE":
						outToClient.writeBytes("+CS725 closing connection\n");
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