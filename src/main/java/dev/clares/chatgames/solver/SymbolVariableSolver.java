package dev.clares.chatgames.solver;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Solves ChatGames' multiline symbol equations, including a symbol named by "solve for". */
public final class SymbolVariableSolver {
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final Pattern TARGET=Pattern.compile("(?iu)(?:solve\\s+for|resuelve\\s+(?:para|el\\s+valor\\s+de)|encuentra\\s+(?:el\\s+valor\\s+de)?)\\s*[:：]?\\s*[`'\"]([^`'\"\\s]{1,16})[`'\"]");
    private static final Pattern EQUATION=Pattern.compile("^(.{1,160}?)\\s*=\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*$");

    public static boolean hasTarget(String block) { return TARGET.matcher(block).find(); }

    public static Optional<String> solve(String block) {
        try {
            Matcher targetMatcher=TARGET.matcher(block);
            if (!targetMatcher.find()) return Optional.empty();
            String target=targetMatcher.group(1);
            List<Map<String,BigDecimal>> rows=new ArrayList<>();
            List<BigDecimal> totals=new ArrayList<>();
            Set<String> symbols=new LinkedHashSet<>();
            for (String line:block.split("\\R")) {
                Matcher equation=EQUATION.matcher(line.trim());
                if (!equation.matches()) continue;
                Map<String,BigDecimal> coefficients=parseSum(equation.group(1));
                if (coefficients.isEmpty()) continue;
                rows.add(coefficients);
                totals.add(new BigDecimal(equation.group(2)));
                symbols.addAll(coefficients.keySet());
            }
            if (!symbols.contains(target) || symbols.size()>8 || rows.size()<symbols.size() || rows.size()>12) return Optional.empty();
            List<String> variables=new ArrayList<>(symbols);
            int n=variables.size(), m=rows.size();
            BigDecimal[][] matrix=new BigDecimal[m][n+1];
            for (int row=0;row<m;row++) {
                for (int col=0;col<n;col++) matrix[row][col]=rows.get(row).getOrDefault(variables.get(col),BigDecimal.ZERO);
                matrix[row][n]=totals.get(row);
            }
            int pivotRow=0;
            for (int col=0;col<n;col++) {
                int nonzero=pivotRow;
                while (nonzero<m && matrix[nonzero][col].signum()==0) nonzero++;
                if (nonzero==m) return Optional.empty();
                BigDecimal[] swap=matrix[pivotRow];matrix[pivotRow]=matrix[nonzero];matrix[nonzero]=swap;
                BigDecimal divisor=matrix[pivotRow][col];
                for (int j=col;j<=n;j++) matrix[pivotRow][j]=matrix[pivotRow][j].divide(divisor,MC);
                for (int row=0;row<m;row++) if(row!=pivotRow) {
                    BigDecimal factor=matrix[row][col];
                    for (int j=col;j<=n;j++) matrix[row][j]=matrix[row][j].subtract(factor.multiply(matrix[pivotRow][j],MC),MC);
                }
                pivotRow++;
            }
            for (int row=n;row<m;row++) if (matrix[row][n].signum()!=0) return Optional.empty();
            return Optional.of(matrix[variables.indexOf(target)][n].stripTrailingZeros().toPlainString());
        } catch (IllegalArgumentException | ArithmeticException e) { return Optional.empty(); }
    }

    private static Map<String,BigDecimal> parseSum(String source) {
        Map<String,BigDecimal> terms=new LinkedHashMap<>();
        int sign=1,start=0;
        for (int i=0;i<=source.length();i++) {
            if (i<source.length() && source.charAt(i)!='+' && source.charAt(i)!='-') continue;
            String symbol=source.substring(start,i).strip();
            if (symbol.isEmpty()) {
                if (i==0 && i<source.length()) {sign=source.charAt(i)=='-'?-1:1;start=i+1;continue;}
                throw new IllegalArgumentException("Empty term");
            }
            if (symbol.length()>16 || symbol.matches(".*[=\\d`].*")) throw new IllegalArgumentException("Invalid symbol");
            terms.merge(symbol,BigDecimal.valueOf(sign),BigDecimal::add);
            if (i<source.length()) sign=source.charAt(i)=='-'?-1:1;
            start=i+1;
        }
        return terms;
    }
}
