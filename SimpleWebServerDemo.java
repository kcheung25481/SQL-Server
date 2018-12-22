/* [SimpleWebServerDemo.java]
 * Description: This is an example of a web server.
 * The program  waits for a client and accepts a message. 
 * It then responds to the message and quits.
 * This server demonstrates how to employ multithreading to accepts multiple clients
 * @author Kevin
 */

//imports for network communication
import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Scanner;
import java.sql.*;


class SimpleWebServerDemo {
  
  ServerSocket serverSock;// server socket for connection
  static Boolean running = true;  // controls if the server is accepting clients
  static Boolean accepting = true;
  /** Main
    * @param args parameters from command line
    */
  public static void main(String[] args) {
    createDatabase();
    new SimpleWebServerDemo().go(); //start the server
  }
  
  /** Go
    * Starts the server
    */
  public void go() {
    System.out.println("Waiting for a client connection..");
    
    Socket client = null;//hold the client connection
    
    try {
      serverSock = new ServerSocket(80);  //assigns an port to the server
      
      while(accepting) {  //this loops to accept multiple clients
        client = serverSock.accept();  //wait for connection
        System.out.println("Client connected");
        //Note: you might want to keep references to all clients if you plan to broadcast messages
        //Also: Queues are good tools to buffer incoming/outgoing messages
        Thread t = new Thread(new ConnectionHandler(client)); //create a thread for the new client and pass in the socket
        t.start(); //start the new thread
//        accepting=false; //only accepts one at a time, SO FAR**********************************************************************************************
      }
    }catch(Exception e) { 
      // System.out.println("Error accepting connection");
      //close all and quit
       try {
       client.close();
       }catch (Exception e1) { 
       System.out.println("Failed to close socket");
       }
       System.exit(-1);
    }
  }
  
  //***** Inner class - thread for client connection
  class ConnectionHandler implements Runnable {
    private DataOutputStream output; //assign printwriter to network stream
    private BufferedReader input; //Stream for network input
    private Socket client;  //keeps track of the client socket
    private boolean running;
    
    //GUI Stuff
    private JButton sendButton, htmlButton;
    private JTextField typeField;
    private JTextArea messageArea; 
    private JPanel southPanel;
    
    /* ConnectionHandler
     * Constructor
     * @param the socket belonging to this client connection
     */    
    ConnectionHandler(Socket s) { 
      this.client = s;  //constructor assigns client to this    
      try {  //assign all connections to client
        this.output =  new DataOutputStream(client.getOutputStream());
        InputStreamReader stream = new InputStreamReader(client.getInputStream());
        this.input = new BufferedReader(stream);
      }catch(IOException e) {
        e.printStackTrace();        
      }            
      running=true;

    } //end of constructor
    
    
    /* run
     * executed on start of thread
     */
    public void run() {
      
      int pageNum = 0; //Starting page number is 0
      
      //Get a message from the client
      String userName = "";
      String message= "";
      
      boolean ready = false;
      String postMessage = "";
      //Get a message from the client
      while(running) {  // loop until a message is received  
        
        try {
          if (input.ready()) { //check for an incoming messge

            //Declare variables for use
            String URL = "";
            int gotURL = 0;
            int contentLength = 0;
            
            while (input.ready()) {
              message = input.readLine();  //get a message from the client

              //Set url
              if (gotURL < 1) {
                URL = message;
                gotURL++;
              }
              
            }
            
            URL = URL.substring(URL.indexOf("/") + 1, URL.indexOf(" HTTP")); //Check the url without the HTTP
            
            if (URL.equals("")) { //If it is on the homepage
              sendHTML("homepage.html");
            }
            if (URL.equals("favicon.ico")) { //Set the icon in the url
              sendHTML(URL);
              ready = false; //do not proceed with the loading of the page
            }
            if (URL.equals("image.jpg")) { //Set the bottom image in the url
              sendHTML(URL);
              ready = false; //do not proceed with the loading of the page
            }

            if (ready == true) { //check if the url has loaded
              if (URL.substring(0,8).equals("sign-in?")) { //If this is the first part of the url it is the home page
                pageNum = 1; //Set page to an integer
                userName = URL.substring(URL.indexOf("Username=") + 9, URL.indexOf("&pass")); //get password and username from url
              } else if(URL.substring(0,8).equals("profile?")) { //If this is the first part of the url it is the profile
                pageNum = 3; //Set page to an integer
              } else if(URL.substring(0,8).equals("log-out?")) { //If this is the first part of the url it is checking if the user wants to log out
                sendHTML("homepage.html");
                pageNum = 0; //Set page to an integer
              } else if (URL.substring(0,9).equals("register?")) { //If this is the first part of the url it is the registration page
                pageNum = 2; //Set page to an integer
              } else if (URL.substring(0,12).equals("editProf?")) { //If this is the first part of the url it is the edit page
                pageNum = 4; //Set page to an integer
              }
            }

            ready = true; // the second time around, we can do this thing
            
            if (pageNum == 1) { //if at the homepage, so therefore the log-in/register button is being pressed
                String password = URL.substring(URL.indexOf("password=") + 9, URL.indexOf("&opti")); //get entered password
                int option = Integer.parseInt(URL.substring(URL.indexOf("option=") + 7));  //check for either registration or log-in
      
                
                if (option == 0) { 
                  Boolean passCorrect = checkPass(userName, password); //test credentials entered
                  if (passCorrect == true) { //If credentials exist
                    sendProfile("profile.html", userName); //move to profile page
                  } else if (passCorrect == false) { //If credentials do not match
                    sendHTML("nouserpass.html"); //Error page
                  }
                } else if (option == 1) { //register the person
                  Boolean alreadyReg = checkUsername(userName); //If the username has been registered
                  if (alreadyReg == false) {
                    sendHTML("alreadyReg.html"); //Error page
                  } else if (alreadyReg == true) {
                    register(userName, password); //Create new user
                    sendHTML("createProf.html");
                  }
                }
              
            }
            if (pageNum == 2) { //if at creating profile page
              String name = URL.substring(URL.indexOf("name=") + 5, URL.indexOf("&color")); //get name entered
              String backgroundColor = URL.substring(URL.indexOf("color=") + 6, URL.indexOf("&description")); //get favourite colour entered
              String description = URL.substring(URL.indexOf("description=") + 12); //get description entered
              insert(userName, name, backgroundColor, description); //save to sql
              sendProfile("profile.html", userName); //go to profile page
            }
            if (pageNum == 3) { //if editing profile was clicked
              sendProfile("editProf.html", userName);
            }
            
            if (pageNum == 4) { //if at editing profile page
              String name = URL.substring(URL.indexOf("name=") + 5, URL.indexOf("&color")); //get name entered
              String backgroundColor = URL.substring(URL.indexOf("color=") + 6, URL.indexOf("&description")); //get facourite colour entered
              String description = URL.substring(URL.indexOf("description=") + 12); //get description entered

              update(userName, name, backgroundColor, description); //save new parameters to sql
              sendProfile("profile.html", userName); //go to profile page
            }

          }
          
        //Catch a non-exisitant message error  
        }catch (IOException e) {
          System.out.println("Failed to receive a message from the client");
          e.printStackTrace();
        }
      }
      
      //close the socket
       try {
       input.close();
       output.close();
       client.close();
       }catch (Exception e) {
       System.out.println("Failed to close socket");
       }
       
    } 
    
    /********************METHODS********************/
    
    /*
     * removeSpaces
     * @param string the original string
     * method to remove '+' from a string and replace it with ' '
     */
    public String removeSpaces(String input) {
      while (input.indexOf("+") != -1) {
        input = input.substring(0, input.indexOf("+")) + " " + input.substring(input.indexOf("+") +1);
      }
      return input;
    }
    
    /**
     * sendProfile
     * @param HTML to decide which file to send
     * @param username to search sql database
     * uses sql to send profile stored
     */
    public void sendProfile (String HTML, String userName) {
      File original = new File(HTML);
      File webFile = new File("temp.html");
      
      //copy the original file to temp.html, but replace anything between % signs
      try {
        Scanner input = new Scanner(original);
        PrintWriter print = new PrintWriter(new FileOutputStream(webFile));
        while (input.hasNextLine()) {
          //check if % signs in a line and replace text
          String line = input.nextLine();
          String editedLine = line;
          if (line.indexOf('%') != -1) { //if the %character exists within a line
            int first = line.indexOf('%');
            int second = line.substring(line.indexOf('%') + 1).indexOf('%') + first +1;
            String toReplace = line.substring(first + 1, second);
            if (toReplace.equals("NAME")) {
              //get name from SQL
              editedLine = line.substring(0, first) + select(userName, "name") + line.substring(second + 1);
            } else if (toReplace.equals("COLOR")) {
              editedLine = line.substring(0, first) + select(userName, "backgroundcolor") + line.substring(second + 1);
            } else if (toReplace.equals("DESCRIPTION")) {
              editedLine = line.substring(0, first) + select(userName, "deinputription") + line.substring(second + 1);
            }
          }
          //print edited line to the temp html file
          print.println(removeSpaces(editedLine));
        }
        print.close();
        sendHTML("temp.html");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    
    /*
     * register
     * @param username that is to be saved
     * @param password that is to be saved
     * registeres new users into the sql
     */
    public void register (String user, String pass) {
      File storePass = new File("passwords.txt");
      
      try {
        
        //Create printer to txt file
        PrintWriter print = new PrintWriter(new FileOutputStream(storePass,true));
        
        //Store credentials
        print.println(user);
        print.println(pass);
        print.println("");
        print.close();
        System.out.println("successful registration");
      }
      
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    
    /*
     * checkPass
     * @param username that is to be checked
     * @param password that is to be checked
     * @return boolean if such account exisits
     * checks if there is such an account with both the username and password
     */
    public boolean checkPass (String userName, String password) {
      File passwords = new File("passwords.txt"); //Stored credentials in a txt file

      try {
        
        //Create a scanner to read file
        Scanner input = new Scanner(passwords);
        
        //Parse through the txt file to read the saved passwords and usernames
        while (input.hasNextLine()) {
          String user = input.nextLine();
          if (user.equals(userName)) {
            String pass = input.nextLine();
            if (pass.equals(password)) {
              return true; //if such combination exists
            }
          }
        }
        input.close();
      }
      //Catch if file is now found
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      
      return false; //if such combination does not exist
      
    }
    
    /*
     * checkUsername
     * @param username that is to be checked
     * @return boolean if such account exisits
     * checks if the username has been taken
     */
    public boolean checkUsername (String userName) {
      userName = userName.toLowerCase();
      File userNames = new File("passwords.txt"); //Stored credentials in a txt file
      try {
        
         //Create a scanner to read file
        Scanner input = new Scanner(userNames);
        
        //Parse through the txt file to read the saved usernames
        while (input.hasNextLine()) {
          String user = input.nextLine().toLowerCase();
          if (user.equals(userName)) {
            return false; //If such username exists
          }
        }
        input.close();
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      
      return true; //If such username does not exist
      
    }
    
    /**
     * sendHTML
     * @param HTML determines which file to send
     * sends html file for quick response to the user
     */
    public void sendHTML (String HTML) {
      
      File webFile = new File(HTML);
      BufferedInputStream in = null;
      
      try {
        in = new BufferedInputStream(new FileInputStream(webFile));
        int data;
        
        System.out.println("File Size: " +webFile.length());
        byte[] d = new byte[(int)webFile.length()];
        
        output.writeBytes("HTTP/1.1 200 OK" + "\n");
        output.flush();
        
        output.writeBytes("Content-Type: text/html"+"\n");
        output.flush();
        output.writeBytes("Content-Length: " + webFile.length() + "\n\n");
        output.flush();
        
        String t="";
        
        
        in.read(d,0,(int)webFile.length());
        
        
        System.out.print(d[0]);
        System.out.println();
        System.out.println("read: " +webFile.length()+" bytes");
        output.write(d,0,d.length);
        System.out.println("sent: " +webFile.length()+" bytes");
        
        
        output.flush();
        
      } catch (IOException e) { 
        e.printStackTrace();}               
 
      return;
    } 
    
  } 
  
  /**
   * update
   * @param username that is stored
   * @param name that is stored
   * @param backgroundcolor that is stored
   * @param description that is stored
   * updates sql
   */
  public static void update(String username, String name, String backgroundcolor, String description) {
    Connection connect = null;
    Statement state = null;
    
    try {
      Class.forName("org.sqlite.JDBC");
      connect = DriverManager.getConnection("jdbc:sqlite:test.db");
      connect.setAutoCommit(false);
      System.out.println("Opened database successfully");
      
      state = connect.createStatement();
      
      //updates information
      String sql1 = "UPDATE CUSTOM set NAME = '"+name+"' where USERNAME= '"+username+"';";
      String sql2= "UPDATE CUSTOM set BACKGROUNDCOLOR = '"+backgroundcolor+"' where USERNAME= '"+username+"';";
      String sql3 = "UPDATE CUSTOM set DESCRIPTION = '"+description+"' where USERNAME= '"+username+"';";
      state.executeUpdate(sql1);
      state.executeUpdate(sql2);
      state.executeUpdate(sql3);
      connect.commit();
      
      state.close();
      connect.close();
    } catch ( Exception e ) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
  }
  
  /**
   * createDatabase
   * creates the SQL database
   */
  public static void createDatabase() {
    Connection connect = null;
    Statement state = null;
    
    try {
      Class.forName("org.sqlite.JDBC");
      connect = DriverManager.getConnection("jdbc:sqlite:test.db");
      System.out.println("Opened database successfully");
      
      state = connect.createStatement();
      //create table if it doesn't exist
      //with 'columns' of "username", "name", "text", "backgroundcolor", and "description"
      String sql = "CREATE TABLE IF NOT EXISTS CUSTOM" +
        "(USERNAME TEXT PRIMARY KEY NOT NULL, " + 
        " NAME TEXT NOT NULL, " + 
        " BACKGROUNDCOLOR TEXT NOT NULL, " + 
        " DESCRIPTION TEXT)"; 
      
      //properties:
      //primary key = must be a unique value
      //text = must be text
      //not null = must exist
      
      state.executeUpdate(sql);
      state.close();
      connect.close();
    } catch ( Exception e ) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    System.out.println("Table created successfully");
  }
  
  /**
   * insert
   * @param username to be stored
   * @param name to be stored
   * @param backgroundColor to be stored
   * @param description to be stored
   * adds more rows to database
   */
  public static void insert(String username, String name, String backgroundColor, String description ) {
    Connection connect = null;
    Statement state = null;
    
    try {
      Class.forName("org.sqlite.JDBC");
      connect = DriverManager.getConnection("jdbc:sqlite:test.db");
      connect.setAutoCommit(false);
      System.out.println("Opened database successfully");
      
      state = connect.createStatement();
      String sql = "INSERT INTO CUSTOM (USERNAME,NAME,BACKGROUNDCOLOR,DESCRIPTION) " +
        "VALUES ('"+username+"', '"+name+"', '"+backgroundColor+"', '"+description+"');"; //pretty self explanatory, see insert method for reference
      state.executeUpdate(sql);
      
      state.close();
      connect.commit();
      connect.close();
    } catch ( Exception e ) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
    System.out.println("Records created successfully");
  }
  
  /**
   * select
   * @param username that is to be searched
   * @param need requested parameter
   * get info from database
   */
  public static String select(String username, String need) {
    String query = "";
    Connection connect = null;
    Statement state = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connect = DriverManager.getConnection("jdbc:sqlite:test.db");
      connect.setAutoCommit(false);
      System.out.println("Opened database successfully");
      
      state = connect.createStatement();
      ResultSet rs = state.executeQuery( "SELECT * FROM CUSTOM WHERE USERNAME = '"+username+"';"); 
      
      while ( rs.next() ) {
        //store all parameters in variable
        String  name = rs.getString("name");
        String  backgroundcolor = rs.getString("backgroundcolor");
        String description = rs.getString("description");
        
        //set query equal to what is needed
        if (need.equals("name" )) {
          query = name;
        } else if (need.equals("backgroundcolor")) {
          query = backgroundcolor;
        } else if (need.equals("description")) {
          query = description;
        }

      }
      rs.close();
      state.close();
      connect.close();
    } catch ( Exception e ) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    System.out.println("Operation done successfully");
    return query;
  }
  
}