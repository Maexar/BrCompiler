package recovery;

import java.util.Stack;
import parser.BrCompilerConstants;

public class DelimiterBalancer {

    private static class Delimiter {
        int tokenKind;
        int line;
        int column;

        Delimiter(int tokenKind, int line, int column) {
            this.tokenKind = tokenKind;
            this.line = line;
            this.column = column;
        }
    }

    private Stack<Delimiter> stack;

    public DelimiterBalancer() {
        stack = new Stack<>();
    }

    public void processToken(int tokenKind, int line, int column) throws UnbalancedDelimiterException {
        if (isOpening(tokenKind)) {
            stack.push(new Delimiter(tokenKind, line, column));
        } else if (isClosing(tokenKind)) {
            if (stack.isEmpty()) {
                String closeName = getDelimiterName(tokenKind);
                throw new UnbalancedDelimiterException(
                    "'" + closeName + "' inesperado - nenhum bloco foi aberto", 
                    tokenKind, line, column);
            }
            Delimiter last = stack.peek();
            if (!matches(last.tokenKind, tokenKind)) {
                String expected = getClosingName(last.tokenKind);
                String found = getDelimiterName(tokenKind);
                throw new UnbalancedDelimiterException(
                    "Esperado '" + expected + "' mas encontrado '" + found + "'", 
                    tokenKind, line, column);
            }
            stack.pop();
        }
    }

    public void checkBalance() throws UnbalancedDelimiterException {
        if (!stack.isEmpty()) {
            Delimiter last = stack.peek();
            String openName = getDelimiterName(last.tokenKind);
            String closeName = getClosingName(last.tokenKind);
            throw new UnbalancedDelimiterException(
                "Bloco '" + openName + "' nao foi fechado - falta '" + closeName + "'", 
                last.tokenKind, last.line, last.column);
        }
    }

    private boolean isOpening(int kind) {
        return kind == BrCompilerConstants.ABREBLOCO || kind == BrCompilerConstants.ABRIREXP;
    }

    private boolean isClosing(int kind) {
        return kind == BrCompilerConstants.FECHABLOCO || kind == BrCompilerConstants.FECHAREXP;
    }

    private boolean matches(int openKind, int closeKind) {
        if (openKind == BrCompilerConstants.ABREBLOCO && closeKind == BrCompilerConstants.FECHABLOCO)
            return true;
        if (openKind == BrCompilerConstants.ABRIREXP && closeKind == BrCompilerConstants.FECHAREXP)
            return true;
        return false;
    }

    private String getDelimiterName(int kind) {
        switch (kind) {
            case BrCompilerConstants.ABREBLOCO: return "abre-te-sesamo";
            case BrCompilerConstants.FECHABLOCO: return "fecha-te-sesamo";
            case BrCompilerConstants.ABRIREXP: return "[";
            case BrCompilerConstants.FECHAREXP: return "]";
            default: return "desconhecido";
        }
    }

    private String getClosingName(int openKind) {
        switch (openKind) {
            case BrCompilerConstants.ABREBLOCO: return "fecha-te-sesamo";
            case BrCompilerConstants.ABRIREXP: return "]";
            default: return "desconhecido";
        }
    }

    public static class UnbalancedDelimiterException extends Exception {
        private static final long serialVersionUID = 1L;
        public int tokenKind;
        public int line;
        public int column;

        public UnbalancedDelimiterException(String message, int tokenKind, int line, int column) {
            super(message + " [linha " + line + ", coluna " + column + "]");
            this.tokenKind = tokenKind;
            this.line = line;
            this.column = column;
        }
    }
}
