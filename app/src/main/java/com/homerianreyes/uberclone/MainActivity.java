package com.homerianreyes.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.homerianreyes.uberclone.databinding.ActivityMainBinding;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.shashank.sony.fancytoastlib.FancyToast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //for guest users
    @Override
    public void onClick(View view) {

        if (binding.edtGuest.getText().toString() == null) {
            FancyToast.makeText(this, "Are you a Driver or a Passenger?", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
        }

        if (binding.edtGuest.getText().toString().equals("Driver") || binding.edtGuest.getText().toString().equals("Passenger")) {

            if (ParseUser.getCurrentUser() == null) {

                ParseAnonymousUtils.logIn(new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {

                        if (user != null && e == null) {
                            FancyToast.makeText(MainActivity.this, "Welcome Anonymous", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS,false).show();

                            user.put("userType", binding.edtGuest.getText().toString());
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {

                                    if (e == null) {

                                        transitionToPassengerActivity();
                                        transitionToDriverRequestListActivity();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        } else {
            FancyToast.makeText(this, "Error, check the spelling of your input", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
        }
    }

    public void rootTappedLayout(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        try {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    enum State {
        SIGNUP, LOGIN
    }

    private State state;
    private ActivityMainBinding binding;
    private View viewRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setup data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        viewRoot = binding.getRoot();
        setContentView(viewRoot);

        //if user is still logged in
        ParseInstallation.getCurrentInstallation().saveInBackground();
        if (ParseUser.getCurrentUser() != null) {
            transitionToPassengerActivity();
            transitionToDriverRequestListActivity();
        }

        //initialize
        state = State.SIGNUP;
        binding.btnOneTimeLogin.setOnClickListener(this);

        binding.btnSignUpAndLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (state == State.SIGNUP) {

                    if (binding.rdbDriver.isChecked() == false && binding.rdbPassenger.isChecked() == false) {
                        FancyToast.makeText(MainActivity.this, "Are you a driver or a passenger?", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                        return;
                    }

                    ParseUser appUser = new ParseUser();
                    appUser.setUsername(binding.edtUsername.getText().toString());
                    appUser.setPassword(binding.edtPassword.getText().toString());

                    if (binding.rdbDriver.isChecked()) {
                        appUser.put("userType", "Driver");
                    } else if (binding.rdbPassenger.isChecked()) {
                        appUser.put("userType", "Passenger");
                    }

                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setMessage("signing up... Please wait");
                    progressDialog.show();
                    appUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {

                            if (e == null) {
                                FancyToast.makeText(MainActivity.this, "Signed Up!", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS,false).show();
                                transitionToPassengerActivity();
                                transitionToDriverRequestListActivity();
                            }
                            binding.edtUsername.getText().clear();
                            binding.edtPassword.getText().clear();
                            if (binding.rdbDriver.isChecked()) {
                                binding.rdbDriver.setChecked(false);
                            } else if (binding.rdbPassenger.isChecked()) {
                                binding.rdbPassenger.setChecked(false);
                            }
                            progressDialog.dismiss();
                        }
                    });
                } else if (state == State.LOGIN) {

                    if (binding.edtUsername.getText().toString().equals("") || binding.edtPassword.getText().toString().equals("")) {
                        FancyToast.makeText(MainActivity.this, "Enter username and password", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        return;
                    }

                        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Logging in... Please wait");
                        progressDialog.show();
                        ParseUser.logInInBackground(binding.edtUsername.getText().toString(), binding.edtPassword.getText().toString(), new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {

                                if (user != null && e == null) {
                                    transitionToPassengerActivity();
                                    FancyToast.makeText(MainActivity.this, "User Logged In!", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                    transitionToPassengerActivity();
                                    transitionToDriverRequestListActivity();
                                }
                                binding.edtUsername.getText().clear();
                                binding.edtPassword.getText().clear();
                                progressDialog.dismiss();
                            }
                        });

                }//else if
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_signup_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.loginItem:

                if (state == State.SIGNUP) {

                    state = State.LOGIN;
                    item.setTitle("Sign Up");
                    binding.btnSignUpAndLogin.setText("Log in");
                    binding.radioBtnGrp.setVisibility(View.INVISIBLE);

                    binding.edtUsername.getText().clear();
                    binding.edtPassword.getText().clear();
                    if (binding.rdbDriver.isChecked()) {
                        binding.rdbDriver.setChecked(false);
                    } else if (binding.rdbPassenger.isChecked()) {
                        binding.rdbPassenger.setChecked(false);
                    }
                } else if (state == State.LOGIN) {

                    state = State.SIGNUP;
                    item.setTitle("Log in");
                    binding.btnSignUpAndLogin.setText("Sign Up");
                    binding.radioBtnGrp.setVisibility(View.VISIBLE);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void transitionToPassengerActivity(){
        //if user is logged in
        if (ParseUser.getCurrentUser() != null) {
            //if user is a passenger
            if (ParseUser.getCurrentUser().get("userType").equals("Passenger")) {

                Intent intent = new Intent(MainActivity.this, PassengerActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void transitionToDriverRequestListActivity() {
        //if user is logged in
        if (ParseUser.getCurrentUser() != null) {
            //if user is a driver
            if (ParseUser.getCurrentUser().get("userType").equals("Driver")) {

                Intent intent = new Intent(this, DriverRequestListActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

}