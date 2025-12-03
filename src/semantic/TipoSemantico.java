package semantic;

/**
 * Enum que representa os tipos de dados semanticos do BrCompiler
 * Tipos primitivos: stonks (int), fiado (float), textao (string), eh-migue (bool)
 * Estruturas de dados: tropa-do-gordao (lista), sanduiche-iche (pilha)
 */
public enum TipoSemantico {
    // Tipos primitivos
    INTEIRO("stonks"),           // stonks - tipo inteiro
    DECIMAL("fiado"),            // fiado - tipo float/decimal
    TEXTO("textao"),             // textao - tipo string
    LOGICO("eh-migue"),          // eh-migue - tipo booleano
    
    // Estruturas de dados
    LISTA_INTEIRO("tropa-do-gordao stonks"),
    LISTA_DECIMAL("tropa-do-gordao fiado"),
    LISTA_TEXTO("tropa-do-gordao textao"),
    LISTA_LOGICO("tropa-do-gordao eh-migue"),
    
    PILHA_INTEIRO("sanduiche-iche stonks"),
    PILHA_DECIMAL("sanduiche-iche fiado"),
    PILHA_TEXTO("sanduiche-iche textao"),
    PILHA_LOGICO("sanduiche-iche eh-migue"),
    
    // Tipo especial para funcoes sem retorno
    VAZIO("vazio"),
    
    // Tipo indefinido/erro
    INDEFINIDO("indefinido"),
    
    // Tipo generico para listas e pilhas (quando nao se sabe o subtipo)
    LISTA("tropa-do-gordao"),
    PILHA("sanduiche-iche");
    
    private final String nomePortugues;
    
    TipoSemantico(String nomePortugues) {
        this.nomePortugues = nomePortugues;
    }
    
    public String getNomePortugues() {
        return nomePortugues;
    }
    
    /**
     * Verifica se este tipo e um tipo numerico (inteiro ou decimal)
     */
    public boolean isNumerico() {
        return this == INTEIRO || this == DECIMAL;
    }
    
    /**
     * Verifica se este tipo e uma lista
     */
    public boolean isLista() {
        return this == LISTA || this == LISTA_INTEIRO || this == LISTA_DECIMAL || 
               this == LISTA_TEXTO || this == LISTA_LOGICO;
    }
    
    /**
     * Verifica se este tipo e uma pilha
     */
    public boolean isPilha() {
        return this == PILHA || this == PILHA_INTEIRO || this == PILHA_DECIMAL || 
               this == PILHA_TEXTO || this == PILHA_LOGICO;
    }
    
    /**
     * Verifica se este tipo e uma estrutura de dados (lista ou pilha)
     */
    public boolean isEstruturaDados() {
        return isLista() || isPilha();
    }
    
    /**
     * Verifica se este tipo e um tipo primitivo
     */
    public boolean isPrimitivo() {
        return this == INTEIRO || this == DECIMAL || this == TEXTO || this == LOGICO;
    }
    
    /**
     * Obtem o tipo base de uma estrutura de dados
     * Ex: LISTA_INTEIRO -> INTEIRO, PILHA_TEXTO -> TEXTO
     */
    public TipoSemantico getTipoBase() {
        switch (this) {
            case LISTA_INTEIRO:
            case PILHA_INTEIRO:
                return INTEIRO;
            case LISTA_DECIMAL:
            case PILHA_DECIMAL:
                return DECIMAL;
            case LISTA_TEXTO:
            case PILHA_TEXTO:
                return TEXTO;
            case LISTA_LOGICO:
            case PILHA_LOGICO:
                return LOGICO;
            default:
                return this;
        }
    }
    
    /**
     * Converte string do token para TipoSemantico
     */
    public static TipoSemantico fromToken(String token) {
        if (token == null) return INDEFINIDO;
        
        switch (token.toLowerCase()) {
            case "stonks":
                return INTEIRO;
            case "fiado":
                return DECIMAL;
            case "textao":
                return TEXTO;
            case "eh-migue":
                return LOGICO;
            case "tropa-do-gordao":
                return LISTA;
            case "sanduiche-iche":
                return PILHA;
            default:
                return INDEFINIDO;
        }
    }
    
    /**
     * Cria tipo de lista a partir do tipo base
     */
    public static TipoSemantico criarTipoLista(TipoSemantico tipoBase) {
        switch (tipoBase) {
            case INTEIRO:
                return LISTA_INTEIRO;
            case DECIMAL:
                return LISTA_DECIMAL;
            case TEXTO:
                return LISTA_TEXTO;
            case LOGICO:
                return LISTA_LOGICO;
            default:
                return LISTA;
        }
    }
    
    /**
     * Cria tipo de pilha a partir do tipo base
     */
    public static TipoSemantico criarTipoPilha(TipoSemantico tipoBase) {
        switch (tipoBase) {
            case INTEIRO:
                return PILHA_INTEIRO;
            case DECIMAL:
                return PILHA_DECIMAL;
            case TEXTO:
                return PILHA_TEXTO;
            case LOGICO:
                return PILHA_LOGICO;
            default:
                return PILHA;
        }
    }
    
    /**
     * Verifica compatibilidade entre dois tipos para atribuicao
     * Retorna true se o tipoOrigem pode ser atribuido ao tipoDestino
     */
    public static boolean saoCompativeis(TipoSemantico tipoDestino, TipoSemantico tipoOrigem) {
        if (tipoDestino == tipoOrigem) {
            return true;
        }
        
        // Indefinido e compativel com tudo (para evitar erros em cascata)
        if (tipoDestino == INDEFINIDO || tipoOrigem == INDEFINIDO) {
            return true;
        }
        
        // Inteiro pode ser atribuido a decimal (promocao de tipo)
        if (tipoDestino == DECIMAL && tipoOrigem == INTEIRO) {
            return true;
        }
        
        // Verifica compatibilidade de listas
        if (tipoDestino.isLista() && tipoOrigem.isLista()) {
            // Lista generica e compativel com qualquer lista tipada
            if (tipoDestino == LISTA || tipoOrigem == LISTA) {
                return true;
            }
            return tipoDestino.getTipoBase() == tipoOrigem.getTipoBase();
        }
        
        // Verifica compatibilidade de pilhas
        if (tipoDestino.isPilha() && tipoOrigem.isPilha()) {
            if (tipoDestino == PILHA || tipoOrigem == PILHA) {
                return true;
            }
            return tipoDestino.getTipoBase() == tipoOrigem.getTipoBase();
        }
        
        return false;
    }
    
    /**
     * Determina o tipo resultante de uma operacao aritmetica
     */
    public static TipoSemantico tipoResultanteAritmetico(TipoSemantico tipo1, TipoSemantico tipo2) {
        // Se algum for indefinido, retorna indefinido
        if (tipo1 == INDEFINIDO || tipo2 == INDEFINIDO) {
            return INDEFINIDO;
        }
        
        // Operacoes aritmeticas so sao validas para tipos numericos
        if (!tipo1.isNumerico() || !tipo2.isNumerico()) {
            return INDEFINIDO;
        }
        
        // Se um deles for decimal, resultado e decimal
        if (tipo1 == DECIMAL || tipo2 == DECIMAL) {
            return DECIMAL;
        }
        
        return INTEIRO;
    }
    
    /**
     * Determina o tipo resultante de uma operacao de comparacao
     * Comparacoes sempre retornam booleano se os operandos forem validos
     */
    public static TipoSemantico tipoResultanteComparacao(TipoSemantico tipo1, TipoSemantico tipo2) {
        if (tipo1 == INDEFINIDO || tipo2 == INDEFINIDO) {
            return INDEFINIDO;
        }
        
        // Comparacoes numericas
        if (tipo1.isNumerico() && tipo2.isNumerico()) {
            return LOGICO;
        }
        
        // Comparacoes de igualdade entre mesmos tipos
        if (tipo1 == tipo2) {
            return LOGICO;
        }
        
        return INDEFINIDO;
    }
    
    /**
     * Determina o tipo resultante de uma operacao logica (&&, |)
     */
    public static TipoSemantico tipoResultanteLogico(TipoSemantico tipo1, TipoSemantico tipo2) {
        if (tipo1 == INDEFINIDO || tipo2 == INDEFINIDO) {
            return INDEFINIDO;
        }
        
        // Operacoes logicas requerem operandos booleanos
        if (tipo1 == LOGICO && tipo2 == LOGICO) {
            return LOGICO;
        }
        
        return INDEFINIDO;
    }
    
    /**
     * Verifica se a concatenacao e permitida (para strings)
     */
    public static boolean permiteConcatenacao(TipoSemantico tipo1, TipoSemantico tipo2) {
        // Concatenacao e permitida se pelo menos um for texto
        return tipo1 == TEXTO || tipo2 == TEXTO;
    }
    
    @Override
    public String toString() {
        return nomePortugues;
    }
}
