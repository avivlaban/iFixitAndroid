package com.dozuki.ifixit.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dozuki.ifixit.App;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.dozuki.Site;
import com.dozuki.ifixit.model.user.User;
import com.dozuki.ifixit.ui.BaseActivity;
import com.dozuki.ifixit.ui.BaseDialogFragment;
import com.dozuki.ifixit.util.api.Api;
import com.dozuki.ifixit.util.api.ApiCall;
import com.dozuki.ifixit.util.api.ApiError;
import com.dozuki.ifixit.util.api.ApiEvent;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.squareup.otto.Subscribe;

import java.io.IOException;

public class LoginFragment extends BaseDialogFragment implements OnClickListener {
   private static final int OPEN_ID_REQUEST_CODE = 4;

   private Button mLogin;
   private Button mRegister;
   private SignInButton mGoogleLogin;
   //private ImageButton mYahooLogin;
   private EditText mEmail;
   private EditText mPassword;
   private TextView mErrorText;
   private ProgressBar mLoadingSpinner;
   private ApiCall mCurAPICall;
   private boolean mHasRegisterBtn = true;
   private boolean mFailedSsoLogin = false;
   private GoogleApiClient mGoogleApiClient;
   private boolean mGoogleLoginInProgress = false;
   private boolean mGoogleLoginClicked = false;

   @Subscribe
   public void onLogin(ApiEvent.Login event) {
      handleLogin(event);
   }

   @Subscribe
   public void onUserInfo(ApiEvent.UserInfo event) {
      handleLogin(event);
   }

   @Subscribe
   public void onSiteInfo(ApiEvent.SiteInfo event) {
      if (!event.hasError()) {
         App.get().setSite(event.getResult());

         initGoogleLoginButton();
      } else {
         Api.getErrorDialog(getActivity(), event).show();
      }
   }

   private void handleLogin(ApiEvent<User> event) {
      if (!event.hasError()) {
         User user = event.getResult();
         ((App)getActivity().getApplication()).login(user, getEmail(),
          getPassword(), true);

         dismiss();
      } else {
         enable(true);
         ApiError error = event.getError();

         mLoadingSpinner.setVisibility(View.GONE);

         // Show input fields
         mEmail.setVisibility(View.VISIBLE);
         mPassword.setVisibility(View.VISIBLE);

         mErrorText.setVisibility(View.VISIBLE);
         mErrorText.setText(error.mMessage);
      }
   }

   /**
    * Required for restoring fragments
    */
   public LoginFragment() {}

   public static LoginFragment newInstance() {
      LoginFragment frag = new LoginFragment();
      frag.setStyle(DialogFragment.STYLE_NO_TITLE,
       android.R.style.Theme_Holo_Light_Dialog);

      return frag;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      App.get().setIsLoggingIn(true);

      Site site = App.get().getSite();
      
      mHasRegisterBtn = site.mPublicRegistration;

      if (site.checkForGoogleLogin()) {
         // Get the clientid so we can perform Google login.
         if (site.mGoogleOAuth2Clientid == null) {
            Api.call(getActivity(), ApiCall.siteInfo());
         }

         mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
          .addApi(Plus.API)
          .addScope(new Scope("email"))
          .addScope(new Scope("profile"))
          .build();
      }

      if (!site.mStandardAuth) {
          Intent intent = new Intent(getActivity(), OpenIDActivity.class);
          intent.putExtra(OpenIDActivity.SINGLE_SIGN_ON, true);
          startActivityForResult(intent, OPEN_ID_REQUEST_CODE);
      }
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
      
      View view = inflater.inflate(R.layout.login_fragment, container, false);

      mEmail = (EditText)view.findViewById(R.id.edit_email);
      mPassword = (EditText)view.findViewById(R.id.edit_password);
      mPassword.setTypeface(Typeface.DEFAULT);

      mLogin = (Button)view.findViewById(R.id.signin_button);
      mRegister = (Button)view.findViewById(R.id.register_button);      
      mGoogleLogin = (SignInButton)view.findViewById(R.id.use_google_login_button);
      //mYahooLogin = (ImageButton)view.findViewById(R.id.use_yahoo_login_button);

      mLogin.setOnClickListener(this);  
      
      if (mHasRegisterBtn) {
         mRegister.setOnClickListener(this);
         mGoogleLogin.setOnClickListener(this);
         //mYahooLogin.setOnClickListener(this);
      } else {
         mRegister.setVisibility(View.GONE);
         mGoogleLogin.setVisibility(View.GONE);
         //mYahooLogin.setVisibility(View.GONE);
      }

      initGoogleLoginButton();

      mErrorText = (TextView)view.findViewById(R.id.login_error_text);
      mErrorText.setVisibility(View.GONE);

      mLoadingSpinner = (ProgressBar)view.findViewById(R.id.login_loading_bar);
      mLoadingSpinner.setVisibility(View.GONE);
      
      return view;
   }

   private void initGoogleLoginButton() {
      if (App.get().getSite().hasGoogleLogin()) {
         // Display the Google Login button because we have a valid clientid.
         mGoogleLogin.setVisibility(View.VISIBLE);
      }
   }

   @Override
   public void onStart() {
      super.onStart();

      App.sendScreenView("/login");
   }

   @Override
   public void onStop() {
      super.onStop();

      if (mGoogleApiClient != null) {
         mGoogleApiClient.disconnect();
      }
   }

   @Override
   public void onResume() {
      super.onResume();

      if (mFailedSsoLogin) {
         // Dismiss the dialog because SSO login failed.
         getDialog().cancel();
      }
   }

   private void login() {
      String email = getEmail();
      String password = getPassword();

      if (email.length() > 0 && password.length() > 0 ) {
         // Hide input fields
         mEmail.setVisibility(View.GONE);
         mPassword.setVisibility(View.GONE);
         
         mLoadingSpinner.setVisibility(View.VISIBLE);
         enable(false);
         mCurAPICall = ApiCall.login(email, password);
         Api.call(getActivity(), mCurAPICall);
      } else {
         if (email.length() < 1) {
            mEmail.requestFocus();
            showKeyboard();
         } else {
            mPassword.requestFocus();
            showKeyboard();
         }
         mErrorText.setText(R.string.empty_field_error);
         mErrorText.setVisibility(View.VISIBLE);
      }
   }

   private String getEmail() {
      return mEmail.getText().toString();
   }

   private String getPassword() {
      return mPassword.getText().toString();
   }

   private void showKeyboard() {
      InputMethodManager in = (InputMethodManager)getActivity()
       .getSystemService(Context.INPUT_METHOD_SERVICE);

      in.toggleSoftInput(InputMethodManager.SHOW_FORCED,
       InputMethodManager.HIDE_IMPLICIT_ONLY);
   }

   private void enable(boolean enabled) {
      mEmail.setEnabled(enabled);
      mPassword.setEnabled(enabled);
      mLogin.setEnabled(enabled);
      if (mHasRegisterBtn) {
         mRegister.setEnabled(enabled);
         mGoogleLogin.setEnabled(enabled);
         //mYahooLogin.setEnabled(enabled);
      }
   }

   @Override
   public void onClick(View v) {
      Intent intent;
      switch (v.getId()) {
          case R.id.use_google_login_button:
             if (!mGoogleApiClient.isConnecting()) {
                mGoogleLoginClicked = true;
                if (mGoogleApiClient.isConnected()) {
                   mGoogleApiClient.reconnect();
                } else {
                   mGoogleApiClient.connect();
                }
             }
             break;
    
          case R.id.use_yahoo_login_button:
             intent = new Intent(getActivity(), OpenIDActivity.class);
             intent.putExtra(OpenIDActivity.LOGIN_METHOD, OpenIDActivity.YAHOO_LOGIN);
             startActivityForResult(intent, OPEN_ID_REQUEST_CODE);
             break;
    
          case R.id.register_button:
             FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
           
             fragmentManager.beginTransaction()
              .remove(this)
              .add(RegisterFragment.newInstance(), null)
              .commit();
              
             break;
             
          case R.id.signin_button:
             InputMethodManager in = (InputMethodManager)getActivity()
              .getSystemService(Context.INPUT_METHOD_SERVICE);

             in.hideSoftInputFromWindow(mEmail.getApplicationWindowToken(),
              InputMethodManager.HIDE_NOT_ALWAYS);
             login();
             break;
      }
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if (requestCode == OPEN_ID_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
         mLoadingSpinner.setVisibility(View.VISIBLE);
         String session = data.getStringExtra(OpenIDActivity.SESSION);
         enable(false);
         mCurAPICall = ApiCall.userInfo(session);
         Api.call(getActivity(), mCurAPICall);
      } else if (requestCode == BaseActivity.GOOGLE_SIGN_IN_REQUEST_CODE) {
         //handleGoogleSignInActivityResult(requestCode, resultCode, data);
      }

      if (!App.get().getSite().mStandardAuth && resultCode != Activity.RESULT_OK) {
         /**
          * Single sign on failed. There aren't any login alternatives so we need
          * to close the dialog. We can't do that here because onResume hasn't been
          * called yet. This sets a flag which is used in onResume to kill the dialog.
          */
         mFailedSsoLogin = true;
      }
   }




   private String getGoogleOAuthScopes() {
      String clientId = App.get().getSite().mGoogleOAuth2Clientid;

      return "oauth2:server:client_id:" + clientId + ":api_scope:" +
       "https://www.googleapis.com/auth/userinfo.profile " +
       "https://www.googleapis.com/auth/userinfo.email";
   }

   @Subscribe
   public void onTokenReceived(GoogleOAuthCodeEvent event) {
      mCurAPICall = ApiCall.googleOauthLogin(event.mCode);
      Api.call(getActivity(), mCurAPICall);
   }

   private static class GoogleOAuthCodeEvent {
      public String mCode;

      public GoogleOAuthCodeEvent(String code) {
         mCode = code;
      }
   }

   public static class GoogleSignInActivityResult {
      public int mRequestCode;
      public int mResultCode;
      public Intent mData;

      public GoogleSignInActivityResult(int requestCode, int resultCode, Intent data) {
         mRequestCode = requestCode;
         mResultCode = resultCode;
         mData = data;
      }
   }



}
