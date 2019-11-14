package com.example.poolcar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mConductor, mPasajero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConductor = (Button) findViewById(R.id.conductor);
        mPasajero = (Button) findViewById(R.id.pasajero);

        mConductor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (MainActivity.this, ConductorLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mPasajero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (MainActivity.this, PasajeroLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}
