import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class TrameSender {
	public static final Word EMPTY = new Word("11111111");

	/**
	 * Envoie la trame sur le stream sortant
	 * @param trame trame à envoyer
	 * @throws IOException
	 */
	public static void sendTrame(OutputStream stream, Trame trame) throws IOException {
		Word t = trame.encode(CRC.CRC_CCITT);
		sendWithoutStuffing(stream, Trame.FLAG);
		sendWithStuffing(stream, t);
		sendWithoutStuffing(stream, Trame.FLAG);
		sendWithoutStuffing(stream, EMPTY);
	}
	/**
	 * Envoie des bits avec du bit stuffing
	 * @param bits
	 * @throws IOException
	 */
	public static void sendWithStuffing(OutputStream stream, Word wrd) throws IOException {
		int nb_of_ones = 0;
		Word.BitIterator iter = wrd.iterator();
		while (iter.hasNext()) {
			boolean b = iter.next();
			stream.write(b? 1 : 0);
			if (b) nb_of_ones += 1;
			else nb_of_ones = 0;
			if (nb_of_ones == 5) {
				stream.write(0);
				nb_of_ones = 0;
			}
		}
	}
	/**
	 * envoie des bits sans bit stuffing
	 * @param bits
	 * @throws IOException
	 */
	public static void sendWithoutStuffing(OutputStream stream, Word wrd) throws IOException {
		int at = 0;
		while (at < wrd.length) {
			boolean b = wrd.getBitAt(at);
			stream.write(b? 1 : 0);
			at += 1;
		}
	}

	public static Word encodeTrame(Trame trame) {
		Word t = encodeWithStuffing(trame.encode(CRC.CRC_CCITT));
		return Word.concat(Trame.FLAG, t, Trame.FLAG, EMPTY);
	}
	public static Word encodeWithStuffing(Word wrd) {
		ArrayList<Boolean> arr = new ArrayList<>();
		int nb_of_ones = 0;
		Word.BitIterator iter = wrd.iterator();
		while (iter.hasNext()) {
			boolean b = iter.next();
			arr.add(b);
			if (b) 
				nb_of_ones += 1;
			else 
				nb_of_ones = 0;
			//System.out.print(nb_of_ones);
			if (nb_of_ones == 5) {
				arr.add(false);
				nb_of_ones = 0;
				//System.out.print('X');
			}
		}
		//System.out.println();
		boolean[] arrb = new boolean[arr.size()];
		for (int i=0; i< arrb.length; i+=1) arrb[i] = arr.get(i);
		return new Word(arrb);
	}
}
