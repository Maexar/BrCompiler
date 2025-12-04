package semantic;

import parser.SimpleNode;
import parser.BrCompilerTreeConstants;
import parser.Token;
import parser.BrCompiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Analisador Semantico do BrCompiler
 * Percorre a AST e realiza verificacoes semanticas:
 * - Declaracao de variaveis/funcoes antes do uso
 * - Verificacao de tipos em operacoes e atribuicoes
 * - Verificacao de escopo
 * - Verificacao de chamadas de funcao
 */
public class AnalisadorSemantico {
    
    // Tabela de simbolos
    private TabelaSimbolos tabelaSimbolos;
    
    // Lista de erros semanticos encontrados
    private List<ErroSemantico> erros;
    
    // Flag para depuracao
    private boolean debug = true;
    
    // Raiz da AST
    private SimpleNode raiz;
    
    /**
     * Construtor
     */
    public AnalisadorSemantico() {
        this.tabelaSimbolos = new TabelaSimbolos();
        this.erros = new ArrayList<>();
    }
    
    /**
     * Inicia a analise semantica a partir da raiz da AST
     * @param raiz No raiz da AST
     */
    public void analisar(SimpleNode raiz) {
        this.raiz = raiz;
        this.erros.clear();
        this.tabelaSimbolos.limpar();
        
        if (raiz == null) {
            System.out.println("[AnalisadorSemantico] AST nula - nada para analisar");
            return;
        }
        
        System.out.println("\n========================================");
        System.out.println("INICIANDO ANALISE SEMANTICA");
        System.out.println("========================================\n");
        
        // Primeira passada: coletar declaracoes de funcoes (para permitir chamadas antes da declaracao)
        primeiraPassada(raiz);
        
        // Segunda passada: analise completa
        analisarNo(raiz);
        
        System.out.println("\n========================================");
        System.out.println("ANALISE SEMANTICA CONCLUIDA");
        System.out.println("Erros encontrados: " + erros.size());
        System.out.println("========================================\n");
    }
    
    /**
     * Primeira passada - coleta declaracoes de funcoes
     */
    private void primeiraPassada(SimpleNode no) {
        if (no == null) return;
        
        if (no.getId() == BrCompilerTreeConstants.JJTDECLARAFUNCAO) {
            coletarDeclaracaoFuncao(no);
        }
        
        // Processa filhos
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            primeiraPassada((SimpleNode) no.jjtGetChild(i));
        }
    }
    
    /**
     * Coleta informacoes de uma declaracao de funcao (primeira passada)
     */
    private void coletarDeclaracaoFuncao(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Coletando declaracao de funcao");
        
        // Estrutura esperada: vai-filhao (tipoRetorno)? identificador (parametros)? bloco
        String nomeFuncao = null;
        TipoSemantico tipoRetorno = TipoSemantico.VAZIO;
        List<Simbolo> parametros = new ArrayList<>();
        int linha = 0;
        int coluna = 0;
        
        // Analisa tokens e filhos do no
        Token token = obterPrimeiroToken(no);
        if (token != null) {
            linha = token.beginLine;
            coluna = token.beginColumn;
        }
        
        int numFilhos = no.jjtGetNumChildren();
        int indiceAtual = 0;
        
        // Verifica se tem tipo de retorno
        if (numFilhos > 0) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(0);
            if (filho.getId() == BrCompilerTreeConstants.JJTTIPODADO) {
                tipoRetorno = extrairTipoDado(filho);
                indiceAtual = 1;
            }
        }
        
        // Busca nome da funcao (esta nos tokens)
        nomeFuncao = buscarIdentificadorNoNo(no);
        
        // Busca parametros
        for (int i = indiceAtual; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTLISTAPARAMETROS) {
                parametros = extrairParametros(filho);
                break;
            }
        }
        
        if (nomeFuncao != null) {
            // Verifica se funcao ja foi declarada
            if (tabelaSimbolos.existeNoEscopoAtual(nomeFuncao)) {
                adicionarErro(ErroSemantico.funcaoJaDeclarada(nomeFuncao, linha, coluna));
            } else {
                tabelaSimbolos.inserirFuncao(nomeFuncao, tipoRetorno, parametros, linha, coluna);
            }
        }
    }
    
    /**
     * Analisa um no da AST (segunda passada)
     */
    private void analisarNo(SimpleNode no) {
        if (no == null) return;
        
        if (debug) {
            System.out.println("[AnalisadorSemantico] Analisando no: " + 
                              BrCompilerTreeConstants.jjtNodeName[no.getId()]);
        }
        
        switch (no.getId()) {
            case BrCompilerTreeConstants.JJTMAIN:
                analisarMain(no);
                break;
                
            case BrCompilerTreeConstants.JJTBLOCO:
                analisarBloco(no);
                break;
                
            case BrCompilerTreeConstants.JJTDECLARAVARIAVEL:
                analisarDeclaracaoVariavel(no);
                break;
                
            case BrCompilerTreeConstants.JJTDECLARAFUNCAO:
                analisarDeclaracaoFuncao(no);
                break;
                
            case BrCompilerTreeConstants.JJTCOMANDOIDENTIFICADOR:
                analisarComandoIdentificador(no);
                break;
                
            case BrCompilerTreeConstants.JJTEXPRESSAOCONDICIONAL:
                analisarExpressaoCondicional(no);
                break;
                
            case BrCompilerTreeConstants.JJTWHILELOOP:
                analisarWhileLoop(no);
                break;
                
            case BrCompilerTreeConstants.JJTFORLOOP:
                analisarForLoop(no);
                break;
                
            case BrCompilerTreeConstants.JJTPRINT:
                analisarPrint(no);
                break;
                
            case BrCompilerTreeConstants.JJTSCAN:
                analisarScan(no);
                break;
                
            case BrCompilerTreeConstants.JJTRETORNO:
                analisarRetorno(no);
                break;
                
            default:
                // Para outros nos, apenas processa os filhos
                analisarFilhos(no);
                break;
        }
    }
    
    /**
     * Analisa os filhos de um no
     */
    private void analisarFilhos(SimpleNode no) {
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            analisarNo((SimpleNode) no.jjtGetChild(i));
        }
    }
    
    /**
     * Analisa o no main (programa principal)
     */
    private void analisarMain(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando programa principal (gambiarra)");
        
        // Processa os filhos (blocos)
        analisarFilhos(no);
    }
    
    /**
     * Analisa um bloco de comandos
     */
    private void analisarBloco(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando bloco");
        
        // Processa os filhos (comandos)
        analisarFilhos(no);
    }
    
    /**
     * Analisa uma declaracao de variavel
     */
 private void analisarDeclaracaoVariavel(SimpleNode no) {
    if (debug) System.out.println("[AnalisadorSemantico] Analisando declaracao de variavel");
    
    // Estrutura: tipoDado listaIdentificadores (receba listaExpressoes)?
    TipoSemantico tipo = TipoSemantico.INDEFINIDO;
    List<String> identificadores = new ArrayList<>();
    List<TipoSemantico> tiposExpressoes = new ArrayList<>();
    boolean temAtribuicao = false;
    
    int numFilhos = no.jjtGetNumChildren();
    
    // Primeiro filho deve ser tipoDado
    if (numFilhos > 0) {
        SimpleNode tipoNo = (SimpleNode) no.jjtGetChild(0);
        if (tipoNo.getId() == BrCompilerTreeConstants.JJTTIPODADO) {
            tipo = extrairTipoDado(tipoNo);
        }
    }
    
    // Segundo filho deve ser listaIdentificadores
    if (numFilhos > 1) {
        SimpleNode listaId = (SimpleNode) no.jjtGetChild(1);
        if (listaId.getId() == BrCompilerTreeConstants.JJTLISTAIDENTIFICADORES) {
            identificadores = extrairIdentificadores(listaId);
        }
    }
    
    // Terceiro filho (se existir) deve ser listaExpressoes (atribuicao)
    if (numFilhos > 2) {
        SimpleNode listaExpr = (SimpleNode) no.jjtGetChild(2);
        if (listaExpr.getId() == BrCompilerTreeConstants.JJTLISTAEXPRESSOES) {
            temAtribuicao = true;
            tiposExpressoes = analisarListaExpressoes(listaExpr);
        }
    }
    
    Token token = obterPrimeiroToken(no);
    int linha = token != null ? token.beginLine : 0;
    int coluna = token != null ? token.beginColumn : 0;
    
    // **CORREÇÃO AQUI**: Remove duplicatas antes de processar
    List<String> identificadoresUnicos = new ArrayList<>();
    java.util.Set<String> vistos = new java.util.HashSet<>();
    
    for (String nome : identificadores) {
        if (!vistos.contains(nome)) {
            identificadoresUnicos.add(nome);
            vistos.add(nome);
        } else {
            // Duplicata detectada na mesma declaracao
            adicionarErro(new ErroSemantico(
                ErroSemantico.TipoErro.VARIAVEL_JA_DECLARADA,
                "Identificador '" + nome + "' declarado mais de uma vez na mesma instrucao",
                linha, coluna
            ));
        }
    }
    
    // Insere cada identificador UNICO na tabela de simbolos
    for (int i = 0; i < identificadoresUnicos.size(); i++) {
        String nome = identificadoresUnicos.get(i);
        
        // Verifica se ja existe no escopo atual
        if (tabelaSimbolos.existeNoEscopoAtual(nome)) {
            adicionarErro(ErroSemantico.variavelJaDeclarada(nome, linha, coluna));
            continue; // **IMPORTANTE**: Não tenta inserir novamente
        }
        
        // Verifica compatibilidade de tipos se houver atribuicao
        if (temAtribuicao && i < tiposExpressoes.size()) {
            TipoSemantico tipoExpr = tiposExpressoes.get(i);
            if (!TipoSemantico.saoCompativeis(tipo, tipoExpr)) {
                adicionarErro(ErroSemantico.atribuicaoInvalida(nome, tipo, tipoExpr, linha, coluna));
            }
            tabelaSimbolos.inserirVariavelInicializada(nome, tipo, linha, coluna);
        } else {
            tabelaSimbolos.inserirVariavel(nome, tipo, linha, coluna);
        }
    }
}
    
    /**
     * Analisa uma declaracao de funcao (segunda passada - corpo da funcao)
     */
    private void analisarDeclaracaoFuncao(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando corpo de funcao");
        
        String nomeFuncao = buscarIdentificadorNoNo(no);
        
        if (nomeFuncao == null) {
            return;
        }
        
        Simbolo funcao = tabelaSimbolos.buscar(nomeFuncao);
        if (funcao == null) {
            return; // Erro ja reportado na primeira passada
        }
        
        // Abre escopo da funcao
        tabelaSimbolos.abrirEscopoFuncao(nomeFuncao, funcao);
        
        // Adiciona parametros ao escopo da funcao
        for (Simbolo param : funcao.getParametros()) {
            Token token = obterPrimeiroToken(no);
            int linha = token != null ? token.beginLine : 0;
            int coluna = token != null ? token.beginColumn : 0;
            
            Simbolo paramNoEscopo = new Simbolo(
                param.getNome(), 
                param.getTipo(), 
                nomeFuncao, 
                tabelaSimbolos.getNivelAtual(),
                linha, coluna
            );
            paramNoEscopo.setCategoria(Simbolo.Categoria.PARAMETRO);
            paramNoEscopo.setInicializado(true);
            tabelaSimbolos.inserir(paramNoEscopo);
        }
        
        // Analisa o bloco da funcao
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTBLOCO) {
                analisarBloco(filho);
            }
        }
        
        // Verifica se funcao com tipo de retorno tem comando devolva
        if (funcao.getTipoRetorno() != TipoSemantico.VAZIO && !funcao.temRetorno()) {
            Token token = obterPrimeiroToken(no);
            int linha = token != null ? token.beginLine : 0;
            int coluna = token != null ? token.beginColumn : 0;
            
            adicionarErro(new ErroSemantico(
                ErroSemantico.TipoErro.FUNCAO_SEM_RETORNO,
                "Funcao '" + nomeFuncao + "' declarada com tipo '" + 
                funcao.getTipoRetorno().getNomePortugues() + "' mas nao possui comando devolva",
                linha, coluna
            ));
        }
        
        // Fecha escopo da funcao
        tabelaSimbolos.fecharEscopo();
    }
    
    /**
     * Analisa um comando que comeca com identificador
     */
    private void analisarComandoIdentificador(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando comando identificador");
        
        String nomeId = buscarIdentificadorNoNo(no);
        
        if (nomeId == null) {
            return;
        }
        
        Token token = obterPrimeiroToken(no);
        int linha = token != null ? token.beginLine : 0;
        int coluna = token != null ? token.beginColumn : 0;
        
        // Verifica se identificador foi declarado
        Simbolo simbolo = tabelaSimbolos.buscar(nomeId);
        if (simbolo == null) {
            adicionarErro(ErroSemantico.variavelNaoDeclarada(nomeId, linha, coluna));
            return;
        }
        
        // Analisa o sufixo do comando
        int numFilhos = no.jjtGetNumChildren();
        if (numFilhos > 0) {
            SimpleNode sufixo = (SimpleNode) no.jjtGetChild(0);
            analisarComandoIdentificadorSufixo(sufixo, simbolo, linha, coluna);
        }
    }
    
    /**
     * Analisa o sufixo de um comando identificador (atribuicao, chamada, etc)
     */
   private void analisarComandoIdentificadorSufixo(SimpleNode no, Simbolo simbolo, int linha, int coluna) {
    if (no == null) return;
    
    // Verifica tokens para determinar tipo de operacao
    Token token = obterPrimeiroToken(no);
    
    // Analisa filhos para expressoes
    int numFilhos = no.jjtGetNumChildren();
    
    // **PRIORIDADE 1**: Operacoes de lista (recrutar, expulsar)
    if (temOperacaoLista(no)) {
        if (!simbolo.getTipo().isLista()) {
            adicionarErro(ErroSemantico.identificadorNaoELista(simbolo.getNome(), linha, coluna));
            return;
        }
        analisarOperacaoLista(no, simbolo, linha, coluna);
        return; // IMPORTANTE: retorna para não cair em outras verificações
    }
    
    // **PRIORIDADE 2**: Operacoes de pilha (montar-sanduba, comer-sanduba)
    if (temOperacaoPilha(no)) {
        if (!simbolo.getTipo().isPilha()) {
            adicionarErro(ErroSemantico.identificadorNaoEPilha(simbolo.getNome(), linha, coluna));
            return;
        }
        analisarOperacaoPilha(no, simbolo, linha, coluna);
        return; // IMPORTANTE: retorna para não cair em outras verificações
    }
    
    // **PRIORIDADE 3**: Chamada de funcao (tem parenteses)
    if (temChamadaFuncao(no)) {
        if (!simbolo.isFuncao()) {
            adicionarErro(ErroSemantico.identificadorNaoEFuncao(simbolo.getNome(), linha, coluna));
            return;
        }
        analisarChamadaFuncao(no, simbolo, linha, coluna);
        return; // IMPORTANTE: retorna para não cair em outras verificações
    }
    
    // **PRIORIDADE 4**: Atribuicao generica (receba)
    if (temTokenAtribuicao(no)) {
        if (simbolo.isFuncao()) {
            adicionarErro(ErroSemantico.identificadorNaoEVariavel(simbolo.getNome(), linha, coluna));
            return;
        }
        
        // Verifica tipo da expressao
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                TipoSemantico tipoExpr = analisarExpressao(filho);
                if (!TipoSemantico.saoCompativeis(simbolo.getTipo(), tipoExpr)) {
                    adicionarErro(ErroSemantico.atribuicaoInvalida(
                        simbolo.getNome(), simbolo.getTipo(), tipoExpr, linha, coluna));
                }
                simbolo.setInicializado(true);
                break;
            }
        }
        return; // IMPORTANTE: retorna após processar
    }
    
    // Se nenhuma das verificações acima passou, analisa filhos genericamente
    analisarFilhos(no);
}
    
    /**
     * Analisa expressao condicional (sepa)
     */
    private void analisarExpressaoCondicional(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando expressao condicional");
        
        // Abre escopo para o bloco condicional
        tabelaSimbolos.abrirEscopo("sepa_" + System.currentTimeMillis());
        
        // Analisa condicoes e blocos
        analisarFilhos(no);
        
        // Fecha escopo
        tabelaSimbolos.fecharEscopo();
    }
    
    /**
     * Analisa loop while (repet)
     */
    private void analisarWhileLoop(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando loop repet");
        
        // Abre escopo para o loop
        tabelaSimbolos.abrirEscopo("repet_" + System.currentTimeMillis());
        
        // Verifica condicao e bloco
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAOLOGICA) {
                TipoSemantico tipoCondicao = analisarExpressaoLogica(filho);
                if (tipoCondicao != TipoSemantico.LOGICO && tipoCondicao != TipoSemantico.INDEFINIDO) {
                    Token token = obterPrimeiroToken(filho);
                    int linha = token != null ? token.beginLine : 0;
                    int coluna = token != null ? token.beginColumn : 0;
                    adicionarErro(ErroSemantico.tipoIncompativel(
                        "condicao do repet", TipoSemantico.LOGICO, tipoCondicao, linha, coluna));
                }
            } else if (filho.getId() == BrCompilerTreeConstants.JJTBLOCO) {
                analisarBloco(filho);
            }
        }
        
        // Fecha escopo
        tabelaSimbolos.fecharEscopo();
    }
    
    /**
     * Analisa loop for (pet)
     */
    private void analisarForLoop(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando loop pet");
        
        // Abre escopo para o loop
        tabelaSimbolos.abrirEscopo("pet_" + System.currentTimeMillis());
        
        // Analisa inicializacao, condicao, incremento e bloco
        analisarFilhos(no);
        
        // Fecha escopo
        tabelaSimbolos.fecharEscopo();
    }
    
    /**
     * Analisa comando print (printa)
     */
    private void analisarPrint(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando comando printa");
        
        // Verifica tipos das expressoes a serem impressas
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTLISTAEXPRESSOES) {
                analisarListaExpressoes(filho);
            }
        }
    }
    
    /**
     * Analisa comando scan (papa-entrada)
     */
    private void analisarScan(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando comando papa-entrada");
        
        String nomeId = buscarIdentificadorNoNo(no);
        
        if (nomeId == null) {
            return;
        }
        
        Token token = obterPrimeiroToken(no);
        int linha = token != null ? token.beginLine : 0;
        int coluna = token != null ? token.beginColumn : 0;
        
        // Verifica se variavel foi declarada
        Simbolo simbolo = tabelaSimbolos.buscar(nomeId);
        if (simbolo == null) {
            adicionarErro(ErroSemantico.variavelNaoDeclarada(nomeId, linha, coluna));
            return;
        }
        
        if (simbolo.isFuncao()) {
            adicionarErro(ErroSemantico.identificadorNaoEVariavel(nomeId, linha, coluna));
            return;
        }
        
        // Marca variavel como inicializada
        simbolo.setInicializado(true);
    }
    
    /**
     * Analisa comando retorno (devolva)
     */
    private void analisarRetorno(SimpleNode no) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando comando devolva");
        
        Token token = obterPrimeiroToken(no);
        int linha = token != null ? token.beginLine : 0;
        int coluna = token != null ? token.beginColumn : 0;
        
        // Verifica se esta dentro de uma funcao
        if (!tabelaSimbolos.estaDentroFuncao()) {
            adicionarErro(ErroSemantico.retornoForaDeFuncao(linha, coluna));
            return;
        }
        
        Simbolo funcaoAtual = tabelaSimbolos.getFuncaoAtual();
        if (funcaoAtual == null) {
            return;
        }
        
        funcaoAtual.setTemRetorno(true);
        
        // Verifica tipo do retorno
        int numFilhos = no.jjtGetNumChildren();
        TipoSemantico tipoRetorno = TipoSemantico.VAZIO;
        
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                tipoRetorno = analisarExpressao(filho);
                break;
            }
        }
        
        // Verifica compatibilidade com tipo declarado da funcao
        TipoSemantico tipoEsperado = funcaoAtual.getTipoRetorno();
        
        if (tipoEsperado == TipoSemantico.VAZIO && tipoRetorno != TipoSemantico.VAZIO) {
            adicionarErro(new ErroSemantico(
                ErroSemantico.TipoErro.TIPO_RETORNO_INVALIDO,
                "Funcao '" + funcaoAtual.getNome() + "' nao deve retornar valor",
                linha, coluna
            ));
        } else if (tipoEsperado != TipoSemantico.VAZIO && tipoRetorno == TipoSemantico.VAZIO) {
            adicionarErro(ErroSemantico.retornoInvalido(
                funcaoAtual.getNome(), tipoEsperado, tipoRetorno, linha, coluna));
        } else if (!TipoSemantico.saoCompativeis(tipoEsperado, tipoRetorno)) {
            adicionarErro(ErroSemantico.retornoInvalido(
                funcaoAtual.getNome(), tipoEsperado, tipoRetorno, linha, coluna));
        }
    }
    
    // ==================== METODOS AUXILIARES ====================
    
    /**
     * Analisa uma lista de expressoes e retorna lista de tipos
     */
    private List<TipoSemantico> analisarListaExpressoes(SimpleNode no) {
        List<TipoSemantico> tipos = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                tipos.add(analisarExpressao(filho));
            }
        }
        
        return tipos;
    }
    
    /**
     * Analisa uma expressao e retorna seu tipo
     */
    private TipoSemantico analisarExpressao(SimpleNode no) {
        if (no == null) return TipoSemantico.INDEFINIDO;
        
        // Expressao pode conter fatores com operadores + e -
        List<TipoSemantico> tiposFatores = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTFATOR) {
                tiposFatores.add(analisarFator(filho));
            }
        }
        
        if (tiposFatores.isEmpty()) {
            return TipoSemantico.INDEFINIDO;
        }
        
        // Se houver mais de um fator, verifica compatibilidade de tipos para adicao/subtracao
        TipoSemantico tipoResultado = tiposFatores.get(0);
        
        for (int i = 1; i < tiposFatores.size(); i++) {
            TipoSemantico tipoAtual = tiposFatores.get(i);
            
            // Verifica se e concatenacao de strings
            if (TipoSemantico.permiteConcatenacao(tipoResultado, tipoAtual)) {
                tipoResultado = TipoSemantico.TEXTO;
            } else {
                TipoSemantico novoTipo = TipoSemantico.tipoResultanteAritmetico(tipoResultado, tipoAtual);
                
                if (novoTipo == TipoSemantico.INDEFINIDO && 
                    tipoResultado != TipoSemantico.INDEFINIDO && 
                    tipoAtual != TipoSemantico.INDEFINIDO) {
                    Token token = obterPrimeiroToken(no);
                    int linha = token != null ? token.beginLine : 0;
                    int coluna = token != null ? token.beginColumn : 0;
                    adicionarErro(ErroSemantico.operacaoInvalida("+/-", tipoResultado, tipoAtual, linha, coluna));
                }
                
                tipoResultado = novoTipo;
            }
        }
        
        return tipoResultado;
    }
    
    /**
     * Analisa um fator e retorna seu tipo
     */
    private TipoSemantico analisarFator(SimpleNode no) {
        if (no == null) return TipoSemantico.INDEFINIDO;
        
        // Fator pode conter termos com operadores * e /
        List<TipoSemantico> tiposTermos = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTTERMO) {
                tiposTermos.add(analisarTermo(filho));
            }
        }
        
        if (tiposTermos.isEmpty()) {
            return TipoSemantico.INDEFINIDO;
        }
        
        TipoSemantico tipoResultado = tiposTermos.get(0);
        
        for (int i = 1; i < tiposTermos.size(); i++) {
            TipoSemantico tipoAtual = tiposTermos.get(i);
            TipoSemantico novoTipo = TipoSemantico.tipoResultanteAritmetico(tipoResultado, tipoAtual);
            
            if (novoTipo == TipoSemantico.INDEFINIDO && 
                tipoResultado != TipoSemantico.INDEFINIDO && 
                tipoAtual != TipoSemantico.INDEFINIDO) {
                Token token = obterPrimeiroToken(no);
                int linha = token != null ? token.beginLine : 0;
                int coluna = token != null ? token.beginColumn : 0;
                adicionarErro(ErroSemantico.operacaoInvalida("*//", tipoResultado, tipoAtual, linha, coluna));
            }
            
            tipoResultado = novoTipo;
        }
        
        return tipoResultado;
    }
    
    /**
     * Analisa um termo e retorna seu tipo
     */
    private TipoSemantico analisarTermo(SimpleNode no) {
        if (no == null) return TipoSemantico.INDEFINIDO;
        
        // Termo pode ser: constante, identificador, chamada de funcao, expressao entre parenteses
        Token token = obterPrimeiroToken(no);
        
        if (token != null) {
            // Verifica tipo de token
            switch (token.kind) {
                case parser.BrCompilerConstants.CONSTANTE_INT:
                    return TipoSemantico.INTEIRO;
                    
                case parser.BrCompilerConstants.CONSTANTE_FLOAT:
                    return TipoSemantico.DECIMAL;
                    
                case parser.BrCompilerConstants.LITERAL_STRING:
                    return TipoSemantico.TEXTO;
                    
                case parser.BrCompilerConstants.TRUE:
                case parser.BrCompilerConstants.FALSE:
                    return TipoSemantico.LOGICO;
                    
                case parser.BrCompilerConstants.IDENTIFICADOR:
                    return analisarIdentificadorEmTermo(token, no);
                    
                case parser.BrCompilerConstants.TAMANHO:
                    return TipoSemantico.INTEIRO; // tamanho sempre retorna inteiro
                    
                case parser.BrCompilerConstants.TOPO:
                    return analisarTopoOperacao(no);
                    
                case parser.BrCompilerConstants.PILHA_VAZIA:
                    return TipoSemantico.LOGICO; // farelo sempre retorna booleano
            }
        }
        
        // Verifica se e uma expressao entre parenteses ou inicializacao de lista
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                return analisarExpressao(filho);
            }
            if (filho.getId() == BrCompilerTreeConstants.JJTINICIALIZACAOLISTA) {
                return analisarInicializacaoLista(filho);
            }
        }
        
        return TipoSemantico.INDEFINIDO;
    }
    
    /**
     * Analisa um identificador usado em um termo
     */
    private TipoSemantico analisarIdentificadorEmTermo(Token token, SimpleNode no) {
        String nome = token.image;
        int linha = token.beginLine;
        int coluna = token.beginColumn;
        
        Simbolo simbolo = tabelaSimbolos.buscar(nome);
        
        if (simbolo == null) {
            adicionarErro(ErroSemantico.variavelNaoDeclarada(nome, linha, coluna));
            return TipoSemantico.INDEFINIDO;
        }
        
        // Verifica se e chamada de funcao (tem sufixo com parenteses)
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTTERMOIDENTIFICADORSUFIXO) {
                if (temChamadaFuncao(filho)) {
                    if (!simbolo.isFuncao()) {
                        adicionarErro(ErroSemantico.identificadorNaoEFuncao(nome, linha, coluna));
                        return TipoSemantico.INDEFINIDO;
                    }
                    analisarChamadaFuncao(filho, simbolo, linha, coluna);
                    return simbolo.getTipoRetorno();
                }
            }
        }
        
        // Verifica inicializacao para variaveis
        if (simbolo.isVariavel() && !simbolo.isInicializado()) {
            adicionarErro(ErroSemantico.variavelNaoInicializada(nome, linha, coluna));
        }
        
        return simbolo.getTipo();
    }
    
    /**
     * Analisa expressao logica e retorna seu tipo
     */
    private TipoSemantico analisarExpressaoLogica(SimpleNode no) {
        if (no == null) return TipoSemantico.INDEFINIDO;
        
        // Expressao logica: expressao operadorLogico expressao
        List<TipoSemantico> tiposExpressoes = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                tiposExpressoes.add(analisarExpressao(filho));
            }
        }
        
        if (tiposExpressoes.size() < 2) {
            return TipoSemantico.LOGICO; // Assume booleano
        }
        
        TipoSemantico tipo1 = tiposExpressoes.get(0);
        TipoSemantico tipo2 = tiposExpressoes.get(1);
        
        // Comparacoes numericas
        if (tipo1.isNumerico() && tipo2.isNumerico()) {
            return TipoSemantico.LOGICO;
        }
        
        // Comparacoes de igualdade
        if (tipo1 == tipo2) {
            return TipoSemantico.LOGICO;
        }
        
        // Tipos incompativeis
        if (tipo1 != TipoSemantico.INDEFINIDO && tipo2 != TipoSemantico.INDEFINIDO) {
            Token token = obterPrimeiroToken(no);
            int linha = token != null ? token.beginLine : 0;
            int coluna = token != null ? token.beginColumn : 0;
            adicionarErro(ErroSemantico.operacaoInvalida("comparacao", tipo1, tipo2, linha, coluna));
        }
        
        return TipoSemantico.INDEFINIDO;
    }
    
    /**
     * Analisa uma chamada de funcao
     */
    private void analisarChamadaFuncao(SimpleNode no, Simbolo funcao, int linha, int coluna) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando chamada de funcao: " + funcao.getNome());
        
        // Coleta tipos dos argumentos
        List<TipoSemantico> tiposArgumentos = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTLISTAEXPRESSOES) {
                tiposArgumentos = analisarListaExpressoes(filho);
                break;
            }
        }
        
        // Verifica numero de argumentos
        int numEsperado = funcao.getNumeroParametros();
        int numRecebido = tiposArgumentos.size();
        
        if (numEsperado != numRecebido) {
            adicionarErro(ErroSemantico.numeroArgumentosIncorreto(
                funcao.getNome(), numEsperado, numRecebido, linha, coluna));
            return;
        }
        
        // Verifica tipos dos argumentos
        List<Simbolo> parametros = funcao.getParametros();
        for (int i = 0; i < numEsperado; i++) {
            TipoSemantico tipoEsperado = parametros.get(i).getTipo();
            TipoSemantico tipoRecebido = tiposArgumentos.get(i);
            
            if (!TipoSemantico.saoCompativeis(tipoEsperado, tipoRecebido)) {
                adicionarErro(ErroSemantico.argumentoInvalido(
                    funcao.getNome(), i, tipoEsperado, tipoRecebido, linha, coluna));
            }
        }
        
        funcao.setUtilizado(true);
    }
    
    /**
     * Analisa operacao de lista
     */
    private void analisarOperacaoLista(SimpleNode no, Simbolo lista, int linha, int coluna) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando operacao de lista");
        
        TipoSemantico tipoBase = lista.getTipo().getTipoBase();
        
        // Verifica tipo da expressao para recrutar
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                TipoSemantico tipoExpr = analisarExpressao(filho);
                if (!TipoSemantico.saoCompativeis(tipoBase, tipoExpr)) {
                    adicionarErro(ErroSemantico.tipoIncompativel(
                        "elemento da lista", tipoBase, tipoExpr, linha, coluna));
                }
            }
        }
    }
    
    /**
     * Analisa operacao de pilha
     */
    private void analisarOperacaoPilha(SimpleNode no, Simbolo pilha, int linha, int coluna) {
        if (debug) System.out.println("[AnalisadorSemantico] Analisando operacao de pilha");
        
        TipoSemantico tipoBase = pilha.getTipo().getTipoBase();
        
        // Verifica tipo da expressao para montar-sanduba
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTEXPRESSAO) {
                TipoSemantico tipoExpr = analisarExpressao(filho);
                if (!TipoSemantico.saoCompativeis(tipoBase, tipoExpr)) {
                    adicionarErro(ErroSemantico.tipoIncompativel(
                        "elemento da pilha", tipoBase, tipoExpr, linha, coluna));
                }
            }
        }
    }
    
    /**
     * Analisa operacao de topo de pilha
     */
    private TipoSemantico analisarTopoOperacao(SimpleNode no) {
        String nomePilha = buscarIdentificadorNoNo(no);
        
        if (nomePilha == null) {
            return TipoSemantico.INDEFINIDO;
        }
        
        Simbolo simbolo = tabelaSimbolos.buscar(nomePilha);
        
        if (simbolo == null) {
            Token token = obterPrimeiroToken(no);
            int linha = token != null ? token.beginLine : 0;
            int coluna = token != null ? token.beginColumn : 0;
            adicionarErro(ErroSemantico.variavelNaoDeclarada(nomePilha, linha, coluna));
            return TipoSemantico.INDEFINIDO;
        }
        
        if (!simbolo.getTipo().isPilha()) {
            Token token = obterPrimeiroToken(no);
            int linha = token != null ? token.beginLine : 0;
            int coluna = token != null ? token.beginColumn : 0;
            adicionarErro(ErroSemantico.identificadorNaoEPilha(nomePilha, linha, coluna));
            return TipoSemantico.INDEFINIDO;
        }
        
        return simbolo.getTipo().getTipoBase();
    }
    
    /**
     * Analisa inicializacao de lista
     */
    private TipoSemantico analisarInicializacaoLista(SimpleNode no) {
        // Retorna tipo generico de lista
        return TipoSemantico.LISTA;
    }
    
    // ==================== METODOS UTILITARIOS ====================
    
    /**
     * Extrai o tipo de dado de um no tipoDado
     */
    private TipoSemantico extrairTipoDado(SimpleNode no) {
        if (no == null) return TipoSemantico.INDEFINIDO;
        
        Token token = obterPrimeiroToken(no);
        if (token == null) return TipoSemantico.INDEFINIDO;
        
        TipoSemantico tipoBase = TipoSemantico.fromToken(token.image);
        
        // Verifica se e lista ou pilha (precisa do segundo token)
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTTIPOLISTA) {
                return extrairTipoLista(filho);
            }
            if (filho.getId() == BrCompilerTreeConstants.JJTTIPOPILHA) {
                return extrairTipoPilha(filho);
            }
        }
        
        return tipoBase;
    }
    
    /**
     * Extrai o tipo de uma lista
     */
    private TipoSemantico extrairTipoLista(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        
        // Busca o segundo token (tipo base da lista)
        Token segundoToken = buscarSegundoToken(no);
        if (segundoToken != null) {
            TipoSemantico tipoBase = TipoSemantico.fromToken(segundoToken.image);
            return TipoSemantico.criarTipoLista(tipoBase);
        }
        
        return TipoSemantico.LISTA;
    }
    
    /**
     * Extrai o tipo de uma pilha
     */
    private TipoSemantico extrairTipoPilha(SimpleNode no) {
        Token segundoToken = buscarSegundoToken(no);
        if (segundoToken != null) {
            TipoSemantico tipoBase = TipoSemantico.fromToken(segundoToken.image);
            return TipoSemantico.criarTipoPilha(tipoBase);
        }
        
        return TipoSemantico.PILHA;
    }
    
    /**
     * Extrai parametros de uma lista de parametros
     */
    private List<Simbolo> extrairParametros(SimpleNode no) {
        List<Simbolo> parametros = new ArrayList<>();
        
        int numFilhos = no.jjtGetNumChildren();
        for (int i = 0; i < numFilhos; i++) {
            SimpleNode filho = (SimpleNode) no.jjtGetChild(i);
            if (filho.getId() == BrCompilerTreeConstants.JJTPARAMETRO) {
                Simbolo param = extrairParametro(filho);
                if (param != null) {
                    parametros.add(param);
                }
            }
        }
        
        return parametros;
    }
    
    /**
     * Extrai um parametro individual
     */
    private Simbolo extrairParametro(SimpleNode no) {
        TipoSemantico tipo = TipoSemantico.INDEFINIDO;
        String nome = null;
        
        Token token = obterPrimeiroToken(no);
        int linha = token != null ? token.beginLine : 0;
        int coluna = token != null ? token.beginColumn : 0;
        
        int numFilhos = no.jjtGetNumChildren();
        if (numFilhos > 0) {
            SimpleNode tipoNo = (SimpleNode) no.jjtGetChild(0);
            if (tipoNo.getId() == BrCompilerTreeConstants.JJTTIPODADO) {
                tipo = extrairTipoDado(tipoNo);
            }
        }
        
        nome = buscarIdentificadorNoNo(no);
        
        if (nome != null) {
            return new Simbolo(nome, tipo, "parametro", 0, linha, coluna);
        }
        
        return null;
    }
    
    /**
     * Extrai lista de identificadores de um no
     */
private List<String> extrairIdentificadores(SimpleNode no) {
    List<String> identificadores = new ArrayList<>();
    
    // Busca apenas no escopo do nó ListaIdentificadores
    Token token = obterPrimeiroToken(no);
    
    // Percorre apenas até encontrar algo que não seja identificador ou vírgula
    while (token != null) {
        if (token.kind == parser.BrCompilerConstants.IDENTIFICADOR) {
            identificadores.add(token.image);
        } else if (token.kind != parser.BrCompilerConstants.VIRGULA) {
            // Qualquer outro token indica fim da lista de identificadores
            break;
        }
        token = token.next;
    }
    
    return identificadores;
}
    
    /**
     * Busca um identificador nos tokens do no
     */
    private String buscarIdentificadorNoNo(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        while (token != null) {
            if (token.kind == parser.BrCompilerConstants.IDENTIFICADOR) {
                return token.image;
            }
            token = token.next;
        }
        return null;
    }
    
    /**
     * Obtem o primeiro token de um no usando reflexao
     */
    private Token obterPrimeiroToken(SimpleNode no) {
        try {
            java.lang.reflect.Field field = no.getClass().getField("jjtGetFirstToken");
            return (Token) field.get(no);
        } catch (Exception e) {
            // Alternativa: tenta acessar via metodo
            try {
                java.lang.reflect.Method method = no.getClass().getMethod("jjtGetFirstToken");
                return (Token) method.invoke(no);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    /**
     * Busca o segundo token do no
     */
    private Token buscarSegundoToken(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        if (token != null && token.next != null) {
            return token.next;
        }
        return null;
    }
    
    /**
     * Verifica se o no contem token de atribuicao (receba)
     */
    private boolean temTokenAtribuicao(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        while (token != null) {
            if (token.kind == parser.BrCompilerConstants.ATRIBUICAO) {
                return true;
            }
            token = token.next;
        }
        return false;
    }
    
    /**
     * Verifica se e uma chamada de funcao
     */
    private boolean temChamadaFuncao(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        while (token != null) {
            if (token.kind == parser.BrCompilerConstants.ABRIRFUNC) {
                return true;
            }
            token = token.next;
        }
        return false;
    }
    
    /**
     * Verifica se e uma operacao de lista
     */
    private boolean temOperacaoLista(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        while (token != null) {
            if (token.kind == parser.BrCompilerConstants.ADICIONAR ||
                token.kind == parser.BrCompilerConstants.REMOVER) {
                return true;
            }
            token = token.next;
        }
        return false;
    }
    
    /**
     * Verifica se e uma operacao de pilha
     */
    private boolean temOperacaoPilha(SimpleNode no) {
        Token token = obterPrimeiroToken(no);
        while (token != null) {
            if (token.kind == parser.BrCompilerConstants.EMPILHAR ||
                token.kind == parser.BrCompilerConstants.DESEMPILHAR) {
                return true;
            }
            token = token.next;
        }
        return false;
    }
    
    // ==================== GERENCIAMENTO DE ERROS ====================
    
    /**
     * Adiciona um erro semantico a lista
     */
    private void adicionarErro(ErroSemantico erro) {
        erros.add(erro);
        System.out.println("[AnalisadorSemantico] " + erro);
    }
    
    /**
     * Verifica se ha erros semanticos
     */
    public boolean temErros() {
        return !erros.isEmpty();
    }
    
    /**
     * Obtem lista de erros
     */
    public List<ErroSemantico> getErros() {
        return new ArrayList<>(erros);
    }
    
    /**
     * Obtem a tabela de simbolos
     */
    public TabelaSimbolos getTabelaSimbolos() {
        return tabelaSimbolos;
    }
    
    /**
     * Gera relatorio de erros semanticos
     */
    public String getRelatorioErros() {
        if (erros.isEmpty()) {
            return "Nenhum erro semantico encontrado.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("RELATORIO DE ERROS SEMANTICOS\n");
        sb.append("========================================\n");
        sb.append("Total de erros: ").append(erros.size()).append("\n\n");
        
        for (int i = 0; i < erros.size(); i++) {
            sb.append("ERRO #").append(i + 1).append(":\n");
            sb.append("  ").append(erros.get(i).toString()).append("\n");
            sb.append("  ").append(erros.get(i).getPosicaoTag()).append("\n\n");
        }
        
        sb.append("========================================\n");
        return sb.toString();
    }
    
    /**
     * Ativa/desativa modo debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
