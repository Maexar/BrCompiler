package recovery; // Altere para 'compiler' se necessário

import parser.BrCompilerConstants;

public class Follow {

    // --- Declaração dos conjuntos (RecoverySets) ---
    
    static public final RecoverySet main = new RecoverySet();
    static public final RecoverySet bloco = new RecoverySet();
    static public final RecoverySet comando = new RecoverySet();
    
    // Alias para conjuntos que são iguais a FOLLOW(comando)
    static public final RecoverySet declaraFuncao = comando;
    static public final RecoverySet declaraVariavel = comando;
    static public final RecoverySet expressaoCondicional = comando;
    static public final RecoverySet comandoIdentificador = comando;
    static public final RecoverySet print = comando;
    static public final RecoverySet scan = comando;
    static public final RecoverySet whileLoop = comando;
    static public final RecoverySet forLoop = comando;
    static public final RecoverySet retorno = comando;
    static public final RecoverySet comandoIdentificadorSufixo = comando;
    static public final RecoverySet sufixoExpressao = comando;

    // Conjuntos com uniões ou específicos
    static public final RecoverySet condicao = new RecoverySet();
    static public final RecoverySet atribuicaoFor = new RecoverySet();
    static public final RecoverySet listaParametros = new RecoverySet();
    static public final RecoverySet parametro = new RecoverySet();
    static public final RecoverySet tipoDado = new RecoverySet();
    
    // Alias para tipos
    static public final RecoverySet tipoLista = tipoDado;
    static public final RecoverySet tipoPilha = tipoDado;
    
    static public final RecoverySet listaIdentificadores = new RecoverySet();
    static public final RecoverySet expressao = new RecoverySet();
    static public final RecoverySet listaExpressoes = new RecoverySet();
    static public final RecoverySet fator = new RecoverySet();
    static public final RecoverySet termo = new RecoverySet();
    
    // Alias para termos
    static public final RecoverySet termoIdentificadorSufixo = termo;
    static public final RecoverySet inicializacaoLista = termo;


    // --- Inicialização dos conjuntos ---
    static {
        // FOLLOW(main) = { <EOF> }
        main.add(Integer.valueOf(BrCompilerConstants.EOF));

        // FOLLOW(bloco) = { <FECHABLOCO> }
        bloco.add(Integer.valueOf(BrCompilerConstants.FECHABLOCO));

        // FOLLOW(comando)
        comando.add(Integer.valueOf(BrCompilerConstants.FECHABLOCO));
        comando.add(Integer.valueOf(BrCompilerConstants.LISTA));
        comando.add(Integer.valueOf(BrCompilerConstants.PILHA));
        comando.add(Integer.valueOf(BrCompilerConstants.INT));
        comando.add(Integer.valueOf(BrCompilerConstants.FLOAT));
        comando.add(Integer.valueOf(BrCompilerConstants.STRING));
        comando.add(Integer.valueOf(BrCompilerConstants.BOOL));
        comando.add(Integer.valueOf(BrCompilerConstants.CONDICIONAL));
        comando.add(Integer.valueOf(BrCompilerConstants.IDENTIFICADOR));
        comando.add(Integer.valueOf(BrCompilerConstants.PRINT));
        comando.add(Integer.valueOf(BrCompilerConstants.SCAN));
        comando.add(Integer.valueOf(BrCompilerConstants.WHILE));
        comando.add(Integer.valueOf(BrCompilerConstants.FOR));
        comando.add(Integer.valueOf(BrCompilerConstants.RETURN));
        comando.add(Integer.valueOf(BrCompilerConstants.FUNCAO));

        // FOLLOW(condicao) = { <SENAO> } U FOLLOW(comando)
        condicao.add(Integer.valueOf(BrCompilerConstants.SENAO));
        condicao.union(comando);

        // FOLLOW(atribuicaoFor) = { <SEPARADOR>, <FECHAREXP> }
        atribuicaoFor.add(Integer.valueOf(BrCompilerConstants.SEPARADOR));
        atribuicaoFor.add(Integer.valueOf(BrCompilerConstants.FECHAREXP));

        // FOLLOW(listaParametros) = { <FECHARFUNC> }
        listaParametros.add(Integer.valueOf(BrCompilerConstants.FECHARFUNC));

        // FOLLOW(parametro) = { <VIRGULA>, <FECHARFUNC> }
        parametro.add(Integer.valueOf(BrCompilerConstants.VIRGULA));
        parametro.add(Integer.valueOf(BrCompilerConstants.FECHARFUNC));

        // FOLLOW(tipoDado) = { <IDENTIFICADOR> }
        tipoDado.add(Integer.valueOf(BrCompilerConstants.IDENTIFICADOR));

        // FOLLOW(ListaIdentificadores) = { <ATRIBUICAO>, <FIMESTRUTURA> }
        listaIdentificadores.add(Integer.valueOf(BrCompilerConstants.ATRIBUICAO));
        listaIdentificadores.add(Integer.valueOf(BrCompilerConstants.FIMESTRUTURA));

        // FOLLOW(Expressao)
        expressao.add(Integer.valueOf(BrCompilerConstants.FIMESTRUTURA));
        expressao.add(Integer.valueOf(BrCompilerConstants.FECHAREXP));
        expressao.add(Integer.valueOf(BrCompilerConstants.FECHARFUNC));
        expressao.add(Integer.valueOf(BrCompilerConstants.VIRGULA));
        expressao.add(Integer.valueOf(BrCompilerConstants.SEPARADOR));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPDIF));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPIGUAL));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPMAIOR));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPMENOR));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPMAIORIGUAL));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPMENORIGUAL));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPAND));
        expressao.add(Integer.valueOf(BrCompilerConstants.OPOR));
        
        // FOLLOW(ListaExpressoes) = { <FECHARFUNC>, <FECHAREXP>, <FIMESTRUTURA> }
        listaExpressoes.add(Integer.valueOf(BrCompilerConstants.FECHARFUNC));
        listaExpressoes.add(Integer.valueOf(BrCompilerConstants.FECHAREXP));
        listaExpressoes.add(Integer.valueOf(BrCompilerConstants.FIMESTRUTURA));

        // FOLLOW(fator) = { <SOMA>, <SUBTRACAO> } U FOLLOW(Expressao)
        fator.add(Integer.valueOf(BrCompilerConstants.SOMA));
        fator.add(Integer.valueOf(BrCompilerConstants.SUBTRACAO));
        fator.union(expressao);

        // FOLLOW(termo) = { <MULTIPLICACAO>, <DIVISAO> } U FOLLOW(fator)
        termo.add(Integer.valueOf(BrCompilerConstants.MULTIPLICACAO));
        termo.add(Integer.valueOf(BrCompilerConstants.DIVISAO));
        termo.union(fator);
    }
}