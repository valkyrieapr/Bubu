package com.example.brigitta.bub;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    Button button;
    EditText number;
    String name;
    Intent studentIntent;
    String studentID;
    TextView namebox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.scanButton);
        number = (EditText)findViewById(R.id.studentID);
        namebox = (TextView) findViewById(R.id.studentName);

        number.addTextChangedListener(scanTextWatcher);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studentID = number.getText().toString();
                studentIntent = new Intent(getApplicationContext(), PaperScanner.class);
                studentIntent.putExtra(Configuration.STD_ID, studentID);
                startActivity(studentIntent);
            }
        });
    }

    private TextWatcher scanTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            studentID = number.getText().toString();
            studentIntent = new Intent(getApplicationContext(), PaperScanner.class);
            studentIntent.putExtra(Configuration.STD_ID, studentID);
            getStudent();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void getStudent(){
        class GetStudent extends AsyncTask <Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                showStudent(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandler rh = new RequestHandler();
                String s = rh.sendGetRequestParam(Configuration.URL_GET_STD, studentID);
                return s;
            }
        }
        GetStudent ge = new GetStudent();
        ge.execute();
    }

    private void showStudent(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray result = jsonObject.getJSONArray(Configuration.TAG_JSON_ARRAY);
            JSONObject c = result.getJSONObject(0);
            name = c.getString(Configuration.TAG_NAME);

            if (name == "null") {
                button.setEnabled(false);
                namebox.setText("");
            } else {
                button.setEnabled(true);
                namebox.setText(name);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}