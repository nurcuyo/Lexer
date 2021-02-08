import java.util.ArrayList;
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
        List<Token> tokens = new ArrayList<Token>();
        while(chars.has(1)){
            tokens.add(lexToken());
        }
        return tokens;
    }

    public Token lexToken() {

        if(peek("[A-Za-z_]")) {
            lexIdentifier();
        }
        else if(peek("[+-","[0-9]")){
            lexNumber();
        }
        else if(peek("\'")){
            lexCharacter();
        }
        else if(peek("\"")){
            lexString();
        }
        else if(peek()){

        }
        else if(peek()){

        }
        else if(peek()){

        }

        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {

        while(peek("[A-Za-z_][A-Za-z0-9_-]*")) {
            match("[A-Za-z_][A-Za-z0-9_-]*");
        }

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

        Lexer iden = new Lexer("&(&#$@# h ^@!(^#)@(");
        Lexer num = new Lexer("a   +213.4");

        while(!iden.peek("[A-Za-z_][A-Za-z0-9_-]*")){
            iden.chars.advance();
        }
        iden.chars.skip();
        while(iden.peek("[A-Za-z0-9_-]*")) {
            System.out.print(iden.chars.get(0));
            iden.match("[A-Za-z0-9_-]*");
        }
        System.out.print(iden.chars.get(0));
        iden.match("[A-Za-z_][A-Za-z0-9_-]*");

        while(!num.peek("[+-]?[0-9]+('.'[0-9]+)?")){
            num.chars.advance();
        }
        num.chars.skip();
        while(num.peek("[+-]?[0-9]+('.'[0-9]+)?")) {
            System.out.print(num.chars.get(0));
            num.match("[+-]?[0-9]+('.'[0-9]+)?");
        }

        System.out.println();
        Token ti = iden.chars.emit(Token.Type.IDENTIFIER);
        Token tn = num.chars.emit(Token.Type.INTEGER);
        System.out.println(ti.toString());
        System.out.println(tn.toString());
    }
}

