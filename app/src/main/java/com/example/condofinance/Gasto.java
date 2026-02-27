package com.example.condofinance;

// Classe simples para representar um lançamento (despesa ou receita).
public class Gasto {

    // 0 = despesa | 1 = receita
    public static final int TIPO_DESPESA = 0;
    public static final int TIPO_RECEITA = 1;

    private final int id;
    private final String descricao;
    private final String data;   // formato esperado: dd/MM/yyyy
    private final double valor;
    private final int tipo;

    public Gasto(int id, String descricao, double valor, String data, int tipo) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getValor() {
        return valor;
    }

    public String getData() {
        return data;
    }

    public int getTipo() {
        return tipo;
    }

    // Ajuda na hora de exibir no app/relatório
    public boolean isDespesa() {
        return tipo == TIPO_DESPESA;
    }
}