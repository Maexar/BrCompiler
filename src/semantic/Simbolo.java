package semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um simbolo na tabela de simbolos
 * Pode ser uma variavel, funcao, parametro ou estrutura de dados
 */
public class Simbolo {
    
    /**
     * Categorias de simbolos
     */
    public enum Categoria {
        VARIAVEL,
        FUNCAO,
        PARAMETRO,
        LISTA,
        PILHA
    }
    
    // Identificacao do simbolo
    private String nome;
    private Categoria categoria;
    private TipoSemantico tipo;
    
    // Informacoes de escopo
    private String escopo;       // Nome do escopo (ex: "global", "funcaoX", etc)
    private int nivelEscopo;     // Nivel de aninhamento do escopo
    
    // Informacoes de posicao no codigo fonte
    private int linhaDeclaracao;
    private int colunaDeclaracao;
    
    // Informacoes especificas para funcoes
    private List<Simbolo> parametros;
    private TipoSemantico tipoRetorno;
    private boolean temRetorno;           // Flag para verificar se funcao retorna valor
    
    // Informacoes de uso
    private boolean inicializado;
    private boolean utilizado;
    
    /**
     * Construtor para variaveis simples
     */
    public Simbolo(String nome, TipoSemantico tipo, String escopo, int nivelEscopo, 
                   int linha, int coluna) {
        this.nome = nome;
        this.tipo = tipo;
        this.escopo = escopo;
        this.nivelEscopo = nivelEscopo;
        this.linhaDeclaracao = linha;
        this.colunaDeclaracao = coluna;
        this.categoria = determinarCategoria(tipo);
        this.parametros = new ArrayList<>();
        this.tipoRetorno = TipoSemantico.VAZIO;
        this.inicializado = false;
        this.utilizado = false;
        this.temRetorno = false;
    }
    
    /**
     * Construtor para funcoes
     */
    public Simbolo(String nome, TipoSemantico tipoRetorno, String escopo, int nivelEscopo,
                   int linha, int coluna, List<Simbolo> parametros) {
        this.nome = nome;
        this.tipo = TipoSemantico.VAZIO; // Funcoes nao tem tipo proprio
        this.tipoRetorno = tipoRetorno;
        this.escopo = escopo;
        this.nivelEscopo = nivelEscopo;
        this.linhaDeclaracao = linha;
        this.colunaDeclaracao = coluna;
        this.categoria = Categoria.FUNCAO;
        this.parametros = parametros != null ? new ArrayList<>(parametros) : new ArrayList<>();
        this.inicializado = true; // Funcoes sao sempre "inicializadas"
        this.utilizado = false;
        this.temRetorno = false;
    }
    
    /**
     * Determina a categoria baseada no tipo
     */
    private Categoria determinarCategoria(TipoSemantico tipo) {
        if (tipo.isLista()) {
            return Categoria.LISTA;
        } else if (tipo.isPilha()) {
            return Categoria.PILHA;
        }
        return Categoria.VARIAVEL;
    }
    
    // Getters
    public String getNome() {
        return nome;
    }
    
    public Categoria getCategoria() {
        return categoria;
    }
    
    public TipoSemantico getTipo() {
        return tipo;
    }
    
    public String getEscopo() {
        return escopo;
    }
    
    public int getNivelEscopo() {
        return nivelEscopo;
    }
    
    public int getLinhaDeclaracao() {
        return linhaDeclaracao;
    }
    
    public int getColunaDeclaracao() {
        return colunaDeclaracao;
    }
    
    public List<Simbolo> getParametros() {
        return new ArrayList<>(parametros);
    }
    
    public int getNumeroParametros() {
        return parametros.size();
    }
    
    public TipoSemantico getTipoRetorno() {
        return tipoRetorno;
    }
    
    public boolean isInicializado() {
        return inicializado;
    }
    
    public boolean isUtilizado() {
        return utilizado;
    }
    
    public boolean isFuncao() {
        return categoria == Categoria.FUNCAO;
    }
    
    public boolean isVariavel() {
        return categoria == Categoria.VARIAVEL || 
               categoria == Categoria.PARAMETRO ||
               categoria == Categoria.LISTA ||
               categoria == Categoria.PILHA;
    }
    
    public boolean isParametro() {
        return categoria == Categoria.PARAMETRO;
    }
    
    public boolean isEstruturaDados() {
        return categoria == Categoria.LISTA || categoria == Categoria.PILHA;
    }
    
    public boolean temRetorno() {
        return temRetorno;
    }
    
    // Setters
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    
    public void setTipo(TipoSemantico tipo) {
        this.tipo = tipo;
    }
    
    public void setInicializado(boolean inicializado) {
        this.inicializado = inicializado;
    }
    
    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
    }
    
    public void setTemRetorno(boolean temRetorno) {
        this.temRetorno = temRetorno;
    }
    
    public void setTipoRetorno(TipoSemantico tipoRetorno) {
        this.tipoRetorno = tipoRetorno;
    }
    
    /**
     * Adiciona um parametro a funcao
     */
    public void adicionarParametro(Simbolo parametro) {
        if (this.categoria == Categoria.FUNCAO) {
            parametro.setCategoria(Categoria.PARAMETRO);
            this.parametros.add(parametro);
        }
    }
    
    /**
     * Obtem o tipo do parametro na posicao especificada
     */
    public TipoSemantico getTipoParametro(int index) {
        if (index >= 0 && index < parametros.size()) {
            return parametros.get(index).getTipo();
        }
        return TipoSemantico.INDEFINIDO;
    }
    
    /**
     * Verifica se os argumentos passados sao compativeis com os parametros
     */
    public boolean verificarArgumentos(List<TipoSemantico> tiposArgumentos) {
        if (tiposArgumentos.size() != parametros.size()) {
            return false;
        }
        
        for (int i = 0; i < parametros.size(); i++) {
            TipoSemantico tipoEsperado = parametros.get(i).getTipo();
            TipoSemantico tipoRecebido = tiposArgumentos.get(i);
            
            if (!TipoSemantico.saoCompativeis(tipoEsperado, tipoRecebido)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Retorna uma string representando a assinatura da funcao
     */
    public String getAssinaturaFuncao() {
        if (categoria != Categoria.FUNCAO) {
            return nome;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(nome).append("(");
        
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parametros.get(i).getTipo().getNomePortugues());
        }
        
        sb.append(")");
        
        if (tipoRetorno != TipoSemantico.VAZIO) {
            sb.append(" : ").append(tipoRetorno.getNomePortugues());
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Simbolo{");
        sb.append("nome='").append(nome).append("'");
        sb.append(", categoria=").append(categoria);
        
        if (categoria == Categoria.FUNCAO) {
            sb.append(", assinatura='").append(getAssinaturaFuncao()).append("'");
        } else {
            sb.append(", tipo=").append(tipo.getNomePortugues());
        }
        
        sb.append(", escopo='").append(escopo).append("'");
        sb.append(", nivel=").append(nivelEscopo);
        sb.append(", linha=").append(linhaDeclaracao);
        sb.append(", inicializado=").append(inicializado);
        sb.append(", utilizado=").append(utilizado);
        sb.append("}");
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Simbolo other = (Simbolo) obj;
        return nome.equals(other.nome) && escopo.equals(other.escopo);
    }
    
    @Override
    public int hashCode() {
        return nome.hashCode() * 31 + escopo.hashCode();
    }
}
