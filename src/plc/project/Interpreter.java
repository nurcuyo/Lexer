package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Environment.PlcObject> args = new ArrayList<>();
        for(Ast.Field f : ast.getFields()){
            args.add(visit(f));
        }
        for(Ast.Method m : ast.getMethods()){
            args.add(visit(m));
        }
        return scope.lookupFunction("main", 0).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {

        try {
            getScope().defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        catch (NoSuchElementException e) {
            getScope().defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        // Define a function in the scope
        Scope currScope = getScope(); // initial scope
        Function<List<Environment.PlcObject>, Environment.PlcObject> lambda =
                (List<Environment.PlcObject> args) -> {
                    // set scope to be a new child of currScope
                    Scope childScope = new Scope(currScope); // define and set scope as child of function scope
                    scope = childScope;
                    for (int i = 0; i < ast.getParameters().size(); i++) {
                        scope.defineVariable(ast.getParameters().get(i), args.get(i));
                    }
                    for (int i = 0; i < ast.getStatements().size(); i++) {
                        try {
                            visit(ast.getStatements().get(i));
                        }
                        catch (Return e) {
                            return e.value;
                        }
                    }

                    return Environment.NIL;
                };

        currScope.defineFunction(ast.getName(), ast.getParameters().size(), lambda);
        scope = currScope;
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        // if the Access has a receiver and it is an Access type
        if(ast.getReceiver() != null && ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access assignReceiever = (Ast.Expr.Access)ast.getReceiver();
            try {
                Ast.Expr accessReceiver = assignReceiever.getReceiver().get();
                visit(accessReceiver).setField(assignReceiever.getName(), visit(ast.getValue()));
            }
            catch (NoSuchElementException e) {
                getScope().lookupVariable(assignReceiever.getName()).setValue(visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
            return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {


        for(Object o : requireType(Iterable.class,visit(ast.getValue()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    scope.defineVariable(ast.getName(), Environment.create(((Environment.PlcObject) o).getValue()));
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }

        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()){
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null){
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        switch(ast.getOperator()) {
            case("AND"):
                return Environment.create(ast.getLeft().equals((new Ast.Expr.Literal(new Boolean(true)))) && ast.getRight().equals((new Ast.Expr.Literal(new Boolean(true)))));
            case("OR"):
                return Environment.create(ast.getLeft().equals((new Ast.Expr.Literal(new Boolean(true)))) || ast.getRight().equals((new Ast.Expr.Literal(new Boolean(true)))));
            case("<"):
                return Environment.create(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) < 0);
            case("<="):
                return Environment.create(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) <= 0);
            case(">"):
                return Environment.create(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) > 0);
            case(">="):
                return Environment.create(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) >= 0);
            case("=="):
                return Environment.create(ast.getLeft().equals(ast.getRight()));
            case("!="):
                return Environment.create(!ast.getLeft().equals(ast.getRight()));
            case("+"):
                try{ //concat
                    requireType(String.class,visit(ast.getLeft()));
                    return Environment.create(ast.getLeft().toString().substring(ast.getLeft().toString().indexOf("=")+1,ast.getLeft().toString().length()-1)+ast.getRight().toString().substring(ast.getRight().toString().indexOf("=")+1,ast.getRight().toString().length()-1));
                }
                catch (RuntimeException e1){ //check other
                    try {
                        requireType(String.class,visit(ast.getRight()));
                        return Environment.create(ast.getLeft().toString().substring(ast.getLeft().toString().indexOf("=")+1,ast.getLeft().toString().length()-1)+ast.getRight().toString().substring(ast.getRight().toString().indexOf("=")+1,ast.getRight().toString().length()-1));
                    }
                    catch (RuntimeException e2) { //add
                        requireType(visit(ast.getLeft()).getValue().getClass(),visit(ast.getRight()));
                        if(visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class)) {
                            return Environment.create(((BigInteger) (visit(ast.getLeft()).getValue())).add((BigInteger) (visit(ast.getRight()).getValue())));
                        }
                        return Environment.create(((BigDecimal) (visit(ast.getLeft()).getValue())).add((BigDecimal) (visit(ast.getRight()).getValue())));
                    }
                }
            case("-"):
                requireType(visit(ast.getLeft()).getValue().getClass(),visit(ast.getRight()));
                if(visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class)) {
                    return Environment.create(((BigInteger) (visit(ast.getLeft()).getValue())).subtract((BigInteger) (visit(ast.getRight()).getValue())));
                }
                return Environment.create(((BigDecimal) (visit(ast.getLeft()).getValue())).subtract((BigDecimal) (visit(ast.getRight()).getValue())));
            case("*"):
                requireType(visit(ast.getLeft()).getValue().getClass(),visit(ast.getRight()));
                if(visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class)) {
                    return Environment.create(((BigInteger) (visit(ast.getLeft()).getValue())).multiply((BigInteger) (visit(ast.getRight()).getValue())));
                }
                return Environment.create(((BigDecimal) (visit(ast.getLeft()).getValue())).multiply((BigDecimal) (visit(ast.getRight()).getValue())));
            case("/"):
                if(ast.getRight().equals(new Ast.Expr.Literal(new BigDecimal("0"))) || ast.getRight().equals(new Ast.Expr.Literal(new BigInteger("0")))){
                    throw new RuntimeException ("You tried to divide by zero! The entire space-time continuum has collapsed as a result :/");
                }
                requireType(visit(ast.getLeft()).getValue().getClass(),visit(ast.getRight()));
                if(visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class)) {
                    return Environment.create(((BigInteger) (visit(ast.getLeft()).getValue())).add((BigInteger) (visit(ast.getRight()).getValue())));
                }
                return Environment.create(((BigDecimal) (visit(ast.getLeft()).getValue())).divide((BigDecimal) (visit(ast.getRight()).getValue()),RoundingMode.HALF_EVEN));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        Scope currScope = getScope();
        try {
            Ast.Expr accessReceiever = ast.getReceiver().get();
            return visit(accessReceiever).getField(ast.getName()).getValue();
        } catch(NoSuchElementException e) {
            return currScope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Ast.Expr> oldArgs = ast.getArguments();
        List<Environment.PlcObject> newArgs = new ArrayList<Environment.PlcObject>();
        for (int i = 0; i < oldArgs.size(); i++) {
            newArgs.add(visit(oldArgs.get(i)));
        }
        try {
            Ast.Expr functionReceiever = ast.getReceiver().get();
            return visit(functionReceiever).callMethod(ast.getName(), newArgs);
        } catch(NoSuchElementException e) {
            return getScope().lookupFunction(ast.getName(), ast.getArguments().size()).invoke(newArgs);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        public Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
