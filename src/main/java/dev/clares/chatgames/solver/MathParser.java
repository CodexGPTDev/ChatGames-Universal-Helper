package dev.clares.chatgames.solver;
import java.math.BigDecimal;
import java.math.MathContext;

/** Bounded recursive descent parser; no code execution or third party evaluator. */
public final class MathParser {
    private static final MathContext MC = MathContext.DECIMAL64;
    private final String input;
    private int pos;
    private MathParser(String input) { this.input = input.replace('×','*').replace('x','*').replace('X','*').replace('·','*').replace('✕','*').replace('÷','/').replace('−','-').replace('–','-').replace('—','-'); }
    public static String solve(String expression) {
        if (expression.length() > 128) throw new IllegalArgumentException("Expression too long");
        MathParser p = new MathParser(expression); BigDecimal result = p.expression(); p.skip();
        if (p.pos != p.input.length()) throw new IllegalArgumentException("Unexpected character");
        return result.stripTrailingZeros().toPlainString();
    }
    private BigDecimal expression() {
        BigDecimal n = term();
        while (true) { skip(); if (take('+')) n = n.add(term(),MC); else if (take('-')) n = n.subtract(term(),MC); else return n; }
    }
    private BigDecimal term() {
        BigDecimal n = power();
        while (true) { skip(); if (take('*')) n = n.multiply(power(),MC); else if (take('/')) n = n.divide(power(),MC); else if (take('%')) n = n.remainder(power(),MC); else return n; }
    }
    private BigDecimal power() { BigDecimal n = unary(); skip(); if (take('^')) { int exponent = power().intValueExact(); if (Math.abs(exponent)>20) throw new IllegalArgumentException("Exponent too large"); n = exponent < 0 ? BigDecimal.ONE.divide(n.pow(-exponent,MC),MC) : n.pow(exponent,MC); } return n; }
    private BigDecimal unary() { skip(); if (take('-')) return unary().negate(); if (take('+')) return unary(); if (take('(')) { BigDecimal x = expression(); skip(); if (!take(')')) throw new IllegalArgumentException("Missing )"); return x; } return number(); }
    private BigDecimal number() { skip(); int start = pos; while (pos<input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos)=='.')) pos++; if (start==pos) throw new IllegalArgumentException("Expected number"); return new BigDecimal(input.substring(start,pos),MC); }
    private void skip() { while (pos<input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
    private boolean take(char c) { if (pos<input.length() && input.charAt(pos)==c) {pos++; return true;} return false; }
}
