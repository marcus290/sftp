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
javac c/TCPClient.java s/TCPServer.java s/SftpServer.java s/Users.java