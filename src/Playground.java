import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

// 0000000000000000000000000000000
// 0000000000000000000000000000000

public class Playground {
	
	public static void main(String... args) {

		
		test_io();

		//new InOutConnector(fakeInput, fakeOutput);

	}

	static void exp_thread() {
		Object lock = new Object();
		Thread a = new Thread(() -> {
			for (int i=0; i < 10; i += 1) {
				synchronized (lock) {
					System.out.println("A LOCK...");
					try {
						Thread.sleep(1000);
						System.out.println("A FREE?");
						lock.notifyAll();
						lock.wait();
					} catch (InterruptedException e) {

					}
					
				}
			}
		});
		Thread b = new Thread(() -> {
			for (int i=0; i < 10; i += 1) {
				synchronized (lock) {
					System.out.println("B LOCK...");
					try {
						Thread.sleep(1000);
						System.out.println("B FREE?");
						lock.notifyAll();
						lock.wait();
					} catch (InterruptedException e) {

					}
					
				}
			}
		});
		//a.setDaemon(true);
		//a.setDaemon(true);
		a.start();
		b.start();
	}

	static void test_io() {
		OutputStream fakeOutput = new FakeReceiver();
		InputStream fakeInput = new FakeSender();
		Logger log = new Logger(true);
		//Trame t = null;
		//Scanner scanner = new Scanner(System.in);
		
		IO io = new IO(fakeInput, fakeOutput);
		io.setLogger(log);

		while (!io.estConnecte() && !io.estFerme()) {
			try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
		if (io.estConnecte()) System.out.println("CONNECTÉ");
		else System.out.println("FERMÉ");
		System.out.println("CANSEND: " + io.canSend());
		System.out.println("CANRECEIVE: " + io.canReceive());
		System.out.println("MODE: " + io.getMode());

		PrintWriter writer = new PrintWriter(io.getOutputStream());
		String str = "Le monde de par chez nous...";
		System.out.println("strlen: " + str.getBytes().length);
		writer.println(str);
		writer.flush();

		while (!io.estFerme()) {
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		}

		io.fermeConnexion();
		System.out.println("FIN");
	}

	static void test_crc(String wrd, String goal) {
		Word word = new Word(wrd);
		Word wgoal = new Word(goal);
		Word crc = CRC.CRC_CCITT.crc(word);
		System.out.println("CRC pour " + word);
		System.out.println("" + crc + " (" + wgoal + ") " + crc.equals(wgoal));
		System.out.println(CRC.CRC_CCITT.isValid(Word.concat(word, crc)));
	}

	static void test_print_trame() {
		System.out.println(Trame.gbn());
		System.out.println(Trame.selectiveRej());
		System.out.println(Trame.p());
		System.out.println(Trame.i(0, new Word("0110111010111001001")));
		System.out.println(Trame.rr(0));
		System.out.println(Trame.rnr(0));
		System.out.println(Trame.rej(0));
		System.out.println(Trame.srej(0));
		System.out.println(Trame.end());
	}

	static class FakeSender extends InputStream {
		Word curr_trame;
		int at = 0;
		Scanner scanner = new Scanner(System.in);
		int n;
		@Override
		public int read() throws IOException {
			if (curr_trame != null && at < curr_trame.length) {
				int b = curr_trame.getBitAt(at)? 1 : 0;
				at += 1;
				//System.out.print(b);
				return b;
			} else {
				//System.out.println();
				Trame t;
				do {
					try {
						t = read_trame(scanner);
						break;
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Trame Invalide");
					}
				} while(true);
				Word new_trame = TrameSender.encodeTrame(t);
				//System.out.println("**" + t + "**");
				curr_trame = new_trame;
				at = 1;
				int b = curr_trame.getBitAt(0)? 1 : 0;
				//System.out.print(b);
				return b;
			}
		}
	}
	static class FakeReceiver extends OutputStream {
		ArrayList<Boolean> curr_word = null;
		int nb_of_ones = 0;
		OutputStream passto = null;
		//Queue<Trame> trames = new ArrayDeque<>();

		public FakeReceiver() {}
		public FakeReceiver(OutputStream src) { this.passto = src; }

		@Override
		public void write(int b) throws IOException {
			if (passto != null) passto.write(b);
			//System.out.print(b!=0? 1 : 0);
			if (nb_of_ones >= 6 && b != 0 && curr_word != null) { // si on a 7 1 de suite, on abandone tout et on recomence
				curr_word = null;
				nb_of_ones += 1;
			}
			else if (curr_word == null) { // on cherche la prochaine trame
				if (b != 0) {
					nb_of_ones += 1;
				} else {
					if (nb_of_ones == 6) { // flag de début de trame
						curr_word = new ArrayList<>();
					}
					nb_of_ones = 0;
				}
			} else {
				if (b != 0) {
					nb_of_ones += 1;
				}
				curr_word.add(b!=0);
				if (b == 0) {
					if (nb_of_ones == 5); // ignore le 0 de bit stuffing
					else if (nb_of_ones == 6) {
						// fin de trame, fabrique l'objet
						if (curr_word.size() < 7) {
							//System.out.println();
							throw new RuntimeException("What the fuck");
						}
						boolean[] bits = new boolean[curr_word.size()-8];
						for (int i=0; i< bits.length; i+=1)
							bits[i] = curr_word.get(i);
						
						Word wrd = new Word(bits);
						//System.out.println("\n"+wrd);
						try {
							Trame t = Trame.decode(wrd, CRC.CRC_CCITT);
							if (t instanceof Trame.I i) {
								String msg = t.getMsg().map(tt -> new String(tt.toByteArray(), StandardCharsets.UTF_8)).orElse("");
								System.out.println("<<< " + t + " (" + msg + ")");
							} else {
								System.out.println("<<< " + t);
							}
						} catch (Trame.TrameException e) {
							e.printStackTrace();
							System.out.println("<<< ERREUR");
						}
						curr_word = null;
					}
					nb_of_ones = 0;
				}
			}
			
		}
		
	}

	static Trame read_trame(Scanner scanner) {
		System.out.print(">>> "); System.out.flush();
		String ln = scanner.nextLine().trim();
		String[] parts = ln.split(" ");
		if (ln.startsWith("#")) {
			Trame t = switch (parts[0]) {
				case "#START" -> Trame.gbn();
				case "#P" -> Trame.p();
				case "#END" -> Trame.end();
				case "#RNR" -> Trame.rnr(Integer.parseInt(parts[1]));
				case "#RR" -> Trame.rr(Integer.parseInt(parts[1]));
				case "#REJ" -> Trame.rej(Integer.parseInt(parts[1]));
				case "#SREJ" -> Trame.srej(Integer.parseInt(parts[1]));
				default -> throw new RuntimeException("Trame inconnu");
			};
			return t;
		}
		int n = Integer.parseInt(parts[0]);
		String smsg = ln.substring(parts[0].length()).trim();
		Trame t = Trame.i(n, new Word(smsg.getBytes()));
		return t;
	}

	static class InOutConnector {
		InputStream src;
		OutputStream dest;
		Thread thread;
		public InOutConnector(InputStream in, OutputStream out) {
			this.src = in;
			this.dest = out;
			thread = new Thread(() -> {
				try {
					do {
						int b = in.read();
						if (b < 0) return;
						out.write(b);
					} while (true);
				} catch (IOException e) {e.printStackTrace();}
			});
			//thread.isDaemon();
			thread.start();
		}
	}

}
