package recovery;

import parser.BrCompiler;
import parser.ParseException;
import parser.Token;
import java.util.ArrayList;
import java.util.List;

/*
 * gerencia erros
 * acumula erros sintaticos encontrados durante a
 * rec do panico
 */

public class ErrorManager {
    
    // Lista de erros encontrados
    private static List<SyntaxError> errors = new ArrayList<>();
    
    // Contador de erros
    private static int errorCount = 0;
    
    // Flag para evitar erros cascata
    private static boolean recoveryInProgress = false;
    
    // Tempo limite para evitar loop infinito (em tokens)
    private static int recoveryTokensConsumed = 0;
    private static final int MAX_RECOVERY_TOKENS = 50;
    
    /**
     * Representa um erro sintático encontrado
     */
    public static class SyntaxError {
        private final String message;
        private final int line;
        private final int column;
        private final String foundToken;
        private final String expectedTokens;
        private final String context;
        
        public SyntaxError(String message, int line, int column, 
                          String foundToken, String expectedTokens, String context) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.foundToken = foundToken;
            this.expectedTokens = expectedTokens;
            this.context = context;
        }
        
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getFoundToken() { return foundToken; }
        public String getExpectedTokens() { return expectedTokens; }
        public String getContext() { return context; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ERRO SINTATICO [").append(context).append("]\n");
            sb.append("  Linha ").append(line).append(", Coluna ").append(column).append("\n");
            sb.append("  Token encontrado: '").append(foundToken).append("'\n");
            if (expectedTokens != null && !expectedTokens.isEmpty()) {
                sb.append("  Era esperado: ").append(expectedTokens).append("\n");
            }
            if (message != null && !message.isEmpty()) {
                sb.append("  Mensagem: ").append(message).append("\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * Registra um erro sintático (com proteção contra cascata)
     */
    public static void addError(ParseException e, String context, RecoverySet syncSet) {
        // Evita erros redundantes durante recuperação
        if (recoveryInProgress) {
            return;
        }
        
        // Evita múltiplos erros no mesmo contexto
        if (errors.stream().anyMatch(err -> err.context.equals(context) && 
                                   err.line == getErrorLine(e) && 
                                   err.foundToken.equals(getErrorToken(e)))) {
            return;
        }
        
        errorCount++;
        
        Token errorToken = null;
        if (e.currentToken != null && e.currentToken.next != null) {
            errorToken = e.currentToken.next;
        } else if (e.currentToken != null) {
            errorToken = e.currentToken;
        }
        
        String foundToken = (errorToken != null && errorToken.image != null) 
                           ? errorToken.image 
                           : "EOF";
        
        int line = (errorToken != null) ? errorToken.beginLine : 0;
        int column = (errorToken != null) ? errorToken.beginColumn : 0;
        
        String expectedTokens = buildExpectedTokensString(e);
        String message = buildErrorMessage(e, context, syncSet);
        
        SyntaxError error = new SyntaxError(message, line, column, 
                                           foundToken, expectedTokens, context);
        errors.add(error);
        
        // NÃO FAZ PRINT IMEDIATO - apenas registra
        // O print será feito no relatório final
    }
    
    /**
     * Inicia processo de recuperação
     */
    public static void startRecovery() {
        recoveryInProgress = true;
        recoveryTokensConsumed = 0;
    }
    
    /**
     * Finaliza processo de recuperação
     */
    public static void endRecovery() {
        recoveryInProgress = false;
    }
    
    /**
     * Verifica se está em processo de recuperação
     */
    public static boolean isRecoveryInProgress() {
        return recoveryInProgress;
    }
    
    /**
     * Registra token consumido durante recuperação
     */
    public static void tokenConsumed() {
        if (recoveryInProgress) {
            recoveryTokensConsumed++;
            // Proteção contra loop infinito
            if (recoveryTokensConsumed > MAX_RECOVERY_TOKENS) {
                throw new RuntimeException("Recuperacao infinita detectada - mais de " + 
                                         MAX_RECOVERY_TOKENS + " tokens consumidos");
            }
        }
    }
    
    /**
     * Constrói a mensagem de erro apropriada
     */
    private static String buildErrorMessage(ParseException e, String context, RecoverySet syncSet) {
        StringBuilder msg = new StringBuilder();
        
        // Verifica se é erro de bloco não fechado
        if (e.expectedTokenSequences != null && e.expectedTokenSequences.length > 0) {
            for (int[] sequence : e.expectedTokenSequences) {
                for (int tokenType : sequence) {
                    if (tokenType == BrCompiler.FECHABLOCO) {
                        return "Bloco nao fechado - faltou fecha-te-sesamo";
                    }
                }
            }
        }
        
        // Mensagens específicas por contexto
        switch (context) {
            case "condicao":
                return "Estrutura condicional malformada - verifique sepa [...] abre-te-sesamo ... fecha-te-sesamo";
            case "expressaoCondicional":
                return "Expressao condicional incompleta";
            case "declaraVariavel":
                return "Declaracao de variavel invalida";
            case "forLoop":
                return "Estrutura de repeticao pet malformada";
            case "whileLoop":
                return "Estrutura de repeticao repet malformada";
            case "declaraFuncao":
                return "Declaracao de funcao invalida";
            case "comando":
                return "Comando invalido";
            case "bloco":
                return "Bloco de comandos malformado";
            default:
                return "Erro sintatico em " + context;
        }
    }
    
    /**
     * Constrói string com tokens esperados (com proteção null)
     */
    private static String buildExpectedTokensString(ParseException e) {
        if (e.expectedTokenSequences == null || e.expectedTokenSequences.length == 0) {
            return "";
        }
        
        StringBuilder expected = new StringBuilder();
        boolean first = true;
        
        for (int[] sequence : e.expectedTokenSequences) {
            if (!first) expected.append(" ou ");
            
            for (int i = 0; i < sequence.length; i++) {
                if (i > 0) expected.append(" ");
                expected.append("'").append(BrCompiler.obterTokenPortugues(sequence[i])).append("'");
            }
            first = false;
        }
        
        return expected.toString();
    }
    
    /**
     * Obtém linha do erro (com proteção)
     */
    private static int getErrorLine(ParseException e) {
        Token token = null;
        if (e.currentToken != null && e.currentToken.next != null) {
            token = e.currentToken.next;
        } else if (e.currentToken != null) {
            token = e.currentToken;
        }
        return (token != null) ? token.beginLine : 0;
    }
    
    /**
     * Obtém token do erro (com proteção)
     */
    private static String getErrorToken(ParseException e) {
        Token token = null;
        if (e.currentToken != null && e.currentToken.next != null) {
            token = e.currentToken.next;
        } else if (e.currentToken != null) {
            token = e.currentToken;
        }
        return (token != null && token.image != null) ? token.image : "EOF";
    }
    
    /**
     * Retorna todos os erros acumulados
     */
    public static List<SyntaxError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Retorna número total de erros
     */
    public static int getErrorCount() {
        return errorCount;
    }
    
    /**
     * Verifica se há erros
     */
    public static boolean hasErrors() {
        return errorCount > 0;
    }
    
    /**
     * Gera relatório completo de erros
     */
    public static String getErrorReport() {
        if (errors.isEmpty()) {
            return "Nenhum erro encontrado.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("\n============================================\n");
        report.append("  RELATORIO DE ERROS SINTATICOS\n");
        report.append("============================================\n");
        report.append("Total de erros encontrados: ").append(errorCount).append("\n\n");
        
        for (int i = 0; i < errors.size(); i++) {
            report.append("ERRO #").append(i + 1).append(":\n");
            report.append(errors.get(i).toString());
            report.append("\n");
        }
        
        report.append("============================================\n");
        return report.toString();
    }
    
    /**
     * Limpa todos os erros (preparar para novo parsing)
     */
    public static void clear() {
        errors.clear();
        errorCount = 0;
        recoveryInProgress = false;
        recoveryTokensConsumed = 0;
    }
}
