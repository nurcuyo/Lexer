package plc.project;

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
        while(chars.has(0)){
            Token t = (lexToken());
            if(t.getType()!= null) tokens.add(t);
        }
        return tokens;
    }

    public Token lexToken() {

        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (peek("[+-]", "[0-9]") || peek("[0-9]")) {
            return lexNumber();
        } else if (peek("\'")) {
            match("\'");
            return lexCharacter();
        } else if (peek("\"")) {
            match("\"");
            return lexString();
        } else if (match("\\\\", "[bnrt\'\"\\\\]")) {
            lexEscape();
            return new Token(null, "", 0);
        } else if (match("[ \b\n\r\t]")) {
            chars.skip();
            return new Token(null, "", 0);
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {

        while(match("[A-Za-z0-9_-]"));
        Token id = (chars.emit(Token.Type.IDENTIFIER));
        return id;
    }

    public Token lexNumber() {

        if(match("[+-]"));
        while(match("[0-9]"));
        if(peek("\\.","[0-9]")){
            match("\\.");
            while(match("[0-9]"));
            Token de = (chars.emit(Token.Type.DECIMAL));
            return de;
        }
        else {
            Token in = (chars.emit(Token.Type.INTEGER));
            return in;
        }
    }

    public Token lexCharacter() {

        if(!match("([^\'\\n\\r\\\\])") && !match("\\\\", "[bnrt\'\"\\\\]")){
            throw new ParseException("Invalid Character",chars.index);
        }
        else if(!match("\'")){
            throw new ParseException("Mismatched Single Quote",chars.index);
        }
        else {
            Token ch = chars.emit(Token.Type.CHARACTER);
            return ch;
        }
    }

    public Token lexString() {

        while ((match("([^\"\\n\\r\\\\])") || match("\\\\", "[bnrt\'\"\\\\]"))) { /* go */}
        if (match("\"")) {
            Token str = chars.emit(Token.Type.STRING);
            return str;
        }
        else {
            throw new ParseException("Invalid Character",chars.index);
        }
    }

    public void lexEscape() {
        chars.skip();
    }

    public Token lexOperator() {
        if(match("[<>!=]")){
            match("=");
        }
        else match(".");
        Token op = chars.emit(Token.Type.OPERATOR);
        return op;
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
}