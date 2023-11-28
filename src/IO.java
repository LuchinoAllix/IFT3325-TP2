import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Classe gérant l'échange de trame d'un point à l'autre
 * Tu lui fournit deux stream pour les bits sortant et entrant et elle va s'occuper d'attendre les trames et de récupérer les msg.
 * Ne ferme pas les streams originaux à la fin alors tu dois t'en occuper toi même.
 * 
 * À l'interne, utilise deux thread pour la lecture et l'écriture des signaux entrant/sortant. 
 * Les opérations sont synchronisées pour éviter que les threads se pile sur les pieds. 
 * L’envoi et la réception se fait bit par bit.
 * 
 * Un InputStream et OutputStream sont mis à la disposition de l'utilisateur pour écrire/lire des bytes sans se soucier des trames.
 * 
 * <h3>Envoi de trame</h3>
 * <p>
 * À chaque itération, le thread responsable d'écrire les trames a trois étapes:
 * </p>
 * <ol>
 * 	<li>Si on avait précedemment envoyer un RNR, mais que l'on peut maintenant recevoir plus de données, rajouter un RR à la queue de contrôle</li>
 * <li>Envoyer toutes les trames de contrôles dans la queue</li>
 * <li>Envoyer le plus de trame I possible</li>
 * </ol>
 * <p>
 * Pour créer la prochaine trame I, la méthode <code>mkNextTrame()</code> est appelé. Celle-ci prend jusqu'à 1024 bytes du buffer d'écriture comme message de trame. S'il n'y a plus de byte à envoyer, elle retourne rien et on passe à la prochaine itération.
 * </p>
 * <p>
 * L'envoi d'une trame se déroule comme suit:
 * </p>
 * <ol>
 * <li>La trame est encodé avec <code>CRC_CCITT</code> et transformé en chaîne de bits</li>
 * <li>n envoi (sans bit stuffing) le flag de début de trame</li>
 * <li>on envoi (avec bit stuffing) la chaîne</li>
 * <li>on envoi (sans bit stuffing) le flag de fin de trame</li>
 * <li>on envoi <code>11111111</code> pour bien délimiter --- Ça permet de ne pas inventer une trame s'il y avait une erreur dans la précédentes et qu'on avait abandonner la lecture</li>
 * </ol>
 * 
 * <h3>Réception de trame</h3>
 * <p>
 * La recherche de la prochaine trame reçu se déroule de la manière suivante:
 * </p>
 * <ol>
 * <li>On lit les bits reçu jusqu'à ce que l'on trouve le flag de début de trame</li>
 * <li>On récolte les bits de la trame un par un jusqu'à ce qu'on arrive au flag de fin de trame. On enlève les 0 de bit stuffing, et si l'on trouve sept 1 d'affiler, on lance une erreur et on rejette la trame (ça va simplement nous faire passer à la prochaine itération)</li>
 * <li>on décode les bits reçu. la vérification du FCD se fait en même temps et s'il y a une erreur on rejette la trame</li>
 * <li>la trame est traité selon son type, le mode de connexion et le status</li>
 * </ol>
 * 
 * <h3>Traitement d'une trame</h3>
 * <p>
 * Toute les trames sont ignorées si la connexion est fermé. 
 * Si le status est à NEW, toute les trames sont ignorées sauf les trames C. 
 * En status WAITING, tout est ignoré sauf les trames F et R.
 * </p>
 * <p>
 * Autrement, la trame est traité selon le son type et le mode.
 * </p>
 * <h4>A (RR et RNR)</h4>
 * <p>Lorsque l'on reçoit une trame A, les choses suivantes se passent:</p>
 * <ol>
 * <li>Si le status est WAITING, passer à CONNECTED</li>
 * <li>avancer la fenêtre d'envoi selon le numéro</li>
 * <li>indiquer que l'on peut envoyer d'autre trame ou non selon si c'est un RR ou RNR</li>
 * </ol>
 * <h4>C</h4>
 * <p>
 * On ignore ces trames sauf si on est en status NEW. Dans ce cas, on envoi un RR et un P initial en réponse.
 * </p>
 * <h4>F</h4>
 * <p>
 * lors de la réception d'un F, on ferme les streams et on arrête de lire/envoyer. Le status passe à CLOSED
 * </p>
 * <h4>P</h4>
 * <p>
 * À la réception d'un P, on ne fait qu'en envoyer un à notre tour
 * </p>
 * <h4>R (REJ et SREJ)</h4>
 * <p>
 * Lors de la réception d'un R, on passe la logique à notre mode. 
 * Dans le cas de GBN, on ne fait que déplacer la fenêtre d'envoi pour l'aligner avec la trame désirée afin de signaler que c'est la prochaine à envoyer.
 * </p>
 * <h4>I</h4>
 * <p>
 * Encore une fois, on passe la logique à notre mode. Dans le cas de GBN, on vérifie que le numéro de la trame est:
 * </p>
 * <ol>
 * <li>Dans la fenêtre de réception (Si elle ne l'est pas, on l'ignore)</li>
 * <li>La trame désirée</li>
 * </ol>
 * <p>
 * S'il s'agit de la bonne trame, on ajoute les données contenues à notre buffer de lecture, on avance la fenêtre de réception et on envoi un RR (ou un RNR si notre buffer est plein),
 * sinon, on envoi un REJ
 * </p>
 * 
 * <h3>Interface</h3>
 * <p>
 * Après la création de l'objet, il faut que l'un utilise la méthode <code>ouvreConnexion()</code> pour établir la connexion avec l'autre.
 * </p>
 * <p>
 * Une fois établi, l'objet donne accès à un <code>OutputStream</code> pour envoyer des données. 
 * Tout les bytes que ce stream reçoit sont ajouter à un buffer d'écriture et ils seront utilisés pour créer les trames I. 
 * De l'autre côté, il suffit de prendre l'<code>InputStream</code> que fourni l'objet afin d'y lire les bytes dans son buffer de lecture.
 * </p>
 * <p>
 * À la fin, il est important d'appeler la méthode <code>fermeConnexion</code> afin d'arrêter les threads et de libérer les ressources. 
 * IO ne ferme pas les streams qui lui sont donné au début. 
 * Il faudra donc les fermer vous même.
 * </p>
 * 
 * <h3>Temporisateur</h3>
 * <p>
 * IO possède également un troisème thread sous la forme d'un \verb#Timer# qui agit comme temporisateur. 
 * Trois différents renvoi sont gérer par le temporisateur présentement:
 * </p>
 * <ul>
 * <li>Le renvoi des P si tu es le serveur et que tu n'a pas reçu le P depuis un bon moment. Reset à chaque P reçu</li>
 * <li>Le renvoi de RR si tu attend des trames mais que tu n'en a pas reçu depuis un moment. Reset à chaque RR, REJ ou SREJ envoyé. Arrêté lors de l'envoi d'un RNR</li>
 * <li>Le renvoi des trames I à partir du début de la fenêtre si aucune réponse n'a été reçu. Reset à l'envoi d'un I, arrêter à la réception d'un RR, RNR, REJ et SREJ</li>
 * </ul>
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
	
	public static enum Role {SERVER, CLIENT}
	/**
	 * Mode pour la reception des trame I
	 * S'occupe de la logique d'ack et rej
	 */
	public static enum Mode {
		/**
		 * Go-Back-N
		 */
		GBN(7) {
			//Trame mk_rejet(int n) { return Trame.rej(n); }
			@Override
			void update_out(int n, IO self) {
				n = n%8;
				synchronized (self.out_lock) {
					// avec un rej, on conclu que l'on a recu la trame n-1 et les precedentes
					self.avancer_out(n);
					// on met out_at à n pour la renvoyer
					self.out_at = n;
				}
			}
			@Override
			public boolean supporte(Mode m) {
				return switch (m) {
					case GBN -> true;
					default -> false;
				};
			}
			@Override
			Trame update_in(IO self, Trame.I trame) {
				int n = trame.getNum().get();
				// vérifier que la trame est dans la fenetre pertinente
				if (IO.in(n, self.in_at, self.in_at+this.taille_fenetre)) {
					// vérifier que c'est la trame attendu
					if (n == self.in_at) {
						// dans ce mode, on n'utilise pas vraiment le buffer
						// ajouter le message au buffer
						self.logln("\tajout du msg au buffer");
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
								self.temporisateur.cancel(self.temp_ack);
								self.can_receive = false;
								self.logln("\tenvoi rnr("+n+")");
								return Trame.rnr(n);
							} else {
								self.temporisateur.reset(self.temp_ack);
								self.can_receive = true;
								self.logln("\tenvoi rr("+n+")");
								return Trame.rr(n);
							}
						}
					} else {
						// on n'a pas a déplacer la fenêtre
						// on envoi un rejet
						self.temporisateur.reset(self.temp_ack);
						synchronized (self.in_lock) { self.can_receive = true; }
						self.logln("\tpas la trame attendu");
						self.logln("\tenvoi rej("+n+")");
						return Trame.rej(self.in_at);
					}
				} else {
					// sinon on ignore
					self.logln("\tignore (trame déjà reçu)");
					return null;
				}
			}
			Trame.C open(){ return Trame.gbn(); }
		}, 
		/**
		 * Selective Reject
		 */
		SELECT(3) {
			//@Override
			//Trame mk_rejet(int n) { return Trame.srej(n); }
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
								self.logln("\tajout du message au buffer");
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
						//n = n-1; // précédent
						//n = n<0? n+8 : n;
						if (self.read_len >= IO.MAX_BYTES_IN_BUFFER) {
							self.temporisateur.cancel(self.temp_ack);
							self.can_receive = false;
							self.logln("\tenvoi rnr("+n+")");
							return Trame.rnr(n);
						} else {
							self.temporisateur.reset(self.temp_ack);
							self.can_receive = true;
							self.logln("\tenvoi rr("+n+")");
							return Trame.rr(n);
						}
					} else {
						// on veut ajouter cette trame au buffer
						self.logln("\tpas la trame attendu; ajout au buffer pour plus tard");
						synchronized (self.in_lock) {
							self.in_buffer[n] = trame;
							self.can_receive = true;
						}
						// on avance rien, mais on veut envoyer un srej
						self.temporisateur.reset(self.temp_ack);
						self.logln("\tenvoi srej("+n+")");
						return Trame.srej(n);
					}
				} else {
					// sinon on ignore
					self.logln("\tignore (trame déjà reçu)");
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
		//abstract Trame mk_rejet(int n);
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
		public static Mode fromNum(int n) {
			return switch (n) {
				case 0 -> Mode.GBN;
				case 1 -> Mode.SELECT;
				default -> throw new IllegalArgumentException("numéro invalide");
			};
		}
		public static int numOf(Mode m) {
			return switch (m) {
				case GBN -> 0;
				case SELECT -> 0;
			};
		}
		public int num() {return numOf(this);}
	}
	/**
	 * le nombre maximal de bytes qu'on met dans le buffer avant d'envoyer un RNR
	 */
	private static final int MAX_BYTES_IN_BUFFER = 4096;
	/**
	 * Nombre maximal de byte que l'on envoi en msg dans une trame
	 */
	private static final int MAX_I_TRAME_SIZE = 1024;
	/**
	 * un mot invalide qui va causer une erreur si lu
	 */
	private static final Word BIT_INV = new Word("11111111");
	/**
	 * Flag de début et fin de trame
	 */
	private static final Word FLAG = new Word("01111110");
	/**
	 * Délais, en millisecondes, avant que le temporisateur effectue une action
	 */
	private static final long DELAIS_TEMPORISATEUR = 3000;
	
	/**
	 * État de la connexion
	 */
	private Status status = Status.NEW;
	/**
	 * Mode pour le rejet de trame
	 */
	private Mode mode = null;
	/**
	 * si vrai, on arrête tout même s'il nous restait des choses à envoyer
	 */
	private boolean kill = false;
	/**
	 * vrai lorsque l'autre noeud peut recevoir plus de trame I
	 */
	private boolean can_send = true;
	/**
	 * vrai lorsque ce noeud peut recevoir plus de trame I
	 */
	private boolean can_receive = true;
	/**
	 * pour si on veut des traces. permet d'imprimer de manière personnalisé
	 */
	private Logger logger = null; 
	
	/**
	 * un objet quelqu'onque pour verouiller le buffer de lecture
	 */
	private Object read_lock = new Lock();
	/**
	 * tableau contenant les bytes qui peuvent être lu de ce IO
	 */
	private byte[] read_buffer = new byte[0];
	/**
	 * byte auquel on est rendu
	 */
	private int read_at = 0;
	/**
	 * nb de bytes qu'il nous reste à lire
	 */
	private int read_len = 0;
	/**
	 * pour lires les bytes reçu dans les trames
	 */
	private IOInputStream read_stream;

	/**
	 * verou pour l'écriture
	 */
	private Object write_lock = new Lock(); 
	/**
	 * buffer de byte à envoyer
	 */
	private byte[] write_buffer = new byte[0];
	/**
	 * position du prochain byte à écrire
	 */
	private int write_at = 0;
	/**
	 * nombre de byte restant à écrire.
	 */
	private int write_len = 0;
	/**
	 * Stream fourni à l'utilisateur pour envoyer des bytes qui seront mis dans des trames
	 */
	private IOOutputStream write_stream;

	/**
	 * pour lire les trames entrantes
	 */
	private InputStream in_stream;
	/**
	 * trame attendu
	 */
	private int in_at = 0;
	/**
	 * buffer de trames entrantes; pour selective reject
	 */
	private Trame.I[] in_buffer = new Trame.I[8];
	/**
	 * verou pour la réception de trame
	 */
	private Object in_lock = new Lock();
	/**
	 * Thread de réception
	 */
	private Thread in_thread;

	/**
	 * Stream pour écrire les trames sortantes
	 */
	private OutputStream out_stream;
	/**
	 * Verou pour la sortie de trame
	 */
	private Object out_lock = new Lock();
	/**
	 * contient les trames sortantes qu'on envoie présentement
	 */
	private Trame[] out_buffer = new Trame[8];
	/**
	 * première trame de la fenêtre courante
	 */
	private int out_start = 0;
	/**
	 * prochaine trame I à envoyer
	 */
	private int out_at = 0;
	/**
	 * Queue pour les trames de contrôles à envoyer
	 */
	private Queue<Trame> out_ctrl_queue = new ArrayDeque<>();
	/**
	 * Thread de sortie
	 */
	private Thread out_thread;
//	private final float chance_bit_errone; // probabilité (0.0-1.0) qu'un bit envoyé soit à la place déterminé aléatoirement
//	private final boolean skip_erreur; // flag pour rapidement éviter les erreurs

	/**
	 * Indique si ce noeud agit comme client ou serveur. 
	 * La différence est que c'est le serveur qui initie le ping-pong avec les trames P
	 */
	private Role role = null;
	/**
	 * Exécute différente tâche après un délais, notament le renvoi de trame pour lesquels on n'a pas eu de réponse
	 */
	private Temporisateur temporisateur = new Temporisateur();
	/**
	 * Marqueur de temporisateur pour l'envoi de trame P
	 */
	private Marker temp_p = new Marker(this) {
		@Override
		public void run() {
			self.queue_ctrl(Trame.p());
			self.temporisateur.reset(this);
			self.logln("N'a pas reçu P de puis un moment, renvoi");
		}
	};
	/**
	 * Marqueur de temporisateur pour le renvoi des trames I si on ne reçoit pas de ACK après un moment
	 */
	private Marker temp_send = new Marker(this) {
		@Override
		public void run() {
			synchronized (self.out_lock) {
				self.out_at = self.out_start;
			}
			self.logln("N'a pas reçu de ACK de puis un moment, renvoi les trames I");
		}
	};
	/**
	 * Marqueur de temporisateur pour le renvoi d'un RR si on attend des trames mais que nous n'en avons pas reçu depuis un moment
	 */
	private Marker temp_ack = new Marker(this) { // temporisateur pour l'envoi des acks
		@Override
		public void run() {
			Trame t = Trame.rr(self.in_at);
			self.queue_ctrl(t);
			self.temporisateur.reset(this);
			self.logln("N'a pas reçu de trame I de puis un moment, renvoi RR");
		}
	};

	/**
	 * Créer un noeud IO
	 * @param input InputStream sur lequel ce noeud lit les bits entrant
	 * @param output OutputStream sur lequel ce noeud écrit les bits sortant
	 */
	public IO(InputStream input, OutputStream output) {
//		if (chance_bit_errone < 0) chance_bit_errone = 0;
//		else if (chance_bit_errone > 1) chance_bit_errone = 1;
//		this.chance_bit_errone = chance_bit_errone;
//		this.skip_erreur = this.chance_bit_errone == 0;
		this.out_stream = output;
		this.in_stream = input;
		this.in_thread = new InputThread(this);
		this.out_thread = new OutputThread(this);
		this.in_thread.start();
		this.out_thread.start();
	}
//	public IO(InputStream input, OutputStream output) { this(input, output, 0); }

	/**
	 * fonction s'occupant de l'envoie de toutes les trames.
	 * 
	 * Boucle tant que la connexion existe.
	 * À chaque itération, envoi toutes les trames de contrôle dans la queue et le plus de trame I possible
	 * Créer de nouvelle trame I au besoins à partir du buffer d'écriture
	*/ 
	private void send() {
		try {
			do {
				if (this.kill) break;
				// Étape 0: si on peut recommencer à recevoir des trames, le signaler
				synchronized (this.in_lock) {
					if (!this.can_receive && this.read_len < IO.MAX_BYTES_IN_BUFFER) {
						this.can_receive = true;
						this.temporisateur.reset(temp_ack);
						queue_ctrl(Trame.rr(this.in_at));
						this.logln("Peut maintenant recevoir des trames I");
					}
				}
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
					this.temporisateur.reset(temp_send);
				}
			} while (this.status != Status.CLOSED);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			// ferme le in_stream
			this.logln("Ferme les stream sortants");
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
			synchronized(this.out_lock) {
				this.out_lock.notifyAll();
				this.out_buffer = null;
			}
		}
		this.close_all();
	}
	
	/**
	 * Boucle s'occupant de recevoir toutes les trames entrantes
	 * 
	 * Reçoit les trames, s'assure de leur intégrité puis les dispatch pour leur traitement
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
						this.logln("<< " + t);
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
			// ferme le in_stream
			this.logln("Ferme les stream entrant");
			try {
				if (in_stream != null) in_stream.close();
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
			this.close_all();
		}
	}
	/** gère la réception des trames de type inconnu
	 * Ne devrait pas être appelé
	*/
	@SuppressWarnings("unused")
	private boolean receive(Trame t) throws Trame.TrameException {
		// on ignore
		this.logln("\tignore");
		return false;
	}
	/**
	 * S'occupe de recevoir les ACK et d'avancer la fenêtre d'envoie
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	@SuppressWarnings("unused")
	 private boolean receive(Trame.A t) throws Trame.TrameException {
		if (this.status == Status.NEW || this.status == Status.CLOSED) { // ignore
			this.logln("\tignore (aucune connexion)");
			return false; 
		} 
		if (this.status == Status.WAITING) { // confirmation de la connection 
			this.status = Status.CONNECTED; 
			this.role = Role.CLIENT; 
			synchronized (this) { this.notifyAll(); }
			this.logln("\tactive la connexion");
		}
		
		int n = t.getNum().get();
		// on assume qu'on ne recoit pas d'ack pour une trame pas envoyer
		// 1. avancer la fenêtre
		// tite optimisation: si on n'a pas a déplacer la fenêtre, on fait juste quitter
		synchronized (this.out_lock) {
			avancer_out(n);
			this.can_send = t.ready();
		}
		this.temporisateur.cancel(this.temp_send);
		this.logln("\tprépare à l'envoi");
		// indiquer si l'on peut envoyer plus de trame
		return true;
	}
	/**
	 * Gère les demandes de connexion
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	@SuppressWarnings("unused")
	 private boolean receive(Trame.C t) throws Trame.TrameException {
		// si on est pas en attente de connexion, on ignore
		if (this.status == Status.NEW) {
			// on active la connexion et on envoi un P et un RR en confirmation
			this.status = Status.CONNECTED;
			this.role = Role.SERVER;
			queue_ctrl(Trame.rr(this.in_at));
			queue_ctrl(Trame.p());
			this.temporisateur.reset(temp_p);
			this.logln("\tenvoi confirmation");
			return true;
		} else {
			// ignore
			this.logln("\tignore (pas de connexion)");
			return false;
		}
	}
	/**
	 * Gère la réception d'une trame de fin de connexion
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	@SuppressWarnings("unused")
	 private boolean receive(Trame.F t) throws Trame.TrameException {
		if (this.status == Status.CONNECTED || this.status == Status.WAITING){
			close_all();
			return true;
		}
		this.logln("\tignore (pas de connexion)");
		return false; // sinon ignore
	}
	/**
	 * Gère la réception de trame d'information, d'ajout des bytes au buffer et d'envoie de confirmation/rejet
	 * @param t
	 * @return
	 * @throws Trame.TrameException si la trame contient une erreur
	 */
	@SuppressWarnings("unused")
	 private boolean receive(Trame.I t) throws Trame.TrameException {
		if (this.can_receive && this.status == Status.CONNECTED && this.mode != null) { // si on n'a pas encore de connexion, on ignore les trames I
			// on délègue la logique au mode
			Trame ret = this.mode.update_in(this, t);
			// on envoie la trame par la queue de ctrl
			queue_ctrl(ret);
			return true;
		}
		this.logln("\tignore (pas de connexion)");
		return false;
	}
	/**
	 * Gère la réception d'une trame P
	 * @param t
	 * @return
	 * @throws Trame.TrameException
	 */
	@SuppressWarnings("unused")
	 private boolean receive(Trame.P t) throws Trame.TrameException {
		// on fait juste le renvoyer si on est connecté
		if (this.status == Status.CONNECTED){
			queue_ctrl(Trame.p());
			if (this.role == Role.SERVER) this.temporisateur.reset(this.temp_p);
			this.logln("\trenvoi");
			return true;
		}
		// sinon ignore
		this.logln("\tignore (pas de connexion)");
		return false;
	}
	/**
	 * Gère la réception d'une trame de rejet
	 * @param t
	 * @return
	 * @throws Trame.TrameException si le mode de rejet demander est invalide
	 */
	@SuppressWarnings("unused")
	private boolean receive(Trame.R t) throws Trame.TrameException {
		if (this.status == Status.CONNECTED) {
			int n = t.getNum().get();
			Mode m = t.selectif()? Mode.SELECT : Mode.GBN;
			if (this.mode == null || !this.mode.supporte(m)) throw new Trame.TrameException("Mode de rejet invalide");
			m.update_out(n, this);
			this.temporisateur.cancel(this.temp_send);
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
		int b = this.in_stream.read();
		if (b < 0) return Optional.empty();
		return Optional.of(b != 0);
	}
	/**
	 * ferme tout et lâche les ressources. Bloque le thread jusqu'à temps que tout soit fermé
	 * @return true lorsque tout est fermé
	 */
	private synchronized boolean close_all() {
		this.status = Status.CLOSED;
		while (this.in_stream != null && this.out_stream != null) { // on attend
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
		this.logln(">> " + trame);
		Word bits = trame.encode(CRC.CRC_CCITT);
		send_wout_stuffing(FLAG);
		send_w_stuffing(bits);
		send_wout_stuffing(FLAG);
		send_wout_stuffing(BIT_INV); // on envoie des bits invalide pour bien séparer les trames
		out_stream.flush();
	}
	/**
	 * Créer la prochaine trame I en prenant jusqu'à 1024 bits du buffer d'écriture. 
	 * @return la nouvelle trame I. Empty s'il n'y a rien a envoyer
	 */
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
		//if (!this.skip_erreur && Math.random() <= this.chance_bit_errone) {
		//	desire = Math.random() <= 0.5? false : true;
		//}
		return desire? 1 : 0;
	}

	/**
	 * Ajoute un trame de ctrl à la queue
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
	/**
	 * Indique si ce noeud est présentement connecté.
	 * @return
	 */
	public boolean estConnecte() {
		return this.status == Status.CONNECTED;
	}
	/**
	 * Retourne l'état de la connexion de ce noeud
	 * @return
	 */
	public Status geStatus() {
		return this.status;
	}
	/**
	 * Indique si la connexion de ce noeud est fermée
	 * @return
	 */
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
	/**
	 * Ajoute un logger. retourne l'ancien
	 * @param logger
	 * @return
	 */
	public Logger setLogger(Logger logger) {
		Logger old = this.logger;
		this.logger = logger;
		return old;
	}
	/**
	 * Imprime sur le logger s'il existe
	 * @param msg
	 */
	private void logln(String msg) { if (this.logger != null) this.logger.println(msg); }

	/**
	 * InputStream lisant les bits reçu dans les trames. Lorsque l'on reçoit des bytes, ils sont placés dans un buffer. Ce stream lit les bytes de ce buffer.
	 * S'il n'y a pas de byte à lire, bloque le thread jusqu'à ce qu'il y en aille
	 */
	private static class IOInputStream extends InputStream {
		private IO self;
		private IOInputStream(IO self) { this.self = self; }
		@Override
		public int read() throws IOException {
			if (self.status == Status.NEW || self.status == Status.WAITING) throw new IOException("Connexion pas encore ouverte");
			synchronized (self.read_lock) {
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
	/**
	 * Output stream pour envoyer des bytes. Tout les bytes passés sont ajouter au buffer d'écriture afin que le thread de sortie puisse les utiliser pour créer des trames I.
	 */
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

	/**
	 * Thread s'occupant de rouler <code>receive()</code>
	 */
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
	/**
	 * Thread s'occupant de rouler <code>send</code>
	 */
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

	/**
	 * Objet vide qui sera utiliser comme mutex
	 */
	private static class Lock {}
	/**
	 * Gère un timer. permet de reset ou annulé certains événements.
	 * Utiliser pour renvoyer des trames si on n'a pas de réponse depuis un moment
	 */
	private static class Temporisateur {
		HashMap<Marker, TimerTask> markers = new HashMap<>();
		private Timer timer = new Timer(true);
		void reset(Marker marker) {
			TimerTask old = this.markers.get(marker);
			if (old != null) old.cancel();
			TimerTask new_task = new MarkerTask(marker);
			this.timer.schedule(new_task, DELAIS_TEMPORISATEUR);
			this.markers.put(marker, new_task);
		}
		void cancel(Marker marker) {
			TimerTask old = this.markers.remove(marker);
			if (old != null) old.cancel();
		}
	}
	/**
	 * Décrit un des évenement que le temporisateur doir surveiller
	 */
	private static class Marker {
		IO self;
		Marker(IO self) { this.self = self; }
		public void run() {}
	}
	/**
	 * Wrapper autour de TimerTask pour que celle ci utilise la fonction run() du marqueur désiré
	 */
	private static class MarkerTask extends TimerTask {
		Marker src;
		MarkerTask(Marker src) {this.src = src;}
		@Override
		public void run() { this.src.run(); }
		
	}
}
