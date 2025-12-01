package recovery;

import parser.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ErrorManager {
    private static List<SyntaxError> errors = new ArrayList<>();
    private static boolean recoveryInProgress = false;
    private static int tokensConsumedDuringRecovery = 0;

    public static class SyntaxError {
        private String message;
        private int line;
        private int column;
        private String context;
        private String foundToken;
        private String expectedTokens;

        public SyntaxError(String message, int line, int column, String context, 
                          String foundToken, String expectedTokens) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.context = context;
            this.foundToken = foundToken;
            this.expectedTokens = expectedTokens;
        }

        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getContext() { return context; }
        public String getFoundToken() { return foundToken; }
        public String getExpectedTokens() { return expectedTokens; }
        
        /**
         * Verifica se este erro é um erro em cascata (linha 0 ou EOF sem posicao valida)
         */
        public boolean isCascadingError() {
            return (line == 0 && column == 0) || 
                   (foundToken != null && foundToken.equals("EOF") && line == 0);
        }
        
        /**
         * Verifica se este erro é duplicado de outro (mesma linha/coluna/token)
         */
        public boolean isDuplicateOf(SyntaxError other) {
            if (other == null) return false;
            return this.line == other.line && 
                   this.column == other.column &&
                   this.foundToken != null && 
                   this.foundToken.equals(other.foundToken);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            if (line > 0) {
                sb.append(" [linha ").append(line).append(", col ").append(column).append("]");
            }
            if (foundToken != null && !foundToken.isEmpty()) {
                sb.append("\n  Token encontrado: ").append(foundToken);
            }
            if (expectedTokens != null && !expectedTokens.isEmpty()) {
                sb.append("\n  Era esperado: ").append(expectedTokens);
            }
            return sb.toString();
        }
    }

    public static void addError(ParseException e, String context, RecoverySet recoverySet) {
        if (e == null) return;

        String foundToken = "";
        int line = 0;
        int column = 0;

        if (e.currentToken != null && e.currentToken.next != null) {
            foundToken = e.currentToken.next.image;
            line = e.currentToken.next.beginLine;
            column = e.currentToken.next.beginColumn;
        } else if (e.currentToken != null) {
            foundToken = e.currentToken.image;
            line = e.currentToken.beginLine;
            column = e.currentToken.beginColumn;
        }

        String expectedTokens = "";
        if (e.expectedTokenSequences != null && e.expectedTokenSequences.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < e.expectedTokenSequences.length; i++) {
                if (i > 0) sb.append(" ou ");
                int[] seq = e.expectedTokenSequences[i];
                for (int j = 0; j < seq.length; j++) {
                    if (j > 0) sb.append(" ");
                    sb.append("'").append(parser.BrCompiler.obterTokenPortugues(seq[j])).append("'");
                }
            }
            expectedTokens = sb.toString();
        }

        String errorMessage = gerarMensagemErro(context, foundToken, expectedTokens);
        
        SyntaxError newError = new SyntaxError(
            errorMessage,
            line,
            column,
            context,
            foundToken,
            expectedTokens
        );

        System.out.println("[ErrorManager] addError chamado - context: " + context + 
                          ", recoveryInProgress: " + recoveryInProgress);

        // FILTRO 1: Ignora erros em cascata (linha 0, coluna 0)
        if (newError.isCascadingError()) {
            System.out.println("[ErrorManager] Erro em cascata IGNORADO (linha 0): " + errorMessage);
            return;
        }
        
        // FILTRO 2: Ignora duplicatas (mesma posicao e token)
        if (!errors.isEmpty()) {
            SyntaxError lastError = errors.get(errors.size() - 1);
            if (newError.isDuplicateOf(lastError)) {
                System.out.println("[ErrorManager] Erro duplicado IGNORADO: " + errorMessage);
                return;
            }
        }

        errors.add(newError);
        System.out.println("[ErrorManager] ERRO ADICIONADO: " + errorMessage + 
                          " [linha " + line + ", col " + column + "]");
        System.out.println("[ErrorManager] Total de erros agora: " + errors.size());
    }

    private static String gerarMensagemErro(String context, String foundToken, String expectedTokens) {
        switch (context) {
            case "main":
                if (foundToken.equals("EOF")) {
                    return "Bloco nao fechado - faltou fecha-te-sesamo";
                }
                return "Erro sintatico em main";
                
            case "declaraVariavel":
                return "Declaracao de variavel invalida";
                
            case "expressaoCondicional":
            case "condicao":
                return "Estrutura condicional malformada";
                
            case "print":
                return "Comando printa invalido";
                
            case "scan":
                return "Comando papa-entrada invalido";
                
            case "whileLoop":
                return "Estrutura de repeticao repet invalida";
                
            case "forLoop":
                return "Estrutura de repeticao pet invalida";
                
            case "declaraFuncao":
                return "Declaracao de funcao vai-filhao invalida";
                
            case "comandoIdentificador":
                return "Comando identificador invalido";
                
            case "comandoIdentificadorSufixo":
                return "Comando invalido apos identificador";
                
            case "comando":
                return "Comando invalido";
                
            case "bloco":
                return "Bloco de comandos malformado";
                
            default:
                return "Erro sintatico em " + context;
        }
    }

    public static void startRecovery() {
        recoveryInProgress = true;
        tokensConsumedDuringRecovery = 0;
        System.out.println("[ErrorManager] Iniciando recuperacao de panico");
    }

    public static void endRecovery() {
        recoveryInProgress = false;
        System.out.println("[ErrorManager] Finalizando recuperacao de panico - tokens consumidos: " + 
                          tokensConsumedDuringRecovery);
    }

    public static void tokenConsumed() {
        if (recoveryInProgress) {
            tokensConsumedDuringRecovery++;
        }
    }

    public static boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static List<SyntaxError> getErrors() {
        return new ArrayList<>(errors);
    }

    public static void clear() {
        errors.clear();
        recoveryInProgress = false;
        tokensConsumedDuringRecovery = 0;
        System.out.println("[ErrorManager] Estado limpo");
    }

    public static String getErrorReport() {
        if (errors.isEmpty()) {
            return "Nenhum erro encontrado.";
        }

        StringBuilder report = new StringBuilder();
        report.append("\n========================================\n");
        report.append("RELATORIO DE ERROS DE SINTAXE\n");
        report.append("========================================\n");
        report.append("Total de erros: ").append(errors.size()).append("\n\n");

        for (int i = 0; i < errors.size(); i++) {
            report.append("ERRO #").append(i + 1).append(":\n");
            report.append(errors.get(i).toString());
            report.append("\n\n");
        }

        report.append("========================================\n");
        return report.toString();
    }
}
