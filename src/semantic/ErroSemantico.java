package semantic;

/**
 * Representa um erro semantico encontrado durante a analise
 */
public class ErroSemantico {
    
    /**
     * Tipos de erros semanticos
     */
    public enum TipoErro {
        VARIAVEL_NAO_DECLARADA("Variavel nao declarada"),
        VARIAVEL_JA_DECLARADA("Variavel ja declarada neste escopo"),
        VARIAVEL_NAO_INICIALIZADA("Variavel utilizada antes de ser inicializada"),
        
        FUNCAO_NAO_DECLARADA("Funcao nao declarada"),
        FUNCAO_JA_DECLARADA("Funcao ja declarada"),
        FUNCAO_SEM_RETORNO("Funcao declarada com tipo de retorno mas nao retorna valor"),
        
        TIPO_INCOMPATIVEL("Tipos incompativeis"),
        TIPO_ATRIBUICAO_INVALIDA("Tipo da expressao incompativel com tipo da variavel"),
        TIPO_OPERACAO_INVALIDA("Operacao invalida para os tipos dos operandos"),
        TIPO_RETORNO_INVALIDO("Tipo do valor retornado incompativel com tipo declarado da funcao"),
        TIPO_ARGUMENTO_INVALIDO("Tipo do argumento incompativel com tipo do parametro"),
        
        NUMERO_ARGUMENTOS_INCORRETO("Numero de argumentos incorreto na chamada de funcao"),
        
        RETORNO_FORA_FUNCAO("Comando devolva usado fora de funcao"),
        
        OPERACAO_LISTA_INVALIDA("Operacao invalida para lista"),
        OPERACAO_PILHA_INVALIDA("Operacao invalida para pilha"),
        INDICE_LISTA_INVALIDO("Indice de lista deve ser inteiro"),
        
        DIVISAO_POR_ZERO("Possivel divisao por zero"),
        
        IDENTIFICADOR_NAO_E_FUNCAO("Identificador nao e uma funcao"),
        IDENTIFICADOR_NAO_E_VARIAVEL("Identificador nao e uma variavel"),
        IDENTIFICADOR_NAO_E_LISTA("Identificador nao e uma lista"),
        IDENTIFICADOR_NAO_E_PILHA("Identificador nao e uma pilha");
        
        private final String descricao;
        
        TipoErro(String descricao) {
            this.descricao = descricao;
        }
        
        public String getDescricao() {
            return descricao;
        }
    }
    
    // Tipo do erro
    private TipoErro tipo;
    
    // Mensagem detalhada
    private String mensagem;
    
    // Posicao no codigo fonte
    private int linha;
    private int coluna;
    
    // Identificador relacionado ao erro (se houver)
    private String identificador;
    
    // Tipos envolvidos (para erros de incompatibilidade de tipos)
    private TipoSemantico tipoEsperado;
    private TipoSemantico tipoEncontrado;
    
    /**
     * Construtor basico com mensagem
     */
    public ErroSemantico(TipoErro tipo, String mensagem, int linha, int coluna) {
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
        this.identificador = null;
    }
    
    /**
     * Construtor privado para uso interno com identificador
     */
    private ErroSemantico(TipoErro tipo, String identificador, int linha, int coluna, boolean usarIdentificador) {
        this.tipo = tipo;
        this.identificador = identificador;
        this.linha = linha;
        this.coluna = coluna;
        this.mensagem = gerarMensagemPadrao();
    }
    
    /**
     * Factory method para criar erro com identificador
     */
    public static ErroSemantico comIdentificador(TipoErro tipo, String identificador, int linha, int coluna) {
        return new ErroSemantico(tipo, identificador, linha, coluna, true);
    }
    
    /**
     * Construtor para erros de tipo
     */
    public ErroSemantico(TipoErro tipo, String identificador, 
                         TipoSemantico tipoEsperado, TipoSemantico tipoEncontrado,
                         int linha, int coluna) {
        this.tipo = tipo;
        this.identificador = identificador;
        this.tipoEsperado = tipoEsperado;
        this.tipoEncontrado = tipoEncontrado;
        this.linha = linha;
        this.coluna = coluna;
        this.mensagem = gerarMensagemTipos();
    }
    
    /**
     * Gera mensagem padrao baseada no tipo de erro e identificador
     */
    private String gerarMensagemPadrao() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.getDescricao());
        
        if (identificador != null && !identificador.isEmpty()) {
            sb.append(": '").append(identificador).append("'");
        }
        
        return sb.toString();
    }
    
    /**
     * Gera mensagem detalhada para erros de tipos
     */
    private String gerarMensagemTipos() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.getDescricao());
        
        if (identificador != null && !identificador.isEmpty()) {
            sb.append(" em '").append(identificador).append("'");
        }
        
        if (tipoEsperado != null && tipoEncontrado != null) {
            sb.append(". Esperado: '").append(tipoEsperado.getNomePortugues());
            sb.append("', encontrado: '").append(tipoEncontrado.getNomePortugues()).append("'");
        }
        
        return sb.toString();
    }
    
    // Getters
    public TipoErro getTipo() {
        return tipo;
    }
    
    public String getMensagem() {
        return mensagem;
    }
    
    public int getLinha() {
        return linha;
    }
    
    public int getColuna() {
        return coluna;
    }
    
    public String getIdentificador() {
        return identificador;
    }
    
    public TipoSemantico getTipoEsperado() {
        return tipoEsperado;
    }
    
    public TipoSemantico getTipoEncontrado() {
        return tipoEncontrado;
    }
    
    /**
     * Verifica se e um erro de declaracao
     */
    public boolean isErroDeclaracao() {
        return tipo == TipoErro.VARIAVEL_NAO_DECLARADA || 
               tipo == TipoErro.VARIAVEL_JA_DECLARADA ||
               tipo == TipoErro.FUNCAO_NAO_DECLARADA ||
               tipo == TipoErro.FUNCAO_JA_DECLARADA;
    }
    
    /**
     * Verifica se e um erro de tipo
     */
    public boolean isErroTipo() {
        return tipo == TipoErro.TIPO_INCOMPATIVEL ||
               tipo == TipoErro.TIPO_ATRIBUICAO_INVALIDA ||
               tipo == TipoErro.TIPO_OPERACAO_INVALIDA ||
               tipo == TipoErro.TIPO_RETORNO_INVALIDO ||
               tipo == TipoErro.TIPO_ARGUMENTO_INVALIDO;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ERRO SEMANTICO");
        
        if (linha > 0) {
            sb.append(" [linha ").append(linha);
            if (coluna > 0) {
                sb.append(", coluna ").append(coluna);
            }
            sb.append("]");
        }
        
        sb.append(": ").append(mensagem);
        
        return sb.toString();
    }
    
    /**
     * Gera string para destacar posicao do erro
     */
    public String getPosicaoTag() {
        return "[POSICAO]" + linha + "," + coluna + "[/POSICAO]";
    }
    
    // Factory methods para criar erros comuns
    
    public static ErroSemantico variavelNaoDeclarada(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.VARIAVEL_NAO_DECLARADA, nome, linha, coluna);
    }
    
    public static ErroSemantico variavelJaDeclarada(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.VARIAVEL_JA_DECLARADA, nome, linha, coluna);
    }
    
    public static ErroSemantico variavelNaoInicializada(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.VARIAVEL_NAO_INICIALIZADA, nome, linha, coluna);
    }
    
    public static ErroSemantico funcaoNaoDeclarada(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.FUNCAO_NAO_DECLARADA, nome, linha, coluna);
    }
    
    public static ErroSemantico funcaoJaDeclarada(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.FUNCAO_JA_DECLARADA, nome, linha, coluna);
    }
    
    public static ErroSemantico tipoIncompativel(String contexto, TipoSemantico esperado, 
                                                  TipoSemantico encontrado, int linha, int coluna) {
        return new ErroSemantico(TipoErro.TIPO_INCOMPATIVEL, contexto, esperado, encontrado, linha, coluna);
    }
    
    public static ErroSemantico atribuicaoInvalida(String variavel, TipoSemantico tipoVar, 
                                                    TipoSemantico tipoExpr, int linha, int coluna) {
        return new ErroSemantico(TipoErro.TIPO_ATRIBUICAO_INVALIDA, variavel, tipoVar, tipoExpr, linha, coluna);
    }
    
    public static ErroSemantico operacaoInvalida(String operacao, TipoSemantico tipo1, 
                                                  TipoSemantico tipo2, int linha, int coluna) {
        return new ErroSemantico(TipoErro.TIPO_OPERACAO_INVALIDA, operacao, tipo1, tipo2, linha, coluna);
    }
    
    public static ErroSemantico retornoInvalido(String funcao, TipoSemantico esperado, 
                                                 TipoSemantico encontrado, int linha, int coluna) {
        return new ErroSemantico(TipoErro.TIPO_RETORNO_INVALIDO, funcao, esperado, encontrado, linha, coluna);
    }
    
    public static ErroSemantico argumentoInvalido(String funcao, int posicao, TipoSemantico esperado, 
                                                   TipoSemantico encontrado, int linha, int coluna) {
        String contexto = funcao + " (argumento " + (posicao + 1) + ")";
        return new ErroSemantico(TipoErro.TIPO_ARGUMENTO_INVALIDO, contexto, esperado, encontrado, linha, coluna);
    }
    
    public static ErroSemantico numeroArgumentosIncorreto(String funcao, int esperado, 
                                                           int encontrado, int linha, int coluna) {
        String msg = "Numero de argumentos incorreto na chamada de '" + funcao + 
                    "'. Esperado: " + esperado + ", encontrado: " + encontrado;
        return new ErroSemantico(TipoErro.NUMERO_ARGUMENTOS_INCORRETO, msg, linha, coluna);
    }
    
    public static ErroSemantico retornoForaDeFuncao(int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.RETORNO_FORA_FUNCAO, "devolva", linha, coluna);
    }
    
    public static ErroSemantico identificadorNaoEFuncao(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.IDENTIFICADOR_NAO_E_FUNCAO, nome, linha, coluna);
    }
    
    public static ErroSemantico identificadorNaoEVariavel(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.IDENTIFICADOR_NAO_E_VARIAVEL, nome, linha, coluna);
    }
    
    public static ErroSemantico identificadorNaoELista(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.IDENTIFICADOR_NAO_E_LISTA, nome, linha, coluna);
    }
    
    public static ErroSemantico identificadorNaoEPilha(String nome, int linha, int coluna) {
        return ErroSemantico.comIdentificador(TipoErro.IDENTIFICADOR_NAO_E_PILHA, nome, linha, coluna);
    }
}
