import java.util.Optional;

/**
 * Représentation d'une trame. Ne contient pas le flag parce que c'est toujours le même pis que l'emmetteur peut décider d'utiliser celui qu'il veut.
 * 
 * Chaque type de trame est un sous-type afin de pouvoir utiliser du pattern matching et de tirer avantage du polymorphisme et de la surcharge de méthode
 */
public abstract sealed class Trame permits Trame.I, Trame.C, Trame.A, Trame.R, Trame.F, Trame.P {

	/**
	 * Retourne le type de cette trame
	 * @return le type de la trame
	 */
	public abstract Trame.Type getType();
	/**
	 * Retourne le numéro de cette trame (ou rien si la trame n'a pas de numéro pour une quelqu'onque raison)
	 * @return le numéro de la trame
	 */
	public Optional<Integer> getNum() {return Optional.empty();}
	/**
	 * Retourne les données, si présente, transportées par cette trames
	 * @return les données de la trame
	 */
	public Optional<Word> getMsg() {return Optional.empty();}
	/**
	 * calcule le byte à utliser lors de l'encodage de cette trame. je ne sais pas pourquoi elle est publique...
	 * @return encodage du numéro de la trame
	 */
	public byte getNumByte() { return 0; }

	/**
	 * Trame d'information. C'est la seule trame qui contient un message
	 */
	public static final class I extends Trame {
		/**
		 * Numéro de la trame
		 */
		private int num;
		/**
		 * données de la trame
		 */
		private Word msg;
		@Override
		public Trame.Type getType() { return Trame.Type.I; }
		private I(int num, Word msg) {
			this.num = num;
			this.msg = msg;
		}
		@Override
		public Optional<Integer> getNum() { return Optional.of(this.num); }
		@Override
		public Optional<Word> getMsg() { return Optional.of(msg); }
		@Override
		public byte getNumByte() {
			return (byte)Character.forDigit(this.getNum().orElse(0), 10);
		}
	}
	/**
	 * Trame de demande de connexion.
	 */
	public static final class C extends Trame {
		private int num;
		private C(int num) {
			this.num = num;
		}
		@Override
		public Trame.Type getType() { return Trame.Type.C; }
		@Override
		public Optional<Integer> getNum() { return Optional.of(this.num); }
		/**
		 * Indique si la demande de connexion est avec le protocole Go-Back-N
		 * @return true si on a demandé Go-Back-N, false si c'est l'autre
		 */
		public boolean goBackN() { return num == 0; }
		@Override
		public byte getNumByte() {
			return (byte)Character.forDigit(this.num, 10);
		}
	}
	/**
	 * Trame d'aquitement (RR et RNR)
	 */
	public static final class A extends Trame {
		/**
		 * Numéro de la prochaine trame désiré. Indique que toute les trames avant ont été reçu
		 */
		private int num;
		private A(int num) {
			this.num = num;
		}
		@Override
		public Trame.Type getType() { return Trame.Type.A; }
		@Override
		public Optional<Integer> getNum() { return Optional.of(this.num&127); }
		/**
		 * Indique si l'autre point est prêt à recevoir plus de trame I
		 * @return true si on peut envoyer plus de trame I, sinon false
		 */
		public boolean ready() { return (num&128) == 0;}
		@Override
		public byte getNumByte() {
			if ((this.num&128) == 0) return (byte)Character.forDigit(this.num, 10);
			else return (byte)num;
		}
	}
	/**
	 * Trame de rejet (REJ et SREJ)
	 */
	public static final class R extends Trame {
		/**
		 * Numéro de la trame rejeté. Indique que toute les trames avant ont été reçu
		 */
		private int num;
		private R(int num) {
			this.num = num;
		}
		@Override
		public Trame.Type getType() { return Trame.Type.R; }
		@Override
		public Optional<Integer> getNum() { return Optional.of(this.num&127); }
		/**
		 * Indique si la trame demande un rejet sélectif
		 * @return true si on ne doit renvoyer que la trame n, false si on doit renvoyer celles suivantes également
		 */
		public boolean selectif() { return (num&128) == 0;}
		@Override
		public byte getNumByte() {
			if ((this.num&128) == 0) return (byte)Character.forDigit(this.num, 10);
			else return (byte)num;
		}

	}
	/**
	 * Trame de fin de connexion
	 */
	public static final class F extends Trame {
		private F() {}
		@Override
		public Trame.Type getType() { return Trame.Type.F; }
	}
	/**
	 * Trame P (pour Ping-Pong)
	 */
	public static final class P extends Trame {
		private P() {}
		@Override
		public Trame.Type getType() { return Trame.Type.P; }
	}
	/**
	 * Énumération des types de trame (parce que java a pas terminé d'implémenter le pattern matching et parfois chiâle)
	 */
	public static enum Type { I,C,A,R,F,P;
		/**
		 * Retourne un type à partir de son numéro. Utilise l'ordre I=0, C, A, R, F, P
		 * @param typenum numéro du type
		 * @return le type
		 * @throws IllegalArgumentException si le numéro ne correspond à aucun type
		 */
		public static Type from(int typenum) {
			return switch (typenum) {
				case 0 -> I;
				case 1 -> C;
				case 2 -> A;
				case 3 -> R;
				case 4 -> F;
				case 5 -> P;
				default -> throw new IllegalArgumentException("Type de trame invalide");
			};
		}
		/**
		 * Retourne le numéro du type. Utilise l'ordre I=0, C, A, R, F, P
		*/
		public int indexOf() {
			return switch (this) {
				case I -> 0;
				case C -> 1;
				case A -> 2;
				case R -> 3;
				case F -> 4;
				case P -> 5;
			};
		}
	} // au cas ou t'aime pas le pattern matching pour raison X

	/**
	 * Construit une trame selon une chaîne de bits. La chaîne de bits doit être de la forme [ type(1 byte) | num (1 byte) | msg (0-* bits) | crc (selon le générateur) ]
	 * @param bits chaîne de bits à décoder
	 * @param gen CRC utiliser pour vérifier l'intégriter de la classe
	 * @return La trame décodé
	 * @throws TrameException si la trame est invalide, que ce soit parce que le numéro ou le type est invalide, ou que le crc est rejeté
	 */
	public static Trame decode(Word bits, CRC gen) throws TrameException {
		int gen_len = gen.codeLength();
		int min_nb_of_bits = 16 + gen_len;
		if (bits.length < min_nb_of_bits) throw new TrameException("Trame trop petite"); 
		// vérification du crc
		Word check = gen.crc(bits);
		if (check.countOne() != 0) throw new TrameException("crc incorecte");

		int crc_start = bits.length - gen_len;
		try {
			Type type = Type.from(bits.getByteAt(0).value()&255);
			int num = bits.getByteAt(8).value()&255;
			if ((num & 128) == 0) { // si le premier bit est un 1 (donc on n'a pas recu un caractère ascii), on veut l'interprété comme un cas spécial
				num = Character.digit(num, 10); // le numéro de la trame est transféré comme un la repr ASCII du chiffre
				if (num < 0 || num > 7) throw new TrameException("numéro de trame invalide");
			}
			
			Word msg = bits.subWord(16, crc_start);
			return switch (type) {
				case I -> new I(num, msg);
				case C -> new C(num);
				case A -> new A(num);
				case R -> new R(num);
				case F -> new F();
				case P -> new P();
			};
		} catch (IllegalArgumentException e) {throw new TrameException(e);}
	}
	/**
	 * Transforme cette trame en chaîne de bits. Utilise <code>gen</code> pour trouver le code vérificateur
	 * @param gen le crc à utiliser
	 * @return chaîne de bits représentant la trame, prêt à être envoyer
	 */
	public Word encode(CRC gen) {
		Word type = new Word(this.getType().indexOf());
		Word num = new Word(this.getNumByte()); // char est 16-bits, les chiffres sont dans ASCII donc 8 bits. puisque java utilise les codepoint unicode, un simple cast de la sorte sufit
		Word sous = switch (this.getType()) {
			case I -> Word.concat(type, num, this.getMsg().get());
			default -> Word.concat(type, num);
		};
		Word crc = gen.crc(sous);
		return Word.concat(sous, crc);
	}

	/**
	 * Erreur lors de la création d'une trame
	 */
	public static class TrameException extends Exception {
		public TrameException(String msg) {super(msg);}
		public TrameException(Throwable src) {super(src);}
		public TrameException(String msg, Throwable src) {super(msg, src);}
		public TrameException() {super();}
	}

	/**
	 * Créer une trame RR aquittant n
	 * @param n le numéro de la prochaine trame désiré
	 * @return RR(n)
	 * @throws IllegalArgumentException si n est invalide (n < 0 ou n > 7)
	 */
	public static Trame.A rr(int n) {
		if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
		return new Trame.A(n);
	}
	/**
	 * Créer une trame RNR aquittant n
	 * @param n le numéro de la prochaine trame désiré
	 * @return RNR(n)
	 * @throws IllegalArgumentException si n est invalide (n < 0 ou n > 7)
	 */
	public static Trame.A rnr(int n) {
		if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
		return new Trame.A(n|128);
	}
	/**
	 * Créer une trame REJ rejetant n
	 * @param n le numéro de la trame rejeté
	 * @return REJ(n)
	 * @throws IllegalArgumentException si n est invalide (n < 0 ou n > 7)
	 */
	public static Trame.R rej(int n) {
		if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
		return new Trame.R(n);
	}
	/**
	 * Créer une trame SREJ rejetant n
	 * @param n le numéro de la trame rejeté
	 * @return SREJ(n)
	 * @throws IllegalArgumentException si n est invalide (n < 0 ou n > 7)
	 */
	public static Trame.R srej(int n) {
		if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
		return new Trame.R(n|128);
	}
	/**
	 * Créer une trame de demande de connexion avec Go-Back-N
	 * @return la demande de connexion
	 */
	public static Trame.C gbn() {
		return new Trame.C(0);
	}
	/**
	 * Créer une trame de fin de connexion
	 * @return la demande de fin de connexion
	 */
	public static Trame.F end() {
		return new Trame.F();
	}
	/**
	 * Créer une trame P
	 * @return P
	 */
	public static Trame.P p() {
		return new Trame.P();
	}
	/**
	 * Créer une trame d'information avec le numéro n et les données msg
	 * @param n le numéro de la trame
	 * @param msg les données
	 * @return la trame d'information
	 * @throws IllegalArgumentException si n est invalide (n < 0 ou n > 7)
	 */
	public static Trame.I i(int n, Word msg) {
		if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
		return new Trame.I(n, msg);
	}
	/**
	 * Créer une trame de demande de connexion avec Selective Reject
	 * @return la demande de connexion
	 */
	public static Trame.C selectiveRej() {
		return new Trame.C(1);
	}
}
