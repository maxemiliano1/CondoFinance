package com.example.condofinance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LancamentosActivity extends AppCompatActivity {

    private static final Locale LOCALE_BR = new Locale("pt", "BR");
    private static final SimpleDateFormat FORMATO_DATA = new SimpleDateFormat("dd/MM/yyyy", LOCALE_BR);

    private DatabaseHelper bancoDados;

    private EditText editData;
    private EditText editValor;
    private AutoCompleteTextView editDescricao;
    private Button btnSalvar;
    private ListView listView;
    private RadioGroup groupTipo;

    // Quando for -1: modo inserir. Quando tiver ID: modo editar.
    private int idEmEdicao = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lancamentos);

        bancoDados = new DatabaseHelper(this);

        editData = findViewById(R.id.editData);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        btnSalvar = findViewById(R.id.btnSalvarLancamento);
        listView = findViewById(R.id.listaLancamentosRecentes);
        groupTipo = findViewById(R.id.groupTipo);

        configurarCampoData();
        atualizarLista();

        btnSalvar.setOnClickListener(v -> salvarOuAtualizar());
    }

    private void configurarCampoData() {
        Calendar calendario = Calendar.getInstance();
        editData.setText(FORMATO_DATA.format(calendario.getTime()));

        // Força o uso do DatePicker para não ter data digitada errada
        editData.setFocusable(false);
        editData.setClickable(true);

        editData.setOnClickListener(v -> {
            int ano = calendario.get(Calendar.YEAR);
            int mes = calendario.get(Calendar.MONTH);
            int dia = calendario.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    LancamentosActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        String dataFormatada = String.format(LOCALE_BR, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        editData.setText(dataFormatada);
                    },
                    ano, mes, dia
            );
            datePicker.show();
        });
    }

    private void salvarOuAtualizar() {
        String data = editData.getText().toString().trim();
        String descricao = editDescricao.getText().toString().trim();
        String valorString = editValor.getText().toString().trim();

        if (data.isEmpty() || descricao.isEmpty() || valorString.isEmpty()) {
            Toast.makeText(this, "Preencha data, descrição e valor.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aceita vírgula também (ex: 10,50)
        valorString = valorString.replace(",", ".");

        double valor;
        try {
            valor = Double.parseDouble(valorString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido. Ex: 10,50", Toast.LENGTH_SHORT).show();
            return;
        }

        if (valor <= 0) {
            Toast.makeText(this, "Informe um valor maior que zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        int tipo = (groupTipo.getCheckedRadioButtonId() == R.id.radioEntrada)
                ? Gasto.TIPO_RECEITA
                : Gasto.TIPO_DESPESA;

        boolean ok;

        if (idEmEdicao == -1) {
            ok = bancoDados.inserirGasto(data, descricao, valor, tipo);
        } else {
            ok = bancoDados.atualizarGasto(idEmEdicao, data, descricao, valor, tipo);
        }

        if (ok) {
            Toast.makeText(this, (idEmEdicao == -1) ? "Lançamento salvo." : "Lançamento atualizado.", Toast.LENGTH_SHORT).show();
            limparFormulario();
            atualizarLista();
        } else {
            Toast.makeText(this, "Não foi possível salvar. Tente novamente.", Toast.LENGTH_LONG).show();
        }
    }

    private void limparFormulario() {
        editDescricao.setText("");
        editValor.setText("");

        // volta para modo inserir
        idEmEdicao = -1;
        btnSalvar.setText("Salvar Lançamento");
        groupTipo.check(R.id.radioSaida);
    }

    private void atualizarLista() {
        List<Gasto> listaDeGastos = bancoDados.listarGastos();

        // Atualiza sugestões do AutoComplete (descrições já usadas)
        List<String> sugestoes = bancoDados.listarDescricoesUnicas();
        ArrayAdapter<String> adapterSugestoes =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sugestoes);
        editDescricao.setAdapter(adapterSugestoes);

        GastoAdapter adapter = new GastoAdapter(this, listaDeGastos, new GastoAdapter.OnAcaoClickListener() {

            @Override
            public void onEditarClick(Gasto gasto) {
                preencherFormularioParaEdicao(gasto);
            }

            @Override
            public void onExcluirClick(Gasto gasto) {
                confirmarExclusao(gasto);
            }
        });

        listView.setAdapter(adapter);
    }

    private void preencherFormularioParaEdicao(Gasto gasto) {
        editData.setText(gasto.getData());
        editDescricao.setText(gasto.getDescricao());
        editValor.setText(String.valueOf(gasto.getValor()));
        editValor.setText(String.format(LOCALE_BR, "%.2f", gasto.getValor()));

        if (gasto.getTipo() == Gasto.TIPO_RECEITA) {
            groupTipo.check(R.id.radioEntrada);
        } else {
            groupTipo.check(R.id.radioSaida);
        }

        idEmEdicao = gasto.getId();
        btnSalvar.setText("Atualizar Registro");
    }

    private void confirmarExclusao(Gasto gasto) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir lançamento")
                .setMessage("Deseja excluir: \"" + gasto.getDescricao() + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    boolean ok = bancoDados.excluirGasto(gasto.getId());
                    if (ok) {
                        Toast.makeText(this, "Registro excluído.", Toast.LENGTH_SHORT).show();
                        atualizarLista();
                    } else {
                        Toast.makeText(this, "Não foi possível excluir.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}