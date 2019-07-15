package com.swap.views.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.swap.R;
import com.swap.utilities.EmojiExcludeFilter;
import com.swap.utilities.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpEmailActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonBack;
    Button buttonNext;
    EditText editTextEmail;
    String firstName;
    String lastName;
    String DOB;
    TextView textViewSingUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_email);
        findViewById();

    }

    private void findViewById() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("FirstName")) {
                firstName = extras.getString("FirstName");
                lastName = extras.getString("LastName");
                DOB = extras.getString("DOB");
            }
        }
        buttonBack = (Button) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(this);
        buttonNext = (Button) findViewById(R.id.buttonNext);
        buttonNext.setOnClickListener(this);
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        textViewSingUp = (TextView) findViewById(R.id.textViewSingUp);
        editTextEmail.setFilters(new InputFilter[]{new EmojiExcludeFilter(this)});
        Utils.spaceRemove(editTextEmail);
        editTextEmail_TextChangedListener();
        setFontOnViews();
    }

    private void setFontOnViews() {
        buttonBack.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        buttonNext.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        editTextEmail.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        textViewSingUp.setTypeface(Utils.setFont(this, "avenir-light.ttf"));

    }

    private void editTextEmail_TextChangedListener() {
        editTextEmail.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editTextEmail.getText().toString().matches("^0")) {
                    // Not allowed
                    Toast.makeText(getApplicationContext(), "0 is not allowed", Toast.LENGTH_LONG).show();
                    editTextEmail.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.buttonNext:
                validation();
                break;
        }
    }

    private void validation() {
        String email = editTextEmail.getText().toString().trim();
        if (email.isEmpty()) {
            editTextEmail.setError(getString(R.string.emailShouldNotBeEmpty));
        } else if (!isValidMail(email)||email.contains("-")) {
            editTextEmail.setError(getString(R.string.pleaseEnterValidEmail));
        } else {
            Intent signUpEmail = new Intent(SignUpEmailActivity.this, SignUpUsernameActivity.class);
            signUpEmail.putExtra("FirstName", firstName);
            signUpEmail.putExtra("LastName", lastName);
            signUpEmail.putExtra("DOB", DOB);
            signUpEmail.putExtra("Email", email);
            startActivity(signUpEmail);
        }
    }

    private boolean isValidMail(String email) {
        boolean check;
        Pattern p;
        Matcher m;
        String EMAIL_STRING = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        p = Pattern.compile(EMAIL_STRING);

        m = p.matcher(email);
        check = m.matches();

        /*if(!check) {
            txtEmail.setError("Not Valid Email");
        }*/
        return check;
    }
}
