import java.net.*;
import java.io.*;

public class Sender{
	/**
	 * Classe pour simuler un client qui veut communiquer avec un server (receiver).
	 * 
	 * Fait appel à IO pour gérer les trames et utiliser le protocol de go back and.
	 */

	private static Socket clientSocket;
	private static InputStream in_stream;
	private static OutputStream out_stream;
	private static IO io;
	private static PrintStream writer;

	/**
	 * Permet d'établir une connection avec le serveur (reveiver) à travers le port fourni. 
	 * ip est l'adresse de la machine à savoir  "127.0.0.1" si on veut éxécuter localement.
	 * Le mode permet de choisir si on veut go back and ou selective reject.
	 * @param ip
	 * @param port
	 * @param mode
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static void startConnection(String ip, int port, IO.Mode mode) throws UnknownHostException, IOException {
		System.out.print("Connexion");
		int i = 0;
		while (clientSocket == null) {
			if (i < 3) {
				System.out.print("."); System.out.flush();
				i += 1;
			}
			try {
				clientSocket = new Socket(ip, port);
			} catch (IOException e){
				clientSocket = null;
				try { Thread.sleep(800); } catch (InterruptedException x) {}
			}
		}
		System.out.println(" Établie!");
		
		in_stream = clientSocket.getInputStream();
		out_stream = clientSocket.getOutputStream();
		io = new IO(in_stream,out_stream);

		Logger log = new Logger();
		io.setLogger(log);
		writer = new PrintStream(io.getOutputStream());
		// boucle jusqu'à ce qu'on soit connecté, ou qu'on soit assuré que la connexion est impossible
		while (!io.ouvreConnexion(mode)) try { Thread.sleep(100); } catch (InterruptedException e) {}
	}


	/**
	 * Ferme la connection et tous les streams utilisés.
	 * @throws IOException
	 */
	public static void stopConnection() throws IOException {
		io.fermeConnexion();
		io.getInputStream().close();
		io.getOutputStream().close();
		clientSocket.close();
	}

	public static void main(String[] args) {
		int port = 0;
		int méthode = 0;
		String machine = "";
		if(args.length !=4){System.out.println("Il faut 4 arguments. (<machine> <port> <fichier> <mode>)");System.exit(0);}

		try {
			port = Integer.parseInt(args[1]);
			méthode = Integer.parseInt(args[3]);
			IO.Mode mode = IO.Mode.fromNum(méthode);
			File file = new File(args[2]);
			BufferedReader input = new BufferedReader(new FileReader(file));
			
			startConnection(machine,port,mode);
			String ln;
			while ((ln = input.readLine()) != null && !io.estFerme()) {
				//System.out.println(ln);
				writer.println(ln);
			}
			//writer.print(input);
			//writer.flush();
			//int i = 0;
			while ((io.data() > 0 || !io.allReceived()) && !io.estFerme()) {
				Thread.sleep(100);
				//System.out.println(i);
			}
			//System.out.println("ici");
			input.close();
			stopConnection();

		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port et la méthode doivent être des entiers.");
			System.exit(0);
		} catch (FileNotFoundException e) {
			System.out.println("Fichier mentionné non trouvé.");
			System.exit(0);
		} catch (UnknownHostException e) {
			System.out.println("L'adresse IP n'est pas valide.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
