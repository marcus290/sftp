# sftp
A simple sftp client/server according to RFC913 for educational purposes only.

https://github.com/marcus290/sftp

- Intended for use in Windows only.
- The connection is set to `"localhost", 6789`.
- `test.sh` will compile the java source code for client and server, initialise the storage directories and then run the functional tests.
- `compile.sh` will only compile the java source code for client and server.

The directory structure is initialised as follows:
```
sftp
    -> c
        -> TCPClient.class
        -> stor (client side storage directory)
            -> fromClient.txt
    -> s
        -> TCPServer.class
        -> stor (server side storage directory)
            -> fromServer.txt
            -> waves.jpg
    -> test_inputs (contains all client commands for the tests)
```

## Tests
Tests are run via
```
./tests.sh
```
All client input commands executed by the tests are in the `/test_inputs` folder.

### Test 1 - USER, PASS, ACCT, DONE
Test 1 starts the server and enters:
- an incorrect username, which should fail to progress the login process
- a username in the database, which should cause the server to prompt for password and account inputs
- an incorrect password
- a correct password
- an incorrect user account
- a correct user account, which should result in the user successfully logging in
- `DONE`

Client command inputs (automatically executed) are:
```
USER user_not_in_database
USER user_1
PASS wrong_password
PASS pass_1
ACCT acct_not_in_database
ACCT acct_1
DONE
```

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

### Tests 2i, 2ii, 2iii
Tests users who require (i) no account, (ii) no password, (iii) neither account nor password respectively.
All 3 users should successfully log in and close the connection via `DONE`.

### Test 3 - LIST, CDIR, DONE
Test 3 checks the output of `LIST F`, `LIST V` and `CDIR` commands. The default directory is `./s/stor` which holds the files stored on the server side.

`CDIR` will attempt to change to `/tmp` which will error for Windows, then correctly `CDIR` to `C:\\`. Another `LIST V` will check whether the directory was properly changed.

Client command inputs (automatically executed) are:
```
USER charlie
LIST F
LIST V
CDIR /tmp
CDIR C:\\
LIST V
DONE
```

### Test 4 - STOR, NAME, KILL
Test 4 checks the functionality of `STOR`, `NAME` and `KILL`. The following commands are sent:
- Various invalid `STOR` commands, which return error messages
- Correct `STOR` command (`OLD` mode) for `fromClient.txt` (13 bytes), and `SIZE` command
- `LIST V` to show it is saved to the server side
- Correct `STOR` command (APP mode) for `fromClient.txt` (13 bytes), and `SIZE` command
- `LIST V` to show filesize of `fromClient.txt` has doubled to 26 bytes because append has occurred
- `NAME` to rename the file to `newNameFromClient.txt`
- `LIST V` to show renaming done
- `KILL` to delete the file
- `LIST V` to show delete successful
- `DONE`

Client command inputs (automatically executed) are:
```
USER charlie
STOR
STOR test_wrong_mode fromClient.txt
STOR OLD test_invalid_filename
STOR OLD fromClient.txt
SIZE 13
LIST V
STOR APP fromClient.txt
SIZE 13
LIST V
NAME fromClient.txt
TOBE newNameFromClient.txt
LIST V
KILL newNameFromClient.txt
LIST V
DONE
```

### Test 5 - RETR, TYPE
Test 5 checks the functionality of `RETR` and `TYPE` with the following commands:
- Various invalid `RETR` commands, which return error messages
- Correct `RETR` command for `waves.jpg`
- Cancel the retrieval via `STOP`
- Reenter `RETR` command for `waves.jpg`
- Confirm the retrieval via `SEND`
- `TYPE A` (ascii mode), `TYPE B` (binary mode) and `TYPE C` (continuous mode)
- Switch to `TYPE A` (ascii mode)
- `RETR` command for `fromServer.txt`, which will be sent by ascii encoding
- Confirm the retrieval via `SEND`
- `DONE`

Client command inputs (automatically executed) are:
```
USER charlie
RETR
RETR test_invalid_filename
RETR waves.jpg
STOP
RETR waves.jpg
SEND
TYPE A
TYPE B
TYPE C
TYPE A
RETR fromServer.txt
SEND
DONE
```

After closing the connection, we check `waves.jpg` and `fromServer.txt` are successfully retrieved to the client side directory by `cd c/stor; ls`.

