import java.util.Optional;

/**
 * S'occupe de la logique pour calculer le code crc pour une chaîne de bits donnée
 * 
 * Offre également une méthode statique pour éviter de créer un objet si le besoins est rare, et une constante représentant le CRC_CCITT
 */
public class CRC {
    /**
     * Générateur CRC_CCITT, qui est souvent utilisé (10001000000100001)
     */
    public static final CRC CRC_CCITT = new CRC(new Word("10001000000100001"));

    /**
     * la chaine génératrice
     */
    private final Word generator;
    /**
     * Créer une instance de CRC, qui assurera que la même chaine est toujours utilisé
     * @param gen chaîne génératrice
     */
    public CRC(Word gen) {
        this.generator = mk_gen(gen);
    }
    /**
     * Retourne la chaîne génératrice
     * @return chaîne génératrice de ce CRC
     */
    public Word getGenerator(){ return this.generator; }
    /**
     * Retourne la longueur d'un code de vérification de ce CRC
     * @return longueur du code cde vérification
     */
    public int codeLength() { return this.generator.length-1; }

    /**
     * Calcul le code crc de la chaîne de bits msg.
     * @param generator le générateur à utiliser
     * @param msg la chaîne dont on veut le code vérificateur
     * @return le code vérificateur
     */
    public static Word crc(Word generator, Word msg) {
        Word gen = mk_gen(generator);
        return compute_crc(gen, msg);
    }
    /**
     * Calcul le code crc de la chaîne de bits msg.
     * @param msg la chaîne dont on veut le code vérificateur
     * @return le code vérificateur
     */
    public Word crc(Word msg) {
        return compute_crc(this.generator, msg);
    }

    /**
     * Calcule si <code>msg</code> est valide avec un code de vérification de ce crc
     * @param gen générateur
     * @param msg le mot à vérifier
     * @return true si <code>msg</code> est valide, false s'il contient une erreur
     */
    public static boolean isValid(Word generator, Word msg) {
        Word gen = mk_gen(generator);
        return verify(gen, msg);
    }
    /**
     * Calcule si <code>msg</code> est valide avec un code de vérification de ce crc
     * @param msg le mot à vérifier
     * @return true si <code>msg</code> est valide, false s'il contient une erreur
     */
    public boolean isValid(Word msg) {
        return verify(this.generator, msg);
    }

    /**
     * Trim la chaîne de bits donné pour qu'elle commence au premier 1
     * @param src la chaîne originale
     * @return le générateur
     */
    private static Word mk_gen(Word src) {
         int from = src.firstOne().orElseThrow(() -> new IllegalArgumentException());
        if (from==0)
            return src;
        else
            return src.subWord(from, src.length);
    }
    /**
     * Calcule le code vérificateur en ajoutant <code>codeLength()</code> 0 à la fin de <code>msg</code>
     * puis appliquant successivement l'opération XOR sur les deux chaînes en alignant leur premier 1 jusqu'à ce qu'il n'y aie des 1 que dans les bits ajoutés
     * @param gen générateur
     * @param msg la chaîne dont on veut le code vérificateur
     * @return le code vérificateur
     */
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

    /**
     * Calcule si <code>msg</code> est valide avec un code de vérification de ce crc
     * @param gen générateur
     * @param msg le mot à vérifier
     * @return true si <code>msg</code> est valide, false s'il contient une erreur
     */
    private static boolean verify(Word gen, Word msg) {
        if (msg.length < gen.length-1) return false;
        // extend le message avec des 0s
        Optional<Integer> align = msg.firstOne();
        while (align.isPresent() && align.get() < msg.length) {
            // dans la boucle, on est certain que l'on est aligné avec un 1, alors on peut xor
            msg = Word.xor(msg, gen, align.get(), Word.ApplyMode.FIRST); // mode a first parce que l'on veut garder uniquement les bits de padded_msg
            align = msg.firstOne(align.get());
        }
        // retourne les len derniers bits
        Word code = msg.subWord(msg.length, msg.length);
        return code.countOne() == 0;
    }
}
