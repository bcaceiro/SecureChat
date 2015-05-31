package client;


import javax.crypto.*;
import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.xml.bind.DatatypeConverter;

@SuppressWarnings("deprecation")
public class ChatClient implements Runnable {


    private Socket socket              = null;
    private Thread thread              = null;
    private DataInputStream  console   = null;
    private DataOutputStream streamOut = null;
    private ChatClientThread client    = null;


    private PublicKey serverPubKey;
    private Cipher messageCipher;
    private SecretKey secretKey;


    public ChatClient(String serverName, int serverPort) {

        System.out.println("Establishing connection to server...");

        try {
            // Establishes connection with server (name and port)
            socket = new Socket(serverName, serverPort);
            System.out.println("Connected to server: " + socket);
            start();
        } catch(UnknownHostException uhe) {
            // Host unkwnown
            System.out.println("Error establishing connection - host unknown: " + uhe.getMessage());
        } catch(IOException ioexception) {
            // Other error establishing connection
            System.out.println("Error establishing connection - unexpected exception: " + ioexception.getMessage());
        }
    }

    public void run() {

        try {
            messageCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            messageCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        String plainText;
        byte [] encryptedText;

        while (thread != null) {
            try {
               // Sends message from console to server

               // Encrypt message to send
                plainText = console.readLine();
                encryptedText = messageCipher.doFinal(plainText.getBytes());

                streamOut.write(encryptedText);
                streamOut.flush();

            } catch(IOException ioexception) {
               System.out.println("Error sending string to server: " + ioexception.getMessage());
               stop();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }


    public void handle(String msg) {
        // Receives message from server
        if (msg.equals(".quit")) {
            // Leaving, quit command
            System.out.println("Exiting...Please press RETURN to exit ...");
            stop();
        }
        else
            // else, writes message received from server to console
            System.out.println(msg);
    }

    // Inits new client thread
    public void start() throws IOException {

        console   = new DataInputStream(System.in);
        streamOut = new DataOutputStream(socket.getOutputStream());


        //load server public key
        File filePublicKey = new File("serverPub.key");
        FileInputStream keyfos = new FileInputStream("serverPub.key");
        byte[] serverPublicKey = new byte[(int) filePublicKey.length()];
        keyfos.read(serverPublicKey);
        keyfos.close();

        try {

            serverPubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(serverPublicKey));

            //System.out.println("SERVER PUB KEY: " + serverPubKey.toString());

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        //generate session key
        KeyGenerator keyGen = null;
        try {

            keyGen = KeyGenerator.getInstance("AES");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        keyGen.init(256); // for example
        secretKey = keyGen.generateKey();

        //encrypt session key
        Cipher cipher = null;
        byte[] encryptedKey = null;
        byte[] keyToEncrypt = secretKey.getEncoded();
        //System.out.println("-> " + keyToEncrypt.length + "   " + keyToEncrypt.toString());

        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, serverPubKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            encryptedKey = cipher.doFinal(keyToEncrypt);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        System.out.println("Encryted Data: " + encryptedKey);
        System.out.println("Secret Key: " + DatatypeConverter.printBase64Binary(keyToEncrypt));

        //send session key to server
        streamOut.write(encryptedKey);

        if (thread == null) {
            client = new ChatClientThread(this, socket);
            thread = new Thread(this);
            thread.start();
        }
    }

    // Stops client thread
    public void stop() {

        if (thread != null) {
            thread.stop();
            thread = null;
        }

        try {

            if (console   != null)
                console.close();
            if (streamOut != null)
                streamOut.close();
            if (socket    != null)
                socket.close();

        } catch(IOException ioe) {
            System.out.println("Error closing thread..."); }
            client.close();
            client.stop();
        }


    public static void main(String args[]) {
        ChatClient client = null;
        if (args.length != 2)
            // Displays correct usage syntax on stdout
            System.out.println("Usage: java ChatClient host port");
        else
            // Calls new client
            client = new ChatClient(args[0], Integer.parseInt(args[1]));
    }

}

@SuppressWarnings("deprecation")
class ChatClientThread extends Thread {

    private Socket           socket   = null;
    private ChatClient       client   = null;
    private DataInputStream  streamIn = null;

    public ChatClientThread(ChatClient _client, Socket _socket) {

        client   = _client;
        socket   = _socket;
        open();  
        start();
    }
   
    public void open() {
        try {

            streamIn  = new DataInputStream(socket.getInputStream());

        } catch(IOException ioe) {
            System.out.println("Error getting input stream: " + ioe);
            client.stop();
        }
    }
    
    public void close() {
        try {

            if (streamIn != null)
                streamIn.close();

        } catch(IOException ioe) {
            System.out.println("Error closing input stream: " + ioe);
        }
    }
    
    public void run() {
        while (true) {
            try {

                client.handle(streamIn.readUTF());

            } catch(IOException ioe) {
                System.out.println("Listening error: " + ioe.getMessage());
                client.stop();
            }
        }
    }
}

