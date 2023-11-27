import java.util.Optional;

public class CRC {
    public static final CRC CRC_CCITT = new CRC(new Word("10001000000100001"));

    private final Word generator;
    public CRC(Word gen) {
        this.generator = mk_gen(gen);
    }

    public Word getGenerator(){ return this.generator; }

    public static Word crc(Word generator, Word msg) {
        Word gen = mk_gen(generator);
        return compute_crc(gen, msg);
    }
    public Word crc(Word msg) {
        return compute_crc(this.generator, msg);
    }

    private static Word mk_gen(Word src) {
         int from = src.firstOne().orElseThrow(() -> new IllegalArgumentException());
        if (from==0)
            return src;
        else
            return src.subWord(from, src.length);
    }
    private static Word compute_crc(Word gen, Word msg) {
        int len = gen.length-1; // longueur de la reponse du crc
        if (len < 1) return new Word();
        // extend le message avec des 0s
        Word padded_msg = Word.zeroExtend(msg, len);
        Optional<Integer> align = padded_msg.firstOne();
        while (align.isPresent() && align.get() < msg.length) {
            // dans la boucle, on est certain que l'on est aligné avec un 1, alors on peut xor
            padded_msg = Word.xor(padded_msg, gen, align.get(), Word.ApplyMode.FIRST); // mode a first parce que l'on veut garder uniquement les bits de padded_msg
            align = padded_msg.firstOne(align.get());
        }
        // retourne les len derniers bits
        return padded_msg.subWord(msg.length, padded_msg.length);
    }
}
