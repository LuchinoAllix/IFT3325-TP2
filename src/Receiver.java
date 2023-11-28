import java.net.*;
import java.io.*;

public class Receiver{

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static IO io;

	public static void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		clientSocket = serverSocket.accept();
		io = new IO(clientSocket.getInputStream(),clientSocket.getOutputStream());
	}

	public static void stop() throws IOException {
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
			start(port);
			// Quelle condition pour stop le receiver ?
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
