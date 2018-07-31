package com.example.android.medex;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.florent37.materialtextfield.MaterialTextField;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class SignupDetailActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignUpDetailActivity";
    private boolean mobileOk = true;
    private boolean emailOk = true;
    private boolean groupOk = false;
    private boolean districtOk = false;

    String[] groups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB-", "AB+" };
    String[] districts = {"Alappuzha", "Ernakulam", "Idukki", "Kannur", "Kasaragod", "Kollam", "Kottayam", "Kozhikode",
            "Malappuram", "Palakkad", "Pathanamthitta", "Thiruvananthapuram", "Thrissur", "Wayanad"};

    String personName;
    String personEmail;
    String personId;
    String personMobile;
    String personDistrict;
    String personBloodGroup;
    Uri personPhoto;

    ImageView circleImageView;
    EditText userName;
    EditText userEmail;
    EditText userMobile;
    EditText userDistrict;
    EditText userBloodGroup;

    Button mSignUp;
    Person person;

    FirebaseFirestore db;
    Map<String, Object> user;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_detail);

        Typeface raleway_regular = Typeface.createFromAsset(this.getAssets(),"fonts/Raleway-Regular.ttf" );

        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        mAuth = FirebaseAuth.getInstance();

        userName.setTypeface(raleway_regular);
        userEmail.setTypeface(raleway_regular);
        userMobile.setTypeface(raleway_regular);
        userDistrict.setTypeface(raleway_regular);
        userBloodGroup.setTypeface(raleway_regular);
        mSignUp.setTypeface(raleway_regular);

        userName = findViewById(R.id.username);
        userEmail = findViewById(R.id.email);
        userMobile = findViewById(R.id.mobileno);
        userDistrict = findViewById(R.id.district);
        userBloodGroup = findViewById(R.id.bloodgroup);
        circleImageView = findViewById(R.id.circleImageView);
        mSignUp = findViewById(R.id.sign_up);

        mSignUp.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        userName.setText(currentUser.getDisplayName());
        userEmail.setText(currentUser.getEmail());
        Picasso.get().load(currentUser.getPhotoUrl()).into(circleImageView);

        personPhoto = currentUser.getPhotoUrl();
        personName = currentUser.getDisplayName();
        personId = currentUser.getUid();
    }

    private void addUserDataToFireStore() {

        personEmail = userEmail.getText().toString().trim();
        personMobile = userMobile.getText().toString().trim();
        personBloodGroup = userBloodGroup.getText().toString().trim();
        personDistrict = userDistrict.getText().toString().trim();

        if(personMobile.length() != 10)
        {
            mobileOk = false;
        }

        if(!validEmail(personEmail))
        {
            emailOk = false;
        }

        String District = personDistrict.substring(0, 1).toUpperCase() + personDistrict.substring(1).toLowerCase();

        if(Arrays.asList(districts).contains(District)){
            districtOk = true;
        }

        String Blood = personBloodGroup.substring(0,1).toUpperCase() + personBloodGroup.substring(1);

        if(Arrays.asList(groups).contains(Blood)) {
            groupOk = true;
        }

        if(mobileOk && emailOk && districtOk && groupOk)
        {
//            person.setPersonEmail(personEmail);
//            person.setPersonMobile(personMobile);
//            person.setPersonBloodGroup(personBloodGroup);
//            person.setPersonDistrict(personDistrict);

            user = new HashMap<>();
            user.put("id", personId);
            user.put("name", personName);
            user.put("mobile", personMobile);
            user.put("email", personEmail);
            user.put("district", District);
            user.put("blood", Blood);
            user.put("pic", personPhoto.toString());

            db.collection("users")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            Intent intent = new Intent(SignupDetailActivity.this, HomeActivity.class);
                            intent.putExtra("docId", documentReference.getId());
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                            mSignUp.setBackgroundResource(R.drawable.button_text_color);
                            Toast.makeText(SignupDetailActivity.this, "Sign up Failed, Try Again", Toast.LENGTH_LONG).show();
                        }
                    });
        }
        else if (!mobileOk)
        {
            Toast.makeText(this, "Enter 10 digit mobile number", Toast.LENGTH_SHORT).show();
        }
        else if (!emailOk)
        {
            Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
        }
        else if (!districtOk)
        {
            Toast.makeText(this, District + " is not a district in kerala", Toast.LENGTH_SHORT).show();
        }
        else if (!groupOk)
        {
            Toast.makeText(this, Blood + " is not accepted ( A+, B+ , ..)", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sign_up:
                addUserDataToFireStore();
                break;
        }
    }

    private boolean validEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }
}
