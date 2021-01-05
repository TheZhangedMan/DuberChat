# DuberChat
A simple chatting software modelled after old chatting softwares.
* This is the summative for the fourth unit of ICS4UE, demonstrating basic networking principles in Java
* This was a group project in which another group member was responsible for server-side interactions and I was responsible for client-side interactions
* The user interface consists of two JFrames: Members and Conversation
    * The Members JFrame lists all users connected to the network and their respective statuses
        * It is also the JFrame in which one's own status can be changed
    * The Conversation JFrame shows all received messages from the network and enables the user to sent messages to the network
* Users are able to directly message, or whisper, to others by selecting who to whisper to in the Conversation JFrame
    * The Conversation JFrame defaults to sending to all other users connected to the network