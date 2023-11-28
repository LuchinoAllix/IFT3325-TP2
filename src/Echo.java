import java.io.InputStream;
import java.io.OutputStream;

public class Echo {
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
						int b = this.in.read();
						if (b < 0) break;
						while (true) {
							try {
								this.out.write(b);
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
