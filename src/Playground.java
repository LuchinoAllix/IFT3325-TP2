import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Scanner;

// 0000000000000000000000000000000
// 0000000000000000000000000000000

public class Playground {
	
	public static void main(String... args) {
		OutputStream fakeOutput = new FakeReceiver();
		Trame t = null;
		Scanner scanner = new Scanner(System.in);

		Trame p = Trame.p();

		System.out.println(p.encode(CRC.CRC_CCITT));


		while (t == null || t.getType() != Trame.Type.F) {
			t = null;
			do {
				try {
					System.out.println();
					t = read_trame(scanner);
					break;
				} catch (Exception e) {
					System.out.println("Trame Invalide");
				}
			} while(true);
			if (t != null)
			{
				try {
					TrameSender.sendTrame(fakeOutput, t);
				} catch (IOException e) {
					System.out.println("ERREUR");
				}
			}
		}
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

	static class ConsoleInputStream extends InputStream {
		Word curr_trame;
		int at = 0;
		Scanner scanner = new Scanner(System.in);
		int n;
		@Override
		public int read() throws IOException {
			if (curr_trame != null && at < curr_trame.length) {
				int b = curr_trame.getBitAt(at)? 1 : 0;
				at += 1;
				return b;
			} else {
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
				Word new_trame = Word.concat(new Word("01111110"), t.encode(CRC.CRC_CCITT), new Word("0111111011111111"));
				curr_trame = new_trame;
				at = 0;
				return read();
			}
		}
		
	}
	static class FakeReceiver extends OutputStream {
		ArrayList<Boolean> curr_word = null;
		int nb_of_ones = 0;

		//Queue<Trame> trames = new ArrayDeque<>();

		@Override
		public void write(int b) throws IOException {
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
							System.out.println("\n<< " + t);
						} catch (Trame.TrameException e) {
							e.printStackTrace();
							System.out.println("\n<< ERREUR");
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

}
