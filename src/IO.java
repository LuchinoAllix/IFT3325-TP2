import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Queue;

/**
 * Classe gérant l'échange de trame d'un point à l'autre
 * Tu lui fournit deux stream pour les bits sortant et entrant et elle va s'occuper d'attendre les trames et de récupérer les msg.
 * Ne ferme pas les streams originaux à la fin alors tu dois t'en occuper toi même
 */
public class IO {
	/**
	 * Status de la patente
	 */
	public static enum Status {
		/**
		 * En attente d'une demande de réception
		 */
		NEW, 
		/**
		 * A envoyer une demande de connexion et attend la réponse
		*/
		WAITING, 
		/**
		 * Est présentement connecté
		*/
		CONNECTED, 
		/**
		 * La connexion est terminé
		*/
		CLOSED}
	
	//public static enum Role {SERVER, CLIENT}
	/**
	 * Mode pour la reception des trame I
	 * S'occupe de la logique d'ack et rej
	 */
	public static enum Mode {
		/**
		 * Go-Back-N
		 */
		GBN(7) {
			Trame mk_rejet(int n) { return Trame.rej(n); }
			void update_out(int n, IO self) {
				n = n%8;
				synchronized (self.out_lock) {
					// avec un rej, on conclu que l'on a recu la trame n-1 et les precedentes
					self.avancer_out
			(n);
					// on met out_at à n pour la renvoyer
					self.out_at = n;
				}
			}
			public boolean supporte(Mode m) {
				return switch (m) {
					case GBN -> true;
					default -> false;
				};
			}
			Trame update_in(IO self, Trame.I trame) {
				int n = trame.getNum().get();
				// vérifier que la trame est dans la fenetre pertinente
				if (IO.in(n, self.in_at, self.in_at+this.taille_fenetre)) {
					// vérifier que c'est la trame attendu
					if (n == self.in_at) {
						// dans ce mode, on n'utilise pas vraiment le buffer
						// ajouter le message au buffer
						synchronized (self.read_lock) {
							byte[] msg = trame.getMsg().get().toByteArray();
							int new_len = self.read_len + msg.length;
							byte[] new_buffer = new byte[new_len];
							System.arraycopy(self.read_buffer, self.read_at, new_buffer, 0, self.read_len);
							System.arraycopy(msg, 0, new_buffer, self.read_len, msg.length);
							self.read_at = 0;
							self.read_len = new_len;
							self.read_buffer = new_buffer;
						}
						// incr. l'attente
						synchronized (self.in_lock) {
							self.in_at = (n+1)%8;
							// envoyer une confirmation selon la place qu'il nous reste
							if (self.read_len >= IO.MAX_BYTES_IN_BUFFER) {
								return Trame.rnr(n);
							} else {
								return Trame.rr(n);
							}
						}
					} else {
						// on n'a pas a déplacer la fenêtre
						// on envoi un rejet
						return Trame.rej(self.in_at);
					}
				} else {
					// sinon on ignore
					return null;
				}
			}
			Trame.C open(){ return Trame.gbn(); }
		}, 
		/**
		 * Selective Reject
		 */
		SELECT(3) {
			@Override
			Trame mk_rejet(int n) { return Trame.srej(n); }
			@Override
			void update_out(int n, IO self) {
				n = n%8;
				synchronized (self.out_lock) {
					// avec un rej, on conclu que l'on a recu la trame n-1 et les precedentes
					self.avancer_out(n);
					// on ne change pas at
				}
				// renvoyer la trame dans les trames de ctrl
				self.queue_ctrl(self.out_buffer[n]);
			}
			@Override
			public boolean supporte(Mode m) {
				return switch (m) {
					case GBN -> true;
					case SELECT -> true;
					default -> false;
				};
			}
			@Override
			Trame update_in(IO self, Trame.I trame) {
				int n = trame.getNum().get()%8;
				// vérifier que la trame est dans la fenetre pertinente
				if (IO.in(n, self.in_at, self.in_at+this.taille_fenetre)) {
					// vérifier que c'est la trame attendu
					if (n == self.in_at) {
						// ajouter le message au buffer (et tout les recus apres)
						synchronized (self.in_lock) {
							self.in_buffer[n] = trame;
							while ((trame = self.in_buffer[n]) != null) {
								// on va créer beaucoup d'array mais bon
								synchronized (self.read_lock) {
									byte[] msg = trame.getMsg().get().toByteArray();
									int new_len = self.read_len + msg.length;
									byte[] new_buffer = new byte[new_len];
									System.arraycopy(self.read_buffer, self.read_at, new_buffer, 0, self.read_len);
									System.arraycopy(msg, 0, new_buffer, self.read_len, msg.length);
									self.read_at = 0;
									self.read_len = new_len;
									self.read_buffer = new_buffer;
								}
								self.in_buffer[n] = null;
								n = (n+1)%8;
							}
							// incrémenter l'attente
							self.in_at = n;
							// parce qu'on efface les trames lu au fur et à mesure, on a pas besoins de les effacers ici
						}
						// envoyer une confirmation selon la place qu'il nous reste
						n = n-1; // précédent
						n = n<0? n+8 : n;
						if (self.read_len >= IO.MAX_BYTES_IN_BUFFER) {
							return Trame.rnr(n);
						} else {
							return Trame.rr(n);
						}
					} else {
						// on veut ajouter cette trame au buffer
						synchronized (self.in_lock) {
							self.in_buffer[n] = trame;
						}
						// on avance rien, mais on veut envoyer un srej
						return Trame.srej(n);
					}
				} else {
					// sinon on ignore
					return null;
				}
			}
			Trame.C open(){ return Trame.selectiveRej(); }
		};
		/**
		 * Taille de la fenêtre d'envoi
		 */
		public final int taille_fenetre;
		private Mode(int n) { this.taille_fenetre = n;}
		/**
		 * Créer une trame de rejet pour la trame n
		 * @param n
		 * @return
		 */
		abstract Trame mk_rejet(int n);
		/**
		 * Outil pour la réception d'un ack. met à jour la fenêtre d'envoi
		 * @param n
		 * @param self
		 */
		abstract void update_out(int n, IO self);
		/**
		 * Indique si la méthode de rejet d'un autre mode est supporté
		 * @param m
		 * @return
		 */
		public abstract boolean supporte(Mode m);
		/**
		 * Outil pour la réception d'une trame I.
		 * Vérifie que la trame est désiré, ajoute le contenue au buffer de lecture et met à jour le buffer/fenêtre de réception
		 * @param self
		 * @param trame
		 * @return
		 */
		abstract Trame update_in(IO self, Trame.I trame);
		/**
		 * Retourne la trame demandant une connexion avec ce mode
		 * @return
		 */
		abstract Trame.C open();
	}
	private static final int MAX_BYTES_IN_BUFFER = 4096; // le nombre maximal de bytes qu'on met dans le buffer avant d'envoyer un RNR
	private static final int MAX_I_TRAME_SIZE = 1024;
	private static final Word BIT_INV = new Word("11111111");
	private static final Word FLAG = new Word("01111110");
	
	private Status status = Status.NEW;
	private Mode mode = null;
	private boolean kill = false; // si vrai, on arrête tout même s'il nous restait des choses à envoyer
	private boolean can_send = true; // vrai lorsque le récepteur peut recevoir plus de trame I
	
	private Object read_lock = new Lock(); // un objet quelqu'onque pour verouiller le buffer de lecture
	private byte[] read_buffer = new byte[0]; // tableau contenant les bytes qui peuvent être lu de ce IO
	private int read_at = 0; // byte auquel on est rendu
	private int read_len = 0; // nb de bytes qu'il nous reste à lire
	private IOInputStream read_stream; // pour lires les bytes reçu dans les trames

	private Object write_lock = new Lock(); // verou pour l'écriture
	private byte[] write_buffer = new byte[0]; // buffer de byte à envoyer
	private int write_at = 0; //  prochain byte à écrire
	private int write_len = 0; // nombre de byte restant à écrire
	private IOOutputStream write_stream; // pour rajouter des bytes à envoyer dans les trames

	private InputStream in_Stream; // pour lire les trames entrantes
	private int in_at = 0; // trame attendu
	private Trame.I[] in_buffer = new Trame.I[8]; // buffer de trames entrantes; pour selective reject
	private Object in_lock = new Lock();
	private Thread in_thread;

	private OutputStream out_stream; // pour écrire les trames sortantes
	private Object out_lock = new Lock();
	private Trame[] out_buffer = new Trame[8]; // contient les trames sortantes qu'on envoie présentement
	private int out_start = 0; // première trame de la fenêtre courante
	private Integer out_at = 0; // prochaine trame I à envoyer; est un Integer au lieu de int pour pouvoir l'utiliser comme mutex
	private Queue<Trame> out_ctrl_queue = new ArrayDeque<>(); // queue pour les trames de controle
	private Thread out_thread;
	private final float chance_bit_errone; // probabilité (0.0-1.0) qu'un bit envoyé soit à la place déterminé aléatoirement
	private final boolean skip_erreur; // flag pour rapidement éviter les erreurs

	public IO(InputStream input, OutputStream output, float chance_bit_errone) {
		if (chance_bit_errone < 0) chance_bit_errone = 0;
		else if (chance_bit_errone > 1) chance_bit_errone = 1;
		this.chance_bit_errone = chance_bit_errone;
		this.skip_erreur = this.chance_bit_errone == 0;
		this.out_stream = output;
		this.in_Stream = input;
		this.in_thread = new InputThread(this);
		this.out_thread = new OutputThread(this);
		this.in_thread.start();
		this.out_thread.start();
	}
	public IO(InputStream input, OutputStream output) { this(input, output, 0); }

	/**
	 * fonction s'occupant de l'envoie de toutes les trames
	*/ 
	private void send() {
		// même si on ferme la connection, on veut être certain que l'on a envoyé tout
		try {
			do {
				if (this.kill) break;
				// Étape 1: envoyer toutes les trames de controle
				synchronized (this.out_ctrl_queue) {
					while (!this.out_ctrl_queue.isEmpty()) {
						Trame trame = this.out_ctrl_queue.poll();
						send_trame(trame);
					}
				}
				
				// Étape 2: envoyer toutes les trames I
				if (this.can_send && this.mode != null) {
					synchronized (this.out_lock) {
						while (this.out_at != (this.out_start + this.mode.taille_fenetre)) {
							Trame t = this.out_buffer[this.out_at];
							if (t == null) { // si t == null, alors on peut remplir avec des trames de la queue
								Optional<Trame> next = mk_prochaine_trame();
								t = this.out_buffer[this.out_at] = next.orElse(null);
								if (t == null) break; // si t est encore null, on a plus rien a envoyer alors on quitte la boucle
							}
							send_trame(t);
							this.out_at += 1;
						}
					}
				}
			} while (this.status != Status.CLOSED);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			// ferme le in_Stream
			try {
				if (in_Stream != null) in_Stream.close();
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				in_Stream = null;
			}
			// signale qu'on peut arrêter
			synchronized (this) {
				this.notifyAll();
			}
			synchronized(this.out_lock) {
				this.out_lock.notifyAll();
				this.out_buffer = null;
			}
		}
		
	}
	
	/**
	 * Boucle s'occupant de recevoir toutes les trames entrantes
	 */
	private void receive() {
		try {
			do  {
				if (this.kill) break;
				try {
					Optional<Trame> t = read_next_trame();
					if (!t.isPresent()) { // stream fermé, on quitte
						this.status = Status.CLOSED;
					} else {
						receive(t.get());
					}
				} catch (Trame.TrameException e) {
					// ignore la trame
				}
			} while (this.status == Status.CLOSED);
		} catch (IOException e) {
			// erreur avec le socket, on ferme
			this.status = Status.CLOSED;
			System.err.println(e);
		} finally {
			// ferme le in_Stream
			try {
				if (out_stream != null) out_stream.close();
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				out_stream = null;
			}
			// signale qu'on peut arrêter
			synchronized (this) {
				this.notifyAll();
			}
			synchronized(this.in_lock) {
				this.in_lock.notifyAll();
				this.in_buffer = null;
			}
		}
		

	}
	/** gère la réception des trames de type inconnu
	 * Ne devrait pas être appelé
	*/
	private boolean receive(Trame t) throws Trame.TrameException {
		// on ignore
		return false;
	}
	/**
	 * S'occupe de recevoir les ACK et d'avancer la fenêtre d'envoie
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	private boolean receive(Trame.A t) throws Trame.TrameException {
		if (this.status == Status.NEW) { return false; } // ignore
		if (this.status == Status.WAITING) { this.status = Status.CONNECTED; } // confirmation de la connection 
		int n = t.getNum().get();
		// on assume qu'on ne recoit pas d'ack pour une trame pas envoyer
		// 1. avancer la fenêtre
		// tite optimisation: si on n'a pas a déplacer la fenêtre, on fait juste quitter
		synchronized (this.out_lock) {
			avancer_out(n+1);
			this.can_send = t.ready();
		}
		// indiquer si l'on peut envoyer plus de trame
		return true;
	}
	/**
	 * Gère les demandes de connexion
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	private boolean receive(Trame.C t) throws Trame.TrameException {
		// si on est pas en attente de connexion, on ignore
		if (this.status == Status.NEW) {
			// on active la connexion et on envoi un P et un RR en confirmation
			this.status = Status.CONNECTED;
			queue_ctrl(Trame.rr(this.in_at));
			queue_ctrl(Trame.p());
			return true;
		} else {
			// ignore
			return false;
		}
	}
	/**
	 * Gère la réception d'une trame de fin de connexion
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	private boolean receive(Trame.F t) throws Trame.TrameException {
		if (this.status == Status.CONNECTED || this.status == Status.WAITING){
			this.status = Status.CLOSED;
			return true;
		}
		return false; // sinon ignore
	}
	/**
	 * Gère la réception de trame d'information, d'ajout des bytes au buffer et d'envoie de confirmation/rejet
	 * @param t
	 * @return
	 * @throws Trame.TrameException si la trame contient une erreur
	 */
	private boolean receive(Trame.I t) throws Trame.TrameException {
		if ((this.status != Status.NEW && this.status != Status.WAITING) || this.mode == null) { // si on n'a pas encore de connexion, on ignore les trames I
			// on délègue la logique au mode
			Trame ret = this.mode.update_in(this, t);
			// on envoie la trame par la queue de ctrl
			queue_ctrl(ret);
			return true;
		}
		return false;
	}
	/**
	 * Gère la réception d'une trame P
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	private boolean receive(Trame.P t) throws Trame.TrameException {
		// on fait juste le renvoyer si on est connecté
		if (this.status == Status.CONNECTED){
			queue_ctrl(Trame.p());
			return true;
		}
		// sinon ignore
		return false;
	}
	/**
	 * Gère la réception d'une trame de rejet
	 * @param t
	 * @return
	 * @throws Trame.TrameException si le mode de rejet demander est invalide
	 */
	private boolean receive(Trame.R t) throws Trame.TrameException {
		if (this.status == Status.WAITING) {
			this.status = Status.CONNECTED;
			synchronized (this) { this.notifyAll(); }
		}
		if (this.status == Status.CONNECTED) {
			int n = t.getNum().get();
			Mode m = t.selectif()? Mode.SELECT : Mode.GBN;
			if (this.mode == null || !this.mode.supporte(m)) throw new Trame.TrameException("Mode de rejet invalide");
			m.update_out(n, this);
			return true;
		}
		return false;
	}
	/**
	 * Lit la prochaine trame du stream entrant
	 * @return la trame, ou empty si la connexion est fermé
	 */
	private Optional<Trame> read_next_trame() throws IOException, Trame.TrameException {
		// assume qu'on est connecté
		
		int nb_of_ones = 0;
		// cherche le flag de début de trame
		while (true) { // on va sortir manuellement
			Optional<Boolean> bit = read_next_bit();
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
			curr_bit = read_next_bit();
			if (curr_bit.isEmpty()) {
				// ceci est une erreur, le stream a pris fin, mais on n'a pas recu toute la trame
				throw new IOException();
			}
			boolean b = curr_bit.get();
			if (nb_of_ones == 5 && !b) { // 0 de bit stuffing, on ignore
				nb_of_ones = 0;
				continue;
			} else if (nb_of_ones == 5) { // 6e 1, donc flag de fin ou erreur
				curr_bit = read_next_bit();
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
	private Optional<Boolean> read_next_bit() throws IOException{
		if (this.status == Status.CLOSED || this.kill) return Optional.empty();
		int b = this.in_Stream.read();
		if (b < 0) return Optional.empty();
		return Optional.of(b != 0);
	}
	/**
	 * ferme tout et lâche les ressources. Bloque le thread jusqu'à temps que tout soit fermé
	 * @return true lorsque tout est fermé
	 */
	private synchronized boolean close_all() {
		this.status = Status.CLOSED;
		while (this.in_Stream != null && this.out_stream != null) { // on attend
			try {
				this.wait();
			} catch (InterruptedException e) {}
		}
		// abandonne les autres ressources
		synchronized(this.read_lock) {
			this.read_lock.notifyAll();
			this.read_buffer = null;
		}
		synchronized(this.write_lock) {
			this.write_lock.notifyAll();
			this.write_buffer = null;
		}
		return true;
	}
	/**
	 * Envoie la trame sur le stream sortant
	 * @param trame trame à envoyer
	 * @throws IOException
	 */
	private void send_trame(Trame trame) throws IOException {
		if (this.status == Status.CLOSED || this.kill) return;
		Word bits = trame.encode(CRC.CRC_CCITT);
		send_wout_stuffing(FLAG);
		send_w_stuffing(bits);
		send_wout_stuffing(FLAG);
		send_wout_stuffing(BIT_INV); // on envoie des bits invalide pour bien séparer les trames
		out_stream.flush();
	}
	private Optional<Trame> mk_prochaine_trame() {
		while (this.status == Status.CONNECTED) synchronized (this.write_lock) {
			if (this.write_len > 0) { // on a des bytes a envoyer !!!
				int len = Math.max(MAX_I_TRAME_SIZE, this.write_len);
				byte[] bytes = new byte[len];
				System.arraycopy(this.write_buffer, this.write_at, bytes, 0, len);
				Word msg = new Word(bytes, len);
				this.write_at += len;
				this.write_len -= len;
				Trame t = Trame.i(this.out_at, msg);
				return Optional.of(t);
			}
		}
		return Optional.empty();
	}
	/**
	 * Envoie des bits avec du bit stuffing
	 * @param bits
	 * @throws IOException
	 */
	private void send_w_stuffing(Word bits) throws IOException {
		int at = 0;
		int number_of_one = 0;
		while (at < bits.length) {
			boolean b = bits.getBitAt(at);
			out_stream.write(write_bit(b));
			if (b) {
				number_of_one += 1;
				if (number_of_one == 5) {
					out_stream.write(write_bit(false));
					number_of_one = 0;
				}
			}
			at += 1;
		}
	}
	/**
	 * envoie des bits sans bit stuffing
	 * @param bits
	 * @throws IOException
	 */
	private void send_wout_stuffing(Word bits) throws IOException {
		int at = 0;
		while (at < bits.length) {
			boolean b = bits.getBitAt(at);
			out_stream.write(write_bit(b));
			at += 1;
		}
	}
	private int write_bit(boolean desire) {
		if (!this.skip_erreur && Math.random() <= this.chance_bit_errone) {
			desire = Math.random() <= 0.5? false : true;
		}
		return desire? 1 : 0;
	}

	/**
	 * Ajoute un trame de ctrl
	 * @param t
	 */
	private void queue_ctrl(Trame t) {
		synchronized (this.out_ctrl_queue) {
			this.out_ctrl_queue.add(t);
		}
	}

	/**
	 * Vérifie que n se trouve dans une tranche d'un anneau
	 * @param n
	 * @param start
	 * @param end
	 * @return
	 */
	private static boolean in(int n, int start, int end) {
		n = n%8; start = start%8; end = end%8;
		return end > start? n>=start && n<end : n>=start || n<end;
	}
	/**
	 * Avance la fenetre de sortie et efface les trames qui ne sont plus utile
	 * @param at
	 */
	private void avancer_out(int at) {
		at %= 8;
		if (this.out_start == at || this.mode == null) return;
		// on veut 'effacer' les trames du buffer sur lesquels la fenêtre avance
		// 1.1 trouver le nombre de trame à effacer
		int nb_a_effacer = this.out_start < at ? at-this.out_start : at-this.out_start;
		// 1.2 déplacer la fenêtre
		this.out_start = at;
		// 1.3 effacer les trames
		for (int i=1; i<=nb_a_effacer; i+=1) {
			int n = (this.out_start+this.mode.taille_fenetre-i)%8;
			if (n < 0) n += 8;
			this.out_buffer[n] = null;
		}
	}

	/**
	 * Tente d'ouvrir la connexion. Verouille le thread jusqu'à temps que la connexion réussisse ou échoue (échec automatique si aucune réponse après un certain délais)
	 * @param mode
	 * @return
	 */
	public synchronized boolean ouvreConnexion(Mode mode) {
		if (this.status == Status.NEW) {
			this.status = Status.WAITING;
			this.mode = mode;
			queue_ctrl(Trame.gbn());
			try {
				this.wait(3000);
			} catch (InterruptedException e) {}
			if (this.status == Status.CONNECTED) return true;
			else if (this.status == Status.WAITING) {
				this.status = Status.NEW;
			}
		}
		return false;
	}
	/**
	 * Ferme la connexion. verouille le thread jusqu'à ce qu'il soit fermé
	 * @return
	 */
	public boolean fermeConnexion() {
		close_all();
		return true;
	}
	public boolean estConnecte() {
		return this.status == Status.CONNECTED;
	}
	public Status geStatus() {
		return this.status;
	}
	public boolean estFerme() {
		return this.status == Status.CLOSED;
	}

	/**
	 * Retourne un InputStream bloquant permettant de lire les bytes reçu par ce IO
	 * Fermer le stream ferme ce IO
	 * @return
	 */
	public InputStream getInputStream() {
		if (this.read_stream == null) {
			this.read_stream = new IOInputStream(this);
		}
		return this.read_stream;
	}
	/**
	 * Retourne un OutputStream bloquant permettant d'écrire des bytes à envoyer
	 * Fermer le stream ferme ce IO
	 * @return
	 */
	public OutputStream getOutputStream() {
		if (this.write_stream == null) {
			this.write_stream = new IOOutputStream(this);
		}
		return this.write_stream;
	}

	private static class IOInputStream extends InputStream {
		private IO self;
		private IOInputStream(IO self) { this.self = self; }
		@Override
		public int read() throws IOException {
			if (self.status == Status.NEW || self.status == Status.WAITING) throw new IOException("Connexion pas encore ouverte");
			synchronized (self.read_buffer) {
				while (self.status == Status.CONNECTED) {
					if (self.read_len == 0) try {self.read_lock.wait();} catch (InterruptedException e) {}
					else {
						int ret = self.read_buffer[self.read_at];
						self.read_at += 1;
						self.read_len -= 1;
						return ret;
					}
				}
			}
			return -1;
		}
		@Override
		public int available() {
			return self.read_len;
		}
		@Override
		public void close() {
			self.close_all();
		}
	}
	private static class IOOutputStream extends OutputStream {
		private IO self;
		private IOOutputStream(IO self) { this.self = self; }
		@Override
		public void write(int b) throws IOException {
			if (self.status == Status.NEW || self.status == Status.WAITING) throw new IOException("Connexion pas encore ouverte");
			if (self.status == Status.CLOSED) throw new IOException("Connexion fermée");
			synchronized (self.write_lock) {
				// réarranger le buffer
				// si on n'a plus de place, il faut créer un nouveau, sinon on peut le réutiliser
				byte[] new_buffer = self.write_buffer.length == self.write_len? new byte[Math.max(1, self.write_buffer.length*2)] : self.write_buffer;
				// on regarde si on doit copier le buffer
				if (self.write_buffer.length == self.write_len || self.write_at != 0) {
					System.arraycopy(self.write_buffer, self.write_at, new_buffer, 0, self.write_len);
					self.write_at = 0;
				}
				// rajouter le byte
				new_buffer[self.write_len] = (byte)b;
				self.write_len += 1;
				self.write_buffer = new_buffer;

				self.write_lock.notifyAll();
			}
		}
		@Override
		public void close() {
			self.close_all();
		}
	}

	private static class InputThread extends Thread {
		private IO self;
		private InputThread(IO self) { 
			this.self = self; 
			this.setDaemon(true);
		}
		@Override
		public void run() {
			self.receive();
		}
		@Override
		public void interrupt() {
			super.interrupt();
			self.close_all();
		}
	}
	private static class OutputThread extends Thread {
		private IO self;
		private OutputThread(IO self) { 
			this.self = self; 
			this.setDaemon(true);
		}
		@Override
		public void run() {
			self.send();
		}
		@Override
		public void interrupt() {
			super.interrupt();
			self.close_all();
		}
	}

	private static class Lock {}
}
