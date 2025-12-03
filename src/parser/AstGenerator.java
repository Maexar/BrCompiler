package parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de árvore sintática abstrata (AST) para BrCompiler.
 * Cria uma representação hierárquica do código analisado.
 */
public class AstGenerator implements BrCompilerConstants {

    private static class Node {
        String name;
        List<Node> children = new ArrayList<>();

        Node(String name) { this.name = name; }

        void add(Node n) { children.add(n); }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append("\"name\":\"").append(escape(name)).append("\"");
            if (!children.isEmpty()) {
                sb.append(",\"children\":[");
                for (int i = 0; i < children.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(children.get(i).toJson());
                }
                sb.append(']');
            }
            sb.append('}');
            return sb.toString();
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    private static Token currentToken;
    private static Token lastToken;

    public static String generateAst(String code) {
        if (code == null) return "{}";
        try {
            BrCompiler.ReInit(new StringReader(code));
            currentToken = null;
            lastToken = null;
            
            Node root = new Node("Program");
            
            // Consome INICIOPROG (gambiarra)
            nextToken();
            if (currentToken.kind != INICIOPROG) {
                return "{\"error\":\"Programa deve começar com 'gambiarra'\"}";
            }
            root.add(new Node("ProgramStart: " + currentToken.image));
            
            // Consome ABREBLOCO
            nextToken();
            if (currentToken.kind != ABREBLOCO) {
                return "{\"error\":\"Esperado 'abre-te-sesamo' após 'gambiarra'\"}";
            }
            root.add(new Node("BlockOpen: " + currentToken.image));
            
            // Processa bloco principal
            Node mainBlock = new Node("MainBlock");
            nextToken();
            while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                Node stmt = parseStatement();
                if (stmt != null) {
                    mainBlock.add(stmt);
                }
            }
            
            // Adiciona o bloco
            root.add(mainBlock);
            
            // Adiciona FECHABLOCO se existir
            if (currentToken.kind == FECHABLOCO) {
                root.add(new Node("BlockClose: " + currentToken.image));
            }
            
            return root.toJson();

        } catch (Exception e) {
            return "{\"error\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    private static void nextToken() {
        lastToken = currentToken;
        currentToken = BrCompiler.getNextToken();
    }

    private static Token peek() {
        return BrCompiler.getToken(1);
    }

    private static Node parseStatement() {
        if (currentToken == null) return null;
        
        switch (currentToken.kind) {
            case INT:
            case FLOAT:
            case STRING:
            case BOOL:
                return parseVariableDeclaration();
            
            case IDENTIFICADOR:
                // Pode ser atribuição ou chamada de função
                Token next = peek();
                if (next != null && next.kind == ATRIBUICAO) {
                    return parseAssignment();
                } else if (next != null && next.kind == ABRIRFUNC) {
                    return parseFunctionCall();
                }
                nextToken(); // Avança se não reconhecido
                return null;
            
            case PRINT:
                return parsePrint();
            
            case SCAN:
                return parseScan();
            
            case FOR:
                return parseFor();
            
            case WHILE:
                return parseWhile();
            
            case CONDICIONAL:
                return parseIf();
            
            case FUNCAO:
                return parseFunctionDeclaration();
            
            case RETURN:
                return parseReturn();
            
            case FIMESTRUTURA:
                nextToken(); // Consome 'br'
                return null;
            
            case FECHABLOCO:
                return null; // Fim do bloco
            
            default:
                nextToken(); // Avança token desconhecido
                return null;
        }
    }

    private static Node parseVariableDeclaration() {
        Node node = new Node("VariableDeclaration (line:" + currentToken.beginLine + ")");
        
        // Tipo
        String type = currentToken.image;
        node.add(new Node("Type: " + type));
        nextToken();
        
        // Identificador
        if (currentToken.kind == IDENTIFICADOR) {
            node.add(new Node("Identifier: " + currentToken.image));
            nextToken();
        }
        
        // Atribuição opcional
        if (currentToken.kind == ATRIBUICAO) {
            nextToken(); // Consome 'receba'
            Node value = parseExpression();
            if (value != null) {
                Node assignment = new Node("InitialValue");
                assignment.add(value);
                node.add(assignment);
            }
        }
        
        // Consome 'br'
        if (currentToken.kind == FIMESTRUTURA) {
            nextToken();
        }
        
        return node;
    }

    private static Node parseAssignment() {
        Node node = new Node("Assignment (line:" + currentToken.beginLine + ")");
        
        // Identificador
        node.add(new Node("Identifier: " + currentToken.image));
        nextToken();
        
        // Consome 'receba'
        if (currentToken.kind == ATRIBUICAO) {
            nextToken();
        }
        
        // Valor
        Node value = parseExpression();
        if (value != null) {
            node.add(value);
        }
        
        // Consome 'br'
        if (currentToken.kind == FIMESTRUTURA) {
            nextToken();
        }
        
        return node;
    }

    private static Node parseExpression() {
        if (currentToken == null) return null;
        
        Node left = parseTerm();
        
        while (currentToken != null && isOperator(currentToken.kind)) {
            Node op = new Node("BinaryOp: " + currentToken.image);
            op.add(left);
            nextToken();
            Node right = parseTerm();
            if (right != null) {
                op.add(right);
            }
            left = op;
        }
        
        return left;
    }

    private static Node parseTerm() {
        if (currentToken == null) return null;
        
        Node node = null;
        
        switch (currentToken.kind) {
            case CONSTANTE_INT:
                node = new Node("IntLiteral: " + currentToken.image);
                nextToken();
                break;
            
            case CONSTANTE_FLOAT:
                node = new Node("FloatLiteral: " + currentToken.image);
                nextToken();
                break;
            
            case LITERAL_STRING:
                node = new Node("StringLiteral: " + currentToken.image);
                nextToken();
                break;
            
            case TRUE:
            case FALSE:
                node = new Node("BoolLiteral: " + currentToken.image);
                nextToken();
                break;
            
            case IDENTIFICADOR:
                Token next = peek();
                if (next != null && next.kind == ABRIRFUNC) {
                    node = parseFunctionCall();
                } else {
                    node = new Node("Identifier: " + currentToken.image);
                    nextToken();
                }
                break;
            
            case ABRIRFUNC:
                nextToken(); // Consome '('
                node = parseExpression();
                if (currentToken.kind == FECHARFUNC) {
                    nextToken(); // Consome ')'
                }
                break;
        }
        
        return node;
    }

    private static Node parsePrint() {
        Node node = new Node("Print (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'printa'
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            Node expr = parseExpression();
            if (expr != null) {
                node.add(expr);
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken(); // Consome ')'
            }
        }
        
        if (currentToken.kind == FIMESTRUTURA) {
            nextToken(); // Consome 'br'
        }
        
        return node;
    }

    private static Node parseScan() {
        Node node = new Node("Scan (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'papa-entrada'
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            if (currentToken.kind == IDENTIFICADOR) {
                node.add(new Node("Identifier: " + currentToken.image));
                nextToken();
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken(); // Consome ')'
            }
        }
        
        if (currentToken.kind == FIMESTRUTURA) {
            nextToken(); // Consome 'br'
        }
        
        return node;
    }

    private static Node parseFor() {
        Node node = new Node("ForLoop (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'pet'
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            
            // Inicialização
            Node init = parseStatement();
            if (init != null) {
                Node initNode = new Node("Init");
                initNode.add(init);
                node.add(initNode);
            }
            
            // Condição
            Node cond = parseExpression();
            if (cond != null) {
                Node condNode = new Node("Condition");
                condNode.add(cond);
                node.add(condNode);
            }
            
            if (currentToken.kind == FIMESTRUTURA) {
                nextToken(); // Consome 'br'
            }
            
            // Incremento
            Node inc = parseExpression();
            if (inc != null) {
                Node incNode = new Node("Increment");
                incNode.add(inc);
                node.add(incNode);
            }
            
            if (currentToken.kind == FECHARFUNC) {
                nextToken(); // Consome ')'
            }
        }
        
        // Corpo do loop
        if (currentToken.kind == ABREBLOCO) {
            nextToken();
            Node body = new Node("Body");
            while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                Node stmt = parseStatement();
                if (stmt != null) {
                    body.add(stmt);
                }
            }
            if (currentToken.kind == FECHABLOCO) {
                nextToken();
            }
            node.add(body);
        }
        
        return node;
    }

    private static Node parseWhile() {
        Node node = new Node("WhileLoop (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'repet'
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            Node cond = parseExpression();
            if (cond != null) {
                Node condNode = new Node("Condition");
                condNode.add(cond);
                node.add(condNode);
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken(); // Consome ')'
            }
        }
        
        // Corpo do loop
        if (currentToken.kind == ABREBLOCO) {
            nextToken();
            Node body = new Node("Body");
            while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                Node stmt = parseStatement();
                if (stmt != null) {
                    body.add(stmt);
                }
            }
            if (currentToken.kind == FECHABLOCO) {
                nextToken();
            }
            node.add(body);
        }
        
        return node;
    }

    private static Node parseIf() {
        Node node = new Node("IfStatement (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'sepa'
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            Node cond = parseExpression();
            if (cond != null) {
                Node condNode = new Node("Condition");
                condNode.add(cond);
                node.add(condNode);
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken(); // Consome ')'
            }
        }
        
        // Bloco then
        if (currentToken.kind == ABREBLOCO) {
            nextToken();
            Node thenBody = new Node("ThenBlock");
            while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                Node stmt = parseStatement();
                if (stmt != null) {
                    thenBody.add(stmt);
                }
            }
            if (currentToken.kind == FECHABLOCO) {
                nextToken();
            }
            node.add(thenBody);
        }
        
        // Bloco else (opcional)
        if (currentToken.kind == SENAO) {
            nextToken(); // Consome 'da-teus-pulo'
            if (currentToken.kind == ABREBLOCO) {
                nextToken();
                Node elseBody = new Node("ElseBlock");
                while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                    Node stmt = parseStatement();
                    if (stmt != null) {
                        elseBody.add(stmt);
                    }
                }
                if (currentToken.kind == FECHABLOCO) {
                    nextToken();
                }
                node.add(elseBody);
            }
        }
        
        return node;
    }

    private static Node parseFunctionDeclaration() {
        Node node = new Node("FunctionDeclaration (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'vai-filhao'
        
        // Tipo de retorno
        if (isType(currentToken.kind)) {
            node.add(new Node("ReturnType: " + currentToken.image));
            nextToken();
        }
        
        // Nome da função
        if (currentToken.kind == IDENTIFICADOR) {
            node.add(new Node("FunctionName: " + currentToken.image));
            nextToken();
        }
        
        // Parâmetros
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            Node params = new Node("Parameters");
            while (currentToken.kind != FECHARFUNC && currentToken.kind != EOF) {
                if (isType(currentToken.kind)) {
                    Node param = new Node("Parameter");
                    param.add(new Node("Type: " + currentToken.image));
                    nextToken();
                    if (currentToken.kind == IDENTIFICADOR) {
                        param.add(new Node("Name: " + currentToken.image));
                        nextToken();
                    }
                    params.add(param);
                    
                    if (currentToken.kind == VIRGULA) {
                        nextToken();
                    }
                } else {
                    nextToken();
                }
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken();
            }
            node.add(params);
        }
        
        // Corpo da função
        if (currentToken.kind == ABREBLOCO) {
            nextToken();
            Node body = new Node("Body");
            while (currentToken.kind != FECHABLOCO && currentToken.kind != EOF) {
                Node stmt = parseStatement();
                if (stmt != null) {
                    body.add(stmt);
                }
            }
            if (currentToken.kind == FECHABLOCO) {
                nextToken();
            }
            node.add(body);
        }
        
        return node;
    }

    private static Node parseFunctionCall() {
        Node node = new Node("FunctionCall: " + currentToken.image);
        nextToken(); // Consome identificador
        
        if (currentToken.kind == ABRIRFUNC) {
            nextToken(); // Consome '('
            Node args = new Node("Arguments");
            while (currentToken.kind != FECHARFUNC && currentToken.kind != EOF) {
                Node arg = parseExpression();
                if (arg != null) {
                    args.add(arg);
                }
                if (currentToken.kind == VIRGULA) {
                    nextToken();
                }
            }
            if (currentToken.kind == FECHARFUNC) {
                nextToken();
            }
            if (!args.children.isEmpty()) {
                node.add(args);
            }
        }
        
        return node;
    }

    private static Node parseReturn() {
        Node node = new Node("Return (line:" + currentToken.beginLine + ")");
        nextToken(); // Consome 'devolva'
        
        Node expr = parseExpression();
        if (expr != null) {
            node.add(expr);
        }
        
        if (currentToken.kind == FIMESTRUTURA) {
            nextToken(); // Consome 'br'
        }
        
        return node;
    }

    private static boolean isOperator(int kind) {
        return kind == SOMA || kind == SUBTRACAO || kind == MULTIPLICACAO || 
               kind == DIVISAO || kind == OPMAIOR || kind == OPMENOR || 
               kind == OPIGUAL || kind == OPDIF || kind == OPMAIORIGUAL || 
               kind == OPMENORIGUAL || kind == OPAND || kind == OPOR;
    }

    private static boolean isType(int kind) {
        return kind == INT || kind == FLOAT || kind == STRING || kind == BOOL;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}