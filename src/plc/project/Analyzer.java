package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(Ast.Field f : ast.getFields()){
            visit(f);
        }
        for(Ast.Method m : ast.getMethods()){
            visit(m);
        }
        Environment.Function mainFunc = getScope().lookupFunction("main", 0);
        if (mainFunc.getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        boolean valueHere = false;
        if (ast.getValue().isPresent()) {
            valueHere = true;
            visit(ast.getValue().get());
        }
        Environment.Variable newVar = getScope().defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL);
        ast.setVariable(newVar);
        if (valueHere) {
            requireAssignable(getScope().lookupVariable(ast.getName()).getType(), ast.getValue().get().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //ast.setFunction(scope.defineFunction(ast.getName(),ast.getName(),,,args->Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        boolean valueHere = false;
        if (ast.getValue().isPresent()) {
            valueHere = true;
            visit(ast.getValue().get());
        }
        if (ast.getTypeName().isPresent()) {
            Environment.Variable newVar = getScope().defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()) , Environment.NIL);
            ast.setVariable(newVar);
        }
        else {
            try {
                Environment.Variable newVar = getScope().defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
                ast.setVariable(newVar);
            } catch(Exception e) {
                throw new RuntimeException();
            }
        }
        if (valueHere) {
            requireAssignable(getScope().lookupVariable(ast.getName()).getType(), ast.getValue().get().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expr.Access) {
            visit(ast.getReceiver());
            visit(ast.getValue());
            requireAssignable(ast.getValue().getType(), ast.getReceiver().getType());
            return null;
        }
        throw new RuntimeException();
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if(ast.getThenStatements().isEmpty()) throw new RuntimeException();
        try{
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getThenStatements()){
                visit(((Ast.Stmt.Expression)stmt).getExpression());
            }
        } finally {
            scope = scope.getParent();
        }
        try{
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getElseStatements()){
                visit(((Ast.Stmt.Expression)stmt).getExpression());
            }
        } finally {
            scope = scope.getParent();
        }
        return null;

    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if(ast.getStatements().isEmpty()){
            throw new RuntimeException();
        }
        try{
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            for(Ast.Stmt stmt : ast.getStatements()){
                visit(((Ast.Stmt.Expression)stmt).getExpression());
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        requireAssignable(Environment.getType(method.getReturnTypeName().get()), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null){
            ast.setType(Environment.Type.NIL);
            return null;
        }
        if (ast.getLiteral() instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        if (ast.getLiteral() instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        if (ast.getLiteral() instanceof String){
            ast.setType(Environment.Type.STRING);
            return null;
        }
        if (ast.getLiteral() instanceof BigInteger){
            if(((BigInteger)ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0){
                throw new RuntimeException();
            }
            ast.setType(Environment.Type.INTEGER);
            return null;
        }
        if (ast.getLiteral() instanceof BigDecimal) {
            if (((BigDecimal) ast.getLiteral()).compareTo(new BigDecimal(Double.MAX_VALUE)) > 0) {
                throw new RuntimeException();
            }
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        visit((Ast.Expr.Binary)ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        visit(ast.getRight());
        visit(ast.getLeft());
        if (op.equals("AND") || op.equals("OR")){
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            requireAssignable(ast.getRight().getType(), ast.getLeft().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")){
            if(op.equals("+") && ast.getLeft().getType().getName().equals("String") || ast.getRight().getType().getName().equals("String")){
                ast.setType(Environment.Type.STRING);
            }
            else{
                try{
                    requireAssignable(Environment.Type.INTEGER,ast.getLeft().getType());
                    requireAssignable(Environment.Type.INTEGER,ast.getRight().getType());
                }
                catch(RuntimeException e){
                    requireAssignable(Environment.Type.DECIMAL,ast.getLeft().getType());
                    requireAssignable(Environment.Type.DECIMAL,ast.getRight().getType());
                }
                finally {
                    ast.setType(ast.getLeft().getType());
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        try {
            Ast.Expr receiver = ast.getReceiver().get();
            // if receiver exists, the variable is a field of the receiver
            visit(receiver);
            ast.setVariable(receiver.getType().getField(ast.getName()));
        } catch (NoSuchElementException e) {
            // if field doesn't exist, variable is in the scope
            ast.setVariable(getScope().lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        try {
            Ast.Expr receiver = ast.getReceiver().get();
            // if receiver exists, the variable is a method in the receiver (index 0?)
            visit(receiver);
            ast.setFunction(receiver.getType().getMethod(ast.getName(), ast.getArguments().size()));
            List<Environment.Type> receiverArgs = receiver.getType().getMethod(ast.getName(), ast.getArguments().size()).getParameterTypes();
            for (int i = 1; i < receiverArgs.size(); i++) {
                visit(ast.getArguments().get(i-1));
                requireAssignable(ast.getArguments().get(i-1).getType(), receiverArgs.get(i));
            }
        } catch (NoSuchElementException e) {
            ast.setFunction(getScope().lookupFunction(ast.getName(), ast.getArguments().size()));
            List<Environment.Type> scopeArgs =  getScope().lookupFunction(ast.getName(), ast.getArguments().size()).getParameterTypes();
            for (int i = 0; i < scopeArgs.size(); i++) {
                visit(ast.getArguments().get(i));
                requireAssignable(scopeArgs.get(i), ast.getArguments().get(i).getType());
            }
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.getName().equals("Any")
            || (target.getName().equals("Comparable") && (type.getName().equals("Integer") || type.getName().equals("Decimal") || type.getName().equals("Character") || type.getName().equals("String")))
            || target.getName().equals(type.getName())){
            return;
        }
        else throw new RuntimeException();
    }

}
