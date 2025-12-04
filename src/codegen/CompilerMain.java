package codegen;

import java.io.*;
import java.nio.file.*;

/**
 * Classe principal para compilar programas BrCompiler para executáveis.
 * Integra o parser, gerador de código C e o compilador GCC.
 * 
 * Uso:
 *   java codegen.CompilerMain <arquivo.br>
 *   java codegen.CompilerMain <arquivo.br> -o <nome_saida>
 *   java codegen.CompilerMain <arquivo.br> -run (compila e executa)
 */
public class CompilerMain {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputName = null;
        boolean runAfterCompile = false;
        boolean keepCFile = false;
        boolean verbose = false;
        
        // Processa argumentos
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    if (i + 1 < args.length) {
                        outputName = args[++i];
                    }
                    break;
                case "-run":
                case "-r":
                    runAfterCompile = true;
                    break;
                case "-keep":
                case "-k":
                    keepCFile = true;
                    break;
                case "-verbose":
                case "-v":
                    verbose = true;
                    break;
                default:
                    System.err.println("Argumento desconhecido: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }
        
        // Define nome de saída padrão
        if (outputName == null) {
            outputName = inputFile.replaceAll("\\.br$", "");
            if (outputName.equals(inputFile)) {
                outputName = "output";
            }
        }
        
        try {
            System.out.println("=== BrCompiler - Compilador para Executavel ===\n");
            
            // 1. Lê o arquivo .br
            if (verbose) System.out.println("[1/4] Lendo arquivo: " + inputFile);
            String brCode = Files.readString(Paths.get(inputFile));
            
            // 2. Gera código C
            if (verbose) System.out.println("[2/4] Gerando código C...");
            CCodeGenerator generator = new CCodeGenerator();
            String cCode = generator.generate(brCode);
            
            if (verbose) {
                System.out.println("\n--- Codigo C Gerado ---");
                System.out.println(cCode);
                System.out.println("--- Fim do Codigo C ---\n");
            }
            
            // 3. Compila para executável
            if (verbose) System.out.println("[3/4] Compilando com GCC...");
            ExecutableCompiler compiler = new ExecutableCompiler();
            compiler.setKeepIntermediateFiles(keepCFile);
            
            ExecutableCompiler.CompilationResult result = compiler.compile(cCode, outputName);
            
            if (!result.success) {
                System.err.println("\nFalha na compilacao:");
                System.err.println(result.errorMessage);
                System.exit(1);
            }
            
            System.out.println("\nSucesso! " + result.message);
            
            // 4. Executa (opcional)
            if (runAfterCompile) {
                if (verbose) System.out.println("[4/4] Executando programa...");
                System.out.println("\n=== Saida do Programa ===");
                ExecutableCompiler.ExecutionResult execResult = compiler.execute(result.executablePath);
                System.out.println(execResult.output);
                System.out.println("========================\n");
                
                if (!execResult.success) {
                    System.err.println("Aviso: Programa terminou com codigo de erro: " + execResult.exitCode);
                }
            } else {
                System.out.println("\nPara executar: " + result.executablePath);
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("ERRO: Arquivo nao encontrado: " + inputFile);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERRO: Erro ao ler arquivo: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERRO: Erro durante compilacao: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    /**
     * Compila um código BR diretamente (sem arquivo).
     */
    public static CompilationResult compileCode(String brCode, String outputName) {
        CompilationResult result = new CompilationResult();
        
        try {
            // Gera código C
            CCodeGenerator generator = new CCodeGenerator();
            String cCode = generator.generate(brCode);
            result.cCode = cCode;
            
            // Compila
            ExecutableCompiler compiler = new ExecutableCompiler();
            ExecutableCompiler.CompilationResult compResult = compiler.compile(cCode, outputName);
            
            result.success = compResult.success;
            result.executablePath = compResult.executablePath;
            result.errorMessage = compResult.errorMessage;
            
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
        }
        
        return result;
    }
    
    private static void printUsage() {
        System.out.println("Uso: java codegen.CompilerMain <arquivo.br> [opções]");
        System.out.println("\nOpções:");
        System.out.println("  -o <nome>     Nome do executável de saída");
        System.out.println("  -run, -r      Compila e executa imediatamente");
        System.out.println("  -keep, -k     Mantém arquivo .c intermediário");
        System.out.println("  -verbose, -v  Modo verboso (mostra código C gerado)");
        System.out.println("\nExemplos:");
        System.out.println("  java codegen.CompilerMain programa.br");
        System.out.println("  java codegen.CompilerMain programa.br -o meu_programa");
        System.out.println("  java codegen.CompilerMain programa.br -run");
        System.out.println("  java codegen.CompilerMain programa.br -v -k");
    }
    
    /**
     * Resultado da compilação completa.
     */
    public static class CompilationResult {
        public boolean success;
        public String cCode;
        public String executablePath;
        public String errorMessage;
    }
}
