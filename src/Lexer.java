import java.util.List;

public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);

    }

    public CharStream thing(){
        return chars;
    }

    //repeatedly lexes the charstream, passing into specific type functions when such a type is found
    public List<Token> lex() {
        List<Token> tokens;
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexToken() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexNumber() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
    }

    //checks to see if the specified pattern occurs in the following characters
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]))
                return false;
        }
        return true;
    }

    //uses peek to advance the string past matching tokens
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        //determines if the passed offset is within range
        public boolean has(int offset) {
            return index + offset < input.length();
        }

        //gets the character at a specified offset
        public char get(int offset){
            return input.charAt(index + offset);
        }

        //pushes the index forward one, effectively removing the left-most character
        public void advance() {
            index ++;
            length ++;
        }

        //resets current token length to zero, starting a new token
        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }

    ///temporary main

    public static void main(String[] args){

        Lexer l = new Lexer("hello");

        Lexer.CharStream c = new Lexer.CharStream("hello");
        Token t = c.emit(Token.Type.IDENTIFIER);

        c.advance();

        System.out.println(l.peek("[A-Za-z_]","[A-Za-z0-9_-]*"));

    }
}

