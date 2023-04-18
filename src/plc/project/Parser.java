package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        Ast.Source ast = new Ast.Source(new ArrayList<Ast.Field>(), new ArrayList<Ast.Method>());
        while(peek("LET") || peek("DEF")){
            if(match("LET")) {
                ast.getFields().add(parseField());
            }
            else{
                match("DEF");
                ast.getMethods().add(parseMethod());
            }
        }
        return ast;
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        String typename = "Any";
        if (match(":")){
            if(!match(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier",tokens.get(0).getIndex());
            typename = (tokens.get(-1).getLiteral());
        }
        Optional<Ast.Expr> value;
        if(match("="))
            value = Optional.of(parseExpression());
        else value = Optional.empty();
        if (tokens.has(1)) match(";");
        return new Ast.Field(name,typename,value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!match(Token.Type.IDENTIFIER)){
            throw new ParseException("Expected identifier",tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral();
        if(!match("(")){
            throw new ParseException("Expected '('",tokens.get(0).getIndex());
        }
        ArrayList<String> params = new ArrayList<String>();
        ArrayList<String> typeName = new ArrayList<String>();
        if (match(Token.Type.IDENTIFIER)){
            params.add(tokens.get(-1).getLiteral());
            while(match(",") && match(Token.Type.IDENTIFIER)){
                params.add(tokens.get(-1).getLiteral());
                match(":");
                typeName.add(tokens.get(0).getLiteral());
                tokens.advance();
            }
        }
        if(!match(")")){
            throw new ParseException("Expected ')'",tokens.get(0).getIndex());
        }
        Optional<String> typename = Optional.empty();
        if (match(":")){
            if(!match(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier",tokens.get(0).getIndex());
            typename = Optional.of(tokens.get(-1).getLiteral());
        }
        if(!match("DO")) throw new ParseException("Expected 'DO'",tokens.get(0).getIndex());
        ArrayList<Ast.Stmt> dos = new ArrayList<Ast.Stmt>();
        while(!peek("END")) {
            dos.add(parseStatement());
        }
        return new Ast.Method(name,params,typeName,typename,dos);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (peek("LET")){
            return parseDeclarationStatement();
        }
        else if (peek("IF")){
            return parseIfStatement();
        }
        else if (peek("FOR")){
            return parseForStatement();
        }
        else if (peek("WHILE")){
            return parseWhileStatement();
        }
        else if (peek("RETURN")){
            return parseReturnStatement();
        }
        else {
            Ast.Expr expr = parseExpression();
            if(match("=")){ //assignment
                Ast.Expr expr2 = parseExpression();
                if (!match(";")){
                    throw new ParseException("Expected ';'",tokens.get(0).getIndex());
                }
                return new Ast.Stmt.Assignment(expr,expr2);
            }
            if (!match(";")){
                throw new ParseException("Expected ';'",tokens.get(0).getIndex());
            }
            return new Ast.Stmt.Expression(expr);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier",tokens.get(0).getIndex()); //TODO index
        }
        String name = tokens.get(-1).getLiteral();
        Optional<String> typename = Optional.empty();
        if (match(":")){
            if(!match(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier",tokens.get(0).getIndex());
            typename = Optional.of(tokens.get(-1).getLiteral());
        };
        Optional<Ast.Expr> value = Optional.empty();
        if(match("=")){
            value = Optional.of(parseExpression());
        }
        if (tokens.has(1) && !match(";")){
            throw new ParseException("Expected ';'",tokens.get(0).getIndex()); //TODO change index?????
        }
        return new Ast.Stmt.Declaration(name, typename,value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expr expr = parseExpression();
        if(!match("DO")) throw new ParseException("Expected 'DO'",tokens.get(0).getIndex());
        ArrayList<Ast.Stmt> thens = new ArrayList<Ast.Stmt>();
        ArrayList<Ast.Stmt> elses = new ArrayList<Ast.Stmt>();
        while(!peek("ELSE") && !peek("END")){
            thens.add(parseStatement());
        }
        if(match("ELSE")){
            while(!peek("END")){
                elses.add(parseStatement());
            }
        }
        if(!match("END")){
            throw new ParseException("Expected 'END'",tokens.get(0).getIndex());
        }
        return new Ast.Stmt.If(expr,thens,elses);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        match("FOR");
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        if(!match("IN")) throw new ParseException("Expected 'IN'",tokens.get(0).getIndex());
        Ast.Expr expr = parseExpression();
        if(!match("DO")) throw new ParseException("Expected 'DO'",tokens.get(0).getIndex());
        ArrayList<Ast.Stmt> dos = new ArrayList<Ast.Stmt>();
        while(!peek("END")){
            dos.add(parseStatement());
        }
        if(!match("END")){
            throw new ParseException("Expected 'END'",tokens.get(0).getIndex());
        }
        return new Ast.Stmt.For(name,expr,dos);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expr expr = parseExpression();
        if(!match("DO")) throw new ParseException("Expected 'DO'",tokens.get(0).getIndex());
        ArrayList<Ast.Stmt> dos = new ArrayList<Ast.Stmt>();
        while(!peek("END")){
            dos.add(parseStatement());
        }
        return new Ast.Stmt.While(expr,dos);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expr expr = parseExpression();
        if (!match(";")){
            throw new ParseException("Expected ';'",tokens.get(0).getIndex()); //TODO change index?????
        }
        return new Ast.Stmt.Return(expr);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression(); //TODO change this
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr expr = parseEqualityExpression();
        if(peek("AND")||peek("OR")){
            if(match("AND")){
                return new Ast.Expr.Binary("AND",expr,parseLogicalExpression());
            }
            if(match("OR")){
                return new Ast.Expr.Binary("OR",expr,parseLogicalExpression());
            }
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr expr = parseAdditiveExpression();
        if(match("<")){
            return new Ast.Expr.Binary("<",expr,parseEqualityExpression());
        }
        if(match("<=")){
            return new Ast.Expr.Binary("<=",expr,parseEqualityExpression());
        }
        if(match(">")){
            return new Ast.Expr.Binary(">",expr,parseEqualityExpression());
        }
        if(match(">=")){
            return new Ast.Expr.Binary(">=",expr,parseEqualityExpression());
        }
        if(match("==")){
            return new Ast.Expr.Binary("==",expr,parseEqualityExpression());
        }
        if(match("!=")){
            return new Ast.Expr.Binary("!=",expr,parseEqualityExpression());
        }
        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr expr =  parseMultiplicativeExpression();
        if(match("+")){
            return new Ast.Expr.Binary("+",expr,parseAdditiveExpression());
        }
        if(match("-")){
            return new Ast.Expr.Binary("-",expr,parseAdditiveExpression());
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expr =  parseSecondaryExpression();
        if(match("*")){
            return new Ast.Expr.Binary("*",expr,parseMultiplicativeExpression());
        }
        if(match("/")){
            return new Ast.Expr.Binary("/",expr,parseMultiplicativeExpression());
        }
        return expr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr receiver = parsePrimaryExpression();
        while (match(".")) {
            if(!match(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier",tokens.get(0).getIndex());
            String name = tokens.get(-1).getLiteral();
            if(match("(")) {
                List<Ast.Expr> funkArgs = new ArrayList<Ast.Expr>();
                while(!match(")")) {
                    funkArgs.add(parseExpression());
                    match(",");
                }
                receiver = new Ast.Expr.Function(Optional.of(receiver), name, funkArgs);
            }
            else receiver = new Ast.Expr.Access(Optional.of(receiver), name);
        }
        return receiver;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     *
     * primary_expression ::=
     *     'NIL' | 'TRUE' | 'FALSE' |
     *     integer | decimal | character | string |
     *     '(' expression ')' |
     *     identifier ('(' (expression (',' expression)*)? ')')?
     *
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if(match("TRUE")){
            return new Ast.Expr.Literal(true);
        }
        if(match("FALSE")){
            return new Ast.Expr.Literal(false);
        }
        if(match("NIL")){
            return new Ast.Expr.Literal(null);
        }
        if(match(Token.Type.INTEGER)){
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        if(match(Token.Type.DECIMAL)){
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        if(match(Token.Type.STRING)){
            String name = (tokens.get(-1).getLiteral());
            name = name.substring(1,name.length()-1);
            boolean changed = true;
            while(changed && name.indexOf("\\")>-1) {
                int length = name.length();
                name = name.replace(('\\' + "b"), "\b");
                name = name.replace(('\\' + "n"), "\n");
                name = name.replace(('\\' + "r"), "\r");
                name = name.replace(('\\' + "t"), "\t");
                name = name.replace(('\\' + "\'"), "\'");
                name = name.replace(('\\' + "\""), "\"");
                name = name.replace(("\\\\"), "\\");
                changed = name.length()==length;
            }
            return new Ast.Expr.Literal(name);
        }
        if(match(Token.Type.CHARACTER)){
            String name = (tokens.get(-1).getLiteral());
            name = name.substring(1,name.length()-1);
            boolean changed = true;
            while(changed && name.indexOf("\\")>-1) {
                int length = name.length();
                name = name.replace(('\\' + "b"), "\b");
                name = name.replace(('\\' + "n"), "\n");
                name = name.replace(('\\' + "r"), "\r");
                name = name.replace(('\\' + "t"), "\t");
                name = name.replace(('\\' + "\'"), "\'");
                name = name.replace(('\\' + "\""), "\"");
                name = name.replace(("\\\\"), "\\");
                changed = name.length()==length;
            }
            return new Ast.Expr.Literal(name.charAt(0));
        }
        else if (match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();
            //TODO function if nex is "("
            if(match("(")) {
                Ast.Expr.Function expr = new Ast.Expr.Function(Optional.empty(), name, new ArrayList<Ast.Expr>());;
                if (!peek(")")){
                    expr.getArguments().add(parseExpression());
                    while (match(",")) {
                        expr.getArguments().add(parseExpression());
                    }
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", tokens.index);
                }
                return expr;
            }
            return new Ast.Expr.Access(Optional.empty(), name);
        }
        else if (match("(")){
            Ast.Expr expr = parseExpression();
            if (!match(")")){
                throw new ParseException("Expected ')'",tokens.get(0).getIndex()); //index?
            }
            return new Ast.Expr.Group(expr);
        }
        else {
            throw new ParseException("Invalid expression",tokens.get(0).getIndex());
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++){
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
