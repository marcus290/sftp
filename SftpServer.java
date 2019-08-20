import java.io.*; 
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SftpServer {
	final String[] cmd = { "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE"}; 
	
	int openConn; 
	
	final int REQ_USER = 0;
	final int REQ_ACCT_PASS = 1;
	final int REQ_ACCT = 2;
	final int REQ_PASS = 3;
	final int AUTH_DONE = 4;
	int auth;
	int dir_auth_req = 0;
	
	Users users;
	String curr_user;
	String curr_account;

	char type = 'B';
	final Character[] valid_types = {'A', 'B', 'C'};

	File curr_dir;

	public SftpServer() throws FileNotFoundException {
		this.openConn = 1;
		this.auth = REQ_USER;
		this.users = new Users();
		this.curr_dir = new File("./storage/");
	}

    public void run(Socket connectionSocket) throws Exception {
		String command;
		ArrayList<String> command_arr = new ArrayList<String>();

		File rf;

		try (
			BufferedReader inFromClient = new BufferedReader(
				new InputStreamReader(connectionSocket.getInputStream())); 
			DataOutputStream  outToClient = 
				new DataOutputStream(connectionSocket.getOutputStream()); )
		{
			// Send the greeting
			outToClient.writeBytes("+CS725 SFTP Service\0\n");

			while (openConn == 1) {
				command = inFromClient.readLine(); 
				command_arr.clear();
				command_arr.addAll(
					Arrays.asList(command.split("\\s+|\0"))
				);

				boolean to_rename = false;

				// Check for valid commands depending on whether user is authenticated
				if (!(
					this.auth == REQ_USER && command_arr.get(0).equals("USER") || 
					this.auth == REQ_ACCT_PASS && command_arr.get(0).matches("ACCT|PASS") ||
					this.auth == REQ_ACCT && command_arr.get(0).equals("ACCT") ||
					this.auth == REQ_PASS && command_arr.get(0).equals("PASS") ||
					this.auth == AUTH_DONE && Arrays.asList(this.cmd).contains(command_arr.get(0)) ||
					to_rename && command_arr.get(0).equals("TOBE")
				)) {
					System.out.println("Invalid command received. Waiting for next command.");
				}

				switch (command_arr.get(0)) {
					case "USER":
					if (command_arr.size() > 1 && this.users.inUsers(command_arr.get(1))) {
						this.curr_user = command_arr.get(1);
						if (!this.users.needAccPass(command_arr.get(1))) {
							outToClient.writeBytes(String.format("!%s logged in\0\n", command_arr.get(1)));
							this.auth = AUTH_DONE;
						} else {
							outToClient.writeBytes("+User-id valid, send account and password\0\n");
							this.auth = REQ_ACCT_PASS;
						}
					} else {
						outToClient.writeBytes("-Invalid user-id, try again\0\n");
					}
					break;

					case "ACCT":
					ArrayList<String> accounts = this.users.getAccounts(this.curr_user);
					if (accounts.size() > 0) {
						if (accounts.contains("") || (command_arr.size() > 1 && 
						accounts.contains(command_arr.get(1)))) {
							this.curr_account = command_arr.get(1);
						} else {
							outToClient.writeBytes("-Invalid account, try again\0\n");
							break;	
						}
					} 
					if (
						this.users.getPassword(this.curr_user).equals("") 
						|| this.auth == REQ_ACCT
					) {
						if (this.dir_auth_req == 1) {
							if (! (this.curr_dir.canRead() && this.curr_dir.canWrite())) {
								outToClient.writeBytes("!Changed working dir to " + this.curr_dir + "\0\n");
								this.dir_auth_req = 0;
							} else {
								outToClient.writeBytes("-Cannot connect to " + this.curr_dir 
								+ ", please send account/password again\0\n");
								this.auth = REQ_ACCT_PASS;
								break;
							}
						} else {
							outToClient.writeBytes("! Account valid, logged-in\0\n");
						}
						this.auth = AUTH_DONE;
					} else {
						outToClient.writeBytes("+Account valid, send password\0\n");
						this.auth = REQ_PASS;
					}
					break;

					case "PASS":
					String pw = this.users.getPassword(this.curr_user);
					if (pw.equals("") || (command_arr.size() > 1 && pw.equals(command_arr.get(1)))) {
						if (
							this.users.getAccounts(this.curr_user).size() == 0
							|| this.auth == REQ_PASS
						) {
							if (this.dir_auth_req == 1) {
								if (! (this.curr_dir.canRead() && this.curr_dir.canWrite())) {
									outToClient.writeBytes("!Changed working dir to " + this.curr_dir + "\0\n");
									this.dir_auth_req = 0;
								} else {
									outToClient.writeBytes("-Cannot connect to " + this.curr_dir 
									+ ", please send account/password again\0\n");
									this.auth = REQ_ACCT_PASS;
									break;
								}
							} else {
								outToClient.writeBytes("! Logged in\0\n");
							}
							this.auth = AUTH_DONE;
						} else {
							outToClient.writeBytes("+Send account\0\n");
							this.auth = REQ_ACCT;
						}
					} else {
						outToClient.writeBytes("-Wrong password, try again\0\n");
					}
					break;

					case "TYPE":
					if (
						command_arr.size() > 1 && 
						Arrays.asList(this.valid_types).contains(command_arr.get(1).charAt(0))
					) {
						outToClient.writeBytes(String.format(
							"+Using %s mode\0\n", 
							command_arr.get(1).equals("A") ? "Ascii" : 
							command_arr.get(1).equals("B") ? "Binary" : "Continuous"
						));
						type = command_arr.get(1).charAt(0);
					} else {
						outToClient.writeBytes("-Type not valid\0\n");
					}
					break;

					case "LIST":
					try {
						if (command_arr.size() >= 1) {
							File f;
							if (command_arr.size() == 2) {
								f = this.curr_dir;
							} else {
								f = new File(command_arr.get(2));
							}
							if (!f.exists())
								throw new FileNotFoundException();
							File[] files = f.listFiles();
							outToClient.writeBytes(String.format(
								"+%s\n", 
								(command_arr.size() == 2) ? this.curr_dir : command_arr.get(2)
							));
							for (File file: files) {
								if (command_arr.get(1).equals("V")) {
									Date modified = new Date(file.lastModified());
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm");
									
									outToClient.writeBytes(
										(file.canRead() ? "r" : "-") +
										(file.canWrite() ? "w" : "-") +
										(file.canExecute() ? "x" : "-") +
										String.format(" %10d ", file.length()) +
										sdf.format(modified) + " "
									);
								}
								outToClient.writeBytes(file.getName() + "\n");
								
							}
							outToClient.writeBytes("\0\n");
						}
						
					} catch (Exception e) {
						outToClient.writeBytes(String.format("-%s\0\n", e));
					}
					break;

					case "CDIR":
					if (command_arr.size() > 1) {
						File nf = new File(command_arr.get(1));
						if (nf.isDirectory()) {
							this.curr_dir = nf;
							if (this.curr_dir.canRead() && this.curr_dir.canWrite()) {
								outToClient.writeBytes(
									"!Changed working dir to " + command_arr.get(1)  + "\0\n"
								);
							} else {
								outToClient.writeBytes("+directory ok, send account/password\0\n");
								this.auth = REQ_ACCT_PASS;
								this.dir_auth_req = 1;
							}
						} else {
							outToClient.writeBytes(
								"-Can't connect to directory because invalid directory specified\0\n"
							);
						}
					} else {
						outToClient.writeBytes("-Can't connect to directory because no directory specified\0\n");
					}
					break;

					case "KILL":
					if (command_arr.size() > 1) {
						File df = new File(command_arr.get(1));
						if (df.isFile()) {
							try {
								df.delete();
							} catch (Exception e) {
								outToClient.writeBytes(String.format("-Not deleted because %s\0\n", e));
							}
						} else {
							outToClient.writeBytes(
								"-Not deleted because the file was not found\0\n"
							);
						}
					} else {
						outToClient.writeBytes("-Not deleted because no file specified\0\n");
					}
					break;

					case "NAME":
					if (command_arr.size() > 1) {
						rf = new File(command_arr.get(1));
						if (rf.isFile()) {
							outToClient.writeBytes("+File exists\0\n");
							to_rename = true;
						} else {
							outToClient.writeBytes("-Can't find " + command_arr.get(1) + "\0\n");
						}
					} else {
						outToClient.writeBytes("-File wasn't renamed because no file specified\0\n");
					}
					break;

					case "TOBE":
					if (command_arr.size() > 1) {
						File new_name = new File(command_arr.get(1));
						try {
							if (rf.renameTo(new_name)) {
								to_rename = false;
							}
						} catch (Exception e) {
							outToClient.writeBytes(String.format("-File not renamed because %s\0\n", e));
						}
					} else {
						outToClient.writeBytes("-File wasn't renamed because no file specified\0\n");
					}
					break;

					case "DONE":
					outToClient.writeBytes("+CS725 closing connection\0\n");
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