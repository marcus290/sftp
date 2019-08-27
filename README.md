# sftp
A simple sftp client/server for educational purposes only

## Tests
Tests are run via
```
./tests.sh
```
`tests.sh` will compile the java source code, prepare the server/client directories, and start the server.

####Test 1 - USER, PASS, ACCT, DONE
Test 1 starts the server and enters:
- an incorrect username, which should fail to progress the login process
- a username in the database, which should cause the server to prompt for password and account inputs
- an incorrect password
- a correct password
- an incorrect user account
- a correct user account, which should result in the user successfully logging in
- `DONE`
The expected output is:
```
+CS725 SFTP Service
-Invalid user-id, try again
+User-id valid, send account and password
-Wrong password, try again
+Send account
-Invalid account, try again
! Account valid, logged-in
+CS725 closing connection
```

####Tests 2i, 2ii, 2iii
Test 3 users who require (i) no account, (ii) no password, (iii) neither account nor password respectively.
All 3 users should successfully log in and close the connection via DONE.

####Test 3 - LIST, CDIR, DONE
Test 3 checks the output of `LIST F`, `LIST V` and `CDIR` commands. The default directory is `./s/stor` which holds the files stored on the server side.
`CDIR` will attempt to change to both `C:\\` for Windows and `~/Downloads` for Ubuntu and will fail depending on the operating system. Another `LIST V` will check whether the directory was properly changed.

####Test 4 - STOR, NAME, KILL
Test 4 checks the function of `STOR`, `NAME` and `KILL`. The following commands are sent:
- Various invalid STOR commands, which return error messages
- Correct STOR command (OLD mode) for `fromClient.txt` (13 bytes), and `SIZE` command
- `LIST V` to show it is saved to the server side
- Correct STOR command (APP mode) for `fromClient.txt` (13 bytes), and `SIZE` command
- `LIST V` to show filesize of `fromClient.txt` has doubled to 26 bytes because append has occurred
- `NAME` to rename the file to `newNameFromClient.txt`
- `LIST V` to show renaming done
- `KILL` to delete the file
- `LIST V` to show delete successful
- `DONE`