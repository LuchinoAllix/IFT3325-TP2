import java.net.*;
import java.io.*;

public class Receiver{
	/**
	 * Classe pour simuler un serveur.
	 * Permet d'établir une connexion pourvu qu'on lui donne un port dipsonible sur la machine.
	 * Fait appel à IO pour gérer les trames et utiliser le protocol de go back and.
	 */

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static IO io;

	/**
	 * Permet d'établir une connexion pourvu qu'on lui donne un port disponible sur la machine.
	 * Crée un IO pour uiliser le protocol go back and.
	 * @param port
	 * @throws IOException
	 */
	public static void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		System.out.print("En attente de connexion... "); System.out.flush();
		clientSocket = serverSocket.accept();
		System.out.println("Établie!");
		io = new IO(clientSocket.getInputStream(),clientSocket.getOutputStream());
		Logger log = new Logger();
		io.setLogger(log);
	}
	/**
	 * Permet de fermer les streams de communication et la socket utilisée
	 * @throws IOException
	 */
	public static void stop() throws IOException {
		io.getInputStream().close();
		io.getOutputStream().close();
		clientSocket.close();
		serverSocket.close();
	}

	/**
	 * Permet de lancer le receiver, il faut fournir les arguments demandé pour le bon fonctionnement, à savoir un port pour établir une connection.
	 * @param args
	 */
	public static void main(String[] args) {
		OutputStream out = new VoidStream();
		byte[] poubelle = new byte[512]; // pour vider le buffer
		int port=0;
		if(args.length !=1){System.out.println("Il faut 1 argument. (<port>)");System.exit(0);}
		try {
			port = Integer.parseInt(args[0]);
			start(port);
			while (!io.estFerme()) {
				try {Thread.sleep(100);} catch (InterruptedException e) {}
				if (io.estConnecte()) out.write(io.getInputStream().read(poubelle));
			}
		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port doit être un entier.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				stop();
				out.close();
			} catch (IOException e) {}
		}
	}
}
