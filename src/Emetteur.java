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

	public String sendMessage(String msg,int i) throws IOException {
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

	public static void main(String[] args) {
		
		Emetteur client = new Emetteur();
		String rep ="";
		int i = 0;
		windowInit();
		TrameO trame = TrameO.makeComTram('C',(byte) i);
		try {
			client.startConnection("127.0.0.1", 6661);
			while(!window[i] && windowWeight()<7){  
				rep = client.sendMessage(trame.toString(),(byte) i);
				//todo pour montrer les trames, faire un truc avec rep
				try {
					trame = TrameO.stringToTrame(rep);
					
				} catch (IOError e) {
					// redemander transmission
					trame = new TrameO();
					rep = trame.toString();
				}
				
				if(trame.num.toString().equals("0000000"+i)){
					window[i]=false;
					i= (i+1) % 7;
				}else{
					
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
