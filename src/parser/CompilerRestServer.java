package parser;

import com.sun.net.httpserver.*;
import recovery.ErrorManager;
import recovery.DelimiterBalancer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class CompilerRestServer {
    private static final int PORT = 8085;
    
    // ===== LOCK ÚNICO PARA TODAS AS OPERAÇÕES =====
    private static final Object COMPILER_LOCK = new Object();
    
    // ===== ESTADO DO COMPILADOR =====
    private static volatile boolean compilerInitialized = false;
    private static BrCompiler compiler = null;
    
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        
        server.createContext("/api/compile", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                handleCompile(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.createContext("/api/health", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
                return;
            }
            
            String response = "{\"status\":\"ok\",\"compiler\":\"BrCompiler\",\"version\":\"1.0\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
        server.createContext("/api/ast", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                handleAst(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.createContext("/api/tokens", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                handleTokens(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.start();
        
        System.out.println("\n");
        System.out.println("SERVIDOR COMPILADOR INICIADO");
        System.out.println("\nEndpoints disponiveis:");
        System.out.println("   POST http://localhost:" + PORT + "/api/compile");
        System.out.println("   POST http://localhost:" + PORT + "/api/ast");
        System.out.println("   POST http://localhost:" + PORT + "/api/tokens");
        System.out.println("   GET  http://localhost:" + PORT + "/api/health");
        System.out.println("\nMANTENHA ESTE TERMINAL ABERTO ENQUANTO DESENVOLVE\n");
    }
    
    private static void handleCORS(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
    
    private static void handleCompile(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String code = extractCode(body);
            
            System.out.println("[DEBUG] Codigo recebido para compilacao");
            
            String result = compileCode(code);
            
            System.out.println("[DEBUG] Resposta: " + result);
            
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            System.out.println("[ERRO] Excecao em /api/compile: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, e.getMessage());
        }
    }
    
    private static void handleAst(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String code = extractCode(body);
            
            System.out.println("[DEBUG] Gerando AST");
            
            String astJson = generateAst(code);
            String result = "{\"success\":true,\"ast\":" + astJson + "}";
            
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            System.out.println("[ERRO] Excecao em /api/ast: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, e.getMessage());
        }
    }
    
    private static void handleTokens(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String code = extractCode(body);
            
            System.out.println("[DEBUG] Extraindo tokens");
            
            String tokensJson = extractTokens(code);
            String result = "{\"success\":true,\"tokens\":" + tokensJson + "}";
            
            System.out.println("[DEBUG] Resposta de tokens enviada");
            
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            System.out.println("[ERRO] Excecao em /api/tokens: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, e.getMessage());
        }
    }
    
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
    
    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMsg) {
        try {
            String response = "{\"success\":false,\"error\":\"" + escapeJson(errorMsg) + "\"}";
            sendJsonResponse(exchange, statusCode, response);
        } catch (Exception ignored) {
            try {
                exchange.close();
            } catch (Exception e) {}
        }
    }
    
    private static String compileCode(String code) {
        synchronized (COMPILER_LOCK) {
            if (code == null || code.trim().isEmpty()) {
                return "{\"success\":false,\"error\":\"Codigo vazio\"}";
            }
            
            try {
                System.out.println("[COMPILE] Iniciando compilacao");
                
                ErrorManager.clear();
                BrCompiler.eof = false;
                BrCompiler.lastError = null;
                BrCompiler.delimiterBalancer = new DelimiterBalancer();
                
                java.io.StringReader reader = new java.io.StringReader(code);
                
                if (!compilerInitialized || compiler == null) {
                    System.out.println("[COMPILE] Criando instancia BrCompiler");
                    compiler = new BrCompiler(reader);
                    compilerInitialized = true;
                    BrCompiler.parserInitialized = true;
                } else {
                    System.out.println("[COMPILE] ReInit do BrCompiler");
                    compiler.ReInit(reader);
                }
                
                compiler.main();
                
                List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
                
                if (ErrorManager.hasErrors()) {
                    allErrors.addAll(ErrorManager.getErrors());
                }
                
                try {
                    BrCompiler.delimiterBalancer.checkBalance();
                } catch (DelimiterBalancer.UnbalancedDelimiterException ex) {
                    System.out.println("[DEBUG] Erro de balanceamento detectado");
                    ErrorManager.SyntaxError balanceError = new ErrorManager.SyntaxError(
                        "Erro de balanceamento: " + ex.getMessage(),
                        ex.line, ex.column, "balanceamento", "EOF", "fecha-te-sesamo"
                    );
                    allErrors.add(balanceError);
                }
                
                if (!allErrors.isEmpty()) {
                    System.out.println("[DEBUG] Total de " + allErrors.size() + " erro(s) encontrado(s)");
                    return buildMultipleErrorsResponse(allErrors);
                }
                
                System.out.println("[COMPILE] Compilacao bem-sucedida!");
                return "{\"success\":true,\"message\":\"Codigo compilado com sucesso\",\"lines\":" + 
                       countLines(code) + "}";
                
            } catch (ParseException e) {
                System.out.println("[DEBUG] ParseException capturada");
                
                List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
                
                if (ErrorManager.hasErrors()) {
                    allErrors.addAll(ErrorManager.getErrors());
                } else {
                    String errorMsg = BrCompiler.handleParseError(e);
                    ErrorManager.SyntaxError parseError = extractErrorFromMessage(errorMsg, "parseException");
                    allErrors.add(parseError);
                }
                
                return buildMultipleErrorsResponse(allErrors);
                
            } catch (TokenMgrError e) {
                System.out.println("[DEBUG] TokenMgrError capturado");
                String errorMsg = BrCompiler.handleTokenMgrError(e);
                ErrorManager.SyntaxError lexError = extractErrorFromMessage(errorMsg, "lexico");
                
                List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
                allErrors.add(lexError);
                return buildMultipleErrorsResponse(allErrors);
                
            } catch (Exception e) {
                System.out.println("[DEBUG] Exception generica capturada");
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
                e.printStackTrace();
                return "{\"success\":false,\"error\":\"" + escapeJson(errorMsg) + "\"}";
            }
        }
    }
    
    private static String extractTokens(String code) {
        synchronized (COMPILER_LOCK) {
            if (code == null || code.trim().isEmpty()) {
                System.out.println("[TOKENS] Codigo vazio, retornando array vazio");
                return "[]";
            }

            try {
                System.out.println("[TOKENS] Iniciando extracao de tokens");
                System.out.println("[TOKENS] Tamanho do codigo: " + code.length());
                
                BrCompiler.ReInit(new StringReader(code));
                
                StringBuilder json = new StringBuilder();
                json.append("[");
                
                boolean first = true;
                int tokenCount = 0;
                int maxTokens = 10000;
                
                System.out.println("[TOKENS] Iniciando loop de tokens");
                
                while (tokenCount < maxTokens) {
                    Token token = BrCompiler.getNextToken();
                    tokenCount++;
                    
                    if (token == null || token.kind == BrCompilerConstants.EOF || token.kind == 0) {
                        System.out.println("[TOKENS] EOF encontrado apos " + tokenCount + " tokens");
                        break;
                    }
                    
                    if (tokenCount % 100 == 0) {
                        System.out.println("[TOKENS] Processados " + tokenCount + " tokens...");
                    }
                    
                    if (!first) {
                        json.append(",");
                    }
                    
                    json.append("{");
                    json.append("\"tipo\":\"").append(escapeJson(getTokenTypeName(token.kind))).append("\",");
                    json.append("\"valor\":\"").append(escapeJson(token.image)).append("\",");
                    json.append("\"linha\":").append(token.beginLine).append(",");
                    json.append("\"coluna\":").append(token.beginColumn);
                    json.append("}");
                    
                    first = false;
                }
                
                if (tokenCount >= maxTokens) {
                    System.out.println("[AVISO] Limite de tokens atingido (" + maxTokens + ")");
                }
                
                json.append("]");
                
                System.out.println("[TOKENS] Total de tokens extraidos: " + (tokenCount - 1));
                return json.toString();
                
            } catch (TokenMgrError e) {
                System.out.println("[ERRO] TokenMgrError: " + e.getMessage());
                e.printStackTrace();
                return "[]";
            } catch (Exception e) {
                System.out.println("[ERRO] Exception: " + e.getMessage());
                e.printStackTrace();
                return "[]";
            }
        }
    }
    
    private static String generateAst(String code) {
        synchronized (COMPILER_LOCK) {
            try {
                System.out.println("[AST] Gerando AST para codigo recebido");
                return AstGenerator.generateAst(code);
            } catch (Exception e) {
                System.out.println("[ERRO] Erro ao gerar AST: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Erro ao gerar AST", e);
            }
        }
    }
    
    private static ErrorManager.SyntaxError extractErrorFromMessage(String errorMsg, String context) {
        int line = 0;
        int column = 0;
        int posicaoStart = errorMsg.indexOf("[POSICAO]");
        int posicaoEnd = errorMsg.indexOf("[/POSICAO]");
        
        if (posicaoStart != -1 && posicaoEnd != -1 && posicaoEnd > posicaoStart) {
            try {
                String posicaoStr = errorMsg.substring(posicaoStart + 9, posicaoEnd);
                String[] partes = posicaoStr.split(",");
                if (partes.length == 2) {
                    line = Integer.parseInt(partes[0].trim());
                    column = Integer.parseInt(partes[1].trim());
                }
                errorMsg = errorMsg.substring(0, posicaoStart) + 
                           (posicaoEnd + 12 < errorMsg.length() ? 
                            errorMsg.substring(posicaoEnd + 12) : "");
                errorMsg = errorMsg.trim();
            } catch (Exception ex) {
                System.out.println("[AVISO] Erro ao extrair posicao: " + ex.getMessage());
            }
        }
        
        return new ErrorManager.SyntaxError(errorMsg, line, column, context, "", "");
    }
    
    private static String buildMultipleErrorsResponse(List<ErrorManager.SyntaxError> errors) {
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":false,\"errors\":[");
        
        boolean first = true;
        for (ErrorManager.SyntaxError error : errors) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"message\":\"").append(escapeJson(error.getMessage())).append("\",");
            json.append("\"line\":").append(error.getLine()).append(",");
            json.append("\"column\":").append(error.getColumn()).append(",");
            json.append("\"context\":\"").append(escapeJson(error.getContext())).append("\",");
            json.append("\"foundToken\":\"").append(escapeJson(error.getFoundToken())).append("\",");
            json.append("\"expectedTokens\":\"").append(escapeJson(error.getExpectedTokens())).append("\"");
            json.append("}");
            first = false;
        }
        
        json.append("]}");
        return json.toString();
    }
    
    private static int countLines(String code) {
        if (code == null || code.isEmpty()) return 0;
        return code.split("\n").length;
    }
    
    private static String extractCode(String json) {
        try {
            int codeIndex = json.indexOf("\"code\":");
            if (codeIndex == -1) return "";
            
            int valueStart = json.indexOf("\"", codeIndex + 7);
            if (valueStart == -1) return "";
            
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length()) {
                if (json.charAt(valueEnd) == '\"' && json.charAt(valueEnd - 1) != '\\') {
                    break;
                }
                valueEnd++;
            }
            
            if (valueEnd >= json.length()) return "";
            
            String code = json.substring(valueStart + 1, valueEnd);
            return code.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
        } catch (Exception e) {
            System.err.println("Erro ao extrair codigo: " + e.getMessage());
        }
        return "";
    }
    
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "'")
                   .replace("\n", " ")
                   .replace("\r", " ")
                   .replace("\t", " ")
                   .replace("\\", "/");
    }
    
    private static String getTokenTypeName(int tokenKind) {
        try {
            if (tokenKind < 0 || tokenKind >= BrCompilerConstants.tokenImage.length) {
                return "DESCONHECIDO";
            }
            
            String tokenImage = BrCompilerConstants.tokenImage[tokenKind];
            
            if (tokenImage.startsWith("\"") && tokenImage.endsWith("\"")) {
                return tokenImage.substring(1, tokenImage.length() - 1);
            }
            
            if (tokenImage.startsWith("<") && tokenImage.endsWith(">")) {
                return tokenImage.substring(1, tokenImage.length() - 1);
            }
            
            return tokenImage;
        } catch (Exception e) {
            System.out.println("[ERRO] Erro ao obter nome do token: " + e.getMessage());
            return "TOKEN_" + tokenKind;
        }
    }
}