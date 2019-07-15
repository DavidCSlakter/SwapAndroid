package com.swap.views.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.swap.R;
import com.swap.utilities.EmojiExcludeFilter;
import com.swap.utilities.Utils;

public class SignUpNameActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonBack;
    Button buttonNext;
    EditText editTextFirstName;
    EditText editTextLastName;
    TextView textViewWhatYourName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_name);
        findViewById();
    }

    private void findViewById() {
        buttonBack = (Button) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(this);
        buttonNext = (Button) findViewById(R.id.buttonNext);
        buttonNext.setOnClickListener(this);
        editTextFirstName = (EditText) findViewById(R.id.editTextFirstName);

        editTextFirstName.setFilters(new InputFilter[]{new EmojiExcludeFilter(this)});
        editTextFirstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String result = editable.toString().replaceAll(" ", "");
                if (!editable.toString().equals(result)) {
                    editTextFirstName.setText(result);
                    editTextFirstName.setSelection(result.length());
                    // alert the user
                }
            }
        });
        editTextLastName = (EditText) findViewById(R.id.editTextLastName);
        textViewWhatYourName = (TextView) findViewById(R.id.textViewWhatYourName);
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isWhitespace(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        editTextLastName.setFilters(new InputFilter[]{filter});
        editTextLastName.setFilters(new InputFilter[]{new EmojiExcludeFilter(this)});
        setFontOnViews();
        firstLetterCaps(editTextFirstName);
        firstLetterCaps(editTextLastName);
    }

    private void setFontOnViews() {
        buttonBack.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        buttonNext.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        editTextFirstName.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        editTextLastName.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
        textViewWhatYourName.setTypeface(Utils.setFont(this, "avenir-light.ttf"));
    }

    private void firstLetterCaps(EditText editText) {
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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
        if (Utils.isNetworkConnected(SignUpNameActivity.this)) {
            String firstName = editTextFirstName.getText().toString().trim();
            String lastName = editTextLastName.getText().toString().trim();
            if (firstName.isEmpty()) {
                editTextFirstName.setError(getString(R.string.firstNameShouldNotBeEmpty));
            } else if (lastName.isEmpty()) {
                editTextLastName.setError(getString(R.string.lastNameShouldNotBeEmpty));
            } else {
                Intent signUpName = new Intent(SignUpNameActivity.this, SignUpDOBActivity.class);
                signUpName.putExtra("FirstName", firstName);
                signUpName.putExtra("LastName", lastName);
                startActivity(signUpName);
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();

        }
    }

}