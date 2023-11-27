import java.util.Optional;

public abstract  sealed class Trame permits Trame.I, Trame.C, Trame.A, Trame.R, Trame.F, Trame.P {
    //private static final char[] charnum = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public abstract Trame.Type getType();
    public Optional<Integer> getNum() {return Optional.empty();}
    public Optional<Word> getMsg() {return Optional.empty();}
    public byte getNumByte() { return 0; }

    public static final class I extends Trame {
        private int num;
        private Word msg;
        @Override
        public Trame.Type getType() { return Trame.Type.I; }
        private I(int num, Word msg) {
            this.num = num;
            this.msg = msg;
        }
        @Override
        public Optional<Integer> getNum() { return Optional.of(this.num); }
        @Override
        public Optional<Word> getMsg() { return Optional.of(msg); }
        public byte getNumByte() {
            return (byte)Character.forDigit(this.getNum().orElse(0), 10);
        }
    }
    public static final class C extends Trame {
        private int num;
        private C(int num) {
            this.num = num;
        }
        @Override
        public Trame.Type getType() { return Trame.Type.C; }
        @Override
        public Optional<Integer> getNum() { return Optional.of(this.num); }
        public boolean goBackN() { return num == 0; }
        public byte getNumByte() {
            return (byte)Character.forDigit(this.num, 10);
        }
    }
    public static final class A extends Trame {
        private int num;
        private A(int num) {
            this.num = num;
        }
        @Override
        public Trame.Type getType() { return Trame.Type.A; }
        @Override
        public Optional<Integer> getNum() { return Optional.of(this.num&127); }
        public boolean ready() { return (num&128) == 0;}
        public byte getNumByte() {
            if ((this.num&128) == 0) return (byte)Character.forDigit(this.num, 10);
            else return (byte)num;
        }
    }
    public static final class R extends Trame {
        private int num;
        private R(int num) {
            this.num = num;
        }
        @Override
        public Trame.Type getType() { return Trame.Type.R; }
        @Override
        public Optional<Integer> getNum() { return Optional.of(this.num&127); }
        public boolean selectif() { return (num&128) == 0;}
        public byte getNumByte() {
            if ((this.num&128) == 0) return (byte)Character.forDigit(this.num, 10);
            else return (byte)num;
        }

    }
    public static final class F extends Trame {
        private F() {}
        @Override
        public Trame.Type getType() { return Trame.Type.F; }
    }
    public static final class P extends Trame {
        private P() {}
        @Override
        public Trame.Type getType() { return Trame.Type.P; }
    }
    public static enum Type { I,C,A,R,F,P;
        public static Type from(int typenum) {
            return switch (typenum) {
                case 0 -> I;
                case 1 -> C;
                case 2 -> A;
                case 3 -> R;
                case 4 -> F;
                case 5 -> P;
                default -> throw new IllegalArgumentException("type de trame invalide");
            };
        }
        public int indexOf() {
            return switch (this) {
                case I -> 0;
                case C -> 1;
                case A -> 2;
                case R -> 3;
                case F -> 4;
                case P -> 5;
            };
        }
    } // au cas ou t'aime pas le pattern matching pour raison X

    public static Trame decode(Word bits, CRC gen) throws TrameException {
        int gen_len = Math.max(0, gen.getGenerator().length-1);
        int min_nb_of_bits = 16 + gen_len;
        if (bits.length < min_nb_of_bits) throw new TrameException("Trame trop petite"); 
        // vérification du crc
        Word check = gen.crc(bits);
        if (check.countOne() != 0) throw new TrameException("crc incorecte");

        int crc_start = bits.length - gen_len;
        try {
            Type type = Type.from(bits.getByteAt(0).value()&255);
            int num = bits.getByteAt(8).value()&255;
            if ((num & 128) == 0) { // si le premier bit est un 1 (donc on n'a pas recu un caractère ascii), on veut l'interprété comme un cas spécial
                num = Character.digit(num, 10); // le numéro de la trame est transféré comme un la repr ASCII du chiffre
                if (num < 0 || num > 7) throw new TrameException("numéro de trame invalide");
            }
            
            Word msg = bits.subWord(16, crc_start);
            return switch (type) {
                case I -> new I(num, msg);
                case C -> new C(num);
                case A -> new A(num);
                case R -> new R(num);
                case F -> new F();
                case P -> new P();
            };
        } catch (IllegalArgumentException e) {throw new TrameException(e);}
    }
    public Word encode(CRC gen) {
        Word type = new Word(this.getType().indexOf());
        Word num = new Word(this.getNumByte()); // char est 16-bits, les chiffres sont dans ASCII donc 8 bits. puisque java utilise les codepoint unicode, un simple cast de la sorte sufit
        Word sous = switch (this.getType()) {
            case I -> Word.concat(type, num, this.getMsg().get());
            default -> Word.concat(type, num);
        };
        Word crc = gen.crc(sous);
        return Word.concat(sous, crc);
    }

    public static class TrameException extends Exception {
		public TrameException(String msg) {super(msg);}
		public TrameException(Throwable src) {super(src);}
		public TrameException(String msg, Throwable src) {super(msg, src);}
		public TrameException() {super();}
	}

    public static Trame.A rr(int n) {
        if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
        return new Trame.A(n);
    }
    public static Trame.A rnr(int n) {
        if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
        return new Trame.A(n|128);
    }
    public static Trame.R rej(int n) {
        if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
        return new Trame.R(n);
    }
    public static Trame.R srej(int n) {
        if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
        return new Trame.R(n|128);
    }
    public static Trame.C gbn() {
        return new Trame.C(0);
    }
    public static Trame.F end() {
        return new Trame.F();
    }
    public static Trame.P p() {
        return new Trame.P();
    }
    public static Trame.I i(int n, Word msg) {
        if (n < 0 || n > 7) throw new IllegalArgumentException("Numéro de trame invalide");
        return new Trame.I(n, msg);
    }
    public static Trame.C selectiveRej() {
        return new Trame.C(1);
    }
}
