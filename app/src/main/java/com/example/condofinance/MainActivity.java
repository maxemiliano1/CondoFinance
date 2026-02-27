package com.example.condofinance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

// Tela inicial do aplicativo.
// Serve como menu para acessar lançamentos e relatórios.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnIrLancamentos = findViewById(R.id.btnIrLancamentos);
        Button btnIrRelatorios = findViewById(R.id.btnIrRelatorios);

        btnIrLancamentos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LancamentosActivity.class))
        );

        btnIrRelatorios.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RelatoriosActivity.class))
        );
    }
}