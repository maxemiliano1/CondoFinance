package com.example.condofinance;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Tela de relatórios: filtra por mês/ano, calcula totais e permite exportar em PDF.
public class RelatoriosActivity extends AppCompatActivity {

    private static final Locale LOCALE_BR = new Locale("pt", "BR");

    private static final int COR_VERDE = Color.parseColor("#2E7D32");
    private static final int COR_VERMELHO = Color.parseColor("#D32F2F");
    private static final int COR_AZUL = Color.parseColor("#1A237E");

    private DatabaseHelper bancoDados;

    private Spinner spinMes, spinAno;
    private Button btnFiltrar, btnGerarPdf;
    private TextView txtTotalEntradas, txtTotalSaidas, txtTotalSaldo;
    private ListView listaReceitas, listaDespesas;

    // Dados usados também na hora de gerar o PDF
    private final List<Gasto> receitas = new ArrayList<>();
    private final List<Gasto> despesas = new ArrayList<>();
    private double totalReceitas = 0;
    private double totalDespesas = 0;
    private double saldo = 0;
    private String mesAnoAtual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorios);

        bancoDados = new DatabaseHelper(this);

        spinMes = findViewById(R.id.spinMes);
        spinAno = findViewById(R.id.spinAno);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGerarPdf = findViewById(R.id.btnGerarPdf);

        txtTotalEntradas = findViewById(R.id.txtTotalEntradas);
        txtTotalSaidas = findViewById(R.id.txtTotalSaidas);
        txtTotalSaldo = findViewById(R.id.txtTotalSaldo);

        listaReceitas = findViewById(R.id.listaReceitas);
        listaDespesas = findViewById(R.id.listaDespesas);

        configurarFiltros();

        btnFiltrar.setOnClickListener(v -> aplicarFiltro());
        btnGerarPdf.setOnClickListener(v -> gerarPdfECompartilhar());

        // Já abre com o mês/ano atual carregado
        btnFiltrar.performClick();
    }

    private void configurarFiltros() {
        String[] meses = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        ArrayAdapter<String> adapterMes = new ArrayAdapter<>(this, R.layout.item_spinner, meses);
        adapterMes.setDropDownViewResource(R.layout.item_spinner);
        spinMes.setAdapter(adapterMes);

        // faixa de anos (pega ano atual e mais alguns)
        int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
        String[] anos = new String[10];
        for (int i = 0; i < anos.length; i++) {
            anos[i] = String.valueOf(anoAtual + i);
        }

        ArrayAdapter<String> adapterAno = new ArrayAdapter<>(this, R.layout.item_spinner, anos);
        adapterAno.setDropDownViewResource(R.layout.item_spinner);
        spinAno.setAdapter(adapterAno);

        Calendar cal = Calendar.getInstance();
        spinMes.setSelection(cal.get(Calendar.MONTH)); // 0..11

        // seleciona o ano atual no spinner
        String anoAtualStr = String.valueOf(anoAtual);
        for (int i = 0; i < anos.length; i++) {
            if (anos[i].equals(anoAtualStr)) {
                spinAno.setSelection(i);
                break;
            }
        }
    }

    private void aplicarFiltro() {
        int mesSelecionado = spinMes.getSelectedItemPosition() + 1; // 1..12
        String mesFormatado = String.format(LOCALE_BR, "%02d", mesSelecionado);
        String anoSelecionado = spinAno.getSelectedItem().toString();

        mesAnoAtual = mesFormatado + "/" + anoSelecionado;

        carregarDadosDoMes(mesFormatado, anoSelecionado);
        atualizarTotaisNaTela();
        atualizarListas();
    }

    private void carregarDadosDoMes(String mes, String ano) {
        // Pega direto do banco filtrado (bem melhor do que puxar tudo e filtrar na mão)
        List<Gasto> doMes = bancoDados.listarPorMes(mes, ano);

        receitas.clear();
        despesas.clear();
        totalReceitas = 0;
        totalDespesas = 0;

        for (Gasto g : doMes) {
            if (g.getTipo() == Gasto.TIPO_RECEITA) {
                receitas.add(g);
                totalReceitas += g.getValor();
            } else {
                despesas.add(g);
                totalDespesas += g.getValor();
            }
        }

        saldo = totalReceitas - totalDespesas;
    }

    private void atualizarTotaisNaTela() {
        txtTotalEntradas.setText(String.format(LOCALE_BR, "+ R$ %,.2f", totalReceitas));
        txtTotalSaidas.setText(String.format(LOCALE_BR, "- R$ %,.2f", totalDespesas));
        txtTotalSaldo.setText(String.format(LOCALE_BR, "R$ %,.2f", saldo));
        txtTotalSaldo.setTextColor(saldo >= 0 ? COR_VERDE : COR_VERMELHO);
    }

    private void atualizarListas() {
        // Na tela de relatório é só visualização.
        GastoAdapter.OnAcaoClickListener soAviso = new GastoAdapter.OnAcaoClickListener() {
            @Override
            public void onEditarClick(Gasto g) {
                Toast.makeText(RelatoriosActivity.this, "Edite na tela de lançamentos.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onExcluirClick(Gasto g) {
                Toast.makeText(RelatoriosActivity.this, "Exclua na tela de lançamentos.", Toast.LENGTH_SHORT).show();
            }
        };

        listaReceitas.setAdapter(new GastoAdapter(this, receitas, soAviso));
        listaDespesas.setAdapter(new GastoAdapter(this, despesas, soAviso));
    }

    private void gerarPdfECompartilhar() {
        if (mesAnoAtual.isEmpty()) {
            Toast.makeText(this, "Selecione mês e ano primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();

        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            int x = 40;
            int y = 50;

            // Título
            paint.setTextSize(20f);
            paint.setFakeBoldText(true);
            paint.setColor(COR_AZUL);
            canvas.drawText("Relatório Financeiro - Condomínio Nena", x, y, paint);

            // Referência
            y += 30;
            paint.setTextSize(14f);
            paint.setFakeBoldText(false);
            paint.setColor(Color.BLACK);
            canvas.drawText("Referência: " + mesAnoAtual, x, y, paint);

            // Totais
            y += 40;
            paint.setTextSize(16f);
            paint.setColor(COR_VERDE);
            canvas.drawText(String.format(LOCALE_BR, "Entradas: + R$ %,.2f", totalReceitas), x, y, paint);

            y += 22;
            paint.setColor(COR_VERMELHO);
            canvas.drawText(String.format(LOCALE_BR, "Saídas: - R$ %,.2f", totalDespesas), x, y, paint);

            y += 28;
            paint.setTextSize(18f);
            paint.setColor(saldo >= 0 ? COR_VERDE : COR_VERMELHO);
            canvas.drawText(String.format(LOCALE_BR, "Saldo final: R$ %,.2f", saldo), x, y, paint);

            // Receitas
            y += 40;
            y = desenharLista(canvas, paint, "RECEITAS", receitas, COR_VERDE, x, y);

            // Despesas
            y += 25;
            y = desenharLista(canvas, paint, "DESPESAS", despesas, COR_VERMELHO, x, y);

            document.finishPage(page);

            // Salva em cache interno (não precisa pedir permissão)
            File pastaCache = new File(getCacheDir(), "pdfs");
            if (!pastaCache.exists()) pastaCache.mkdirs();

            String nomeArquivo = "Relatorio_" + mesAnoAtual.replace("/", "_") + ".pdf";
            File arquivoPdf = new File(pastaCache, nomeArquivo);

            document.writeTo(new FileOutputStream(arquivoPdf));

            // Compartilha usando FileProvider
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    arquivoPdf
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Compartilhar relatório"));

        } catch (IOException e) {
            Toast.makeText(this, "Não foi possível gerar o PDF.", Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }

    private int desenharLista(Canvas canvas, Paint paint, String titulo, List<Gasto> lista, int corTitulo, int x, int y) {
        paint.setFakeBoldText(true);
        paint.setColor(corTitulo);
        paint.setTextSize(14f);
        canvas.drawText(titulo + ":", x, y, paint);

        y += 18;
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        // Limite simples pra não estourar a página
        int limiteY = 820;

        if (lista.isEmpty()) {
            canvas.drawText("Nenhum registro.", x + 10, y, paint);
            return y + 18;
        }

        for (Gasto g : lista) {
            String linha = String.format(
                    LOCALE_BR,
                    "%s | %s | R$ %,.2f",
                    g.getData(),
                    g.getDescricao(),
                    g.getValor()
            );

            canvas.drawText(linha, x + 10, y, paint);
            y += 16;

            if (y >= limiteY) {
                canvas.drawText("...lista continua (muitos registros para 1 página)...", x + 10, y, paint);
                break;
            }
        }

        return y;
    }
}