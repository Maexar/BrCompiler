package semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Tabela de Simbolos - Gerencia todos os identificadores declarados no programa
 * Suporta escopos aninhados (global, funcao, blocos)
 */
public class TabelaSimbolos {
    
    // Pilha de escopos ativos (do mais antigo para o mais recente)
    private Stack<Escopo> pilhaEscopos;
    
    // Contador de nivel de escopo
    private int nivelAtual;
    
    // Lista de todos os simbolos ja declarados (para relatorios)
    private List<Simbolo> todosSimbolos;
    
    // Nome do escopo global
    private static final String ESCOPO_GLOBAL = "global";
    
    /**
     * Classe interna que representa um escopo
     */
    private static class Escopo {
        String nome;
        int nivel;
        Map<String, Simbolo> simbolos;
        Simbolo funcaoAtual; // Se for escopo de funcao, guarda referencia para ela
        
        Escopo(String nome, int nivel) {
            this.nome = nome;
            this.nivel = nivel;
            this.simbolos = new HashMap<>();
            this.funcaoAtual = null;
        }
        
        Escopo(String nome, int nivel, Simbolo funcao) {
            this(nome, nivel);
            this.funcaoAtual = funcao;
        }
    }
    
    /**
     * Construtor - inicializa a tabela com escopo global
     */
    public TabelaSimbolos() {
        this.pilhaEscopos = new Stack<>();
        this.nivelAtual = 0;
        this.todosSimbolos = new ArrayList<>();
        
        // Cria escopo global automaticamente
        abrirEscopo(ESCOPO_GLOBAL);
    }
    
    /**
     * Abre um novo escopo (para blocos, funcoes, etc)
     */
    public void abrirEscopo(String nome) {
        Escopo novoEscopo = new Escopo(nome, nivelAtual);
        pilhaEscopos.push(novoEscopo);
        nivelAtual++;
        
        System.out.println("[TabelaSimbolos] Escopo aberto: " + nome + " (nivel " + (nivelAtual - 1) + ")");
    }
    
    /**
     * Abre um novo escopo de funcao
     */
    public void abrirEscopoFuncao(String nomeFuncao, Simbolo funcao) {
        Escopo novoEscopo = new Escopo(nomeFuncao, nivelAtual, funcao);
        pilhaEscopos.push(novoEscopo);
        nivelAtual++;
        
        System.out.println("[TabelaSimbolos] Escopo de funcao aberto: " + nomeFuncao + " (nivel " + (nivelAtual - 1) + ")");
    }
    
    /**
     * Fecha o escopo atual
     */
    public void fecharEscopo() {
        if (pilhaEscopos.size() > 1) { // Mantem pelo menos o escopo global
            Escopo escopoFechado = pilhaEscopos.pop();
            nivelAtual--;
            
            System.out.println("[TabelaSimbolos] Escopo fechado: " + escopoFechado.nome);
            
            // Opcional: verificar variaveis nao utilizadas
            verificarVariaveisNaoUtilizadas(escopoFechado);
        }
    }
    
    /**
     * Verifica variaveis declaradas mas nao utilizadas no escopo
     */
    private void verificarVariaveisNaoUtilizadas(Escopo escopo) {
        for (Simbolo simbolo : escopo.simbolos.values()) {
            if (!simbolo.isUtilizado() && !simbolo.isParametro()) {
                System.out.println("[TabelaSimbolos] AVISO: Variavel '" + simbolo.getNome() + 
                                  "' declarada mas nunca utilizada (linha " + simbolo.getLinhaDeclaracao() + ")");
            }
        }
    }
    
    /**
     * Insere um novo simbolo no escopo atual
     * @return true se inserido com sucesso, false se ja existe no escopo atual
     */
    public boolean inserir(Simbolo simbolo) {
        if (pilhaEscopos.isEmpty()) {
            return false;
        }
        
        Escopo escopoAtual = pilhaEscopos.peek();
        
        // Verifica se ja existe no escopo atual
        if (escopoAtual.simbolos.containsKey(simbolo.getNome())) {
            System.out.println("[TabelaSimbolos] ERRO: Simbolo '" + simbolo.getNome() + 
                              "' ja declarado neste escopo");
            return false;
        }
        
        // Insere o simbolo
        escopoAtual.simbolos.put(simbolo.getNome(), simbolo);
        todosSimbolos.add(simbolo);
        
        System.out.println("[TabelaSimbolos] Simbolo inserido: " + simbolo.getNome() + 
                          " (tipo: " + simbolo.getTipo() + ", escopo: " + escopoAtual.nome + ")");
        
        return true;
    }
    
    /**
     * Insere uma variavel no escopo atual
     */
    public boolean inserirVariavel(String nome, TipoSemantico tipo, int linha, int coluna) {
        Escopo escopoAtual = pilhaEscopos.peek();
        Simbolo simbolo = new Simbolo(nome, tipo, escopoAtual.nome, nivelAtual - 1, linha, coluna);
        return inserir(simbolo);
    }
    
    /**
     * Insere uma variavel inicializada no escopo atual
     */
    public boolean inserirVariavelInicializada(String nome, TipoSemantico tipo, int linha, int coluna) {
        Escopo escopoAtual = pilhaEscopos.peek();
        Simbolo simbolo = new Simbolo(nome, tipo, escopoAtual.nome, nivelAtual - 1, linha, coluna);
        simbolo.setInicializado(true);
        return inserir(simbolo);
    }
    
    /**
     * Insere uma funcao no escopo atual
     */
    public boolean inserirFuncao(String nome, TipoSemantico tipoRetorno, List<Simbolo> parametros, 
                                  int linha, int coluna) {
        Escopo escopoAtual = pilhaEscopos.peek();
        Simbolo funcao = new Simbolo(nome, tipoRetorno, escopoAtual.nome, nivelAtual - 1, 
                                      linha, coluna, parametros);
        return inserir(funcao);
    }
    
    /**
     * Busca um simbolo pelo nome em todos os escopos visiveis
     * Procura do escopo mais interno para o mais externo
     * @return o Simbolo encontrado ou null se nao existir
     */
    public Simbolo buscar(String nome) {
        // Busca do escopo mais interno para o mais externo
        for (int i = pilhaEscopos.size() - 1; i >= 0; i--) {
            Escopo escopo = pilhaEscopos.get(i);
            if (escopo.simbolos.containsKey(nome)) {
                Simbolo simbolo = escopo.simbolos.get(nome);
                simbolo.setUtilizado(true);
                return simbolo;
            }
        }
        return null;
    }
    
    /**
     * Busca um simbolo apenas no escopo atual
     */
    public Simbolo buscarNoEscopoAtual(String nome) {
        if (pilhaEscopos.isEmpty()) {
            return null;
        }
        return pilhaEscopos.peek().simbolos.get(nome);
    }
    
    /**
     * Verifica se um simbolo existe em qualquer escopo visivel
     */
    public boolean existe(String nome) {
        return buscar(nome) != null;
    }
    
    /**
     * Verifica se um simbolo existe no escopo atual
     */
    public boolean existeNoEscopoAtual(String nome) {
        return buscarNoEscopoAtual(nome) != null;
    }
    
    /**
     * Obtem o nome do escopo atual
     */
    public String getEscopoAtual() {
        if (pilhaEscopos.isEmpty()) {
            return "";
        }
        return pilhaEscopos.peek().nome;
    }
    
    /**
     * Obtem o nivel do escopo atual
     */
    public int getNivelAtual() {
        return nivelAtual - 1;
    }
    
    /**
     * Verifica se estamos dentro de uma funcao
     */
    public boolean estaDentroFuncao() {
        for (int i = pilhaEscopos.size() - 1; i >= 0; i--) {
            if (pilhaEscopos.get(i).funcaoAtual != null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtem a funcao atual (se estiver dentro de uma)
     */
    public Simbolo getFuncaoAtual() {
        for (int i = pilhaEscopos.size() - 1; i >= 0; i--) {
            if (pilhaEscopos.get(i).funcaoAtual != null) {
                return pilhaEscopos.get(i).funcaoAtual;
            }
        }
        return null;
    }
    
    /**
     * Obtem todos os simbolos de uma categoria especifica
     */
    public List<Simbolo> getSimbolosPorCategoria(Simbolo.Categoria categoria) {
        List<Simbolo> resultado = new ArrayList<>();
        for (Simbolo simbolo : todosSimbolos) {
            if (simbolo.getCategoria() == categoria) {
                resultado.add(simbolo);
            }
        }
        return resultado;
    }
    
    /**
     * Obtem todas as funcoes declaradas
     */
    public List<Simbolo> getFuncoes() {
        return getSimbolosPorCategoria(Simbolo.Categoria.FUNCAO);
    }
    
    /**
     * Obtem todas as variaveis declaradas
     */
    public List<Simbolo> getVariaveis() {
        List<Simbolo> resultado = new ArrayList<>();
        for (Simbolo simbolo : todosSimbolos) {
            if (simbolo.isVariavel()) {
                resultado.add(simbolo);
            }
        }
        return resultado;
    }
    
    /**
     * Obtem todos os simbolos
     */
    public List<Simbolo> getTodosSimbolos() {
        return new ArrayList<>(todosSimbolos);
    }
    
    /**
     * Limpa a tabela de simbolos
     */
    public void limpar() {
        pilhaEscopos.clear();
        todosSimbolos.clear();
        nivelAtual = 0;
        
        // Recria escopo global
        abrirEscopo(ESCOPO_GLOBAL);
    }
    
    /**
     * Gera relatorio da tabela de simbolos
     */
    public String gerarRelatorio() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("TABELA DE SIMBOLOS\n");
        sb.append("========================================\n\n");
        
        // Agrupa por categoria
        List<Simbolo> funcoes = getFuncoes();
        List<Simbolo> variaveis = getVariaveis();
        
        sb.append("FUNCOES (").append(funcoes.size()).append("):\n");
        sb.append("----------------------------------------\n");
        for (Simbolo f : funcoes) {
            sb.append("  ").append(f.getAssinaturaFuncao());
            sb.append(" [linha ").append(f.getLinhaDeclaracao()).append("]\n");
        }
        sb.append("\n");
        
        sb.append("VARIAVEIS (").append(variaveis.size()).append("):\n");
        sb.append("----------------------------------------\n");
        for (Simbolo v : variaveis) {
            sb.append("  ").append(v.getNome());
            sb.append(" : ").append(v.getTipo().getNomePortugues());
            sb.append(" (escopo: ").append(v.getEscopo());
            sb.append(", linha ").append(v.getLinhaDeclaracao()).append(")");
            if (!v.isInicializado()) {
                sb.append(" [nao inicializada]");
            }
            if (!v.isUtilizado()) {
                sb.append(" [nao utilizada]");
            }
            sb.append("\n");
        }
        
        sb.append("\n========================================\n");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return gerarRelatorio();
    }
}
