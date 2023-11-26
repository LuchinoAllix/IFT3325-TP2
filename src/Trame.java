import java.io.IOError;

public class Trame {
	/**
	 * Cette classe représente les trames utilisées pour l'échange de données 
	 * simulée avec le protocol HDLC simplifié.
	*/

	/**
	 * Attributs de la classe Trame, comme requis dans l'ennoncé. 
	*/
	public Word flag;
	public Type type;
	public Word num;
	public Word data;
	public Word crc;

	public static final byte ZERO = 00000000; 

	public Trame(){
		flag = new Word(01111110);
		type = new Type();
		num = new Word(ZERO);
		data = new Word(ZERO);
		crc = new Word(ZERO);
	}

	public Trame(char type,byte num,String data){
		flag = new Word(01111110);
		this.type = new Type(type);
		this.num = new Word(num);
		this.data = new Word(data);
	}

	public String toString(){
		return flag.toString() + type.toString()+num.toString()+data.toString()+crc.toString()+flag.toString();
	}

	public static Trame stringToTrame(String str) throws IOError{
		Trame trame = new Trame();
		int n = str.length();
		trame.flag = new Word(str.substring(0, 8));
		String typeBis = str.substring(8, 16); // todo have a String to char 
		// to edit trame.type
		trame.num = new Word(str.substring(16, 24));
		trame.data=new Word(str.substring(24, n-16));
		trame.crc=new Word(str.substring(n-16, n-8));
		if (!trame.checkCRC()){
			throw new IOError(null);
			// Pas la vraie erreur mais flemme de def
			// une erreure personalisée pour le moment
		}
		return trame;
	}

	public void nullifyData(){
		// pour rendre data de taille nulle pour les accusé de reception ou demande de connexion ou autres
		data = new Word();
	}

	public void makeCRC(){
		//todo
	}

	public boolean checkCRC(){
		//todo
		return false;
	}

	// A voir si d'autres contructeurs seront nécessaire ou pas.
	// Pour l'instant pas besoin de getteurs & setteurs, l'encapsulation peut être évitée (simplifie le code)

}
