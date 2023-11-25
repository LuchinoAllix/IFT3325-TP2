import java.net.*;
import java.io.*;

// Source socket : https://www.baeldung.com/a-guide-to-java-sockets

public class Recepteur{
	/* Ce que cette classe doit faire :
	 * • Recevoir des trames
	 * • Vérifier la présence d’erreurs ou non dans les trames reçues
	 * • Envoyer des accusés de réception (RR).
	 * • Envoyer des REJ en cas d’erreur.
	 */

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	public void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		clientSocket = serverSocket.accept();
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String greeting = in.readLine();
            if ("hello server".equals(greeting)) {
                out.println("hello client");
            }
            else {
                out.println("unrecognised greeting");
            }
	}

	public void stop() throws IOException {
		in.close();
		out.close();
 
		clientSocket.close();
		serverSocket.close();
	}

	public static void main(String[] args) {
		Recepteur server = new Recepteur();
		try {
			server.start(6669);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
