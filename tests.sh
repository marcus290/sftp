#!/bin/bash

# Remove files if present
rm */*.class;
rm c/stor/*;
rm s/stor/*;

# Copy test files to client and server directories
cp test_files/fromClient.txt c/stor/fromClient.txt
cp test_files/fromServer.txt s/stor/fromServer.txt
cp test_files/waves.jpg s/stor/waves.jpg

# Compile java files
javac c/TCPClient.java s/TCPServer.java s/SftpServer.java s/Users.java;


# Start the server
java s/TCPServer &
printf "Server started. Running tests..."

printf "\n\n##### Test 1: USER, ACCT, PASS, DONE #####\n\n";
java c/TCPClient < test_inputs/input1.txt;

# Tests 2i, 2ii, 2iii: users who require (i) no account, (ii) no password, (iii) neither account nor password
printf "\n\n##### Test 2i: users who require no account #####\n";
java c/TCPClient < test_inputs/input2i.txt;
printf "\n##### Test 2ii: users who require no password #####\n";
java c/TCPClient < test_inputs/input2ii.txt;
printf "\n##### Test 2iii: users who require neither account nor password #####\n";
java c/TCPClient < test_inputs/input2iii.txt;

printf "\n\n##### Test 3: LIST F, LIST V, CDIR #####\n\n";
java c/TCPClient < test_inputs/input3.txt;

printf "\n\n##### Test 4: STOR, NAME, KILL #####\n\n";
java c/TCPClient < test_inputs/input4.txt;

printf "\n\n##### Test 5: RETR, TYPE #####\n\n";
java c/TCPClient < test_inputs/input5.txt;
printf "##### Client side directory should include waves.jpg and fromServer.txt retrieved from the server: \n";
cd c/stor;
ls