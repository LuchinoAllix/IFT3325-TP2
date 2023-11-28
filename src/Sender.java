import java.net.*;
import java.io.*;

public class Sender{

	private static Socket clientSocket;
	private static InputStream in_stream;
	private static OutputStream out_stream;
	private static IO io;
	private static PrintStream writer;

	public static void startConnection(String ip, int port, IO.Mode mode) throws UnknownHostException, IOException {
		clientSocket = new Socket(ip, port);
		in_stream = clientSocket.getInputStream();
		out_stream = clientSocket.getOutputStream();
		io = new IO(in_stream,out_stream);
		writer = new PrintStream(io.getOutputStream());
		io.ouvreConnexion(mode);
	}


	public static void stopConnection() throws IOException {
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
			IO.Mode mode = IO.Mode.fromNum(méthode);
			File file = new File(args[2]);
			FileInputStream input = new FileInputStream(file);
			
			startConnection(machine,port,mode);
			writer.print(input);
			
			// Quelle condition pour arrêter le sender ?
			stopConnection();
		} catch (NumberFormatException e) {
			System.out.println("Le numéro de port et la méthode doivent être des entiers.");
			System.exit(0);
		} catch (FileNotFoundException e) {
			System.out.println("Fichier mentionné non trouvé.");
			System.exit(0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}	
	}
}
