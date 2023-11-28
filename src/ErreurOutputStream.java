import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper pour un OutputStream. Délègue toute les opérations à sa source.
 * Par contre, lorsque l'on veut écrire un byte, il y a une chance que ce byte soit choisit aléatoirement, introduisant des erreurs.
 */
public class ErreurOutputStream extends OutputStream {
	/**
	 * Source
	 */
	private OutputStream src;
	/**
	 * probabilité qu'un byte soit choisit aléatoirement. 0 <= chance_erreur <= 1
	 */
	private double chance_erreur;
	/**
	 * Wrap autour de src, en introduisant une probabilité de byte erroné
	 * @param src OutputStream auquel tout sera délègué
	 * @param probErr 0 <= probErr <= 1; probabilité qu'un byte soit choisit aléatoirement lors d'un write()
	 */
	public ErreurOutputStream(OutputStream src, double probErr) {
		this.src = src;
		if (probErr < 0) probErr = 0;
		if (probErr > 1) probErr = 1;
		this.chance_erreur = probErr;
	}
	@Override
	public void write(int b) throws IOException {
		double r = Math.random();
		if (r < this.chance_erreur) { // oupsi
			b = Math.random() < 0.5? 0 : 1;
		}
		src.write(b);
	}
	@Override
	public void close() throws IOException { this.src.close(); }
	@Override
	public void flush() throws IOException { this.src.flush(); }
	
}
