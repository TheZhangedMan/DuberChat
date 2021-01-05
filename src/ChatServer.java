/*[ChatServer.java]
 * A simple chat server that is able to broadcast or send direct messages to multiple clients
 * @author Cindy Wu (template by Mangat)
 * @version 1.1
 */

//imports 
import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ChatServer
 * start and manages server using multiple threads
 */
class ChatServer {
  
  /**
   * Server socket for connection
   */
  ServerSocket serverSock;
  
  /**
   * controls if server is accepting clients
   */
  static Boolean running = true;
  
  /**
   * stores all active clients on the server
   */
  private ArrayList<ConnectionHandler> clients = new ArrayList<>();
  
  /** Main
    * @param args parameters from command line
    */
  public static void main(String[] args) { 
    new ChatServer().go(); //start the server
  }
  
  /** Go
    * Starts the server
    */
  public void go() { 
    System.out.println("Waiting for a client connection..");
    
    Socket client = null;//hold the client connection
    
    try {
      serverSock = new ServerSocket(5000);  //assigns an port to the server
      
      //index to determine client number
      int index = 1;
      
      while(running) {  //this loops to accept multiple clients
        
        client = serverSock.accept();  //wait for connection
        System.out.println("Client connected");
        
        ConnectionHandler newClient = new ConnectionHandler(client, index);
        clients.add(newClient);
        
        index++;
        
        Thread t = new Thread(newClient); //create a thread for the new client and pass in the socket
        t.start(); //start the new thread
        
      }
      
    }catch(Exception e) { 
      
      System.out.println("Error accepting connection");
      //close all and quit
      try {
        client.close();
      }catch (Exception e1) { 
        System.out.println("Failed to close socket");
      }
      System.exit(-1);
    }
  }
  
  /**
   * getUserList
   * returns a string of the other user's and their status to the specified client
   * @param cur - current client that is receiving a list of the other online clients
   * @return A string containing the user names and status' of the other clients
   */
  private synchronized String getUserList (ConnectionHandler cur) {
    
    ArrayList<ConnectionHandler> clients = this.clients;
    
    String userList = "";
    
    for (ConnectionHandler client:clients) {
      if ((!client.equals(cur)) && client.isLoggedIn()) {
        userList += client.getUser()+"|"+client.status+"/";
      }
    }
    return userList;
  }
  
  /**
   * sendToAll
   * sends the specified message to all clients 
   */
  private synchronized void sendToAll (String msg) {
    
    ArrayList<ConnectionHandler> clients = this.clients;
    
    for (ConnectionHandler client:clients) {
      
      try {  
        //send message to specific client
        client.send(msg);
      }
      catch (Exception e) {  
        System.out.println("Could not send message");
        e.printStackTrace();
      }
    }
  }
  
  //splits an input message into recipient and message
  /**
   * ClientMessage
   * @author Cindy Wu
   * static class used to represent a client message as its intended recipient and the message itself
   * (used for client input)
   */
  static class ClientMessage {
    String recipient;
    String message;
    
    /**
     * ClientMessage
     * Constructor - splits a string "<recipient><message>"
     * @param msg
     */
    ClientMessage (String msg) {
      String[] messages = msg.trim().replaceFirst("^<","").replaceFirst(">$","").split("\\s*>*<\\s*");
      this.recipient = messages[0];
      this.message = messages[1];
    }
  }
  
  //***** Inner class - thread for client connection
  /**
   * Connection Handler
   * a runnable class (thread) for client connections; handles all the server-client communication
   */
  class ConnectionHandler implements Runnable { 
    
    /**
     * assign printwriter to network stream
     */
    private PrintWriter output;
    
    /**
     * stream for network input from client
     */
    private BufferedReader input; 
    
    /**
     * keeps track of client socket
     */
    private Socket client;
    
    /**
     * client number
     */
    private int clientNum;
    
    /**
     * keeps track if thread should continue running
     */
    private boolean running; 
    
    /**
     * client username
     */
    private String user;
    
    /**
     * keeps track of whether client has logged in or not
     */
    private boolean loggedIn = false;
    
    /**
     * keeps track of client's status (offline, online, idle, do not disturb)
     */
    private String status = "Offline"; 
    
    /**
     * time of client's last input to check for inactivity
     */
    private long timeLastActive;
    
    /**
     * the amount of time a client has of inactivity before it disconnects (constant; 10 mins)
     */
    private final long TIMEOUT = 60000;
    
    /** ConnectionHandler
      * Constructor
      * @param the socket belonging to this client connection
      */    
    ConnectionHandler(Socket s, int clientNum) { 
      this.client = s;  //constructor assigns client to this   
      this.clientNum = clientNum;
      
      try {  //assign all connections to client
        
        this.output = new PrintWriter(client.getOutputStream());
        InputStreamReader stream = new InputStreamReader(client.getInputStream());
        this.input = new BufferedReader(stream);
        
        timeLastActive = System.currentTimeMillis();
        
      }catch(IOException e) {
        e.printStackTrace();        
      }            
      running=true;
    } //end of constructor
    
    /** getUser
      * returns the username for this client
      * @return user - client's username
      */
    public String getUser() {
      return this.user;
    }
    
    /** isLoggedIn
      * @return true if this client has logged in, false otherwise
      */
    public boolean isLoggedIn() {
      return this.loggedIn;
    }
    
    /** send
      * sends the specified message to this client
      * @param message - message to send to client
      */
    public synchronized void send(String message) {
      output.println(message);
      output.flush();
    }
    
    /** run
      * executed on start of thread
      */
    public void run() {  
      
      //wait for client to login
      while(!loggedIn) {
        try {
          
          if (input.ready()) { //check for an incoming message
            
            //get client username
            this.user = input.readLine();
            
            //check for duplicate usernames
            int dupNum = 0;
            
            for (ConnectionHandler cl:clients) {
              if ((!cl.equals(this)) && (cl.getUser().equals(this.user))) {
                dupNum++;
              }
            }
            
            if (dupNum>0) {
              this.user = this.user+"("+dupNum+")";
            }
            
            loggedIn = true;
            
            System.out.println("[client#"+this.clientNum+"] User <" + user +"> has logged in");
            this.status = "Online";
            
            //send back list of all online users -- each name is separated with a space
            for (ConnectionHandler cl:clients) {
              cl.send(getUserList(cl));
            }
            
            sendToAll(this.user + " has entered the chat");
            
            this.timeLastActive = System.currentTimeMillis();
            
          } else { //if there are no messages, check for timeout
            
            if (System.currentTimeMillis()-this.timeLastActive>=this.TIMEOUT) {
              running = false;
              status = "Offline";
              System.out.println("[client#"+this.clientNum+"] Failed to login due to inactivity.");
              
              this.send("You have been disconnected due to inactivity.");
              break;
            }
          }
          
        } catch (IOException e) {
          System.out.println("[client#"+this.clientNum+"] Login failed");
          
          //send back to client that login failed?
          
          e.printStackTrace();
        } 
      }
      
      //Get a message from the client
      String msg="";
      
      while(running) {  // loop unit a message is received        
        try {
          
          if (input.ready()) { //check for an incoming message
            
            msg = input.readLine();  //get a message from the client
            ClientMessage clientMsg = new ClientMessage(msg);
            
            if (clientMsg.recipient.equals("?")) { 
              
              System.out.println("[client#"+this.clientNum+"] message to server: "+clientMsg.message);
              
              if (clientMsg.message.equals("quit")) {//if client quits -- <?><quit>
                running = false;
                status = "Offline";
                clients.remove(this); //remove client from list
                
              } else if (clientMsg.message.equals("Do Not Disturb")) { //do not disturb mode
                status = "Do Not Disturb";
                
              } else if (clientMsg.message.equals("Idle")) { //idle mode
                status = "Idle";
              }
              
              //update status to everyone -- if quit (this user should not be in the list anymore)
              for (ConnectionHandler cl:clients) {
                cl.send(getUserList(cl));
              }
              
            } else if (clientMsg.recipient.equals("*")) { //send message to everyone -- <*><message>
              sendToAll("*"+this.user+": "+ clientMsg.message); //tagged with asterisk to indicate global message
              
            } else { //direct message -- <recepientUser><message>
              
              for (ConnectionHandler cl:clients) { 
                if (cl.getUser().equals(clientMsg.recipient)) { //only sends to this user and recipient
                  cl.send("[Whisper To "+clientMsg.recipient+"] "+this.user+": "+clientMsg.message);
                  this.send("[Whisper To "+clientMsg.recipient+"] "+this.user+": "+clientMsg.message);
                }
              }
            }
            
            this.timeLastActive = System.currentTimeMillis();
            
          } else { //if there are no messages, check for timeout
            
            if (System.currentTimeMillis()-this.timeLastActive>=this.TIMEOUT) {
              running = false;
              System.out.println("[client#"+this.clientNum+"] User <"+this.user+"> is inactive; disconnecting user");
              
              this.send("You have been disconnected due to inactivity");
              
            } 
          }
          
        } catch (IOException e) { 
          System.out.println("[client#"+this.clientNum+"] Failed to receive msg");
          e.printStackTrace();
        }
      }    
      
      //close the socket
      try {
        input.close();
        output.close();
        client.close();
        
        if (this.user!=null) {
          sendToAll(this.user+" has left the chat");
          System.out.println("[client#"+this.clientNum+"] <"+this.user+"> disconnected/left");
        } 
        
      }catch (Exception e) { 
        System.out.println("[client#"+this.clientNum+"] Failed to close socket");
      }
    } // end of run()
  } //end of inner class   
} //end of Class