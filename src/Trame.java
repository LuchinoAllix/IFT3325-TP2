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

	public Trame(byte flag,Types type,byte num,String data,short crc){
		this.flag = new Word(flag);
		this.type = new Type(type);
		this.num = new Word(num);
		this.data = new Word(data);
		this.crc = new Word(crc);
	}

	// A voir si d'autres contructeurs seront nécessaire ou pas.
	// Pour l'instant pas besoin de getteurs & setteurs, l'encapsulation peut être évitée (simplifie le code)

}
