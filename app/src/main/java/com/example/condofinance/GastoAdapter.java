package com.example.condofinance;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

// Adapter que mostra os gastos na ListView (uma linha do item_gasto.xml por registro).
public class GastoAdapter extends ArrayAdapter<Gasto> {

    public interface OnAcaoClickListener {
        void onEditarClick(Gasto gasto);

        void onExcluirClick(Gasto gasto);
    }

    private static final Locale LOCALE_BR = new Locale("pt", "BR");

    // Cores simples (verde = receita | vermelho = despesa)
    private static final int COR_RECEITA = Color.parseColor("#2E7D32");
    private static final int COR_DESPESA = Color.parseColor("#D32F2F");

    private final OnAcaoClickListener listener;

    public GastoAdapter(Context context, List<Gasto> gastos, OnAcaoClickListener listener) {
        super(context, R.layout.item_gasto, gastos);
        this.listener = listener;
    }

    // ViewHolder para evitar ficar dando findViewById toda hora
    private static class ViewHolder {
        TextView txtTipoLabel;
        TextView txtDescricao;
        TextView txtData;
        TextView txtValor;
        ImageView btnEditar;
        ImageView btnExcluir;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gasto, parent, false);

            h = new ViewHolder();
            h.txtTipoLabel = convertView.findViewById(R.id.txtItemTipoLabel);
            h.txtDescricao = convertView.findViewById(R.id.txtItemDescricao);
            h.txtData = convertView.findViewById(R.id.txtItemData);
            h.txtValor = convertView.findViewById(R.id.txtItemValor);
            h.btnEditar = convertView.findViewById(R.id.btnEditar);
            h.btnExcluir = convertView.findViewById(R.id.btnExcluir);

            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Gasto gasto = getItem(position);
        if (gasto == null) return convertView;

        h.txtDescricao.setText(gasto.getDescricao());
        h.txtData.setText(gasto.getData());

        boolean isReceita = gasto.getTipo() == Gasto.TIPO_RECEITA;

        if (h.txtTipoLabel != null) {
            h.txtTipoLabel.setText(isReceita ? "RECEITA" : "DESPESA");
            h.txtTipoLabel.setTextColor(isReceita ? COR_RECEITA : COR_DESPESA);
        }

        h.txtValor.setTextColor(isReceita ? COR_RECEITA : COR_DESPESA);
        h.txtValor.setText(String.format(
                LOCALE_BR,
                "%s R$ %,.2f",
                isReceita ? "+" : "-",
                gasto.getValor()
        ));

        if (h.btnEditar != null) {
            h.btnEditar.setOnClickListener(v -> listener.onEditarClick(gasto));
        }

        if (h.btnExcluir != null) {
            h.btnExcluir.setOnClickListener(v -> listener.onExcluirClick(gasto));
        }

        return convertView;
    }
}