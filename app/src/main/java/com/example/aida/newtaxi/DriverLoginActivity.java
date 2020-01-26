package com.example.aida.newtaxi;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText nMail, nPassword;
    private Button nLogin, nRegistation;

    private FirebaseAuth nAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener; //listener when off state changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

       nAuth = FirebaseAuth.getInstance(); //gets instance of firebase of current state

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() { //checking for user status
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //whenever we login this will be called and we can move on the next page
                if(user!=null){
                   Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                   return;
                }
            }
        };

        nMail = findViewById(R.id.email);
        nPassword = findViewById(R.id.password);
        nLogin = findViewById(R.id.login);
        nRegistation = findViewById(R.id.registration);

        nRegistation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                final String email = nMail.getText().toString();
                final String password = nPassword.getText().toString();
                nAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(DriverLoginActivity.this, "Kayıt Hatası", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String user_id = nAuth.getCurrentUser().getProviderId(); //get user id
                            DatabaseReference current_user_db= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("name"); //add this user id in firebase child users child drivers
                            current_user_db.setValue(email); //if we dont change nothing will show just to make sure its saved

                        }
                    }
                });
            }
        });

        nLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = nMail.getText().toString();
                final String password = nPassword.getText().toString();
                nAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(DriverLoginActivity.this, "Giriş Hatası", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String user_id = nAuth.getCurrentUser().getProviderId();
                            DatabaseReference current_user_db= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id);
                            current_user_db.setValue(true); //if we dont change nothing will show

                        }
                    }
                });

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        nAuth.addAuthStateListener(firebaseAuthListener); //firebase of listener, starting the listener when ever the acitvity is started
    }

    @Override
    protected void onStop() {
        super.onStop();
        nAuth.removeAuthStateListener(firebaseAuthListener);//stop the listener when we leave activity
    }




}
