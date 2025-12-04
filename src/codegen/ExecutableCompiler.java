package codegen;

import java.io.*;
import java.nio.file.*;

/**
 * Compilador que converte código C para executável usando GCC.
 * Gerencia o processo de compilação e limpeza de arquivos temporários.
 */
public class ExecutableCompiler {
    
    private String gccPath;
    private boolean keepIntermediateFiles;
    
    public ExecutableCompiler() {
        this.gccPath = "gcc"; // Assume que GCC está no PATH
        this.keepIntermediateFiles = false;
    }
    
    public ExecutableCompiler(String gccPath) {
        this.gccPath = gccPath;
        this.keepIntermediateFiles = false;
    }
    
    /**
     * Define se deve manter os arquivos intermediários (.c)
     */
    public void setKeepIntermediateFiles(boolean keep) {
        this.keepIntermediateFiles = keep;
    }
    
    /**
     * Compila código C para executável.
     * 
     * @param cCode Código C a ser compilado
     * @param outputName Nome do executável de saída (sem extensão)
     * @return CompilationResult com status e mensagens
     */
    public CompilationResult compile(String cCode, String outputName) {
        CompilationResult result = new CompilationResult();
        Path cFile = null;
        
        try {
            // Verifica se GCC está disponível
            if (!isGccAvailable()) {
                result.success = false;
                result.errorMessage = "GCC não encontrado. Instale o GCC e adicione ao PATH do sistema.\n" +
                                     "Windows: Instale MinGW ou TDM-GCC\n" +
                                     "Linux: sudo apt-get install gcc\n" +
                                     "macOS: xcode-select --install";
                return result;
            }
            
            // Cria arquivo .c temporário
            String cFileName = outputName + ".c";
            cFile = Paths.get(cFileName);
            Files.writeString(cFile, cCode);
            
            result.cFilePath = cFile.toAbsolutePath().toString();
            
            // Define nome do executável (adiciona .exe no Windows)
            String exeName = outputName;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                exeName += ".exe";
            }
            
            // Compila com GCC
            ProcessBuilder pb = new ProcessBuilder(
                gccPath,
                cFileName,
                "-o", exeName
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Captura saída do GCC
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                result.success = true;
                result.executablePath = Paths.get(exeName).toAbsolutePath().toString();
                result.message = "Compilação bem-sucedida!\n" +
                               "Executável: " + result.executablePath + "\n" +
                               (keepIntermediateFiles ? "Arquivo C: " + result.cFilePath : "");
            } else {
                result.success = false;
                result.errorMessage = "Erro na compilação GCC:\n" + output.toString();
            }
            
        } catch (IOException e) {
            result.success = false;
            result.errorMessage = "Erro de I/O: " + e.getMessage();
        } catch (InterruptedException e) {
            result.success = false;
            result.errorMessage = "Compilação interrompida: " + e.getMessage();
            Thread.currentThread().interrupt();
        } finally {
            // Remove arquivo .c temporário se configurado
            if (!keepIntermediateFiles && cFile != null) {
                try {
                    Files.deleteIfExists(cFile);
                } catch (IOException e) {
                    // Ignora erros ao deletar arquivo temporário
                }
            }
        }
        
        return result;
    }
    
    /**
     * Verifica se o GCC está disponível no sistema.
     */
    private boolean isGccAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(gccPath, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Executa o arquivo executável gerado.
     */
    public ExecutionResult execute(String executablePath) {
        ExecutionResult result = new ExecutionResult();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(executablePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Captura saída do programa
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            result.success = (exitCode == 0);
            result.output = output.toString();
            result.exitCode = exitCode;
            
        } catch (IOException e) {
            result.success = false;
            result.output = "Erro ao executar: " + e.getMessage();
        } catch (InterruptedException e) {
            result.success = false;
            result.output = "Execução interrompida: " + e.getMessage();
            Thread.currentThread().interrupt();
        }
        
        return result;
    }
    
    /**
     * Resultado da compilação.
     */
    public static class CompilationResult {
        public boolean success;
        public String cFilePath;
        public String executablePath;
        public String message;
        public String errorMessage;
        
        @Override
        public String toString() {
            if (success) {
                return message;
            } else {
                return "ERRO: " + errorMessage;
            }
        }
    }
    
    /**
     * Resultado da execução.
     */
    public static class ExecutionResult {
        public boolean success;
        public String output;
        public int exitCode;
        
        @Override
        public String toString() {
            return output + (success ? "" : "\n[Exit Code: " + exitCode + "]");
        }
    }
}
