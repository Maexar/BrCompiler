package codegen;


/**
 * Gerador de código C a partir do código BR.
 * Versão simplificada que faz parsing direto do texto.
 */
public class CCodeGenerator {
    
    private StringBuilder code;
    private int indentLevel;
    
    public CCodeGenerator() {
        this.code = new StringBuilder();
        this.indentLevel = 0;
    }
    
    /**
     * Gera código C a partir do código-fonte BR.
     */
    public String generate(String brCode) throws Exception {
        code.setLength(0);
        indentLevel = 0;
        
        // Headers
        appendLine("#include <stdio.h>");
        appendLine("#include <stdlib.h>");
        appendLine("#include <stdbool.h>");
        appendLine("#include <string.h>");
        appendLine("");
        
        // Função main
        appendLine("int main() {");
        indentLevel++;
        
        // Parse simples linha por linha
        String[] lines = brCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            processLine(line);
        }
        
        appendLine("return 0;");
        indentLevel--;
        appendLine("}");
        
        return code.toString();
    }
    
    private void processLine(String line) {
        // Remove 'br' do final
        line = line.replaceAll("\\s*br\\s*$", "").trim();
        
        // Ignora tokens estruturais
        if (line.equals("gambiarra") || line.equals("abre-te-sesamo") || 
            line.equals("fecha-te-sesamo")) {
            return;
        }
        
        // Declaração de variável: stonks x receba 10
        if (line.matches("^(stonks|fiado|textao|eh-migue)\\s+\\w+.*")) {
            String translated = line
                .replaceFirst("^stonks", "int")
                .replaceFirst("^fiado", "float")
                .replaceFirst("^textao", "char*")
                .replaceFirst("^eh-migue", "bool")
                .replace("receba", "=")
                .replace("sim", "true")
                .replace("nao", "false");
            appendLine(translated + ";");
            return;
        }
        
        // Atribuição: x receba 5
        if (line.contains("receba")) {
            String translated = line.replace("receba", "=");
            appendLine(translated + ";");
            return;
        }
        
        // Print: printa x
        if (line.startsWith("printa")) {
            String expr = line.substring(6).trim();
            appendLine("printf(\"%d\\n\", " + expr + ");");
            return;
        }
        
        // Scan: papa-entrada x
        if (line.startsWith("papa-entrada")) {
            String var = line.substring(12).trim();
            appendLine("scanf(\"%d\", &" + var + ");");
            return;
        }
    }
    
    private void appendLine(String line) {
        code.append("    ".repeat(indentLevel)).append(line).append("\n");
    }
}
