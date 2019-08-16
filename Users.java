import java.io.*; 
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Users {
    public HashMap<String, String[]> userDB;

    public Users() throws FileNotFoundException {
        this.userDB = new HashMap<String, String[]>();

        // Load the users record
        File file = new File("users.txt");
        Scanner sc = new Scanner(file); 
        while (sc.hasNextLine()) {
            String[] buffer = sc.nextLine().split("\t", -1);
            this.userDB.put(buffer[0], Arrays.copyOfRange(buffer, 1, buffer.length));
        }
        sc.close();

    }

    public boolean inUsers(String user) {
        return this.userDB.containsKey(user);
    }
    public boolean needAccPass(String user) {
        return !(
            this.userDB.get(user)[0] == "" &&
            this.userDB.get(user)[1] == ""
        );
    }
}