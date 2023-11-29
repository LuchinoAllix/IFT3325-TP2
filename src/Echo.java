import java.io.InputStream;
import java.io.OutputStream;

/**
 * Prend les bytes disponibles dans in et les places dans out
 */
public class Echo {
	private byte[] bytes = new byte[1024];
	private Thread thread;
	private InputStream in;
	private OutputStream out;
	public Echo(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		this.thread = new Thread(() -> {
			try {
				while (true) {
					try {
						int b = this.in.read(bytes);
						if (b < 0) break;
						while (true) {
							try {
								this.out.write(bytes, 0, b);
								break;
							} catch (IO.NoConnexionException e) {}
						}
					} catch (IO.NoConnexionException e) {}
				}
			} catch (Exception e) {}
		});
		this.thread.setDaemon(true);
		this.thread.start();
	}
}
