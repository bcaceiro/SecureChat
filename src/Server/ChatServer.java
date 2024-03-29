package server;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.net.*;
import java.io.*;
import java.security.*;

@SuppressWarnings("deprecation")
public class ChatServer implements Runnable {


	private ChatServerThread clients[] = new ChatServerThread[20];
	private ServerSocket server_socket = null;
	private Thread thread = null;
	private int clientCount = 0;


	private static KeyPair pair;
	private static PrivateKey priv;
	private static PublicKey pub;

	public static PrivateKey getPriv() {
		return priv;
	}

	public ChatServer(int port) {
		try {
					// Binds to port and starts server
			System.out.println("Binding to port " + port);
			server_socket = new ServerSocket(port);
			System.out.println("Server started: " + server_socket);
			start();

		} catch(IOException ioexception) {
			// Error binding to port
			System.out.println("Binding error (port=" + port + "): " + ioexception.getMessage());
		}
	}
    
	public void run() {

		while (thread != null) {
			try {
				// Adds new thread for new client
				System.out.println("Waiting for a client ...");
				addThread(server_socket.accept());
			} catch(IOException ioexception) {
				System.out.println("Accept error: " + ioexception); stop();
			}
		}
	}
    
   	public void start() {

		if (thread == null) {
			// Starts new thread for client
			thread = new Thread(this);
			thread.start();
		}
	}
    
	public void stop() {

		if (thread != null) {
			// Stops running thread for client
			thread.stop();
			thread = null;
		}
	}
   
	private int findClient(int ID) {
		// Returns client from id
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;
	}
    
	public synchronized void handle(int ID, String input) {
		if (input.equals(".quit")) {
			int leaving_id = findClient(ID);
			// Client exits
			clients[leaving_id].send(".quit");
			// Notify remaing users
			for (int i = 0; i < clientCount; i++)
					if (i!=leaving_id)
						clients[i].send("Client " +ID + " exits..");
			remove(ID);
		}
		else
			// Brodcast message for every other client online
			for (int i = 0; i < clientCount; i++)
				clients[i].send(ID + ": " + input);
	}
    
	public synchronized void remove(int ID) {

		int pos = findClient(ID);

		if (pos >= 0) {
			// Removes thread for exiting client
			ChatServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount-1)
				for (int i = pos+1; i < clientCount; i++)
					clients[i-1] = clients[i];
			clientCount--;

			try {
				toTerminate.close();

			} catch(IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}

			toTerminate.stop();
		}
	}

	/*Cria uma thread cada vez que um cliente se liga*/
	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			// Adds thread for new accepted client
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ChatServerThread(this, socket);

			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch(IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		}
		else
			System.out.println("Client refused: maximum " + clients.length + " reached.");
	}
    
    
	public static void main(String args[]) {
		ChatServer server = null;

		if (args.length != 1) {
			// Displays correct usage for server
			System.out.println("Usage: java ChatServer port");
		}
		else {


			//Generate keys
			try {
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

				SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
				keyGen.initialize(1024, random);


				pair = keyGen.generateKeyPair();
				priv = pair.getPrivate();
				pub = pair.getPublic();

				System.out.println("SERVER PUB KEY: " + pub.toString());


				/* save the public key in a file */
				byte[] key = pub.getEncoded();
				FileOutputStream keyfos = new FileOutputStream("serverPub.key");
				keyfos.write(key);
				keyfos.close();

				// Calls new server
				server = new ChatServer(Integer.parseInt(args[0]));


			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

@SuppressWarnings("deprecation")
class ChatServerThread extends Thread {

    private ChatServer       server    = null;
    private Socket           socket    = null;
    private int              ID        = -1;
    private DataInputStream  streamIn  =  null;
    private DataOutputStream streamOut = null;

	private SecretKey clientSecretKey;

   
    public ChatServerThread(ChatServer _server, Socket _socket) {

        super();
        server = _server;
        socket = _socket;
        ID     = socket.getPort();
    }
    
    // Sends message to client
    public void send(String msg) {
        try {

            streamOut.writeUTF(msg);
            streamOut.flush();

        } catch(IOException ioexception) {
            System.out.println(ID + " ERROR sending message: " + ioexception.getMessage());
            server.remove(ID);
            stop();
        }
    }
    
    // Gets id for client
    public int getID()
    {  
        return ID;
    }
   
    // Runs thread
    public void run() {
        System.out.println("Server Thread " + ID + " running.");

		//Read client secret key
		byte [] encryptedSecretKey = new byte[128];
		try {

			streamIn.read(encryptedSecretKey);

		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("---> " + encryptedSecretKey.toString());


		//Decrypt received Key
		Cipher cipher = null;
		byte [] decryptedSecretKey = null;


		try {

			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, server.getPriv());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		try {
			decryptedSecretKey = cipher.doFinal(encryptedSecretKey);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		System.out.println("Secret Key: " + DatatypeConverter.printBase64Binary(decryptedSecretKey));

		clientSecretKey = new SecretKeySpec(decryptedSecretKey, 0, decryptedSecretKey.length, "AES");




		while (true) {
            try {
                server.handle(ID, streamIn.readUTF());
            } catch(IOException ioe) {
                System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                server.remove(ID);
                stop();
            }
        }
    }
    
    
    // Opens thread
    public void open() throws IOException {

        streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }
    
    // Closes thread
    public void close() throws IOException {

        if (socket != null)    socket.close();
        if (streamIn != null)  streamIn.close();
        if (streamOut != null) streamOut.close();
    }
    
}

