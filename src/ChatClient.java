import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/* [ChatClient.java]
 * An implementation of a basic chat client
 * @author Ethan Zhang
 * @ version 2.0
 * 2020/12/14
 */
class ChatClient {
  private JButton loginButton, sendButton, quitButton, logoButton;
  private String[] statuses = {"Online", "Idle", "Do Not Disturb"};
  private String[] users = {""};
  private String[] whisperOptions; //Stores the options for direct messages or whispers
  private BufferedImage logo;
  private ImageIcon icon;
  private JComboBox<String> statusBox, whisperBox;
  private JTextField usernameField, typeField;
  private JTextArea msgArea, listArea;  
  private JScrollPane chatScroll, listScroll;
  private JLabel usernameLabel, accountLabel, userListLabel, whisperLabel, errorLabel;
  private JFrame loginWindow, chatWindow, userWindow;
  private JPanel loginPanel, logoPanel, southPanel, northPanel;
  private Socket mySocket; //Socket for connection
  private BufferedReader input; //Reader for network stream
  private PrintWriter output;  //PrintWriter for network output
  private boolean running = true; //Thread status via boolean
  private String username;
  
  /**
   * main
   * The main method that runs the program
   * @param args, a String array of arguments from command line
   */
  public static void main(String[] args) { 
    new ChatClient().requestLogin();
  }
  
  /**
   * requestLogin
   * Connects client to the server and boots up a JFrame for username request
   */
  public void requestLogin() {
    // call a method that connects to the server 
    connect("25.6.165.81", 5000);
    
    try {
      logo = ImageIO.read(new File("logo.png"));
    } catch (Exception e) {
      System.out.println("Logo could not be loaded...");
    }
    
    loginWindow = new JFrame("duberChat - Login");
    loginWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    loginPanel = new JPanel();
    loginPanel.setLayout(new GridLayout(5, 0));
    logoPanel = new JPanel();
    
    usernameLabel = new JLabel("Username");
    
    usernameField = new JTextField();
    usernameField.addActionListener(new LoginListener()); //Will send username when button is pressed
    
    loginButton = new JButton("Sign In");
    loginButton.addActionListener(new LoginListener()); //Will send username when enter key is pressed
    
    errorLabel = new JLabel(""); //Error label used to inform client if entered username is invalid
    
    icon = new ImageIcon(logo);
    logoButton = new JButton();
    logoButton.setIcon(icon);
    
    loginPanel.add(usernameLabel);
    loginPanel.add(usernameField);
    loginPanel.add(loginButton);
    loginPanel.add(errorLabel);
    
    logoPanel.add(logoButton);
    
    loginWindow.add(BorderLayout.NORTH, logoButton);
    loginWindow.add(BorderLayout.SOUTH, loginPanel);
    
    loginWindow.setSize(700, 400); 
    loginWindow.setVisible(true);
  }
  
  /**
   * go
   * Boots up necessary JFrames for chatting and begins reading messages from and writing messages to server
   */
  public void go() {
    chatWindow = new JFrame("duberChat - Conversation");
    chatWindow.addWindowListener(new WindowTracker());
    chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    chatWindow.setSize(400,400);
    chatWindow.setVisible(true);
    
    southPanel = new JPanel();
    southPanel.setLayout(new GridLayout(2,0));
    
    sendButton = new JButton("SEND");
    sendButton.addActionListener(new SendListener()); //Will send message when clicked
    
    whisperLabel = new JLabel("Whisper To:");
    
    whisperBox = new JComboBox<String>(users); //JComboBox for client to select desired user to directly message
    whisperBox.setSelectedIndex(0);            //The first option is always empty and is for messaging the main chat
    
    typeField = new JTextField(10);
    typeField.addActionListener(new SendListener()); //Will send message when enter key is pressed
    
    msgArea = new JTextArea();
    msgArea.setEditable(false);
    chatScroll = new JScrollPane(msgArea);
    
    southPanel.add(typeField);
    southPanel.add(sendButton);
    southPanel.add(whisperLabel);
    southPanel.add(whisperBox);
    
    chatWindow.add(BorderLayout.CENTER, chatScroll);
    chatWindow.add(BorderLayout.SOUTH, southPanel);
    
    userWindow = new JFrame("duberChat - Members");
    userWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    userWindow.addWindowListener(new WindowTracker());
    userWindow.setSize(320, 600);
    userWindow.setVisible(true);
    
    northPanel = new JPanel();
    northPanel.setLayout(new GridLayout(2, 2));
    
    accountLabel = new JLabel(username);
    
    quitButton = new JButton("QUIT");
    quitButton.addActionListener(new QuitButtonListener());
    
    statusBox = new JComboBox<String>(statuses);
    statusBox.setSelectedIndex(0);
    statusBox.addActionListener(new StatusListener());
    
    userListLabel = new JLabel("User List");
    
    listArea = new JTextArea();
    listArea.setEditable(false);
    listScroll = new JScrollPane(listArea);
    
    northPanel.add(accountLabel);
    northPanel.add(quitButton);
    northPanel.add(statusBox);
    northPanel.add(userListLabel);
    
    userWindow.add(BorderLayout.NORTH, northPanel);
    userWindow.add(BorderLayout.CENTER, listScroll);
    
    //After connecting loop and keep appending[.append()] to the JTextArea
    readMessagesFromServer();
  }
  
  /**
   * connect
   * Attempts to connect to the server and creates the socket and streams
   * @param ip, a String that corresponds to the IP address to be connected to
   * @param port, an integer value that represents the port number of the communication endpoint
   */
  public Socket connect(String ip, int port) { 
    System.out.println("Attempting to make a connection...");
    
    try {
      mySocket = new Socket("127.0.0.1", 5000); //Attempt socket connection (local address)
                                                //This will wait until a connection is made
      InputStreamReader stream1= new InputStreamReader(mySocket.getInputStream()); //Stream for network input
      input = new BufferedReader(stream1);     
      output = new PrintWriter(mySocket.getOutputStream()); //Assign PrintWriter to network stream
      
    } catch (IOException e) { //When connection error occurs
      System.out.println("Connection to server failed...");
      e.printStackTrace();
    }
    
    System.out.println("Connection successfully established.");
    return mySocket;
  }
  
  /**
   * readMessagesFromServer
   * Starts a loop waiting for server input and then displays it on the text area
   */
  public void readMessagesFromServer() { 
    new Thread() { //Create a new thread so that the main thread is not locked in a while loop
      public void run() {
        while (running) { //Loop until client exits
          try {
            if (input.ready()) { //Check for incoming message
              String msg;          
              msg = input.readLine(); //Read the message
              if (msg.indexOf(":") == -1) { //If the message does not contain a colon, it is not a user message
                if (msg.equals("") || msg.indexOf("/") != -1) { //Check if the incoming message is the user list
                  users = msg.split("/");
                  listArea.setText("");
                  for (int i = 0; i < users.length; i++) {
                    listArea.append(users[i] + "\n");
                  }
                  whisperBox.removeAllItems();
                  whisperBox.addItem("");
                  for (int i = 0; i < users.length; i++) {
                    whisperOptions = new String[users.length];
                    if (users[i].indexOf("|") != -1) {
                      whisperOptions[i] = users[i].substring(0, users[i].indexOf("|"));
                      whisperBox.addItem(whisperOptions[i]);
                    }
                  }
                } else { //If the incoming message is not a user list, it is a miscellaneous message for all to see
                  msgArea.append(msg + "\n");
                }
              } else if (msg.charAt(0) == '*') { //If the message is tagged with an asterisk it is a user message for
                                                 //every user to see
                msgArea.append(msg.substring(1) + "\n");
              } else {
                msgArea.append(msg + "\n");
              }
            }
          } catch (IOException e) { 
            System.out.println("Failed to receive message from the server...");
            e.printStackTrace();
          }
        }
        try { //After leaving the main loop, all sockets must be closed and program terminated
          input.close();
          output.close();
          mySocket.close();
          System.exit(0);
        } catch (Exception e) { 
          System.out.println("Failed to close socket...");
        }
      }
    }.start();
  }
  
  //Inner classes for EventListeners
  
  /**
   * [QuitButtonListener.java]
   * The button listener for quitting the program
   * @author Ethan Zhang
   * 2020/12/14
   */
  class QuitButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent event)  {
      output.println("<?><quit>"); //Send quitting message to server
      output.flush();
      running = false; //Break out of loop
    }     
  }
  
  /**
   * [WindowTracker.java]
   * The window adapter that correctly closes the sockets on JFrame termination
   * @author Ethan Zhang
   * 2020/12/14
   */
  class WindowTracker extends WindowAdapter {
    public void windowClosing(WindowEvent event) {
      output.println("<?><quit>");
      output.flush();
      running = false;
    }
  }
  
  /**
   * [SendListener.java]
   * The action listener for sending messages
   * @author Ethan Zhang
   * 2020/12/14
   */
  class SendListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if (typeField.getText().trim().equals("")) { //Prevents input of only whitespaces
        typeField.setText("");
      } else if (whisperBox.getSelectedItem().equals("")) { //Sends message to all users
        output.println("<*>" + "<" + typeField.getText() + ">");
        output.flush();
        typeField.setText("");
      } else { //Sends message to the user selected in the whisper box
        output.println("<" + whisperBox.getSelectedItem() + "><" + typeField.getText() + ">");
        output.flush();
        typeField.setText("");
      }
    }
  }
  
  /**
   * [LoginListener.java]
   * The action listener for logging in
   * @author Ethan Zhang
   * 2020/12/14
   */
  class LoginListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if ((usernameField.getText().trim().equals("")) || (usernameField.getText().indexOf("/") != -1) ||
          (usernameField.getText().indexOf(":") != -1) || (usernameField.getText().indexOf("*") != -1) ||
          (usernameField.getText().indexOf("?") != -1) || (usernameField.getText().indexOf("<") != -1) ||
          (usernameField.getText().indexOf(">") != -1) || (usernameField.getText().indexOf("|") != -1)) {
        errorLabel.setText("Username must consist of at least one character and cannot contain the following " +
                           "characters: / : * ? < > |");
        usernameField.setText("");
      } else {
        username = usernameField.getText();
        output.println(usernameField.getText());
        output.flush();
        usernameField.setText("");
        loginWindow.dispose();
        go(); //Start program
      }
    }
  }
  
  /**
   * [StatusListener.java]
   * The action listener for setting the client's current status
   * @author Ethan Zhang
   * 2020/12/14
   */
  class StatusListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      JComboBox cb = (JComboBox)event.getSource();
      output.println("<?>" + "<" + (String)cb.getSelectedItem() + ">"); //Set status
      output.flush();
    }
  }
}