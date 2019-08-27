rm */*.class;
rm c/stor/*;
javac c/TCPClient.java s/TCPServer.java s/SftpServer.java s/Users.java;

java s/TCPServer