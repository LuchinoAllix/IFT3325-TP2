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

	public static boolean[] window = new boolean[7];

	public static void windowInit(){
		for (boolean b : window) {
			b=false;
		}
	}
	public static int windowWeight(){
		int res = 0;
		for (boolean b : window) {
			if(b) res++;
		}
		return res;
	}

	public void startConnection(String ip, int port) throws UnknownHostException, IOException {
		clientSocket = new Socket(ip, port);
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	public String sendMessage(String msg,byte i) throws IOException {
		out.println(msg);
		String resp = in.readLine();
		window[i]=true;
		return resp;
	}

	public void stopConnection() throws IOException {
		in.close();
		out.close();
		clientSocket.close();
	}

	public static String getNextData(){
		String data = "";
		//todo lire fichier ou whatever
		return data;
	}

	public static Trame makeTrameConnexion(byte num){
		Trame trame = new Trame('C',num,"");
		trame.nullifyData();
		trame.makeCRC();
		return trame;
	}

	public static Trame makeNextTrame(char type,byte num){
		String data = getNextData();
		Trame trame = new Trame(type,num,data);
		trame.makeCRC();
		return trame;
	}

	public static void main(String[] args) {
		Emetteur client = new Emetteur();
		String rep ="";
		byte i = 0;
		windowInit();
		Trame trame = makeTrameConnexion(i);
		try {
			client.startConnection("127.0.0.1", 6669);
			while(!window[i] && windowWeight()<7){
			rep = client.sendMessage(trame.toString(),i);
			//todo pour montrer les trames, faire un truc avec rep
			try {
				trame = Trame.stringToTrame(rep);
			} catch (IOError e) {
				// redemander transmission
			}
		
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
