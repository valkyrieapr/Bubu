package com.example.brigitta.bub;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button button;
    EditText number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.scanButton);
        number = (EditText)findViewById(R.id.studentID);

        number.addTextChangedListener(scanTextWatcher);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    int StudentID = Integer.parseInt(number.getText().toString());
                    Intent studentIntent = new Intent(getApplicationContext(), PaperScanner.class);
                    studentIntent.putExtra("StudentID", StudentID);
                    startActivity(studentIntent);

                //startActivity(new Intent(MainActivity.this, PaperScanner.class));
            }
        });
    }

    private TextWatcher scanTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String StudentID = number.getText().toString();

            if (StudentID.length() == 6) {
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };
}
