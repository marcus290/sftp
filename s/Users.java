package s;

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
        
        /** User data is loaded from txt file and stored in a HashMap
         * in the form
         * String <username>: String[] {<password>, <accounts>}
         * 
         * <accounts> is stored as a comma separated string of account names*/ 
        while (sc.hasNextLine()) {
            String[] buffer = sc.nextLine().split("\t", -1);
            this.userDB.put(buffer[0], Arrays.copyOfRange(buffer, 1, buffer.length));
        }
        sc.close();

    }

    // Check whether a user is in the database
    public boolean inUsers(String user) {
        return this.userDB.containsKey(user);
    }

    // Check whether a user requires account or password input
    public boolean needAccPass(String user) {
        return (!(
            this.userDB.get(user)[0].equals("") &&
            this.userDB.get(user)[1].equals("")
        ));
    }

    // Return an array list of the account names
    public ArrayList<String> getAccounts(String user) {
        ArrayList<String> accounts = new ArrayList<String>(
            Arrays.asList(this.userDB.get(user)[1].split(","))
        );
        return accounts; 
    }

    // Return the password as a string. If no password, return ""
    public String getPassword(String user) {
        return this.userDB.get(user)[0];
    }
    
}