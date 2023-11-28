import java.net.*;
import java.io.*;
import java.util.Scanner;


public class Sender{
	
	/* Ce que cette classe doit faire :
	 * • Lire des données de fichier,
	 * • Produire des trames et les envoyer,
	 * • Attendre et traiter les accusés de réception,
	 * • Ré-envoyer les données en cas de perte ou d’erreur.
	*/

	private Socket clientSocket;
	private IO io;

	public void startConnection(String ip, int port) throws UnknownHostException, IOException {
		clientSocket = new Socket(ip, port);
		io = new IO(clientSocket.getInputStream(),clientSocket.getOutputStream());
	}


	public void stopConnection() throws IOException {
		io.getInputStream().close();
		io.getOutputStream().close();
		clientSocket.close();
	}

	public static void main(String[] args) {
		int port = 0;
		int méthode = 0;
		String machine = "";
		if(args.length !=4){System.out.println("Il faut 4 arguments.");System.exit(0);}
		try {
			port = Integer.parseInt(args[1]);
			méthode = Integer.parseInt(args[3]);
			File file = new File(args[2]);
			FileInputStream input = new FileInputStream(file);
		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port et la méthode doivent être des entiers.");
			System.exit(0);
		} catch (FileNotFoundException e) {
			System.out.println("Fichier mentionné non trouvé.");
			System.exit(0);
		}

		Sender client = new Sender();
		try {
			client.startConnection(machine, port);
			// Comment envoyer des data ?
			
			// Quelle condition pour arrêter le client ?
			client.stopConnection();

		} catch (IOException e) {
			e.printStackTrace();
		} 
		

		
	}

}
