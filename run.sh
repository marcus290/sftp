cd c;
rm *.class;
rm *.txt;
rm *.jpg;
javac TCPClient.java;
cd ../s;
rm *.class;
javac TCPServer.java SftpServer.java Users.java;
java TCPServer