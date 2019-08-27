# sftp
A simple sftp client/server for educational purposes only

## Tests
Tests are run via
```
./tests.sh
```
`tests.sh` will compile the java source code, prepare the server/client directories, and start the server.

####Test 1
Test 1 starts the server and enters:
- an incorrect username, which should fail to progress the login process
- a username in the database, which should cause the server to prompt for password and account inputs
- an incorrect password
- a correct password
- an incorrect user account
- a correct user account, which should result in the user successfully logging in
The expected output is:
