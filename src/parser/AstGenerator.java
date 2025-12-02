package parser;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador simples de árvore sintática (representação em árvore baseada em tokens).
 * Implementação intencionalmente minimalista: agrupa blocos por ABREBLOCO/FECHABLOCO
 * e cria nós para tokens relevantes. Não altera o parser gerado.
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

    public static String generateAst(String code) {
        if (code == null) return "{}";
        try {
            // Reutiliza o scanner/parsers estáticos gerados para evitar recriar SimpleCharStream
            BrCompiler.ReInit(new StringReader(code));

            Node root = new Node("Program");
            List<Node> stack = new ArrayList<>();
            stack.add(root);

            while (true) {
                Token t = BrCompiler.getNextToken();
                if (t == null) break;
                if (t.kind == 0) break; // EOF

                Node current = stack.get(stack.size() - 1);

                switch (t.kind) {
                    case ABREBLOCO:
                        Node block = new Node("Block (line:" + t.beginLine + ",col:" + t.beginColumn + ")");
                        current.add(block);
                        stack.add(block);
                        break;
                    case FECHABLOCO:
                        // fecha bloco: sobe na pilha
                        if (stack.size() > 1) stack.remove(stack.size()-1);
                        break;
                    case FUNCAO:
                        current.add(new Node("FUNCAO"));
                        break;
                    case IDENTIFICADOR:
                        current.add(new Node("IDENTIFIER: " + safe(t.image)));
                        break;
                    case CONSTANTE_INT:
                    case CONSTANTE_FLOAT:
                        current.add(new Node("CONST: " + safe(t.image)));
                        break;
                    case LITERAL_STRING:
                        current.add(new Node("STRING: " + safe(t.image)));
                        break;
                    case PRINT:
                    case SCAN:
                    case FOR:
                    case WHILE:
                    case RETURN:
                    case CONDICIONAL:
                        current.add(new Node(tokenName(t.kind) + (t.image != null ? (": " + safe(t.image)) : "")));
                        break;
                    default:
                        // para tokens pequenos como operadores, pontuação
                        if (t.image != null && !t.image.trim().isEmpty())
                            current.add(new Node(safe(t.image)));
                        break;
                }
            }

            return root.toJson();

        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }

    private static String tokenName(int kind) {
        try {
            // tenta acessar nome via token image array
            if (kind >= 0 && kind < BrCompilerTokenManager.jjstrLiteralImages.length) {
                String lit = BrCompilerTokenManager.jjstrLiteralImages[kind];
                if (lit != null && !lit.isEmpty()) return lit;
            }
        } catch (Throwable ignored) {}
        return "TOKEN_" + kind;
    }
}
