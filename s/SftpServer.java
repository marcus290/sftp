package s;

import java.io.*; 
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SftpServer {
	final String[] cmd = { 
		"TYPE", "LIST", "CDIR", "KILL", "NAME", "RETR", "STOR", "DONE"
	}; 
	
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
		this.curr_dir = new File("./s/stor/");
	}

	private void sendFile(File sf, long retr_size, char type, Socket connectionSocket) {
		switch (type) {
			case 'A': // Send as ASCII bytes
			try (
				FileReader fr = new FileReader(sf);
				BufferedReader br = new BufferedReader(fr);
			){
				BufferedWriter outputStream = new BufferedWriter(new 
					OutputStreamWriter(connectionSocket.getOutputStream()));
				char[] cbuffer = new char[(int) retr_size];
				br.read(cbuffer, 0, (int) retr_size);
				
				System.out.println(String.format("File stream buffered and sending %d characters", retr_size));
				outputStream.write(cbuffer, 0, (int) retr_size);
				outputStream.flush();
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
				FileInputStream fis = new FileInputStream(sf);
				BufferedInputStream bis = new BufferedInputStream(fis);
			){
				DataOutputStream outputStream = 
					new DataOutputStream(connectionSocket.getOutputStream());
				byte[] bbuffer = new byte[(int) retr_size];
				bis.read(bbuffer, 0, (int) retr_size);

				System.out.println(String.format("File stream buffered and sending %d bytes", retr_size));
				outputStream.write(bbuffer, 0, (int) retr_size);
				outputStream.flush();
			} catch (Exception e) {
				System.out.println(e);
			} 
			break;
		}
		System.out.println("Done.");
	}

	private void storFile(File tf, long stor_size, Socket connectionSocket, String mode) throws Exception {
        int current = 0;
		
		try (
			FileOutputStream fos = new FileOutputStream(tf, (mode.equals("APP")) ? true : false);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
		){
			DataInputStream bytesFromClient = new DataInputStream(connectionSocket.getInputStream());
			byte[] bbuffer = new byte[(int) stor_size];
			int bytesRead;

			do {
				bytesRead = bytesFromClient.read(bbuffer, current, (int) stor_size - current);
				if(bytesRead >= 0) current += bytesRead;
				System.out.println(String.format("Read %d of %d bytes from stream", current, stor_size));
			} while(stor_size - current > 0);

			System.out.println(String.format("File stream buffered and writing %d bytes", stor_size));
			bos.write(bbuffer, 0, (int) stor_size);
			bos.flush();
			System.out.println("Done.");
		} catch (Exception e) {
			System.out.println(e);
			throw e;
		}
	}

    public void run(Socket connectionSocket) throws Exception {
		String command;
		ArrayList<String> command_arr = new ArrayList<String>();

		File rf = new File(""); // Placeholder File object for NAME command
		File sf = new File(""); // Placeholder File object for RETR command
		File tf = new File(""); // Placeholder File object for STOR command

		boolean to_rename = false;
		boolean to_send = false;
		boolean to_stor = false;

		long retr_size = 0;
		long stor_size = 0;
		String mode = "";

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
				if (command == null) continue;
				command_arr.clear();
				command_arr.addAll(
					Arrays.asList(command.split("\\s+|\0"))
				);

				// Check for valid commands depending on whether user is authenticated
				if (!(
					this.auth == REQ_USER && command_arr.get(0).equals("USER") || 
					this.auth == REQ_ACCT_PASS && command_arr.get(0).matches("ACCT|PASS") ||
					this.auth == REQ_ACCT && command_arr.get(0).equals("ACCT") ||
					this.auth == REQ_PASS && command_arr.get(0).equals("PASS") ||
					this.auth == AUTH_DONE && Arrays.asList(this.cmd).contains(command_arr.get(0)) &&
					(!to_rename) && (!to_send) && (!to_stor) ||
					to_rename && command_arr.get(0).equals("TOBE") ||
					to_send && command_arr.get(0).matches("SEND|STOP") ||
					to_stor && command_arr.get(0).equals("SIZE")
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
						File df = new File(this.curr_dir, command_arr.get(1));

						if (df.isFile()) {
							try {
								df.delete();
								System.out.println("File deleted. Continuing...");
								outToClient.writeBytes(String.format("%s deleted\0\n", command_arr.get(1)));
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
						rf = new File(this.curr_dir, command_arr.get(1));
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
						File new_name = new File(this.curr_dir, command_arr.get(1));
						String oldName = rf.toString();
						try {
							if (rf.renameTo(new_name)) {
								to_rename = false;
								outToClient.writeBytes(String.format("+%s renamed to %s\0\n", oldName, new_name));
							}
						} catch (Exception e) {
							outToClient.writeBytes(String.format("-File wasn't renamed because %s\0\n", e));
						}
					} else {
						outToClient.writeBytes("-File wasn't renamed because no new name specified\0\n");
					}
					break;

					case "RETR":
					if (command_arr.size() > 1) {
						sf = new File(this.curr_dir, command_arr.get(1));
						if (sf.isFile()) {
							retr_size = sf.length();
							outToClient.writeBytes(String.format("%d\0\n", retr_size));
							to_send = true;
						} else {
							outToClient.writeBytes("-File doesn't exist\0\n");
						}
					} else {
						outToClient.writeBytes("-File not specified\0\n");
					}
					break;

					case "SEND":
					outToClient.writeBytes("+ok, sending file\0\n");
					sendFile(sf, retr_size, this.type, connectionSocket);
					to_send = false;
					break;

					case "STOP":
					to_send = false;
					retr_size = 0;
					outToClient.writeBytes("+ok, RETR aborted\0\n");
					break;

					case "STOR":
					if (command_arr.size() > 2) {
						tf = new File(this.curr_dir, command_arr.get(2));
						mode = command_arr.get(1);
						switch (mode) {
							case "NEW":
							if (!tf.isFile()) {
								outToClient.writeBytes("+File does not exist, will create new file\0\n");
							} else {
								// Modern day operating systems do not support file generations
								outToClient.writeBytes("-File exists, but system doesn't support generations\0\n");
								continue;
							}
							break;
							case "OLD":
							if (tf.isFile()) {
								outToClient.writeBytes("+Will write over old file\0\n");
							} else {
								outToClient.writeBytes("+Will create new file\0\n");
							}
							break;
							case "APP":
							if (tf.isFile()) {
								outToClient.writeBytes("+Will append to file\0\n");
							} else {
								outToClient.writeBytes("+Will create file\0\n");
							}
							break;
							default:
							outToClient.writeBytes("-Incorrect STOR mode specified, please try again\0\n");
							continue;
						}
						to_stor = true;
					} else {
						outToClient.writeBytes("-Usage: STOR { NEW | OLD | APP } <file-spec>\0\n");
					}
					break;

					case "SIZE":
					boolean enoughMemory = true;
					if (enoughMemory && command_arr.size() > 1) {
						stor_size = Long.parseLong(command_arr.get(1));
						outToClient.writeBytes("+ok, waiting for file\0\n");
						try {
							storFile(tf, stor_size, connectionSocket, mode);
							outToClient.writeBytes(String.format("+Saved %s\0\n", tf));
						} catch (Exception e) {
							System.out.println(e);
							outToClient.writeBytes(String.format("-Couldn't save because %s\0\n", e));
						}
					} else {
						outToClient.writeBytes("-Not enough room, don't send it\0\n");
					}
					to_stor = false;
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