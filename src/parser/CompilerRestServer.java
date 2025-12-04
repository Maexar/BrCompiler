package parser;

import com.sun.net.httpserver.*;
import recovery.ErrorManager;
import recovery.DelimiterBalancer;
import semantic.AnalisadorSemantico;
import semantic.TabelaSimbolos;
import semantic.ErroSemantico;
import semantic.Simbolo;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class CompilerRestServer {
    private static final int PORT = 8085;
    
    // ===== LOCK UNICO PARA TODAS AS OPERACOES =====
    private static final Object COMPILER_LOCK = new Object();
    
    // ===== ESTADO DO COMPILADOR =====
    private static volatile boolean compilerInitialized = false;
    private static BrCompiler compiler = null;
    
    // ===== ANALISADOR SEMANTICO =====
    private static AnalisadorSemantico analisadorSemantico = new AnalisadorSemantico();
    
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
            
            String response = "{\"status\":\"ok\",\"compiler\":\"BrCompiler\",\"version\":\"2.0-semantic\"}";
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
        
        // ===== NOVO ENDPOINT: TABELA DE SIMBOLOS =====
        server.createContext("/api/symbols", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                handleSymbols(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });
        
        server.start();
        
        System.out.println("\n");
        System.out.println("SERVIDOR COMPILADOR INICIADO (COM ANALISE SEMANTICA)");
        System.out.println("\nEndpoints disponiveis:");
        System.out.println("   POST http://localhost:" + PORT + "/api/compile   - Compila codigo (lexico + sintatico + semantico)");
        System.out.println("   POST http://localhost:" + PORT + "/api/ast       - Gera arvore sintatica");
        System.out.println("   POST http://localhost:" + PORT + "/api/tokens    - Extrai tokens");
        System.out.println("   POST http://localhost:" + PORT + "/api/symbols   - Retorna tabela de simbolos");
        System.out.println("   GET  http://localhost:" + PORT + "/api/health    - Verifica status");
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
    
    // ===== NOVO HANDLER: TABELA DE SIMBOLOS =====
    private static void handleSymbols(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String code = extractCode(body);
            
            System.out.println("[DEBUG] Extraindo tabela de simbolos");
            
            String symbolsJson = extractSymbols(code);
            String result = "{\"success\":true,\"symbols\":" + symbolsJson + "}";
            
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            System.out.println("[ERRO] Excecao em /api/symbols: " + e.getMessage());
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
                
                // Limpa estados anteriores
                ErrorManager.clear();
                BrCompiler.eof = false;
                BrCompiler.lastError = null;
                BrCompiler.delimiterBalancer = new DelimiterBalancer();
                
                // Reinicia analisador semantico
                analisadorSemantico = new AnalisadorSemantico();
                analisadorSemantico.setDebug(false);
                
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
                
                // ===== ANALISE LEXICA E SINTATICA =====
                compiler.main();
                
                List<ErrorManager.SyntaxError> syntaxErrors = new ArrayList<>();
                List<ErroSemantico> semanticErrors = new ArrayList<>();
                
                // Coleta erros sintaticos
                if (ErrorManager.hasErrors()) {
                    syntaxErrors.addAll(ErrorManager.getErrors());
                }
                
                // Verifica balanceamento
                boolean hasBalanceError = false;
                try {
                    BrCompiler.delimiterBalancer.checkBalance();
                } catch (DelimiterBalancer.UnbalancedDelimiterException ex) {
                    System.out.println("[DEBUG] Erro de balanceamento detectado");
                    hasBalanceError = true;
                    ErrorManager.SyntaxError balanceError = new ErrorManager.SyntaxError(
                        "Erro de balanceamento: " + ex.getMessage(),
                        ex.line, ex.column, "balanceamento", "EOF", "fecha-te-sesamo"
                    );
                    syntaxErrors.add(balanceError);
                }
                
                // ===== ANALISE SEMANTICA =====
                // So executa se nao houver erros sintaticos
                if (syntaxErrors.isEmpty() && !hasBalanceError) {
                    try {
                        SimpleNode raiz = (SimpleNode) BrCompiler.jjtree.rootNode();
                        
                        if (raiz != null) {
                            System.out.println("[SEMANTIC] Iniciando analise semantica");
                            analisadorSemantico.analisar(raiz);
                            
                            if (analisadorSemantico.temErros()) {
                                semanticErrors.addAll(analisadorSemantico.getErros());
                                System.out.println("[SEMANTIC] " + semanticErrors.size() + " erro(s) semantico(s) encontrado(s)");
                            } else {
                                System.out.println("[SEMANTIC] Analise semantica concluida sem erros");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[SEMANTIC] Erro durante analise semantica: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // ===== MONTA RESPOSTA =====
                if (!syntaxErrors.isEmpty() || !semanticErrors.isEmpty()) {
                    System.out.println("[DEBUG] Total: " + syntaxErrors.size() + " erro(s) sintatico(s), " + 
                                      semanticErrors.size() + " erro(s) semantico(s)");
                    return buildFullErrorsResponse(syntaxErrors, semanticErrors);
                }
                
                // Sucesso!
                System.out.println("[COMPILE] Compilacao bem-sucedida!");
                
                // Retorna tambem informacoes da tabela de simbolos
                TabelaSimbolos tabela = analisadorSemantico.getTabelaSimbolos();
                int numVariaveis = tabela.getVariaveis().size();
                int numFuncoes = tabela.getFuncoes().size();
                
                return "{\"success\":true,\"message\":\"Codigo compilado com sucesso\"," +
                       "\"lines\":" + countLines(code) + "," +
                       "\"symbols\":{\"variables\":" + numVariaveis + ",\"functions\":" + numFuncoes + "}}";
                
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
                
                return buildFullErrorsResponse(allErrors, new ArrayList<>());
                
            } catch (TokenMgrError e) {
                System.out.println("[DEBUG] TokenMgrError capturado");
                String errorMsg = BrCompiler.handleTokenMgrError(e);
                ErrorManager.SyntaxError lexError = extractErrorFromMessage(errorMsg, "lexico");
                
                List<ErrorManager.SyntaxError> allErrors = new ArrayList<>();
                allErrors.add(lexError);
                return buildFullErrorsResponse(allErrors, new ArrayList<>());
                
            } catch (Exception e) {
                System.out.println("[DEBUG] Exception generica capturada");
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
                e.printStackTrace();
                return "{\"success\":false,\"error\":\"" + escapeJson(errorMsg) + "\"}";
            }
        }
    }
    
    // ===== EXTRAI TABELA DE SIMBOLOS =====
    private static String extractSymbols(String code) {
        synchronized (COMPILER_LOCK) {
            if (code == null || code.trim().isEmpty()) {
                return "{\"variables\":[],\"functions\":[]}";
            }
            
            try {
                // Primeiro compila para popular a tabela de simbolos
                ErrorManager.clear();
                BrCompiler.eof = false;
                BrCompiler.lastError = null;
                BrCompiler.delimiterBalancer = new DelimiterBalancer();
                analisadorSemantico = new AnalisadorSemantico();
                analisadorSemantico.setDebug(false);
                
                java.io.StringReader reader = new java.io.StringReader(code);
                
                if (!compilerInitialized || compiler == null) {
                    compiler = new BrCompiler(reader);
                    compilerInitialized = true;
                    BrCompiler.parserInitialized = true;
                } else {
                    compiler.ReInit(reader);
                }
                
                try {
                    compiler.main();
                    
                    // Executa analise semantica
                    SimpleNode raiz = (SimpleNode) BrCompiler.jjtree.rootNode();
                    if (raiz != null) {
                        analisadorSemantico.analisar(raiz);
                    }
                } catch (Exception e) {
                    // Ignora erros - queremos os simbolos que foram coletados
                }
                
                // Monta JSON da tabela de simbolos
                TabelaSimbolos tabela = analisadorSemantico.getTabelaSimbolos();
                return buildSymbolsJson(tabela);
                
            } catch (Exception e) {
                System.out.println("[ERRO] Erro ao extrair simbolos: " + e.getMessage());
                return "{\"variables\":[],\"functions\":[]}";
            }
        }
    }
    
    // ===== MONTA JSON DA TABELA DE SIMBOLOS =====
    private static String buildSymbolsJson(TabelaSimbolos tabela) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Variaveis
        json.append("\"variables\":[");
        List<Simbolo> variaveis = tabela.getVariaveis();
        boolean first = true;
        for (Simbolo v : variaveis) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"name\":\"").append(escapeJson(v.getNome())).append("\",");
            json.append("\"type\":\"").append(escapeJson(v.getTipo().getNomePortugues())).append("\",");
            json.append("\"scope\":\"").append(escapeJson(v.getEscopo())).append("\",");
            json.append("\"line\":").append(v.getLinhaDeclaracao()).append(",");
            json.append("\"initialized\":").append(v.isInicializado()).append(",");
            json.append("\"used\":").append(v.isUtilizado());
            json.append("}");
            first = false;
        }
        json.append("],");
        
        // Funcoes
        json.append("\"functions\":[");
        List<Simbolo> funcoes = tabela.getFuncoes();
        first = true;
        for (Simbolo f : funcoes) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"name\":\"").append(escapeJson(f.getNome())).append("\",");
            json.append("\"returnType\":\"").append(escapeJson(f.getTipoRetorno().getNomePortugues())).append("\",");
            json.append("\"scope\":\"").append(escapeJson(f.getEscopo())).append("\",");
            json.append("\"line\":").append(f.getLinhaDeclaracao()).append(",");
            json.append("\"paramCount\":").append(f.getNumeroParametros()).append(",");
            json.append("\"signature\":\"").append(escapeJson(f.getAssinaturaFuncao())).append("\",");
            json.append("\"used\":").append(f.isUtilizado());
            json.append("}");
            first = false;
        }
        json.append("]");
        
        json.append("}");
        return json.toString();
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
    
    // ===== MONTA RESPOSTA COM ERROS SINTATICOS E SEMANTICOS =====
    private static String buildFullErrorsResponse(List<ErrorManager.SyntaxError> syntaxErrors, 
                                                   List<ErroSemantico> semanticErrors) {
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":false,");
        
        // Erros sintaticos
        json.append("\"syntaxErrors\":[");
        boolean first = true;
        for (ErrorManager.SyntaxError error : syntaxErrors) {
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
        json.append("],");
        
        // Erros semanticos
        json.append("\"semanticErrors\":[");
        first = true;
        for (ErroSemantico error : semanticErrors) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"message\":\"").append(escapeJson(error.getMensagem())).append("\",");
            json.append("\"line\":").append(error.getLinha()).append(",");
            json.append("\"column\":").append(error.getColuna()).append(",");
            json.append("\"type\":\"").append(escapeJson(error.getTipo().name())).append("\",");
            json.append("\"identifier\":\"").append(escapeJson(error.getIdentificador() != null ? error.getIdentificador() : "")).append("\"");
            json.append("}");
            first = false;
        }
        json.append("],");
        
        // Compatibilidade: mantem array "errors" para a IDE existente
        json.append("\"errors\":[");
        first = true;
        
        // Adiciona erros sintaticos
        for (ErrorManager.SyntaxError error : syntaxErrors) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"message\":\"[SINTAXE] ").append(escapeJson(error.getMessage())).append("\",");
            json.append("\"line\":").append(error.getLine()).append(",");
            json.append("\"column\":").append(error.getColumn()).append(",");
            json.append("\"context\":\"").append(escapeJson(error.getContext())).append("\",");
            json.append("\"foundToken\":\"").append(escapeJson(error.getFoundToken())).append("\",");
            json.append("\"expectedTokens\":\"").append(escapeJson(error.getExpectedTokens())).append("\"");
            json.append("}");
            first = false;
        }
        
        // Adiciona erros semanticos
        for (ErroSemantico error : semanticErrors) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"message\":\"[SEMANTICO] ").append(escapeJson(error.getMensagem())).append("\",");
            json.append("\"line\":").append(error.getLinha()).append(",");
            json.append("\"column\":").append(error.getColuna()).append(",");
            json.append("\"context\":\"semantico\",");
            json.append("\"foundToken\":\"\",");
            json.append("\"expectedTokens\":\"\"");
            json.append("}");
            first = false;
        }
        json.append("]");
        
        json.append("}");
        return json.toString();
    }
    
    // Mantem compatibilidade com versao anterior
    private static String buildMultipleErrorsResponse(List<ErrorManager.SyntaxError> errors) {
        return buildFullErrorsResponse(errors, new ArrayList<>());
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
