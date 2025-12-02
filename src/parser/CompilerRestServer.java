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
    private static BrCompiler compiler = null;
    
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        
        server.createContext("/api/compile", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), 
                                            StandardCharsets.UTF_8);
                    String code = extractCode(body);
                    
                    System.out.println("[DEBUG] Codigo recebido: " + code);
                    
                    String result = compileCode(code);
                    
                    System.out.println("[DEBUG] Resposta: " + result);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, result.getBytes().length);
                    exchange.getResponseBody().write(result.getBytes());
                    exchange.close();
                } catch (Exception e) {
                    System.out.println("[ERRO] Excecao: " + e.getMessage());
                    e.printStackTrace();
                    try {
                        String response = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                        exchange.getResponseBody().write(response.getBytes());
                        exchange.close();
                    } catch (Exception ignored) {}
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.createContext("/api/health", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            
            String response = "{\"status\":\"ok\",\"compiler\":\"BrCompiler\",\"version\":\"1.0\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
     // Endpoint mínimo para retornar a árvore sintática em JSON
        server.createContext("/api/ast", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), 
                                            StandardCharsets.UTF_8);
                    String code = extractCode(body);

                    System.out.println("[DEBUG] Gerando AST para código recebido");
                    String astJson = AstGenerator.generateAst(code);

                    String result = "{\"success\":true,\"ast\":" + astJson + "}";

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, result.getBytes().length);
                    exchange.getResponseBody().write(result.getBytes());
                    exchange.close();
                } catch (Exception e) {
                    try {
                        String response = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                        exchange.getResponseBody().write(response.getBytes());
                        exchange.close();
                    } catch (Exception ignored) {}
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("\n");
        System.out.println("SERVIDOR COMPILADOR INICIADO");
        System.out.println("\nEndpoints disponiveis:");
        System.out.println("   POST http://localhost:" + PORT + "/api/compile");
        System.out.println("   GET  http://localhost:" + PORT + "/api/health");
        System.out.println("\nMANTENHA ESTE TERMINAL ABERTO ENQUANTO DESENVOLVE\n");
    }
    
    private static String compileCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "{\"success\":false,\"error\":\"Codigo vazio\"}";
        }
        
        try {
            System.out.println("[DEBUG] Tentando compilar codigo...");
            
            // RESET COMPLETO DO ESTADO
            ErrorManager.clear();
            BrCompiler.eof = false;
            BrCompiler.lastError = null;
            BrCompiler.delimiterBalancer = new DelimiterBalancer();
            
            java.io.StringReader reader = new java.io.StringReader(code);
            
            if (compiler == null) {
                System.out.println("[DEBUG] Primeira chamada - criando instancia BrCompiler");
                compiler = new BrCompiler(reader);
            } else {
                System.out.println("[DEBUG] ReInit do BrCompiler com novo codigo");
                BrCompiler.ReInit(reader);
            }
            
            // Tenta compilar
            compiler.main();
            
            // CONSOLIDAR TODOS OS ERROS (balanceamento + ErrorManager)
            List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
            
            // 1. Adiciona erros do ErrorManager
            if (ErrorManager.hasErrors()) {
                allErrors.addAll(ErrorManager.getErrors());
            }
            
            // 2. Adiciona erros de balanceamento
            try {
                BrCompiler.delimiterBalancer.checkBalance();
            } catch (DelimiterBalancer.UnbalancedDelimiterException ex) {
                System.out.println("[DEBUG] Erro de balanceamento detectado");
                // Cria erro sintetico para balanceamento
                ErrorManager.SyntaxError balanceError = new ErrorManager.SyntaxError(
                    "Erro de balanceamento: " + ex.getMessage(),
                    ex.line,
                    ex.column,
                    "balanceamento",
                    "EOF",
                    "fecha-te-sesamo"
                );
                allErrors.add(balanceError);
            }
            
            // 3. Se houver erros, retorna todos consolidados
            if (!allErrors.isEmpty()) {
                System.out.println("[DEBUG] Total de " + allErrors.size() + " erro(s) encontrado(s)");
                return buildMultipleErrorsResponse(allErrors);
            }
            
            // Sucesso completo
            System.out.println("[DEBUG] Compilacao bem-sucedida!");
            return "{\"success\":true,\"message\":\"Codigo compilado com sucesso\",\"lines\":" + 
                   countLines(code) + "}";
            
        } catch (ParseException e) {
            System.out.println("[DEBUG] ParseException capturada");
            
            // Consolidar erros do ErrorManager + este ParseException
            List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
            
            if (ErrorManager.hasErrors()) {
                allErrors.addAll(ErrorManager.getErrors());
            } else {
                // Se nao ha erros no ErrorManager, adiciona este ParseException
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
    
    /**
     * Extrai erro de mensagem formatada e cria SyntaxError
     */
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
    
    /**
     * Constroi resposta JSON com multiplos erros (array)
     */
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
        // Procura por "code": e pega tudo até a próxima aspas não escapada
        int codeIndex = json.indexOf("\"code\":");
        if (codeIndex == -1) return "";
        
        // Encontra a abertura da string após "code":
        int valueStart = json.indexOf("\"", codeIndex + 7);
        if (valueStart == -1) return "";
        
        // Procura pelo final da string (aspas não escapada)
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
}
