package dev.clares.chatgames.solver;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/** Solves one-variable linear equations by collecting coefficients on both sides. */
public final class VariableSolver {
    private record Linear(BigDecimal coefficient, BigDecimal constant) { }

    public static Optional<String> solve(String equation) {
        try {
            String normalized=equation.replace('−','-').replace('–','-').replace('—','-')
                    .replace('×','*').replace('·','*').replace('✕','*').replaceAll("\\s+","")
                    .replace('X','x');
            String[] sides=normalized.split("=",-1);
            if (sides.length!=2) return Optional.empty();
            Linear left=parse(sides[0]), right=parse(sides[1]);
            BigDecimal coefficient=left.coefficient.subtract(right.coefficient);
            if (coefficient.signum()==0) return Optional.empty();
            BigDecimal value=right.constant.subtract(left.constant).divide(coefficient,MathContext.DECIMAL64);
            return Optional.of(value.stripTrailingZeros().toPlainString());
        } catch (IllegalArgumentException | ArithmeticException e) { return Optional.empty(); }
    }

    private static Linear parse(String expression) {
        if (expression.isEmpty()) throw new IllegalArgumentException();
        BigDecimal coefficient=BigDecimal.ZERO, constant=BigDecimal.ZERO;
        int start=0;
        for (int i=1;i<=expression.length();i++) {
            if (i<expression.length() && expression.charAt(i)!='+' && expression.charAt(i)!='-') continue;
            String term=expression.substring(start,i);
            if (term.isEmpty() || term.equals("+") || term.equals("-")) throw new IllegalArgumentException();
            int sign=1;
            if (term.charAt(0)=='+') term=term.substring(1);
            else if (term.charAt(0)=='-') {sign=-1;term=term.substring(1);}
            if (term.isEmpty()) throw new IllegalArgumentException();
            if (term.contains("x")) {
                if (!term.matches("(?:\\d+(?:\\.\\d+)?\\*?)?x|x\\*\\d+(?:\\.\\d+)?")) throw new IllegalArgumentException();
                String multiplier=term.startsWith("x*") ? term.substring(2) : term.substring(0,term.indexOf('x')).replace("*","");
                coefficient=coefficient.add((multiplier.isEmpty()?BigDecimal.ONE:new BigDecimal(multiplier)).multiply(BigDecimal.valueOf(sign)));
            } else {
                if (!term.matches("\\d+(?:\\.\\d+)?")) throw new IllegalArgumentException();
                constant=constant.add(new BigDecimal(term).multiply(BigDecimal.valueOf(sign)));
            }
            start=i;
        }
        return new Linear(coefficient,constant);
    }
}
