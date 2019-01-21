package com.example.semaphore.socialmedia;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.semaphore.socialmedia.Model.UserModel;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;



public class UserProfileActivity extends AppCompatActivity  {

    ImageView imageView;
    TextView txtUsername, txtEmail;
    UserModel userModel;
    Button userProfileActivityLogoutButton ;

    GoogleSignInClient googleSignInClient ;
    public String type = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        initializeView();
        if (getIntent() != null && getIntent().getExtras() != null){
            type = (String) getIntent().getExtras().get(Constants.TYPE);
            userModel = (UserModel) getIntent().getExtras().get(Constants.USER_MOPDEL);
        }
//        switch (type) {
//            case (Constants.FB) :
//                FacebookSdk.sdkInitialize(getApplicationContext());
//                break;
//
//
//        }
      setListener();
      setData();

    }

    private void initializeView() {
        imageView = findViewById(R.id.imageView);
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        userProfileActivityLogoutButton = findViewById(R.id.userProfileActivityLogoutButton);

    }


    private void setData() {
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.mipmap.ic_launcher);
        requestOptions.error(R.mipmap.ic_launcher);

        txtUsername.setText(getString(R.string.first_name)+" : "+
                userModel.getFirstName()+"\n" +
                getString(R.string.first_name)+" : " +
                userModel.getLastName());
        txtEmail.setText(userModel.getEmail());
        Glide.with(UserProfileActivity.this)
                .load(userModel.getImageUser())
                .apply(requestOptions)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);




    }

    private void setListener() {
        userProfileActivityLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();

            }});
        }

    @Override
    public void onBackPressed() {
        Toast.makeText(UserProfileActivity.this , "You must logout " ,Toast.LENGTH_LONG).show();
    }
}
