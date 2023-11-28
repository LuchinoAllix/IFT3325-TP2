import java.net.*;
import java.io.*;

// Source socket : https://www.baeldung.com/a-guide-to-java-sockets

public class Receiver{
	/* Ce que cette classe doit faire :
	 * • Recevoir des trames
	 * • Vérifier la présence d’erreurs ou non dans les trames reçues
	 * • Envoyer des accusés de réception (RR).
	 * • Envoyer des REJ en cas d’erreur.
	 */

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private IO io;

	public void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		clientSocket = serverSocket.accept();
		io = new IO(clientSocket.getInputStream(),clientSocket.getOutputStream());
	}

	public void stop() throws IOException {
		io.getInputStream().close();
		io.getOutputStream().close();
		clientSocket.close();
		serverSocket.close();
	}

	public static void main(String[] args) {
		int port=0;
		if(args.length !=1){System.out.println("Il faut 1 argument.");System.exit(0);}
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port doit être un entier.");
			System.exit(0);
		}
		
		Receiver server = new Receiver();
		try {
			server.start(port);
			// Quelle condition pour stop le server ?
			server.stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
