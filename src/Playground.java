public class Playground {
	
	public static void main(String... args) {
		/*
		Word w1 = new Word("0110111010111001001");
		Word w2 = new Word("10001000000100001");
		Word w3 = Word.zeroExtend(w1, w2.length-1);
		int align = 1;
		System.out.println("Hello world");
		System.out.println(align < 0? w3.toString(-align) : w3.toString()); // 
		System.out.println(align > 0? w2.toString(align) : w2.toString()); // 
		Word x = Word.xor(w3, w2, align, Word.ApplyMode.FIRST);
		System.out.println(x);
		System.out.println("------------------------------");
		System.out.println(w1);
		w1.setByteAt(0, (byte)0);
		System.out.println(w1);
		w1.setByteAt(3, (byte)255);
		System.out.println(w1);
		*/
		
		test_crc("0110111010111001001", "1110011010011001");
		Word cc = new Word("10001000000100001");
		
	}

	static void test_crc(String wrd, String goal) {
		Word word = new Word(wrd);
		Word wgoal = new Word(goal);
		Word crc = CRC.CRC_CCITT.crc(word);
		System.out.println("CRC pour " + word);
		System.out.println("" + crc + " (" + wgoal + ") " + crc.equals(wgoal));
		System.out.println(CRC.CRC_CCITT.isValid(Word.concat(word, crc)));
	}
}
