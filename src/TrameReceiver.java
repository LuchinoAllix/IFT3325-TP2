import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Logique pour recevoir des trames
 */
public class TrameReceiver {
	/**
	 * Lit la prochaine trame du stream entrant
	 * @return la trame, ou empty si la connexion est fermé
	 */
	public static Optional<Trame> receiveTrame(InputStream stream) throws Trame.TrameException, IOException {
		// assume qu'on est connecté
		int nb_of_ones = 0;
		ArrayList<Boolean> curr_word = null;

		// On cherche le flag de début en cherchant 6 (et uniquement 6) 1 d'affilé
		while (curr_word == null) {
			Optional<Boolean> r = readNextBit(stream);
			if (r.isEmpty()) return Optional.empty(); // stream terminé
			boolean b = r.get();
			if (b) {
				nb_of_ones += 1;
			} else {
				if (nb_of_ones == 6) curr_word = new ArrayList<>(); // début de la trame
				nb_of_ones = 0;
			}
		}

		// on collectione les bit de la trame
		while (true) {
			// même chose qu'avant, s'assure que le stream existe encore
			Optional<Boolean> r = readNextBit(stream);
			if (r.isEmpty()) return Optional.empty(); // stream terminé
			boolean b = r.get();

			if (b) {
				nb_of_ones += 1;
				if (nb_of_ones >= 7) throw new Trame.TrameException("7 1 d'affilé reçu"); // erreur
				curr_word.add(b); // ajoute le bit à la liste
			} else {
				if (nb_of_ones == 6) { // fin de la trame
					boolean[] bits = new boolean[curr_word.size()-7];
					for (int i=0; i<bits.length; i+=1)
						bits[i] = curr_word.get(i);
					Word w = new Word(bits);
					Trame t = Trame.decode(w, CRC.CRC_CCITT);
					return Optional.of(t);
				} else if (nb_of_ones == 5) {
					// bit de stuffing alors on ne fait rien
				} else {
					curr_word.add(b);
					nb_of_ones = 0;
				}
				nb_of_ones = 0;
			}
		}
	}
	/**
	 * Lit le prochain bit à l'entrée
	 * @return
	 */
	private static Optional<Boolean> readNextBit(InputStream stream) {
		try {
			int b = stream.read();
			if (b < 0) return Optional.empty();
			return Optional.of(b != 0);
		} catch (IOException e) {return Optional.empty();}
	}

	public static Trame receiveTrame(Word bits) throws Trame.TrameException {
		Optional<Integer> ff = bits.find(Trame.FLAG);
		if (ff.isEmpty()) throw new Trame.TrameException("Pas de trame dans la chaîne");
		int s = ff.get() + Trame.FLAG.length;
		Optional<Integer> lf = bits.find(Trame.FLAG, s);
		if (lf.isEmpty()) throw new Trame.TrameException("Pas de trame dans la chaîne");
		int e = lf.get();
		return Trame.decode(bits.subWord(s, e), CRC.CRC_CCITT);
	}
}
