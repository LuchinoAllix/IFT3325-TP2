import java.io.IOException;
import java.io.OutputStream;

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
}
