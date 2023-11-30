import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Permet de logger des choses à la fois sur la console et dans un autre stream
 */
public class Logger {
	private PrintStream file_writer;
	private PrintStream console;

	public Logger(File file, boolean console) {
		this.console = console? System.out : null;
		try {
			if (file != null) this.file_writer = new PrintStream(file);
		} catch (IOException e) {
			System.err.println("N'a pas pu ouvrir le fichier");
			e.printStackTrace();
		}
	}
	public Logger() {this(null, true);}
	public Logger(File file) {this(file, true);}
	public Logger(boolean console) {this(null, console);}

	public void print(Object o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(char o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(boolean o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(char[] o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(double o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(float o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(int o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(long o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}
	public void print(String o) {
		if (this.file_writer != null) this.file_writer.print(o);
		if (this.console != null) this.console.print(o);
	}

	public void println(Object o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(char o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(boolean o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(char[] o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(double o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(float o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(int o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(long o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	public void println(String o) {
		if (this.file_writer != null) this.file_writer.println(o);
		if (this.console != null) this.console.println(o);
	}
	
	public void close() {
		this.console = null;
		if (this.file_writer != null) {
			this.file_writer.close();
			this.file_writer = null;
		}
	}
}
