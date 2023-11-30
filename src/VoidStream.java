import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream qui ne fait que supprimer ses inputs
 */
public class VoidStream extends OutputStream {

	@Override
	public void write(int b) throws IOException {}
	@Override
	public void write(byte[] bytes, int off, int len){}
	@Override
	public void write(byte[] bytes){}
	
}
