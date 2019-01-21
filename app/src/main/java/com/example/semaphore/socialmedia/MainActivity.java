package com.example.semaphore.socialmedia;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.semaphore.socialmedia.Model.UserModel;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.User;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements AuthenticationListener {


    LoginButton loginButton;
    CallbackManager callbackManager;
    UserModel userModel;

    private SignInButton googleSignInButton;
    private GoogleSignInClient googleSignInClient;
    public static final int GoogleSIGNIN = 101;

    Button instagramLoginButton;
    private TwitterLoginButton twitterLoginButton;


    private String token = null;
    private AuthenticationDialog authenticationDialog = null;
    private TwitterAuthClient client;


    TwitterSession twitterSession ;


    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount logedOut = GoogleSignIn.getLastSignedInAccount(this);
        boolean loggedOut = AccessToken.getCurrentAccessToken() == null;
        if (!loggedOut) {
            Toast.makeText(this, "Already Logged In", Toast.LENGTH_LONG).show();
            getUserProfile(AccessToken.getCurrentAccessToken());
        }
        if (logedOut != null) {
            Toast.makeText(this, "Already Logged In", Toast.LENGTH_LONG).show();
            onLoggedIn(logedOut);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login_button);
        instagramLoginButton = findViewById(R.id.instagramLoginButton);
       twitterLoginButton =findViewById(R.id.twitterLoginButton);

        userModel = new UserModel();
        token = userModel.getToken();



        //initialize twitter auth client
        client = new TwitterAuthClient();

        loginButton.setReadPermissions(Arrays.asList("email", "public_profile"));
        callbackManager = CallbackManager.Factory.create();

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                //loginResult.getAccessToken();
                //loginResult.getRecentlyDeniedPermissions()
                //loginResult.getRecentlyGrantedPermissions()
                boolean loggedOut = AccessToken.getCurrentAccessToken() == null;

                if (!loggedOut) {

                    //Using Graph API
                    getUserProfile(AccessToken.getCurrentAccessToken());
                }
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
        AccessTokenTracker fbTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken accessToken, AccessToken accessToken2) {
                if (accessToken2 == null) {

                    Toast.makeText(getApplicationContext(), "You Logged Out.", Toast.LENGTH_LONG).show();
                }
            }
        };
        fbTracker.startTracking();

        googleSignInButton = findViewById(R.id.sign_in_button);


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent googleSignInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(googleSignInIntent, GoogleSIGNIN);
            }
        });

loginUsingTwitter();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GoogleSIGNIN:
                    try {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class);
                        onLoggedIn(googleSignInAccount);
                    } catch (ApiException e) {
                        // The ApiException status code indicates the detailed failure reason.
                        Log.w("googleSigin", "signInResult:failed code=" + e.getStatusCode());

                    }
            }
        }
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // facebook
            LoginManager.getInstance().logOut();

        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
            googleSignInClient.signOut();
        } else if (requestCode == 3 && resultCode == RESULT_OK && data != null) {

            logout();
        }



        // Pass the activity result to the twitterAuthClient.
        if (client != null)
            client.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        twitterLoginButton.onActivityResult(requestCode, resultCode, data);

    }

    private void getUserProfile(AccessToken currentAccessToken) {
        GraphRequest request = GraphRequest.newMeRequest(
                currentAccessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.d("TAG", object.toString());
                        try {
                            String first_name = object.getString("first_name");
                            String last_name = object.getString("last_name");
                            String email = object.getString("email");
                            String id = object.getString("id");
                            String image_url = "https://graph.facebook.com/" + id + "/picture?type=normal";

                            userModel = new UserModel();
                            userModel.setFirstName(first_name);
                            userModel.setLastName(last_name);
                            userModel.setEmail(email);
                            userModel.setId(id);
                            userModel.setImageUser(image_url);

                            if (!userModel.getLastName().isEmpty()) {

                                Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                                intent.putExtra(Constants.USER_MOPDEL, userModel);
                                intent.putExtra(Constants.TYPE, Constants.FB);
                                startActivityForResult(intent, 1);

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name,email,id");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void onLoggedIn(GoogleSignInAccount googleSignInAccount) {
        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);


        String first_name = googleSignInAccount.getGivenName();

        String last_name = googleSignInAccount.getFamilyName();
        String email = googleSignInAccount.getEmail();
        String id = googleSignInAccount.getId();
        String image_url = googleSignInAccount.getPhotoUrl().toString();

        userModel = new UserModel();
        userModel.setFirstName(first_name);
        userModel.setLastName(last_name);
        userModel.setEmail(email);
        userModel.setId(id);
        userModel.setImageUser(image_url);
        intent.putExtra(Constants.TYPE, Constants.G);
        intent.putExtra(Constants.USER_MOPDEL, userModel);
        startActivityForResult(intent, 2);


    }


    public void onClick(View view) {

        authenticationDialog = new AuthenticationDialog(this, this);
        authenticationDialog.setCancelable(true);
        authenticationDialog.show();

    }

    @Override
    public void onTokenReceived(String auth_token) {
        if (auth_token == null)
            return;

        userModel.setToken(auth_token);
        token = auth_token;
        getUserInfoByAccessToken(token);
    }

    private void getUserInfoByAccessToken(String token) {
        new RequestInstagramAPI().execute();
    }

    public void loginUsingTwitter() {



        if (twitterSession == null) {



            //if user is not authenticated start authenticating
            twitterLoginButton.setCallback(new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {

                    // Do something with result, which provides a TwitterSession for making API calls
                     twitterSession = result.data;

                    //call fetch email only when permission is granted
                  //  fetchTwitterEmail(twitterSession);
                    fetchTwitterImage();



                }

                @Override
                public void failure(TwitterException exception) {
                    // Do something on failure
                    Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {

            //if user is already authenticated direct call fetch twitter email api
            Toast.makeText(this, "User already authenticated", Toast.LENGTH_SHORT).show();

            fetchTwitterImage();

        }

    }

    private class RequestInstagramAPI extends AsyncTask<Void, String, String> {

        @Override
        protected String doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(getResources().getString(R.string.get_user_info_url) + token);
            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity httpEntity = response.getEntity();
                return EntityUtils.toString(httpEntity);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (response != null) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.e("response", jsonObject.toString());
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    if (jsonData.has("id")) {
                        //сохранение данных пользователя


                        userModel.setId(jsonData.getString("id"));
                        //TODO: сохранить еще данные


                        userModel.setFirstName(jsonData.getString("username"));
                        userModel.setImageUser(jsonData.getString("profile_picture"));
                        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                        intent.putExtra(Constants.USER_MOPDEL, userModel);
                        startActivityForResult(intent, 3);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Ошибка входа!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }


    public void logout() {

        token = null;
     //   appPreferences.clear();
    }

    private TwitterSession getTwitterSession() {
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        //NOTE : if you want to get token and secret too use uncomment the below code
        /*TwitterAuthToken authToken = session.getAuthToken();
        String token = authToken.token;
        String secret = authToken.secret;*/

        return session;
    }




    public void fetchTwitterImage() {
        //check if user is already authenticated or not
        if (getTwitterSession() != null) {

            //fetch twitter image with other information if user is already authenticated

            //initialize twitter api client
            TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();

            //Link for Help : https://developer.twitter.com/en/docs/accounts-and-users/manage-account-settings/api-reference/get-account-verify_credentials

            //pass includeEmail : true if you want to fetch Email as well
            Call<User> call = twitterApiClient.getAccountService().verifyCredentials(true, false, true);
            call.enqueue(new Callback<User>() {
                @Override
                public void success(Result<User> result) {
                    User user = result.data;
                  //  userDetailsLabel.setText("User Id : " + user.id + "\nUser Name : " + user.name + "\nEmail Id : " + user.email + "\nScreen Name : " + user.screenName);

                    String imageProfileUrl = user.profileImageUrl;
                    userModel.setImageUser(imageProfileUrl);
                    userModel.setEmail(user.email);
                    userModel.setFirstName(user.name);
                    userModel.setId(String.valueOf(user.id));
                    Log.e("twitter", "Data : " + imageProfileUrl);
                    Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                    intent.putExtra(Constants.TYPE, Constants.G);
                    intent.putExtra(Constants.USER_MOPDEL, userModel);
                    startActivityForResult(intent, 4);

                }

                @Override
                public void failure(TwitterException exception) {
                    Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            //if user is not authenticated first ask user to do authentication
            Toast.makeText(this, "First to Twitter auth to Verify Credentials.", Toast.LENGTH_SHORT).show();
        }

    }

}

