package com.example.condofinance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Classe responsável por criar e gerenciar o banco de dados do app.
// Aqui ficam as operações básicas: inserir, listar, atualizar e excluir gastos.
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Condominio.db";
    private static final int DATABASE_VERSION = 2;

    // Tabela e colunas
    private static final String TABLE_GASTOS = "gastos";
    private static final String COL_ID = "id";
    private static final String COL_DATA = "data";
    private static final String COL_DESCRICAO = "descricao";
    private static final String COL_VALOR = "valor";
    private static final String COL_TIPO = "tipo"; // 0 = despesa | 1 = receita

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cria a tabela de gastos quando o banco é criado pela primeira vez.
        String sqlCreate =
                "CREATE TABLE " + TABLE_GASTOS + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_DATA + " TEXT, " +
                        COL_DESCRICAO + " TEXT, " +
                        COL_VALOR + " REAL, " +
                        COL_TIPO + " INTEGER" +
                        ")";
        db.execSQL(sqlCreate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Se a versão do banco mudar, apaga a tabela antiga e cria novamente.
        // (Solução simples usada no trabalho acadêmico)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GASTOS);
        onCreate(db);
    }

    // Insere um novo gasto no banco.
    // Retorna true se salvou corretamente.
    public boolean inserirGasto(String data, String descricao, double valor, int tipo) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues valores = new ContentValues();
            valores.put(COL_DATA, data);
            valores.put(COL_DESCRICAO, descricao);
            valores.put(COL_VALOR, valor);
            valores.put(COL_TIPO, tipo);

            long resultado = db.insert(TABLE_GASTOS, null, valores);
            return resultado != -1;
        } finally {
            db.close();
        }
    }

    // Exclui um gasto pelo ID.
    public boolean excluirGasto(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int linhasAfetadas = db.delete(TABLE_GASTOS, COL_ID + " = ?", new String[]{String.valueOf(id)});
            return linhasAfetadas > 0;
        } finally {
            db.close();
        }
    }

    // Atualiza um gasto pelo ID.
    public boolean atualizarGasto(int id, String data, String descricao, double valor, int tipo) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues valores = new ContentValues();
            valores.put(COL_DATA, data);
            valores.put(COL_DESCRICAO, descricao);
            valores.put(COL_VALOR, valor);
            valores.put(COL_TIPO, tipo);

            int linhasAfetadas = db.update(TABLE_GASTOS, valores, COL_ID + " = ?", new String[]{String.valueOf(id)});
            return linhasAfetadas > 0;
        } finally {
            db.close();
        }
    }

    // Lista todos os gastos (mais recentes primeiro).
    public List<Gasto> listarGastos() {
        List<Gasto> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_GASTOS + " ORDER BY " + COL_ID + " DESC", null);

            while (cursor.moveToNext()) {
                lista.add(criarGastoDoCursor(cursor));
            }

            return lista;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Lista os gastos filtrando por mês e ano (usando o campo data no formato dd/MM/yyyy).
    public List<Gasto> listarPorMes(String mes, String ano) {
        List<Gasto> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String argumentoBusca = "%/" + mes + "/" + ano;
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_GASTOS + " WHERE " + COL_DATA + " LIKE ? ORDER BY " + COL_DATA + " ASC",
                    new String[]{argumentoBusca}
            );

            while (cursor.moveToNext()) {
                lista.add(criarGastoDoCursor(cursor));
            }

            return lista;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Retorna apenas descrições sem repetir, para usar no AutoComplete.
    public List<String> listarDescricoesUnicas() {
        List<String> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT DISTINCT " + COL_DESCRICAO + " FROM " + TABLE_GASTOS + " ORDER BY " + COL_DESCRICAO + " ASC",
                    null
            );

            while (cursor.moveToNext()) {
                lista.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRICAO)));
            }

            return lista;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Converte uma linha do Cursor em um objeto Gasto.
    private Gasto criarGastoDoCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
        String data = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA));
        String descricao = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRICAO));
        double valor = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_VALOR));
        int tipo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TIPO));

        return new Gasto(id, descricao, valor, data, tipo);
    }
}