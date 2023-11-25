import java.net.*;
import java.io.*;

public class Emetteur{
	
	/* Ce que cette classe doit faire :
	 * • Lire des données de fichier,
	 * • Produire des trames et les envoyer,
	 * • Attendre et traiter les accusés de réception,
	 * • Ré-envoyer les données en cas de perte ou d’erreur.
	*/

	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;


	public void startConnection(String ip, int port) throws UnknownHostException, IOException {
		clientSocket = new Socket(ip, port);
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	public String sendMessage(String msg) throws IOException {
		out.println(msg);
		String resp = in.readLine();
		return resp;
	}

	public void stopConnection() throws IOException {
		in.close();
		out.close();
		clientSocket.close();
	}

	public static void main(String[] args) {
		Emetteur client = new Emetteur();
		try {
			client.startConnection("127.0.0.1", 6669);
			String response = client.sendMessage("hello server");
			System.out.println(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
