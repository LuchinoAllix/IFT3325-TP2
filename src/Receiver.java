import java.net.*;
import java.io.*;

public class Receiver{

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static IO io;

	public static void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		System.out.print("En attente de connexion... "); System.out.flush();
		clientSocket = serverSocket.accept();
		System.out.println("Établie!");
		io = new IO(clientSocket.getInputStream(),clientSocket.getOutputStream());
		Logger log = new Logger();
		io.setLogger(log);
	}

	public static void stop() throws IOException {
		io.getInputStream().close();
		io.getOutputStream().close();
		clientSocket.close();
		serverSocket.close();
	}

	public static void main(String[] args) {
		int port=0;
		if(args.length !=1){System.out.println("Il faut 1 argument. (<port>)");System.exit(0);}
		try {
			port = Integer.parseInt(args[0]);
			start(port);
			byte[] poubelle = new byte[512]; // pour vider le buffer
			while (!io.estFerme()) {
				try {Thread.sleep(100);} catch (InterruptedException e) {}
				if (io.estConnecte()) io.getInputStream().read(poubelle);
			}
			stop();
		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port doit être un entier.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
