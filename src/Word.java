import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

/**
 * Une séquence de bit. Utilise un indexage commençant à 0 et un alignement à gauche
 * la plupart des opérations ont une version statique et une version non statique. La version non statique est équivalente à la version statique utilisant this comme premier arguments, mais le résultat est assigné à this.
 * 
 * Est essentiellement un wrapper autour d'un tableau de bytes, mais offre des opérations pour avoir (virtuellement) des chaines de longueur arbitraire et des accès au bits individuels.
 */
public class Word implements Comparable<Word>, Iterable<Boolean> {
	// je suis trisse parce que java ne permet pas:
	// 1- Les entiers non signés
	// 2- les opérations bitwise sur le type byte
	// ce qui fait qu'il faut toujours convertir entre byte et int et gérer l'extension de signe...

	// si le length n'est pas un multiple de 8, les bits du dernier byte ne sont pas tous utilisés.
	// les bits à gauche sont ceux utilisé, ceux à droite sont non définis et peuvent être n'importe quoi
	/**
	 * tableau interne de byte
	 */
	public byte[] array;
	/**
	 * Longueur (en bit) de la chaine
	 */
	public int length;


	// Pourquoi est-ce que java hais les bytes ?
	/**
	 * 0 Parce que c'est plus facile que tapper (byte)0 à chaque fois.
	 */
	private static final byte ZERO = (byte)0;
	/**
	 * 1 Parce que c'est plus facile que tapper (byte)1 à chaque fois.
	 */
	@SuppressWarnings("unused")
	private static final byte ONE = (byte)1;
	/**
	 * 111111111
	 */
	private static final byte FULL = (byte)255;

	/** 
	 * Mot vide
	 */
	public Word(){
		this.array = new byte[0];
		this.length = 0;
	}
	/**
	 * Creer un mot de 8 bit à partir d'un byte
	 * @param b
	 */
	public Word(byte b) {
		System.out.println("byte formula");
		this.array = new byte[]{b};
		this.length = 8;
	}
	/**
	 * Creer un mot de 16 bit à partir d'un short
	 * @param b
	 */
	public Word(short b) {
		this.array = new byte[2];
		for (int i=0; i < 2; i++) {
			this.array[1-i] = (byte)(b >>> 8*i);
		}
		this.length = 16;
	}
	/**
	 * Creer un mot de 32 bit à partir d'un int
	 * @param b
	 */
	public Word(int b) {
		this.array = new byte[4];
		for (int i=0; i < 4; i++) {
			this.array[3-i] = (byte)(b >>> 8*i);
		}
		this.length = 32;
	}
	/**
	 * Creer un mot de 64 bit à partir d'un long
	 * @param b
	 */
	public Word(long b) {
		this.array = new byte[2];
		for (int i=0; i < 8; i++) {
			this.array[7-i] = (byte)(b >>> 8*i);
		}
		this.length = 64;
	}
	/**
	 * Parse le String et construit le mot à partir des 1 et des 0
	 * Ignore les espaces blancs et envoie une erreur si un caractère invalide s'y trouve
	 * @param s
	 */
	public Word(String s) {
		ArrayList<Byte> arr = new ArrayList<>();
		int count = 0;
		this.length = 0;
		int curr = 0;
		for (char c : s.toCharArray()) {
			switch (c) {
				case '0': 	curr = curr << 1;
							count += 1;
							break;
				case '1':	curr = (curr << 1) | 1;
							count += 1;
							break;
				default:
					if (!Character.isWhitespace(c)) {
						throw new RuntimeException("Caractère invalide");
					}
			}
			if (count == 8) {
				arr.add((byte)curr);
				count = 0;
				curr = 0;
				this.length += 8;
			}
		}
		if (count > 0) {
			arr.add((byte)(curr << 8-count));
			this.length += count;
		}
		this.array = new byte[arr.size()];
		for (int i = 0; i < arr.size(); i++) // on ne peut pas simplement faire d'assignation parce que java est michant
			this.array[i] = arr.get(i);
	}
	/**
	 * usage privé principalement. Assume que len <= 8*arr.length
	 * @param arr
	 * @param len
	 */
	public Word(byte[] arr, int len) {
		this.array = arr;
		this.length = len;
	}

	/**
	 * Créer un mot à partir d'un tableau de bytes. Assume que tout les bits sont pertinents donc <code>this.length = 8*orig.length</code>
	 * @param orig
	 */
	public Word(byte[] orig){
		this.array = orig;
		this.length = orig.length*8;
	}
	/**
	 * Clone un mot
	 * @param orig
	 */
	public Word(Word orig) {
		this.array = new byte[orig.array.length];
		System.arraycopy(orig.array, 0, this.array, 0, this.array.length);
		this.length = orig.length;
	}

	/**
	 * Un mot constitué de len x 0
	 * @param len longueur du mot
	 * @return
	 */
	public static Word zero(int len) {
		int n = len/8;
		n += len%8==0? 0 : 1;
		byte[] arr = new byte[n];
		return new Word(arr, len);
	}
	/**
	 * Un mot constitué de len x 1
	 * @param len longueur du mot
	 * @return
	 */
	public static Word one(int len) {
		int n = len/8;
		n += len%8==0? 0 : 1;
		byte[] arr = new byte[n];
		Arrays.fill(arr, (byte)255);
		return new Word(arr, len);
	}

	// records pour contenir plusieurs valeur lors des retours
	// PLZZZZZ java fait juste supporter les tuples S'IL TE PLAÎT!!!!!!!
	/**
	 * Tuple utliser comme type de retour pour certaines opérations qui demande de modifier un byte du mot
	 * Contient le mot modifié ainsi que la valueur du byte précédent
	 */
	public static record SetByteResult(Word result, byte old) {}
	/**
	 * Tuple utliser comme type de retour pour certaines opérations qui demande de modifier un bit du mot
	 * Contient le mot modifié ainsi que la valueur du bit précédent
	 */
	public static record SetBitResult(Word result, boolean old) {}
	/**
	 * Retourne le ième bit de la séquence (partant de la gauche)
	 * @param i
	 * @return
	 */
	public boolean getBitAt(int i) {
		if (i < 0 || i >= this.length) throw new IndexOutOfBoundsException();
		int oct = i >>> 3; // divisé par 8
		byte offset = (byte)(i & 7); // garde les trois dernier chiffre;
		boolean bit = (this.array[oct] & (128 >>> offset)) != 0; // on retourne le 'offset'ième bit du 'oct'ième octet
		return bit;
	}
	/**
	 * Change le ième bit (partant de la gauche) pour la valeur désigné
	 * Modifie ce mot
	 * @param i
	 * @param value
	 * @return la valeur précédente du bit
	 */
	public boolean setBitAt(int i, boolean value) {
		if (i < 0 || i >= this.length) throw new IndexOutOfBoundsException();
		int oct = i >>> 3; // divisé par 8
		byte mask = (byte)(128 >>> (i & 7)); // garde les trois dernier chiffre;
		boolean old = (this.array[oct] & mask) != 0;
		this.array[oct] = value? (byte)(this.array[oct] | mask) : (byte)(this.array[oct] & ~mask);
		return old;
	}
	/**
	 * Retourne le byte commençant à la position pos (0-index).
	 * Si at < 0, alors -at 0 précéderont les bits pertinent
	 * @param pos
	 * @return ByteRequest avec la valeur demandé et le nombre de bits recueuillis
	 */
	public Request<Byte> getByteAt(int pos) {
		return byte_at(this.array, this.length, pos, false);
	}
	/**
	 * Change le byte commençant à pos pour le byte indiquer (0-index). Retourne l'ancienne valeur.
	 * Modifie ce mot
	 * @param pos
	 * @param val
	 * @return ByteRequest contenant l'ancienne valeur et le nombre de bits recueuillis/changés
	 */
	public Request<Byte> setByteAt(int pos, byte val) {
		return set_byte_at(this.array, this.length, pos, val);
	}
	/**
	 * Retourne le int commençant à la position pos (0-index).
	 * Si at < 0, alors -at 0 précéderont les bits pertinent
	 * @param pos
	 * @return Request avec la valeur demandé et le nombre de bits recueuillis
	 */
	public Request<Integer> getIntAt(int pos) {
		int val = 0;
		int len = 0;
		for (int i=0; i < 4; i++) {
			Request<Byte> br = byte_at(this.array, this.length, i*8 + pos, false); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			val <<= 8;
			val |= (br.value&255);
			len += br.len;
		}
		return new Request<Integer>(val, len);
	}
	/**
	 * Change les 32 bits commençant à pos pour le int indiquer (0-index). Retourne l'ancienne valeur.
	 * Modifie ce mot
	 * @param pos
	 * @param val
	 * @return Request contenant l'ancienne valeur et le nombre de bits recueuillis/changés
	 */
	public Request<Integer> setIntAt(int pos, int val) {
		int rval = 0;
		int len = 0;
		for (int i=0; i < 4; i++) {
			byte vb = (byte)(val >>> (8*(3-i)));
			Request<Byte> br = set_byte_at(this.array, this.length, i*8 + pos, vb); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			rval <<= 8;
			rval |= (br.value&255);
			len += br.len;
		}
		return new Request<Integer>(rval, len);
	}
	/**
	 * Retourne le short commençant à la position pos (0-index).
	 * Si at < 0, alors -at 0 précéderont les bits pertinent
	 * @param pos
	 * @return Request avec la valeur demandé et le nombre de bits recueuillis
	 */
	public Request<Short> getShortAt(int pos) {
		short val = 0;
		int len = 0;
		for (int i=0; i < 2; i++) {
			Request<Byte> br = byte_at(this.array, this.length, i*8 + pos, false); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			val <<= 8;
			val |= (br.value&255);
			len += br.len;
		}
		return new Request<Short>(val, len);
	}
	/**
	 * Change les 16 bits commençant à pos pour le short indiquer (0-index). Retourne l'ancienne valeur.
	 * Modifie ce mot
	 * @param pos
	 * @param val
	 * @return Request contenant l'ancienne valeur et le nombre de bits recueuillis/changés
	 */
	public Request<Short> setshortAt(int pos, short val) {
		short rval = 0;
		int len = 0;
		for (int i=0; i < 2; i++) {
			byte vb = (byte)(val >>> (8*(1-i)));
			Request<Byte> br = set_byte_at(this.array, this.length, i*8 + pos, vb); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			rval <<= 8;
			rval |= (br.value&255);
			len += br.len;
		}
		return new Request<Short>(rval, len);
	}
	/**
	 * Retourne le long commençant à la position pos (0-index).
	 * Si at < 0, alors -at 0 précéderont les bits pertinent
	 * @param pos
	 * @return Request avec la valeur demandé et le nombre de bits recueuillis
	 */
	public Request<Long> getLongAt(int pos) {
		long val = 0;
		int len = 0;
		for (int i=0; i < 8; i++) {
			Request<Byte> br = byte_at(this.array, this.length, i*8 + pos, false); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			val <<= 8;
			val |= (br.value&255);
			len += br.len;
		}
		return new Request<Long>(val, len);
	}
	/**
	 * Change les 64 bits commençant à pos pour le long indiquer (0-index). Retourne l'ancienne valeur.
	 * Modifie ce mot
	 * @param pos
	 * @param val
	 * @return Request contenant l'ancienne valeur et le nombre de bits recueuillis/changés
	 */
	public Request<Long> setLongAt(int pos, long val) {
		long rval = 0;
		int len = 0;
		for (int i=0; i < 9; i++) {
			byte vb = (byte)(val >>> (8*(7-i)));
			Request<Byte> br = set_byte_at(this.array, this.length, i*8 + pos, vb); // je pourais invoker getByteAt mais pourquoi empiler sur la stack
			rval <<= 8;
			rval |= (br.value&255);
			len += br.len;
		}
		return new Request<Long>(rval, len);
	}
	
	/**
	 * Interprète les 8 bits de ce mot à partir de at (inclusif) comme un byte.
	 * À la différence de getByteAt, les bits sont aligné à droite et les 0 précédents le mots sont perdu puisqu'on ne garde pas le nombre de bit pertinent
	 * @param at
	 * @return
	 */
	public byte asByte(int at) { // contrairement aux autres, on n'invente pas des bits autours
		if (at < 0 || at >= this.length) throw new IndexOutOfBoundsException();
		BitsRequest br = n_bits_at(this.array, this.length, at, 8, false); // on veut n_bits_at parce que celui-la nous dit il y a combien de bits avant
		int shift = (8-br.offset-br.len)%8;
		return (byte)((br.value&255) >>> shift);
	}
	/**
	 * Interprète les 16 bits de ce mot à partir de at (inclusif) comme un short.
	 * À la différence de getShortAt, les bits sont aligné à droite et les 0 précédents le mots sont perdu puisqu'on ne garde pas le nombre de bit pertinent
	 * @param at
	 * @return
	 */
	public short asShort(int at) { // contrairement aux autres, on n'invente pas des bits autours
		if (at < 0 || at >= this.length) throw new IndexOutOfBoundsException();
		short val = 0;
		int offset = 0;
		int len = 0;
		for (int i=0; i<2; i+=1) {
			BitsRequest br = n_bits_at(this.array, this.length, at, 8, false);
			val <<= 8;
			val |= br.value;
			len += br.len;
			offset += br.offset;
		}
		int shift = (16-offset-len)%16;
		return (short)((val&0xFFFFFFFF) >>> shift);
	}
	/**
	 * Interprète les 32 bits de ce mot à partir de at (inclusif) comme un int.
	 * À la différence de getIntAt, les bits sont aligné à droite et les 0 précédents le mots sont perdu puisqu'on ne garde pas le nombre de bit pertinent
	 * @param at
	 * @return
	 */
	public int asInt(int at) { // contrairement aux autres, on n'invente pas des bits autours
		if (at < 0 || at >= this.length) throw new IndexOutOfBoundsException();
		int val = 0;
		int offset = 0;
		int len = 0;
		for (int i=0; i<4; i+=1) {
			BitsRequest br = n_bits_at(this.array, this.length, at, 8, false);
			val <<= 8;
			val |= br.value;
			len += br.len;
			offset += br.offset;
		}
		int shift = (32-offset-len)%32;
		return val >>> shift;
	}
	/**
	 * Interprète les 64 bits de ce mot à partir de at (inclusif) comme un long.
	 * À la différence de getLongAt, les bits sont aligné à droite et les 0 précédents le mots sont perdu puisqu'on ne garde pas le nombre de bit pertinent
	 * @param at
	 * @return
	 */
	public long asLong(int at) { // contrairement aux autres, on n'invente pas des bits autours
		if (at < 0 || at >= this.length) throw new IndexOutOfBoundsException();
		long val = 0;
		int offset = 0;
		int len = 0;
		for (int i=0; i<8; i+=1) {
			BitsRequest br = n_bits_at(this.array, this.length, at, 8, false);
			val <<= 8;
			val |= br.value;
			len += br.len;
			offset += br.offset;
		}
		int shift = (64-offset-len)%64;
		return val >>> shift;
	}

	/**
	 * Applique la fonction fun(b) au byte b commençant à pos
	 * Modifie ce mot
	 * @param pos position du premier bit du byte (0-index)
	 * @param fun fonction a appliquer
	 * @return ApplyByteRequest contenant l'ancienne et la nouvelle valeur ainsi que le nombre de bits recueillis/changés
	 */
	public ApplyRequest<Byte> applyByteAt(int pos, UnaryByteFunc fun) {
		return apply_byte_at(this.array, this.length, pos, fun);
	}
	/**
	 * Applique la fonction fun(b, val) au byte b commençant à pos
	 * Modifie ce mot
	 * @param pos position du premier bit du byte (0-index)
	 * @param val deuxième argument pour fun
	 * @param fun fonction a appliquer
	 * @return ApplyByteRequest contenant l'ancienne et la nouvelle valeur ainsi que le nombre de bits recueillis/changés
	 */
	public ApplyRequest<Byte> applyByteAt(int pos, byte val, ByteFunc fun){
		return apply_byte_at(this.array, this.length, pos, val, fun);
	}

	public static Word concat(Word... words) {
		ConcatRequest cr = concat_many_words(words);
		return new Word(cr.arr, cr.len);
	}
	/**
	 * Retourne une chaine constitué des bit de start (inclusif) à end (exclusif)
	 * @param start
	 * @param end
	 * @return
	 */
	public Word subWord(int start, int end) {
		if (start < 0 || end > this.length || end < start) throw new IndexOutOfBoundsException();
		return new Word(sub_word(this.array, start, end), end-start);
	}
	/**
	 * Rajoute les bits de other à la fin de ce mot
	 * Modifie ce mot
	 * @param other
	 * @return this
	 */
	public Word concat(Word other) {
		ConcatRequest cr = concat_many_words(this, other);
		this.array = cr.arr;
		this.length = cr.len;
		return this;
	}
	/**
	 * Rajoute les bits de other au début de ce mot
	 * Modifie ce mot
	 * @param other
	 * @return this
	 */
	public Word preConcat(Word other) {
		ConcatRequest cr = concat_many_words(other, this);
		this.array = cr.arr;
		this.length = cr.len;
		return this;
	}
	/**
	 * Rajoute n 0 à la fin du mot
	 * Modifie ce mot
	 * @param n
	 * @return this
	 */
	public Word zeroExtend(int n) {
		return this.concat(Word.zero(n));
	}
	/**
	 * Rajoute n 0 au début du mot
	 * Modifie ce mot
	 * @param n
	 * @return this
	 */
	public Word preZeroExtend(int n) {
		return this.preConcat(Word.zero(n));
	}
	/**
	 * Rajoute n 1 à la fin du mot
	 * Modifie ce mot
	 * @param n
	 * @return this
	 */
	public Word oneExtend(int n) {
		return this.concat(Word.one(n));
	}
	/**
	 * Rajoute n 1 au début du mot
	 * Modifie ce mot
	 * @param n
	 * @return this
	 */
	public Word preOneExtend(int n) {
		return this.preConcat(Word.one(n));
	}
	/**
	 * Type de retour de concat_many_words, parce que java n'a pas de tuple :(
	 */
	private static record ConcatRequest(byte[] arr, int len) {}

	/**
	 * Retourne un nouveau mot composé de <code>src</code> suivit de n 0
	 * @param src mot original
	 * @param n nombre de 0 à rajouter
	 * @return nouveau mot étendu
	 */
	public static Word zeroExtend(Word src, int n) {
		return concat(src, zero(n));
	}
	/**
	 * Retourne un nouveau mot composé de <code>src</code> précédé de n 0
	 * @param src mot original
	 * @param n nombre de 0 à rajouter
	 * @return nouveau mot étendu
	 */
	public static Word preZeroExtend(Word src, int n) {
		return concat(zero(n), src);
	}
	/**
	 * Retourne un nouveau mot composé de <code>src</code> suivit de n 1
	 * @param src mot original
	 * @param n nombre de 1 à rajouter
	 * @return nouveau mot étendu
	 */
	public static Word oneExtend(Word src, int n) {
		return concat(src, one(n));
	}
	/**
	 * Retourne un nouveau mot composé de <code>src</code> précédé de n 1
	 * @param src mot original
	 * @param n nombre de 0 à rajouter
	 * @return nouveau mot étendu
	 */
	public static Word preOneExtend(Word src, int n) {
		return concat(one(n), src);
	}

	/**
	 * Calcule la concaténation de des mots passeé en paramètre
	 * @return
	 */
	private static ConcatRequest concat_many_words(Word... ws) {
		int len = 0;
		for (Word word : ws) len += word.length;
		int n = len/8 + (len%8==0? 0 : 1);
		byte[] arr = new byte[n];

		int curr_w = 0;
		int curr_len = 0;
		int curr_start = 0;
		while (curr_w < ws.length) {
			byte[] w = ws[curr_w].array;
			int offset = curr_len%8;
			if (offset == 0) { // tout est aligné déjà, on peut arraycopy
				System.arraycopy(w, 0, arr, curr_start, w.length);
			} else { // push les byte 1 par 1
				int mask = first_bits_mask(offset);
				int o = 8-offset;
				for (int i = 0; i < w.length; i++) {
					// rajoute les premiers bits au byte courant
					// et initialise le prochain avec les bits restant
					int b1 = (arr[curr_start+i]&mask) | ((w[i]&255) >>> offset);
					int b2 = (w[i]&255) << o;
					arr[curr_start + i] = (byte)b1;
					arr[curr_start+i+1] = (byte)b2;
				}
			}
			curr_len += ws[curr_w].length;
			curr_w += 1;
			curr_start = curr_len/8;
		}

		return new ConcatRequest(arr, len);
	}
	/**
	 * Calcule le sous-mot de start (inclusif) à end (exclusif)
	 * Assume que toutes les vérifications ont été faite
	 * @param src
	 * @param start
	 * @param end
	 * @return
	 */
	private static byte[] sub_word(byte[] src, int start, int end) {
		if (start >= src.length*8 || start < 0 || end > src.length*8)  throw new IndexOutOfBoundsException();
		// trois cas: start==end (trivial), start est un multiple de 8 (facile) et start n'est pas un multiple de 8 (difficile)
		int so = start%8;
		if (start == end) {
			return new byte[0];
		}
		else if (so == 0) {
			int len = end-start;
			int fb = start/8; // index du premier byte
			int lb = (end-1)/8; // index du dernier byte
			int n = nb_byte_for_n_bit(len); // nombre de byte.
			byte[] arr = new byte[n];
			// juste copier tout
			//System.out.println("copy byte "+ fb + " to " + lb);
			System.arraycopy(src, fb, arr, 0, n);
			return arr;
		} else {
			int len = end-start; // nombre de bit
			int n = nb_byte_for_n_bit(len); // nombre de byte dans le sous-mot
			byte[] sub = new byte[n];

			for (int i=0; i < n; i++) {
				byte b = byte_at(src, src.length*8, i*8+start, false).value;
				sub[i] = b;
			}
			return sub;
		}
	}

	/**
	 * record représentant une requête de bits. Tuple de la valeur des bits et du nombre de bits pertinent (0-index, left-align)
	 */
	public static record Request<T>(T value, int len) {}
	/**
	 * Tuple représentant le résultat d'une requête de bits.
	 * Contient la valeur des bits demander, l'index du premier bits pertinent (0 à gauche), le nombre de bits pertinent et un masque ou seul les bits aux positions pertinentes sont à 1
	 */
	public static record BitsRequest(byte value, byte offset, byte len, byte mask) {}
	/**
	 * Similaire à BitsRequest, mais contient à la fois l'ancienne valeur des bits demandé et les bits après l'application d'une fonction
	 * @see BitsRequest
	 */
	public static record ApplyBitsRequest(byte old_value, byte new_value, byte offset, byte len, byte mask) {
		/**
		 * Forme un BitsRequest en utilisant l'ancienne valuer
		 * @return BitsRequest(old_value, offset, len, mask)
		 */
		public BitsRequest request_old() { return new BitsRequest(old_value, offset, len, mask); }
		/**
		 * Forme un BitsRequest en utilisant la nouvelle valeur
		 * @return BitsRequest(new_value, offset, len, mask)
		 */
		public BitsRequest request_new() { return new BitsRequest(new_value, offset, len, mask); }
	}
	/**
	 * Similaire à Request, mais est fait pour les fonctions 'apply' et contient également la nouvelle valeur calculée
	 * @see Request
	 */
	public static record ApplyRequest<T>(T value_old, T value_new, byte len) {
		/**
		 * Forme un Request en utilisant l'ancienne valeur
		 * @return Request(new_value, len)
		 */
		public Request<T> request_old() { return new Request<T>(value_old, len); }
		/**
		 * Forme un Request en utilisant la nouvelle valeur
		 * @return Request(new_value, len)
		 */
		public Request<T> request_new() { return new Request<T>(value_new, len); }
	}
	/**
	 * Indique comment calculer la taille du mot de retour
	 */
	public enum ApplyMode {
		/**
		 * Ne garde que les bits en position dans le premier argument
		 */
		FIRST, // utilise la taille et alignement du premier argument
		/**
		 * Ne garde que les bits en position dans le second argument
		 */
		SECOND, // utilise la taille et alignement du deuxième argument
		/**
		 * Garde tous les bits. Si il y a un trou dans l'alignement, rajoute des bits
		 */
		UNION, // Utilise également les bits des arguments qui ne se superpose pas, assumant que les bits manquant sont égal à fill
		/**
		 * Ne garde que les bits en position dans les deux arguments
		 */
		INTERSECTION // Utilise uniquement les bits superposés
	}
	/**
	 * Applique une fonction sur les bits des deux arguments
	 * @param a premier mots
	 * @param b deuxième mots
	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return nouveau mot correspondant à l'application de fun sur a et b
	 */
	private static Word apply_to_words(Word a, Word b, int alignment, ApplyMode mode, ByteFunc fun, boolean fill) {
		int first_bit = switch (mode) {
			case FIRST -> 0;
			case SECOND -> alignment;
			case UNION -> Math.min(0, alignment);
			case INTERSECTION -> Math.max(0, alignment);
		};
		int last_bit = switch (mode) {
			case FIRST -> a.length;
			case SECOND -> b.length;
			case UNION -> Math.max(a.length, b.length+alignment);
			case INTERSECTION -> Math.min(a.length, b.length+alignment);
		};
		int result_bit_len = last_bit - first_bit;
		int result_arr_len = result_bit_len/8;
		result_arr_len += result_bit_len%8 == 0 ? 0 : 1;
		byte[] result_arr = new byte[result_arr_len];

		for (int i=0; i < result_arr_len; i+=1) {
			int b1_pos = i*8+first_bit;
			int b2_pos = i*8+first_bit-alignment;
			byte b1 = (byte)(byte_at(a.array, a.length, b1_pos, fill).value&255);
			byte b2 = (byte)(byte_at(b.array, b.length, b2_pos, fill).value&255);
			byte rb = fun.apply(b1, b2);
			/*if (t) {
				System.out.println("B1 (" + b1_pos + ")" + stringifyByte(b1) + "; B2 (" + b2_pos + ")" + stringifyByte(b2) + "; RB (" + i*8 + ")" + stringifyByte(rb));
				t = false;
			}*/
			//set_byte_at(result_arr, result_bit_len, i*8, rb);
			result_arr[i] = rb;
		}

		return new Word(result_arr, result_bit_len);
	}
	/**
	 * retourne le byte du mot commençant à at, utilisant la valeur de fill pour les bits manquants
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param fill valeur utiliser pour les bits manquant
	 * @return
	 */
	private static Request<Byte> byte_at(byte[] arr, int bit_len, int at, boolean fill) {
		BitsRequest bits = n_bits_at(arr, bit_len, at, 8, fill);
		return new Request<Byte>(bits.value, bits.len);
	}
	/**
	 * Assigne val au byte commençant à at. Retourne l'ancienne valeur
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param val valeur à assigner
	 * @return l'ancienne valeur
	 */
	private static Request<Byte> set_byte_at(byte[] arr, int bit_len, int at, byte val) {
		BitsRequest bits = set_n_bits_at(arr, bit_len, at, 8, val, false);
		return new Request<Byte>(bits.value, bits.len);
	}
	/**
	 * Applique <code>fun</code> au byte commençant à <code>at</code> en utilisant <code>val</code> comme deuxième argument
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param val deuxième argument pour <code>fun</code>
	 * @param fun fonction sur deux byte
	 * @return (ancienne valeur, nouvelle valeur, nombre de bit pertinent)
	 */
	private static ApplyRequest<Byte> apply_byte_at(byte[] arr, int bit_len, int at, byte val, ByteFunc fun) {
		ApplyBitsRequest bits = apply_n_bits_at(arr, bit_len, at, 8, val, fun, false);
		return new ApplyRequest<Byte>(bits.old_value, bits.new_value, bits.len);
	}
	/**
	 * Similaire à <code>apply_byte_at(byte[], int, int, byte, ByteFunc)</code> mais utilise un fonction unaire
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param fun fonction sur un byte
	 * @return (ancienne valeur, nouvelle valeur, nombre de bit pertinent)
	 */
	private static ApplyRequest<Byte> apply_byte_at(byte[] arr, int bit_len, int at, UnaryByteFunc fun) {
		ApplyBitsRequest bits = apply_n_bits_at(arr, bit_len, at, 8, fun, false);
		return new ApplyRequest<Byte>(bits.old_value, bits.new_value, bits.len);
	}
	/**
	 * Retourne jusqu'à n bits, commençant à la position at. Permet des index qui ne sont pas dans le mot; dans ces cas, la valeur de fill sera utiliser pour les bits manquant.
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param n nombre de bit désiré
	 * @param fill valeur à utiliser pour les bits manquants
	 * @return BitsRequest contenant les bits demandés ainsi que que les informations nécessaires pour identifier les bits pertinents
	 */
	private static BitsRequest n_bits_at(byte[] arr, int bit_len, int at, int n, boolean fill) {
		n = Math.max(Math.min(n, 8), 0);
		if (at+n <= 0 || at >= bit_len) return new BitsRequest(fill? FULL : ZERO, ZERO, ZERO, ZERO);
		else {
			int to = at+n;
			int mask = bit_mask(bit_len, at, to); // masque des bits pertinent dans le byte récolté
			//System.out.println("MASK=" + stringifyByte((byte)mask));
			int imask = (~mask)&255; // masque inverté
			int len = to <= bit_len? n : n - (to-bit_len); // nombre de bits pertinent
			//System.out.println("LEN=" + len);
			if (len == 0 || len > 8) throw new RuntimeException("What the fuck"); // ne devrait jamais arriver
			
			int fbi = at < 0? -1 : at/8; // index du premier byte
			int sbi = (to-1)/8; // index du deuxième byte
			int fb = fbi < 0? (fill? FULL : ZERO) : arr[fbi];
			int sb = sbi >= arr.length? (fill? FULL : ZERO) : arr[sbi];
			int offset = at < 0? (at+8)%8 : at%8;
			fb = (fb&255) << offset;
			sb = (sb&255) >>> (8-offset);
			int b = (fb|sb)&mask;
			if (fill) b |= imask;
			return new BitsRequest((byte)b, (byte)(at%8), (byte)len, (byte)mask);
		}
	}
	/**
	 * Assigne jusqu'à n bits, commençant à la position at. Permet des index qui ne sont pas dans le mot; dans ces cas, la valeur de fill sera utiliser pour les bits manquant.
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param n nombre de bit désiré. 0 <= n <= 8
	 * @param val nouvelle valeur des bits. place le bit le plus à gauche à la position at
	 * @param fill valeur à utiliser pour les bits manquants
	 * @return BitsRequest contenant l'ancienne valeur des bits demandés ainsi que que les informations nécessaires pour identifier les bits pertinents
	 */
	private static BitsRequest set_n_bits_at(byte[] arr, int bit_len, int at, int n, byte val, boolean fill) {
		n = Math.max(Math.min(n, 8), 0);
		if (at+n <= 0 || at >= bit_len) return new BitsRequest(fill? FULL : ZERO, ZERO, ZERO, ZERO);
		else {
			int to = at+n;
			int mask = bit_mask(bit_len, at, to); // masque des bits pertinent dans le byte récolté
			//System.out.println("MASK=" + stringifyByte((byte)mask));
			int imask = (~mask)&255; // masque inverté
			int len = to <= bit_len? n : n - (to-bit_len); // nombre de bits pertinent
			if (len == 0 || len > 8) throw new RuntimeException("What the fuck"); // ne devrait jamais arriver

			// récolte du vieux byte
			int fbi = at < 0? -1 : at/8; // index du premier byte
			int sbi = (to-1)/8; // index du deuxième byte
			int fb = fbi < 0? (fill? FULL : ZERO) : arr[fbi];
			int sb = sbi >= arr.length? (fill? FULL : ZERO) : arr[sbi];
			int offset = at < 0? (at+8)%8 : at%8;
			int fbp = (fb&255) << offset;
			int sbp = (sb&255) >>> (8-offset);
			int b = (fbp|sbp)&mask;
			if (fill) b |= imask;

			// assigne le nouveau byte
			int v = val&mask;
			//System.out.println("VAL=" + stringifyByte((byte)v));
			if (fbi >= 0) {
				int fbv = v >>> offset;
				int fbm = FULL << (8-offset);
				int fbn = (fb&fbm)|fbv;
				//System.out.println("FBV=" + stringifyByte((byte)fbv) + "; FBM=" + stringifyByte((byte)fbm) + " => " + stringifyByte((byte)fbn));
				arr[fbi] = (byte)fbn;
			}
			if (sbi < arr.length && sbi != fbi) {
				int sbv = v << (8-offset);
				int sbm = FULL >>> offset;
				int sbn = (sb&sbm)|sbv;
				//System.out.println("SBV=" + stringifyByte((byte)sbv) + "; SBM=" + stringifyByte((byte)sbm) + " => " + stringifyByte((byte)sbn));
				arr[sbi] = (byte)sbn;
			}

			return new BitsRequest((byte)b, (byte)(at%8), (byte)len, (byte)mask);
		}
	}
	/**
	 * Applique <code>fun</code> sur les n bits commençant à <code>at</code> et assigne le résultat à la position. Permet des index qui ne sont pas dans le mot; dans ces cas, la valeur de fill sera utiliser pour les bits manquant.
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param n nombre de bit désiré. 0 <= n <= 8
	 * @param val deuxième argument pour fun
	 * @param fun fonction sur deux byte
	 * @param fill valeur à utiliser pour les bits manquants
	 * @return ApplyBitsRequest contenant l'ancienne et la nouvelle valeur des bits ainsi que toutes les informations nécessaires pour identifier les bits pertinents
	 */
	private static ApplyBitsRequest apply_n_bits_at(byte[] arr, int bit_len, int at, int n, byte val, ByteFunc fun, boolean fill) {
		n = Math.max(Math.min(n, 8), 0);
		if (at+n <= 0 || at >= bit_len) return new ApplyBitsRequest(fill? FULL : ZERO, fun.apply(ZERO, ZERO), ZERO, ZERO, ZERO);
		else {
			int to = at+n;
			int mask = bit_mask(bit_len, at, to); // masque des bits pertinent dans le byte récolté
			int imask = (~mask)&255; // masque inverté
			int len = to <= bit_len? n : n - (to-bit_len); // nombre de bits pertinent
			if (len == 0 || len > 8) throw new RuntimeException("What the fuck"); // ne devrait jamais arriver
			
			// récolte du vieux byte
			int fbi = at < 0? -1 : at/8; // index du premier byte
			int sbi = (to-1)/8; // index du deuxième byte
			int fb = fbi < 0? (fill? FULL : ZERO) : arr[fbi];
			int sb = sbi >= arr.length? (fill? FULL : ZERO) : arr[sbi];
			int offset = at < 0? (at+8)%8 : at%8;
			int fbp = (fb&255) << offset;
			int sbp = (sb&255) >>> (8-offset);
			int b = (fbp|sbp)&mask;
			if (fill) b |= imask;

			// calcul le nouveau byte
			int v = fun.apply((byte)b, val)&mask;

			// assigne le nouveau byte
			if (fbi >= 0) {
				int fbv = v >>> offset;
				int fbm = FULL << (8-offset);
				int fbn = (fb&fbm)|fbv;
				//System.out.println("FBV=" + stringifyByte((byte)fbv) + "; FBM=" + stringifyByte((byte)fbm) + " => " + stringifyByte((byte)fbn));
				arr[fbi] = (byte)fbn;
			}
			if (sbi < arr.length && sbi != fbi) {
				int sbv = v << (8-offset);
				int sbm = FULL >>> offset;
				int sbn = (sb&sbm)|sbv;
				//System.out.println("SBV=" + stringifyByte((byte)sbv) + "; SBM=" + stringifyByte((byte)sbm) + " => " + stringifyByte((byte)sbn));
				arr[sbi] = (byte)sbn;
			}

			return new ApplyBitsRequest((byte)b, (byte)v, (byte)(at%8), (byte)len, (byte)mask);
		}
	}
	/**
	 * Applique <code>fun</code> sur les n bits commençant à <code>at</code> et assigne le résultat à la position. Permet des index qui ne sont pas dans le mot; dans ces cas, la valeur de fill sera utiliser pour les bits manquant.
	 * @param arr tableau de byte
	 * @param bit_len nombre de bit pertinent dans le tableau
	 * @param at position du premier bit désiré
	 * @param n nombre de bit désiré. 0 <= n <= 8
	 * @param fun fonction sur un byte
	 * @param fill valeur à utiliser pour les bits manquants
	 * @return ApplyBitsRequest contenant l'ancienne et la nouvelle valeur des bits ainsi que toutes les informations nécessaires pour identifier les bits pertinents
	 */
	private static ApplyBitsRequest apply_n_bits_at(byte[] arr, int bit_len, int at, int n, UnaryByteFunc fun, boolean fill) {
		n = Math.max(Math.min(n, 8), 0);
		if (at+n <= 0 || at >= bit_len) return new ApplyBitsRequest(fill? FULL : ZERO, fun.apply(ZERO), ZERO, ZERO, ZERO);
		else {
			int to = at+n;
			int mask = bit_mask(bit_len, at, to); // masque des bits pertinent dans le byte récolté
			int imask = (~mask)&255; // masque inverté
			int len = to <= bit_len? n : n - (to-bit_len); // nombre de bits pertinent
			if (len == 0 || len > 8) throw new RuntimeException("What the fuck"); // ne devrait jamais arriver
			
			// récolte du vieux byte
			int fbi = at < 0? -1 : at/8; // index du premier byte
			int sbi = (to-1)/8; // index du deuxième byte
			int fb = fbi < 0? (fill? FULL : ZERO) : arr[fbi];
			int sb = sbi >= arr.length? (fill? FULL : ZERO) : arr[sbi];
			int offset = at < 0? (at+8)%8 : at%8;
			int fbp = (fb&255) << offset;
			int sbp = (sb&255) >>> (8-offset);
			int b = (fbp|sbp)&mask;
			if (fill) b |= imask;

			// calcul le nouveau byte
			int v = fun.apply((byte)b)&mask;

			// assigne le nouveau byte
			if (fbi >= 0) {
				int fbv = v >>> offset;
				int fbm = FULL << (8-offset);
				int fbn = (fb&fbm)|fbv;
				//System.out.println("FBV=" + stringifyByte((byte)fbv) + "; FBM=" + stringifyByte((byte)fbm) + " => " + stringifyByte((byte)fbn));
				arr[fbi] = (byte)fbn;
			}
			if (sbi < arr.length && sbi != fbi) {
				int sbv = v << (8-offset);
				int sbm = FULL >>> offset;
				int sbn = (sb&sbm)|sbv;
				//System.out.println("SBV=" + stringifyByte((byte)sbv) + "; SBM=" + stringifyByte((byte)sbm) + " => " + stringifyByte((byte)sbn));
				arr[sbi] = (byte)sbn;
			}

			return new ApplyBitsRequest((byte)b, (byte)v, (byte)(at%8), (byte)len, (byte)mask);
		}
	}

	/**
	 * Calcul l'offset necessaire pour w2 pour que les 2 mots soit aligné à droite
	 * @param w2
	 * @return 
	 */
	public int alignRight(Word w2) {
		return this.length - w2.length;
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param alignment nombre de bits que le 2e mot est décalé vers la droite. 0 par défaut
	 * @param mode indique quels bits des mots seront utilisés pour calculer la réponse. INTERSECTION par défaut
	 * @param fill indique quelle valeur sera utilisé pour les bits manquant
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, int alignment, ApplyMode mode, boolean fill) {
		return apply_to_words(w1, w2, alignment, mode, fun, fill);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param alignment nombre de bits que le 2e mot est décalé vers la droite. 0 par défaut
	 * @param mode indique quels bits des mots seront utilisés pour calculer la réponse. INTERSECTION par défaut
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, int alignment, ApplyMode mode) {
		return apply_to_words(w1, w2, alignment, mode, fun, false);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param alignment nombre de bits que le 2e mot est décalé vers la droite. 0 par défaut
	 * @param fill indique quelle valeur sera utilisé pour les bits manquant
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, int alignment, boolean fill) {
		return apply_to_words(w1, w2, alignment, ApplyMode.INTERSECTION, fun, fill);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param mode indique quels bits des mots seront utilisés pour calculer la réponse. INTERSECTION par défaut
	 * @param fill indique quelle valeur sera utilisé pour les bits manquant
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, ApplyMode mode, boolean fill) {
		return apply_to_words(w1, w2, 0, mode, fun, fill);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param alignment nombre de bits que le 2e mot est décalé vers la droite. 0 par défaut
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, int alignment) {
		return apply_to_words(w1, w2, alignment, ApplyMode.INTERSECTION, fun, false);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param mode indique quels bits des mots seront utilisés pour calculer la réponse. INTERSECTION par défaut
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, ApplyMode mode) {
		return apply_to_words(w1, w2, 0, mode, fun, false);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @param fill indique quelle valeur sera utilisé pour les bits manquant
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun, boolean fill) {
		return apply_to_words(w1, w2, 0, ApplyMode.INTERSECTION, fun, fill);
	}
	/**
	 * Applique la fonction fun sur w1 et w2 avec w2 décalé vers la droite d'aligment bits.
	 * Ne modifie pas w1 ou w2
	 * @param w1
	 * @param w2
	 * @param fun fonction à appliquer
	 * @return
	 */
	public static Word apply(Word w1, Word w2, ByteFunc fun) {
		return apply_to_words(w1, w2, 0, ApplyMode.INTERSECTION, fun, false);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public static Word xor(Word w1, Word w2, int alignment, ApplyMode mode, boolean fill) {
		return apply_to_words(w1, w2, alignment, mode, bit_xor, fill);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @return
	 */
	public static Word xor(Word w1, Word w2, int alignment, ApplyMode mode) {
		return apply_to_words(w1, w2, alignment, mode, bit_xor, false);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public static Word xor(Word w1, Word w2, int alignment, boolean fill) {
		return apply_to_words(w1, w2, alignment, ApplyMode.INTERSECTION, bit_xor, fill);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public static Word xor(Word w1, Word w2, ApplyMode mode, boolean fill) {
		return apply_to_words(w1, w2, 0, mode, bit_xor, fill);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @return
	 */
	public static Word xor(Word w1, Word w2, int alignment) {
		return apply_to_words(w1, w2, alignment, ApplyMode.INTERSECTION, bit_xor, false);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @return
	 */
	public static Word xor(Word w1, Word w2, ApplyMode mode) {
		return apply_to_words(w1, w2, 0, mode, bit_xor, false);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public static Word xor(Word w1, Word w2, boolean fill) {
		return apply_to_words(w1, w2, 0, ApplyMode.INTERSECTION, bit_xor, fill);
	}
	/**
	 * Applique l'opération XOR au bit de w1 et w2, avec l'alignement et le mode désigné
	 * @param w1 première opérande
	 * @param w2 deuxième opérande
	 * @return
	 */
	public static Word xor(Word w1, Word w2) {
		return apply_to_words(w1, w2, 0, ApplyMode.INTERSECTION, bit_xor, false);
	}

	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, int aligment, ApplyMode mode, boolean fill) {
		Word w = apply_to_words(this, other, aligment, mode, fun, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche).
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, int aligment, ApplyMode mode) {
		Word w = apply_to_words(this, other, aligment, mode, fun, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, int aligment, boolean fill) {
		Word w = apply_to_words(this, other, aligment, ApplyMode.INTERSECTION, fun, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, ApplyMode mode, boolean fill) {
		Word w = apply_to_words(this, other, 0, mode, fun, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, int aligment) {
		Word w = apply_to_words(this, other, aligment, ApplyMode.INTERSECTION, fun, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, boolean fill) {
		Word w = apply_to_words(this, other, 0, ApplyMode.INTERSECTION, fun, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun, ApplyMode mode) {
		Word w = apply_to_words(this, other, 0, mode, fun, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'application de la fonction fun au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param fun fonction a appliquer sur les bit (par byte. s'il y a moins de bit que 8, ils seront tous à gauche). 
	 * @return
	 */
	public Word apply(Word other, ByteFunc fun) {
		Word w = apply_to_words(this, other, 0, ApplyMode.INTERSECTION, fun, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word xor(Word other, int aligment, ApplyMode mode, boolean fill) {
		Word w = apply_to_words(this, other, aligment, mode, bit_xor, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @return
	 */
	public Word xor(Word other, int aligment, ApplyMode mode) {
		Word w = apply_to_words(this, other, aligment, mode, bit_xor, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word xor(Word other, int aligment, boolean fill) {
		Word w = apply_to_words(this, other, aligment, ApplyMode.INTERSECTION, bit_xor, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word xor(Word other, ApplyMode mode, boolean fill) {
		Word w = apply_to_words(this, other, 0, mode, bit_xor, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
 	 * @param alignment position du premier bit du deuxième mot par rapport au premier bit du premier mot
	 * 0 indique que les bits de gauche sont alignés, autrement décale le deuxième mot de x bits vers la droite (vers la gauche si négatif)
	 * @return
	 */
	public Word xor(Word other, int aligment) {
		Word w = apply_to_words(this, other, aligment, ApplyMode.INTERSECTION, bit_xor, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param fill valeur a utilisé pour les bits manquants
	 * @return
	 */
	public Word xor(Word other, boolean fill) {
		Word w = apply_to_words(this, other, 0, ApplyMode.INTERSECTION, bit_xor, fill);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @param mode indique quel bits seront utilisé pour calculer la réponse. Si le mode demande d'utiliser un bit qui n'existe pas dans un des mot, la valeur de fill sera utilisé pour ce bit
	 * @return
	 */
	public Word xor(Word other, ApplyMode mode) {
		Word w = apply_to_words(this, other, 0, mode, bit_xor, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}
	/**
	 * assigne le résultat de l'opération XOR au bit de ce mot et other, avec l'alignement et le mode désigné
	 * @param other deuxième opérande
	 * @return
	 */
	public Word xor(Word other) {
		Word w = apply_to_words(this, other, 0, ApplyMode.INTERSECTION, bit_xor, false);
		this.array = w.array; // on vol les données de w
		this.length = w.length;
		return this;
	}



	/**
	 * Shift les bits du mot vers la droite.
	 * Modifie ce mot
	 * @param amount nombre de places que les bits seront shiftés vers la droite (gauche si négatif)
	 * @param val valeur des bits entrant. 0 par défaut
	 * @return
	 */
	public Word shift(int amount, boolean val) {
		this.array = shift(this.array, this.length, amount, val);
		return this;
	}
	/**
	 * Shift les bits du mot vers la droite.
	 * Modifie ce mot
	 * @param amount nombre de places que les bits seront shiftés vers la droite (gauche si négatif)
	 * @return
	 */
	public Word shift(int amount) {
		this.array = shift(this.array, this.length, amount, false);
		return this;
	}
	/**
	 * Retourne un nouveau mot correspondant à src mais avec tout les bits shifter de amount places vers la droite (gauche si negatif). La longueur du mot ne change pas
	 * @param src
	 * @param amount
	 * @return
	 */
	public static Word shift(Word src, int amount, boolean val) {
		byte[] arr = shift(src.array, src.length, amount, val);
		return new Word(arr, src.length);
	}
	/**
	 * Retourne un nouveau mot correspondant à src mais avec tout les bits shifter de amount places vers la droite (gauche si negatif), utilisant la valeur de val pour les bit entrant. La longueur du mot ne change pas
	 * @param src
	 * @param amount
	 * @return
	 */
	public static Word shift(Word src, int amount) {
		byte[] arr = shift(src.array, src.length, amount, false);
		return new Word(arr, src.length);
	}
	/**
	 * Retourne un nouveau tableau de byte avec tout les bits shifter de amount places vers la droite (gauche si negatif), utilisant la valeur de fill pour les bit entrant
	 * @param src
	 * @param len
	 * @param amount
	 * @param fill
	 * @return
	 */
	private static byte[] shift(byte[] src, int len, int amount, boolean fill) {
		if (amount == 0) return src;
		byte[] arr = new byte[src.length];
		// un peu d'optimisation
		if (Math.abs(amount) >= len) { // on shift tout les bits qu'on avait alors on peut juste memset tout
			if (fill) Arrays.fill(arr, (byte)255);
			// si val == false, bin arr est déjà init à 0 alors on n'a pas besoins de faire quoi que ce soit
		} else {
			for (int i=0; i < arr.length; i+=1) {
				byte b1 = byte_at(src, len, i*8 + amount, fill).value;
				arr[i] = (byte)(b1 & bit_mask(len, i*8, i*9));
			}
		}
		return src;
	}

	/**
	 * Cherche pattern dans le mot à partir du bit at (inclusif) et, s'il trouve, retourne la position du premier bit de la première instance, sinon retourne empty
	 * @param pattern
	 * @return
	 */
	public Optional<Integer> find(Word pattern, int at) {
		if (pattern == null) throw new NullPointerException(); // méchant
		if (at < 0) at = 0; // il n'y a rien avant
		if (pattern.length == 0) return Optional.of(at); // il y a toujours des chaines vide
		if (at >= this.length) return Optional.empty();
		// pas 10 manieres de le faire...
		outer:
		while (at-this.length < 0) {
			// deux situation: on est aligné sur un byte (facile) ou non (moins facile)
			if (at%8 == 0) {
				// ici on peut checker un byte à la fois au lieu de bit par bit
				int count = 0;
				while (count < pattern.array.length) {
					int mask = first_bits_mask(Math.min(8, pattern.array.length - count*8));
					int b1 = this.array[at/8+count] & mask;
					int b2 = pattern.array[count] & mask;
					if (b1 != b2) {
						at += 1;
						continue outer;
					}
					count += 1;
				}
				return Optional.of(at);
			} else {
				// similaire a avant, mais bit par bit
				int count = 0;
				while (count < pattern.length) {
					boolean b1 = this.getBitAt(at + count);
					boolean b2 = pattern.getBitAt(at + count);
					if (b1^b2) { // b1 et b2 sot different
						at += 1;
						continue outer;
					}
					count += 1;
				}
				return Optional.of(at);
			}
		}
		return Optional.empty();
	}
	/**
	 * Cherche pattern dans le mot et, s'il trouve, retourne la position du premier bit de la première instance, sinon retourne empty
	 * @param pattern
	 * @return
	 */
	public Optional<Integer> find(Word pattern) { // recherche à partir de position 0;
		return this.find(pattern, 0);
	}

	/**
	 * inverse la position des len premiers bits de orig
	 * @param orig
	 * @param len
	 * @return
	 */
	private static byte[] reverse(byte[] orig, int len) {
		// on veur inverser l'ordre des bytes, et les bits dans chaque byte
		byte[] arr = new byte[orig.length];
		for (int i=0; i < arr.length; i+=1) {
			byte b = byte_at(arr, len, len-(8*i)-7, false).value; // on se fou de la longueur
			// le dernier byte vas avoir des 0 en face, et on veut que ceux-ci ce retrouve à la fin, alors on ne 
			// spécifie pas le nombre de bits pertinents
			arr[i] = reverse(b, Math.min(8, len-i*8));
		}
		return arr;
	}
	/**
	 * Retourne un mot correspondant à src, mais avec les positions des bits inversées
	 * @param src
	 * @return
	 */
	public static Word reverse(Word src) {
		byte[] arr = reverse(src.array, src.length);
		return new Word(arr, src.length);
	}
	/**
	 * Inverse la position des bits du mot (le bit 0 va en dernière pos et vice-versa, etc.)
	 * Modifie ce mot
	 * @return this
	 */
	public Word reverse() {
		this.array = reverse(this.array, this.length);
		return this;
	}

	/**
	 * Transforme un byte en String au format binaire avec une longueur len
	 * @param b
	 * @return
	 */
	public static String stringifyByte(byte b, byte len) {
		if (len < 0 || len > 8) throw new IllegalArgumentException("la longueur doit être entre 0 et 8 bits. current: " + len);
		char[] cs = new char[len];
		int count = 0;
		while (count < len) {
			cs[count] = (b & (128 >>> count)) == 0? '0' : '1';
			count += 1;
		}
		return String.valueOf(cs);
	}
	/**
	 * Retourne la représentation binaire du byte donné. Montre toujours les 8 bits.
	 * @param b
	 * @return
	 */
	public static String stringifyByte(byte b) { return stringifyByte(b, (byte)8); }
	@Override
	public String toString() {
		/*
		String s = "";
		for (int i=0; i<this.array.length; i+=1) {
			if (i != 0) s += " ";
			if (i == this.array.length-1) {
				s += stringifyByte(this.array[i], (byte)(this.length%8));
			} else {
				s += stringifyByte(this.array[i]);
			}
		}
		return s;
		*/
		return this.toString(0);
	}
	/**
	 * transforme le mot en String avec un espace entre chaque byte, mais rajoute un nombre de "bit" vide avant pour permettre un alignement avec d'autre mots
	 * @param offset
	 * @return
	 */
	public String toString(int offset) {
		String s = " ".repeat(offset+(offset/8));
		int o = offset%8;
		int count = 0;
		while (count < this.length) {
			if (count != 0) s += " ";
			BitsRequest bs = n_bits_at(this.array, this.length, count, count==0? 8-o : 8, false);
			s += stringifyByte(bs.value, bs.len);
			count += bs.len;
		}
		return s;
	}

	/**
	 * @return retourne la position du premier 1 du mot. Empty si le mot n'a pas de 1
	 */
	public Optional<Integer> firstOne() {
		return this.firstOne(0, this.length);
	}
	/**
	 * @return retourne la position du premier 1 du mot à partir du bit from (inclusif)
	 */
	public Optional<Integer> firstOne(int from) {
		return this.firstOne(from, this.length);
	}
	/**
	 * Retourne la position du premier 1 du mot entre les bit from (inclusif) et to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public Optional<Integer> firstOne(int from, int to) {
		if (this.length == 0) return Optional.empty();
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from > to) return this.firstOne(to, from);

		for (int i=from/8; i <= (to-1)/8; i+=1) {
			int v = this.array[i] & 255; // bitMask(from, to)
			int n = Integer.numberOfLeadingZeros(v)-24; if (n < 0) n = 0;
			if (n < 8 ) {
				int at = i*8 + n;
				return at >= from && at < to? Optional.of(at) : Optional.empty();
			}
		}
		return Optional.empty();
	}
	

	/**
	 * @return retourne la position du dernier 1 du mot. Empty si le mot n'a pas de 0
	 */
	public Optional<Integer> lastOne() {
		return this.lastOne(0, this.length);
	}
	/**
	 * @return retourne la position du dernier 1 du mot à partir du bit from (inclusif)
	 */
	public Optional<Integer> lastOne(int from) {
		return this.lastOne(from, this.length);
	}
	/**
	 * Retourne la position du dernier 1 du mot entre les bit from (inclusif) et to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public Optional<Integer> lastOne(int from, int to) {
		if (this.length == 0) return Optional.empty();
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from > to) return this.firstOne(to, from);

		for (int i=(to-1)/8; i >= from/8; i-=1) {
			int v = this.array[i] & bitMask(i*8, to); // bitMask(from, to)
			int n = Integer.numberOfTrailingZeros(v);
			if (n < 8 ) {
				int at = i*8 + (7-n);
				//System.out.println("at: " + at + "; from: " + from + "; to: " + to);
				return at >= from && at < to? Optional.of(at) : Optional.empty();
			}
		}
		return Optional.empty();
	}
	/**
	 * @return retourne la position du premier 0 du mot. Empty si le mot n'a pas de 0
	 */
	public Optional<Integer> firstZero() {
		return this.firstZero(0, this.length);
	}
	/**
	 * @return retourne la position du premier 0 du mot à partir du bit from (inclusif)
	 */
	public Optional<Integer> firstZero(int from) {
		return this.firstZero(from, this.length);
	}
	/**
	 * Retourne la position du premier 0 du mot entre les bit from (inclusif) et to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public Optional<Integer> firstZero(int from, int to) {
		if (this.length == 0) return Optional.empty();
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from > to) return this.firstOne(to, from);

		for (int i=from/8; i <= to/8; i+=1) {
			int v = (~this.array[i]) & 255; // bitMask(from, to)
			int n = Integer.numberOfLeadingZeros(v)-24; if (n < 0) n = 0;
			if (n < 8 ) {
				int at = i*8 + n;
				return at >= from && at < to? Optional.of(at) : Optional.empty();
			}
		}
		return Optional.empty();
	}
	/**
	 * @return retourne la position du dernier 0 du mot. Empty si le mot n'a pas de 0
	 */
	public Optional<Integer> lastZero() {
		return this.lastZero(0, this.length);
	}
	/**
	 * @return retourne la position du dernier 0 du mot à partir du bit from (inclusif)
	 */
	public Optional<Integer> lastZero(int from) {
		return this.lastZero(from, this.length);
	}
	/**
	 * Retourne la position du dernier 0 du mot entre les bit from (inclusif) et to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public Optional<Integer> lastZero(int from, int to) {
		if (this.length == 0) return Optional.empty();
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from > to) return this.firstOne(to, from);

		for (int i=(to-1)/8; i >= from/8; i-=1) {
			int v = (~this.array[i]) & bitMask(i*8, to); // bitMask(from, to)
			int n = Integer.numberOfTrailingZeros(v);
			if (n < 8 ) {
				int at = i*8 + 7-n;
				return at >= from && at < to? Optional.of(at) : Optional.empty();
			}
		}
		return Optional.empty();
	}
	/**
	 * Compte le nombre de 1 dans le mot
	 * @return
	 */
	public int countOne() {
		return this.countOne(0, this.length);
	}
	/**
	 * Compte le nombre de 1 à partir du bit from (inclusif)
	 * @param from
	 * @return
	 */
	public int countOne(int from) {
		return this.countOne(from, this.length);
	}
	/**
	 * Compte le nombre de 1 entre le bit from (inclusif) et le bit to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public int countOne(int from, int to) {
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from < to) return this.countOne(to, from);

		int count = 0;
		for (int i=from; i < to; i += 1) {
			count += Integer.bitCount(this.array[i]&255);
		}
		return count;
	}
	/**
	 * Compte le nombre de 0 dans le mot
	 * @return
	 */
	public int countZero() {
		return this.countZero(0, this.length);
	}
	/**
	 * Compte le nombre de 0 à partir du bit from (inclusif)
	 * @param from
	 * @return
	 */
	public int countZero(int from) {
		return countZero(from, this.length);
	}
	/**
	 * Compte le nombre de 0 entre le bit from (inclusif) et le bit to (exclusif)
	 * @param from
	 * @param to
	 * @return
	 */
	public int countZero(int from, int to) {
		from = Math.max(0, from);
		to = Math.min(this.length, to);
		if (from < to) return this.countOne(to, from);

		int count = 0;
		for (int i=from; i < to; i += 1) {
			count += Integer.bitCount((~this.array[i])&255);
		}
		return count;
	}

	/**
	 * Longueur du tableau de byte interne
	 * @return
	 */
	public int arrayLen(){return this.array.length;}
	/**
	 * Nombre de bits dans la séquence
	 * @return
	 */
	public int size() {return this.length;}

	public boolean equals(Word other) {
		if (this.length == other.length) {
			for (int i=0; i < this.array.length; i++) {
				if (i == this.array.length - 1) {
					int offset = this.length - 8*i;
					int mask = 256-(256>>>offset); // les i premiers bits
					//System.out.println("last byte (len " + offset + ") #" + stringify_full_byte((byte)mask) + " " + this.array[i] + "|" + other.array[i]);
					if ((byte)(this.array[i]&mask) != (byte)(other.array[i]&mask)) return false;
				} else if (this.array[i] != other.array[i]) { 
					//System.out.println("Byte " + i + " !=");
					return false; 
				} else {
					//System.out.println("Byte " + i + " ==");
				}
			}
			return true;
		}
		return false;
	}
	/**
	 * utilise les premier bits comme hash
	 * @return
	 */
	@Override
	public int hashCode(){
		int hash = 0;
		int iter = Math.min(4, this.array.length);
		for (int i = 0; i < iter; i ++) {
			int shift = i == this.array.length-1? this.length%8 : 8;
			//System.out.println("shift: " + shift);
			hash = hash << shift;
			//System.out.println("shifted hash: " + String.format("%32s", Integer.toBinaryString(hash)).replace(' ', '0'));
			int v = (this.array[i]&255) >>> (8-shift);
			//System.out.println("value:        " + String.format("%32s", Integer.toBinaryString(v)).replace(' ', '0'));
			hash = hash | v;
			//System.out.println("new hash:     " + String.format("%32s", Integer.toBinaryString(hash)).replace(' ', '0'));
		}
		return hash;
	}
	/**
	 * duplique ce mot en copiant le tableau de byte interne
	 */
	@Override
	public Word clone() {
		return new Word(this);
	}

	/**
	 * Retourne un tableau de byte dont les bits sont les mêmes que ceux du mot
	 * @return
	 */
	public byte[] toByteArray(){
		byte[] arr = new byte[this.array.length];
		System.arraycopy(this.array, 0, arr, 0, this.array.length);
		return arr;
	}

	/**
	 * Ordre lexicographique, mot vide est 0
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(Word o) {
		if (this.length < o.length) return -1;
		else if (this.length > o.length) return 1;
		else {
			for (int i=0; i < this.array.length; i++) {
				int shift = i == this.array.length ? this.length%8 : 0;
				int vt = (this.array[i]&255) >>> shift;
				int vo = (o.array[i]&255) >>> shift;
				int comp = Integer.compare(vt, vo);
				if (comp != 0) {
					return comp;
				}
			}
			return 0;
		}
	}
	/**
	 * Retourne la position de ce mot dans l'ordre lexicographique
	 */
	public long order() {
		if (this.length == 0) return 0;
		// ordre du premier mot de taille length
		long start = (1 << this.length) - 1;
		// l'offset du mot par rapport à start est simplement lui même
		// mais puisqu'on est sur un long, on est quand même limité
		// algo ressemble à celui pour le hash
		long offset = 0;
		int iter = Math.min(8, this.array.length);
		for (int i = 0; i < iter; i ++) {
			int shift = i == this.array.length-1? this.length%8 : 8;
			offset = offset << shift;
			long v = (this.array[i]&255) >>> (8-shift);
			offset = offset | v;
		}
		return start + offset;
	}
	/**
	 * Calcul un mot à partir de l'ordre lexicographique donné
	 * @param order
	 * @return
	 */
	public static Word fromOrder(long order) {
		if (order == 0) return new Word();
		int len = (int)fast_log2(order+1);
		long start = (1 << len) - 1;
		long offset = order - (start);

		int number_of_bytes = len/8 + len%8 == 0? 0 : 1;
		byte[] arr = new byte[number_of_bytes];
		// aligner l'offset avec les bytes
		int shift = (8-len%8)%8;
		offset = offset << shift;
		for (int i = 0; i < number_of_bytes; i++) {
			byte b = (byte)(offset >>> 8*i);
			arr[number_of_bytes-1-i] = b;
		}

		return new Word(arr, len);
	}

	/* 
		calcul rapide du log2 (parce que java l'a pas)
	*/
	/**
	 * log en base 2 rapide sur un long
	 * @param num
	 * @return
	 */
	private static long fast_log2(long num) {
		if( num == 0 ) return 0;
    	return 63 - Long.numberOfLeadingZeros(num);
	}
	/**
	 * log en base 2 rapide sur un int
	 * @param num
	 * @return
	 */
	private static int fast_log2(int num) {
		if( num == 0 ) return 0;
    	return 31 - Integer.numberOfLeadingZeros(num);
	}

	/**
	 * masque n'ayant que les n premier bit (fait pour être utiliser avec des byte donc n'utilise que les 8 derniers bits)
	 * @param n
	 * @return
	 */
	private static int first_bits_mask(int n) {
		return 256 - (256>>>n);
	}
	/**
	 * masque n'ayant que les n dernier bit (fait pour être utiliser avec des byte donc n'utilise que les 8 derniers bits)
	 * @param n
	 * @return
	 */
	private static int last_bits_mask(int n) {
		return (1<<n) - 1;
	}
	/**
	 * retourne un masque des bits utilisé par le mot au byte commençant à at
	 * @param at
	 * @return
	 */
	public int bitMask(int at) {
		return bit_mask(this.length, at, at+8);
	}
	/**
	 * Créer un masque de 8 bits. les bits qui sont à des positions valide de ce mot (commençant au bit from (inclusif) jusqu'au bit to (exclusif)) sur une tranche de 8 bits sont vrai
	 * @param from position de début, par rapport à ce mot
	 * @param to position de fin, par rapport à ce mot
	 * @return
	 */
	public int bitMask (int from, int to) {
		return bit_mask(this.length, from, to);
	}
	/**
	 * Créer un masque ou seul le bit à la position at est vrai.
	 * Ne s'applique que sur les 8 derniers bits.
	 * le bit 0 est à gauche.
	 * @param at
	 * @return
	 */
	@SuppressWarnings("unused")
	public static int bit_mask_at(int at) { // on assume toujours que le premier bit est a gauche
		if (at <= 0 || at > 8) return 0;
		return 128 >>> (at-1);
	}
	/**
	 * Créer un masque ou seul le bit à la position at est vrai.
	 * Ne s'applique que sur les 8 derniers bits.
	 * le bit 0 est à droite.
	 * @param at
	 * @return
	 */
	@SuppressWarnings("unused")
	public static int bit_mask_at_inv(int at) { // comme précédent, mais le premier bit est a droite
		if (at <= 0 || at > 8) return 0;
		return 1 << (at-1);
	}
	/**
	 * Sur une tranche de 8 bits, indique quel bits entre from (inclusif) et to (exclusif) font parti d'un mot de longueur len
	 * @param len
	 * @param from
	 * @param to
	 * @return
	 */
	public static int bit_mask(int len, int from, int to) {
		int off_left = from < 0? -from : 0;
		if (off_left >= 8) return 0;
		to = to-from;
		if (to < 0) return 0;
		else if (to > 8-off_left) to = 8-off_left;
		int off_right = 8-to-off_left;
		if (off_right == 0) return 255 >>> off_left;
		return (255 >>> off_left+off_right) << off_right;
	}
	/**
	 *  inverse les len premiers bits du byte
	*/ 
	public static byte reverse(byte b, int len) {
		// puisque c'est pour usage interne, on va assumer que 0 <= len <= 8
		if (len == 0) return 0;
		if (b == 0 || b == (byte)255) return b; // il y a 16 'palindrome' mais j'ai pas envie de tester pour tout alors je ne fais que les plus simples
		int shift = 8-len;
		// 11100100 -> 00111001 -> 10011100 00000000 00000000 00000000 -> 00000000 00000000 00000000 10011100 -> 10011100
		return (byte)(Integer.reverseBytes((b&255) >>> shift) >>> 24);
	}
	/**
	 * inverse les bits du byte
	 * @param b
	 * @return
	 */
	@SuppressWarnings("unused")
	private static byte reverse(byte b) { // cas ou on sait que l'on veut inverser tout le byte alors pas besoins de faire toutes les manipulations
		if (b == 0 || b == (byte)255) return b;
		return (byte)(Integer.reverseBytes(b&255) >>> 24);
	}

	/**
	 * Donne le nombre nécessaire de byte pour stocker n bits
	 * @param n
	 * @return
	 */
	public static int nb_byte_for_n_bit(int n) {
		if (n < 0) throw new IllegalArgumentException("n ne peut pas être négatif");
		return n/8 + (n%8==0? 0 : 1);
	}

	/**
	 * Fonction prenant 2 bytes en paramètre
	 */
	public static interface ByteFunc {
		byte apply(byte a, byte b);
	}
	/**
	 * fonction prenant un byte en paramètre
	 */
	public static interface UnaryByteFunc {
		byte apply(byte b);
	}
	/**
	 * liste de fonction sur byte prédéfinies pour éviter de les recréer à chaque fois
	 */
	@SuppressWarnings("unused")
	public static final ByteFunc	bit_xor = (a, b) -> { return (byte)(a^b); },
									bit_and = (a, b) -> { return (byte)(a&b); },
									bit_or  = (a, b) -> { return (byte)(a|b); },
									bit_nota= (a, b) -> { return (byte)(~a); },
									bit_notb= (a, b) -> { return (byte)(~b); },
									bit_nand= (a, b) -> { return (byte)(~(a&b)); },
									bit_nor = (a, b) -> { return (byte)(~(a|b)); },
									bit_xnor= (a, b) -> { return (byte)(~(a^b)); },
									bit_xa  = (a, b) -> { return (byte)((~a)&b); },
									bit_impl= (a, b) -> { return (byte)(a&(~b)); },
									bit_ifth= (a, b) -> { return (byte)((~a)|b); },
									bit_thif= (a, b) -> { return (byte)((~b)|a); },
									bit_ida = (a, b) -> { return a; },
									bit_idb = (a, b) -> { return b; },
									bit_t   = (a, b) -> { return (byte)(255); },
									bit_f   = (a, b) -> { return 0; };

	/** 
	 * Iterateur sur les bits d'un mot. Possède des méthodes pour recueillir des byte, short, int ou long aussi
	*/
	public static class BitIterator implements Iterator<Boolean> {
		private Word src;
		private int at;
		private BitIterator (Word src) {
			this.src = src;
			this.at = 0;
		}
		private BitIterator (Word src, int at) {
			this.src = src;
			this.at = at;
		}
		/**
		 * Retourne true s'il reste des bits à l'iterateur, sinon false
		 * @return
		 */
		@Override
		public boolean hasNext() {
			return this.at < this.src.length;
		}
		/**
		 * Retourne le nombre de bit restant à l'iterateur
		 * @return
		 */
		public int bitLeft() {
			return Math.max(0, this.src.length-this.at);
		}
		/**
		 * Retourne le prochain bit sous la forme d'un booléen, ou empty
		 * @return
		 */
		public Optional<Boolean> nextBit() {
			if (this.at < this.src.length) {
				boolean b = this.src.getBitAt(this.at);
				this.at += 1;
				return Optional.of(b);
			}
			return Optional.empty();
		}
		/**
		 * Retourne le prochain bit sous la forme d'un booléen, ou null si l'iterateur a terminé
		 * @return
		 */
		@Override
		public Boolean next() {
			return this.nextBit().orElse(null);
		}
		/**
		 * retourne les prochains 8 bits (alignement à gauche)
		 * @return Request comprenant les bits ainsi que le nombre de bits pertinent
		 */
		public Optional<Request<Byte>> nextByte() {
			if (this.at < this.src.length) {
				Request<Byte> br = this.src.getByteAt(this.at);
				this.at += 8;
				return Optional.of(br);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 16 bits (alignement à gauche)
		 * @return Request comprenant les bits ainsi que le nombre de bits pertinent
		 */
		public Optional<Request<Short>> nextShort() {
			if (this.at < this.src.length) {
				Request<Short> br = this.src.getShortAt(this.at);
				this.at += 16;
				return Optional.of(br);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 32 bits (alignement à gauche)
		 * @return Request comprenant les bits ainsi que le nombre de bits pertinent
		 */
		public Optional<Request<Integer>> nextInt() {
			if (this.at < this.src.length) {
				Request<Integer> br = this.src.getIntAt(this.at);
				this.at += 32;
				return Optional.of(br);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 64 bits (alignement à gauche)
		 * @return Request comprenant les bits ainsi que le nombre de bits pertinent
		 */
		public Optional<Request<Long>> nextLong() {
			if (this.at < this.src.length) {
				Request<Long> br = this.src.getLongAt(this.at);
				this.at += 64;
				return Optional.of(br);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 8 bits, interprété comme un entier
		 * @return
		 */
		public Optional<Byte> nextAsByte() {
			if (this.at < this.src.length) {
				byte b = this.src.asByte(this.at);
				this.at += 8;
				return Optional.of(b);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 16 bits, interprété comme un entier
		 * @return
		 */
		public Optional<Short> nextAsShort() {
			if (this.at < this.src.length) {
				short s = this.src.asShort(this.at);
				this.at += 16;
				return Optional.of(s);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 32 bits, interprété comme un entier
		 * @return
		 */
		public Optional<Integer> nextAsInt() {
			if (this.at < this.src.length) {
				int i = this.src.asInt(this.at);
				this.at += 32;
				return Optional.of(i);
			}
			return Optional.empty();
		}
		/**
		 * retourne les prochains 64 bits, interprété comme un entier
		 * @return
		 */
		public Optional<Long> nextAsLong() {
			if (this.at < this.src.length) {
				long l = this.src.asLong(this.at);
				this.at += 64;
				return Optional.of(l);
			}
			return Optional.empty();
		}

		/**
		 * cherche le pattern dans la chaine à partir de la position de l'iterateur et s'arrête au bit imédiatement après
		 * @param pattern
		 * @return
		 */
		public Optional<Integer> find(Word pattern) {
			Optional<Integer> res = this.src.find(pattern, this.at);
			this.at = res.isPresent()? res.get() + pattern.length : this.src.length;
			return res;
		}
		/**
		 * Avance l'iterateur de n position
		 * @param n
		 * @return
		 */
		public boolean skip(int n) {
			this.at += Math.max(0, n);
			return this.at < this.src.length;
		}
		/**
		 * Consume l'iterateur et retourne un mot composé des bits restant à l'iterateur
		 * @return
		 */
		public Word collect() {
			if (this.at >= this.src.length) return new Word();
			Word res = this.src.subWord(this.at, this.src.length);
			this.at = this.src.length;
			return  res;
		}
		/**
		 * Consume l'iterateur et retourne le nombre d'éléments qu'il restait
		 * @return
		 */
		public int count() {
			int n = Math.max(0, this.src.length - this.at);
			this.at = this.src.length;
			return n;
		}

	}
	/**
	 * Retourne un iterateur sur chaque bit de la chaine
	 * @return
	 */
	@Override
	public BitIterator iterator() {
		return new BitIterator(this);
	}
	/**
	 * Retourne un iterateur sur chaque bit de la chaine, commençant à la position indiqué
	 * @param at
	 * @return
	 */
	public BitIterator iterator(int at) {
		return new BitIterator(this, at);
	}
	
}