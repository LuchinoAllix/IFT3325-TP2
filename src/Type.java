
public class Type{
	// A voir si il ne vaut pas mieux faire une classe interne 

	public Word type;

	// TODO : définir type par défaut, genre 0 ?
	public Type(){
		type = new Word(00000000);
	}

	public Type(types type){
		switch (type) {
			case I: 
				this.type=new Word('I');
				break;
			case C: 
				this.type=new Word('C');
				break;
			case A: 
				this.type=new Word('A');
				break;
			case R: 
				this.type=new Word('R');
				break;
			case F: 
				this.type=new Word('F');
				break;
			case P: 
				this.type=new Word('P');
				break;
			default:
				this.type = new Word(00000000);
				break;
		};
	}
}
