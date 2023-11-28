import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

public class TrameReceiver {
	/**
	 * Lit la prochaine trame du stream entrant
	 * @return la trame, ou empty si la connexion est fermé
	 */
	public static Optional<Trame> receiveTrame(InputStream stream) throws Trame.TrameException{
		// assume qu'on est connecté
		
		int nb_of_ones = 0;
		// cherche le flag de début de trame
		while (true) { // on va sortir manuellement
			Optional<Boolean> bit = readNextBit(stream);
			if (bit.isEmpty()) { // fin du stream, on renvoi rien
				return Optional.empty();
			} else {
				boolean b = bit.get();
				if (b) nb_of_ones += 1;
				else {
					if (nb_of_ones == 6) { // flag, on commence à lire
						nb_of_ones = 0;
						break;
					} else {
						nb_of_ones = 0;
					}
					if (nb_of_ones >= 7) throw new Trame.TrameException();
				}
			}
		}

		// lit la trame jusqu'à ce qu'on trouve le flag de fin
		ArrayList<Byte> bytes = new ArrayList<>();
		int curr_byte = 0;
		int curr_bit_index = 0;
		int len = 0;
		Optional<Boolean> curr_bit;
		while (true) {
			curr_bit = readNextBit(stream);
			if (curr_bit.isEmpty()) { return Optional.empty(); }
			boolean b = curr_bit.get();
			if (nb_of_ones == 5 && !b) { // 0 de bit stuffing, on ignore
				nb_of_ones = 0;
				continue;
			} else if (nb_of_ones == 5) { // 6e 1, donc flag de fin ou erreur
				curr_bit = readNextBit(stream);
				if (curr_bit.isPresent() && !curr_bit.get()) { // flag de fin
					// on avait lu 6 bits de ce flag précédement, alors on veut les enlever du mot
					if (curr_bit_index == 6) { // on lisait le 7e bit, donc on n'a qu'à ignorer ce byte
						break;
					} else if (curr_bit_index > 6) { // il y a des bits que l'on veut conserver
						int bits_a_conserver = curr_bit_index - 6;
						int mask = (-256) >> bits_a_conserver;
						curr_byte = curr_byte&mask&255;
						bytes.add((byte)curr_byte);
						len += bits_a_conserver;
						break;
					} else { // des bits déjà dans le tableau faisaient partie du flag et il faut les enlever
						int bits_a_enlever = 6-curr_bit_index;
						// plus simple, car on a qu'a changer la longueur, les bits à la fin vont simplement être ignoré
						len -= bits_a_enlever;
					}
				} else { // 7e 1, donc erreur
					throw new Trame.TrameException();
				}
			} else { // on ajoute le bit
				if (b) curr_byte |= 128 >>> curr_bit_index; // flip le bit 
				curr_bit_index += 1;
				if (curr_bit_index == 8) { // byte compléter, on l'ajoute au tableau
					bytes.add((byte)curr_byte);
					curr_byte = 0;
					curr_bit_index = 0;
					len += 8;
				}
			}
		}
		byte[] arr = new byte[bytes.size()];
		for (int i=0; i<arr.length; i+=1) // parce que java...
			arr[i] = bytes.get(i);
		return Optional.of(Trame.decode(new Word(arr, len), CRC.CRC_CCITT));
	}
	/**
	 * Lit le prochain bit à l'entrée
	 * @return
	 * @throws IOException
	 */
	private static Optional<Boolean> readNextBit(InputStream stream) {
		try {
			int b = stream.read();
			if (b < 0) return Optional.empty();
			return Optional.of(b != 0);
		} catch (IOException e) {return Optional.empty();}
	}
}
