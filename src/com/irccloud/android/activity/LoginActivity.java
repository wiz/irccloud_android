/*
 * Copyright (c) 2013 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;

public class LoginActivity extends FragmentActivity {
	private View login = null;
	private AutoCompleteTextView email;
	private EditText password;
    private EditText host;
    private EditText name;
	private Button loginBtn;
    private Button signupBtn;
    private Button nextBtn;
    private Button sendAccessLinkBtn;
	private TextView connectingMsg = null;
    private TextView TOS = null;
    private TextView forgotPassword = null;
    private TextView enterpriseLearnMore = null;
    private TextView EnterYourEmail = null;
    private TextView hostHint = null;
    private LinearLayout notAProblem = null;
	private ProgressBar progressBar = null;
	private View connecting = null;
    private LinearLayout loginHint = null;
    private LinearLayout signupHint = null;
    private LinearLayout enterpriseHint = null;
    private LinearLayout loginSignupHint = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		getWindow().setBackgroundDrawable(null);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_login);

        connecting = findViewById(R.id.connecting);
		connectingMsg = (TextView)findViewById(R.id.connectingMsg);
		progressBar = (ProgressBar)findViewById(R.id.connectingProgress);

        loginHint = (LinearLayout)findViewById(R.id.loginHint);
        signupHint = (LinearLayout)findViewById(R.id.signupHint);
        hostHint = (TextView)findViewById(R.id.hostHint);

        login = findViewById(R.id.login);
        name = (EditText)findViewById(R.id.name);
        if(savedInstanceState != null && savedInstanceState.containsKey("name"))
            name.setText(savedInstanceState.getString("name"));
        email = (AutoCompleteTextView)findViewById(R.id.email);
        if(BuildConfig.ENTERPRISE)
            email.setHint(R.string.email_enterprise);
        ArrayList<String> accounts = new ArrayList<String>();
        AccountManager am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        for(Account a : am.getAccounts()) {
        	if(a.name.contains("@") && !accounts.contains(a.name))
        		accounts.add(a.name);
        }
        if(accounts.size() > 0)
        	email.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, accounts.toArray(new String[accounts.size()])));

        if(savedInstanceState != null && savedInstanceState.containsKey("email"))
        	email.setText(savedInstanceState.getString("email"));
        
        password = (EditText)findViewById(R.id.password);
        password.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					new LoginTask().execute((Void)null);
					return true;
				}
				return false;
			}
        });
        if(savedInstanceState != null && savedInstanceState.containsKey("password"))
        	password.setText(savedInstanceState.getString("password"));

        host = (EditText)findViewById(R.id.host);
        if(BuildConfig.ENTERPRISE)
            host.setText(NetworkConnection.IRCCLOUD_HOST);
        else
            host.setVisibility(View.GONE);
        host.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    new LoginTask().execute((Void)null);
                    return true;
                }
                return false;
            }
        });
        if(savedInstanceState != null && savedInstanceState.containsKey("host"))
            host.setText(savedInstanceState.getString("host"));
        else
            host.setText(getSharedPreferences("prefs", 0).getString("host", BuildConfig.HOST));

        if(host.getText().toString().equals("api.irccloud.com") || host.getText().toString().equals("www.irccloud.com"))
            host.setText("");

        loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new LoginTask().execute((Void)null);
			}
        });
        loginBtn.setFocusable(true);
    	loginBtn.requestFocus();

        sendAccessLinkBtn = (Button)findViewById(R.id.sendAccessLink);
        sendAccessLinkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResetPasswordTask().execute((Void)null);
            }
        });

        nextBtn = (Button)findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(host.getText().length() > 0) {
                    NetworkConnection.IRCCLOUD_HOST = host.getText().toString();
                    trimHost();

                    new EnterpriseConfigTask().execute((Void)null);
                }
            }
        });

        TOS = (TextView)findViewById(R.id.TOS);
        TOS.setMovementMethod(new LinkMovementMethod());

        forgotPassword = (TextView)findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(forgotPasswordClickListener);

        enterpriseLearnMore = (TextView)findViewById(R.id.enterpriseLearnMore);
        enterpriseLearnMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPackageInstalled("com.irccloud.android", LoginActivity.this)) {
                    startActivity(getPackageManager().getLaunchIntentForPackage("com.irccloud.android"));
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.irccloud.android")));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.irccloud.android")));
                    }
                }
            }

            private boolean isPackageInstalled(String packagename, Context context) {
                PackageManager pm = context.getPackageManager();
                try {
                    pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
                    return true;
                } catch (NameNotFoundException e) {
                    return false;
                }
            }
        });
        enterpriseHint = (LinearLayout)findViewById(R.id.enterpriseHint);

        EnterYourEmail = (TextView)findViewById(R.id.enterYourEmail);

        signupHint.setOnClickListener(signupHintClickListener);
        loginHint.setOnClickListener(loginHintClickListener);

        signupBtn = (Button)findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            new LoginTask().execute((Void)null);
            }
        });

        TextView version = (TextView)findViewById(R.id.version);
        try {
            version.setText("Version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			version.setVisibility(View.GONE);
		}

        Typeface LatoRegular = Typeface.createFromAsset(getAssets(), "Lato-Regular.ttf");
        Typeface LatoLightItalic = Typeface.createFromAsset(getAssets(), "Lato-LightItalic.ttf");

        for (int i=0; i < signupHint.getChildCount(); i++){
            View v = signupHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        for (int i=0; i < loginHint.getChildCount(); i++){
            View v = loginHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        LinearLayout IRCCloud = (LinearLayout)findViewById(R.id.IRCCloud);
        for (int i=0; i < IRCCloud.getChildCount(); i++){
            View v = IRCCloud.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        notAProblem = (LinearLayout)findViewById(R.id.notAProblem);
        for (int i=0; i < notAProblem.getChildCount(); i++){
            View v = notAProblem.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface((i==0)?LatoRegular:LatoLightItalic);
            }
        }

        loginSignupHint = (LinearLayout)findViewById(R.id.loginSignupHint);
        for (int i=0; i < loginSignupHint.getChildCount(); i++){
            View v = loginSignupHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
                ((TextView)v).setOnClickListener((i==0)?loginHintClickListener:signupHintClickListener);
            }
        }

        name.setTypeface(LatoRegular);
        email.setTypeface(LatoRegular);
        password.setTypeface(LatoRegular);
        host.setTypeface(LatoRegular);
        loginBtn.setTypeface(LatoRegular);
        signupBtn.setTypeface(LatoRegular);
        TOS.setTypeface(LatoRegular);
        EnterYourEmail.setTypeface(LatoRegular);
        hostHint.setTypeface(LatoLightItalic);

        if(BuildConfig.ENTERPRISE) {
            name.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.GONE);
            TOS.setVisibility(View.GONE);
            signupHint.setVisibility(View.GONE);
            loginHint.setVisibility(View.GONE);
            forgotPassword.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.VISIBLE);
            enterpriseHint.setVisibility(View.VISIBLE);
            host.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.VISIBLE);
            hostHint.setVisibility(View.VISIBLE);
            host.requestFocus();
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("signup") && savedInstanceState.getBoolean("signup")) {
            signupHintClickListener.onClick(null);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("login") && savedInstanceState.getBoolean("login")) {
            loginHintClickListener.onClick(null);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("forgotPassword") && savedInstanceState.getBoolean("forgotPassword")) {
            forgotPasswordClickListener.onClick(null);
        }
    }

    private OnClickListener signupHintClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            name.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.VISIBLE);
            name.requestFocus();
            TOS.setVisibility(View.VISIBLE);
            signupHint.setVisibility(View.GONE);
            loginHint.setVisibility(View.VISIBLE);
            forgotPassword.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_mid_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
        }
    };

    private OnClickListener loginHintClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            name.setVisibility(View.GONE);
            email.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.VISIBLE);
            signupBtn.setVisibility(View.GONE);
            email.requestFocus();
            TOS.setVisibility(View.GONE);
            loginHint.setVisibility(View.GONE);
            forgotPassword.setVisibility(View.VISIBLE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_top_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
            if(loginSignupHint.getChildAt(1).getVisibility() == View.VISIBLE)
                signupHint.setVisibility(View.VISIBLE);
            else
                enterpriseHint.setVisibility(View.VISIBLE);
        }
    };

    private OnClickListener forgotPasswordClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            loginHint.setVisibility(View.GONE);
            signupHint.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.VISIBLE);
            notAProblem.setVisibility(View.VISIBLE);
            password.setVisibility(View.GONE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.GONE);
            TOS.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.VISIBLE);
            EnterYourEmail.setVisibility(View.VISIBLE);
            forgotPassword.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_only_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
        }
    };

    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	if(login != null && login.getVisibility() == View.VISIBLE) {
	    	if(email != null)
	    		state.putString("email", email.getText().toString());
	    	if(password != null)
	    		state.putString("password", password.getText().toString());
            if(host != null)
                state.putString("host", host.getText().toString());
            if(name != null)
                state.putString("name", name.getText().toString());
            if(signupBtn != null)
                state.putBoolean("signup", signupBtn.getVisibility() == View.VISIBLE);
            if(loginBtn != null)
                state.putBoolean("login", loginBtn.getVisibility() == View.VISIBLE);
            if(sendAccessLinkBtn != null)
                state.putBoolean("forgotPassword", sendAccessLinkBtn.getVisibility() == View.VISIBLE);
    	}
    }

    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("screenlock", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if(NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
            NetworkConnection.getInstance().logout();
            new AccessLinkTask().execute("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/access-link?" + getIntent().getData().getEncodedQuery().replace("&mobile=1","") + "&format=json");
            setIntent(new Intent(this, LoginActivity.class));
        } else if(getSharedPreferences("prefs", 0).contains("session_key")) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            if(getIntent() != null) {
                if(getIntent().getData() != null)
                    i.setData(getIntent().getData());
                if(getIntent().getExtras() != null)
                    i.putExtras(getIntent().getExtras());
            }
            startActivity(i);
            finish();
            return;
    } else {
            connecting.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            checkPlayServices();
    	}
    }

    private class LoginTask extends AsyncTaskEx<Void, Void, JSONObject> {
		@Override
		public void onPreExecute() {
            name.setEnabled(false);
			email.setEnabled(false);
			password.setEnabled(false);
            host.setEnabled(false);
            loginHint.setEnabled(false);
            signupHint.setEnabled(false);
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
            editor.commit();
			loginBtn.setEnabled(false);
            signupBtn.setEnabled(false);
            if(name.getVisibility() == View.VISIBLE)
    			connectingMsg.setText("Creating Account");
            else
                connectingMsg.setText("Signing in");
			progressBar.setIndeterminate(true);
			connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
		}
		
		@Override
		protected JSONObject doInBackground(Void... arg0) {
            try {
                if(!BuildConfig.ENTERPRISE)
                    NetworkConnection.IRCCLOUD_HOST = BuildConfig.HOST;
                JSONObject config = NetworkConnection.getInstance().fetchJSON("https://" + NetworkConnection.IRCCLOUD_HOST + "/config");
                NetworkConnection.IRCCLOUD_HOST = config.getString("api_host");
                trimHost();
            } catch (Exception e) {
                return null;
            }
            if(name.getVisibility() == View.VISIBLE) {
                if (name.getText() != null && name.getText().length() > 0 && email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().signup(name.getText().toString(), email.getText().toString(), password.getText().toString());
                else
                    return null;
            } else {
                if (email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().login(email.getText().toString(), password.getText().toString());
                else
                    return null;
            }
		}

		@Override
		public void onPostExecute(JSONObject result) {
			if(result != null && result.has("session")) {
				try {
					SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
					editor.putString("session_key", result.getString("session"));
                    if(result.has("websocket_host")) {
                        NetworkConnection.IRCCLOUD_HOST = result.getString("websocket_host");
                        NetworkConnection.IRCCLOUD_PATH = result.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
					editor.commit();
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    if(getIntent() != null) {
                        if(getIntent().getData() != null)
                            i.setData(getIntent().getData());
                        if(getIntent().getExtras() != null)
                            i.putExtras(getIntent().getExtras());
                    }
                    startActivity(i);
                    finish();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
                name.setEnabled(true);
				email.setEnabled(true);
				password.setEnabled(true);
                host.setEnabled(true);
				loginBtn.setEnabled(true);
                signupBtn.setEnabled(true);
                loginHint.setEnabled(true);
                signupHint.setEnabled(true);
                connecting.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
				AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                if(name.getVisibility() == View.VISIBLE)
    				builder.setTitle("Sign Up Failed");
                else
                    builder.setTitle("Login Failed");
				String message = "Unable to connect to IRCCloud.  Please try again later.";
				if(result != null) {
					try {
                        if(result.has("message")) {
                            message = result.getString("message");
                            if(message.equalsIgnoreCase("auth") || message.equalsIgnoreCase("email") || message.equalsIgnoreCase("password") || message.equalsIgnoreCase("legacy_account"))
                                if(name.getVisibility() == View.VISIBLE)
                                    message = "Invalid email address or password.  Please try again.";
                                else
                                    message = "Incorrect username or password.  Please try again.";
                            else if(message.equals("json_error"))
                                message = "Invalid response received from the server.  Please try again shortly.";
                            else if(message.equals("invalid_response"))
                                message = "Unexpected response received from the server.  Check your network settings and try again shortly.";
                            else if(message.equals("empty_response"))
                                message = "The server did not respond.  Check your network settings and try again shortly.";
                            else if(message.equals("realname"))
                                message = "Please enter a valid name and try again.";
                            else if(message.equals("email_exists"))
                                message = "This email address is already in use, please sign in or try another.";
                            else if(message.equals("rate_limited"))
                                message = "Rate limited, please try again in a few minutes.";
                            else
                                message = "Error: " + message;
                        }
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else if((name.getVisibility() == View.VISIBLE && (name.getText() == null || name.getText().length() == 0)) || email.getText() == null || email.getText().length() == 0 || password.getText() == null || password.getText().length() == 0) {
                    if(name.getVisibility() == View.VISIBLE)
                        message = "Please enter your name, email address, and password.";
                    else
                        message = "Please enter your username and password.";
                }
				builder.setMessage(message);
				builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				AlertDialog dialog = builder.create();
				dialog.setOwnerActivity(LoginActivity.this);
				try {
					dialog.show();
				} catch (WindowManager.BadTokenException e) {
				}
			}
		}
    }

    private class AccessLinkTask extends AsyncTaskEx<String, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            name.setEnabled(false);
            email.setEnabled(false);
            password.setEnabled(false);
            host.setEnabled(false);
            loginHint.setEnabled(false);
            signupHint.setEnabled(false);
            loginBtn.setEnabled(false);
            signupBtn.setEnabled(false);
            connectingMsg.setText("Signing in");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(String... arg0) {
            try {
                return NetworkConnection.getInstance().fetchJSON(arg0[0]);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            if(result != null && result.has("session")) {
                try {
                    SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                    editor.putString("session_key", result.getString("session"));
                    if(result.has("websocket_host")) {
                        NetworkConnection.IRCCLOUD_HOST = result.getString("websocket_host");
                        NetworkConnection.IRCCLOUD_PATH = result.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
                    editor.commit();
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    if(getIntent() != null) {
                        if(getIntent().getData() != null)
                            i.setData(getIntent().getData());
                        if(getIntent().getExtras() != null)
                            i.putExtras(getIntent().getExtras());
                    }
                    startActivity(i);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                name.setEnabled(true);
                email.setEnabled(true);
                password.setEnabled(true);
                host.setEnabled(true);
                loginBtn.setEnabled(true);
                signupBtn.setEnabled(true);
                loginHint.setEnabled(true);
                signupHint.setEnabled(true);
                connecting.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                builder.setTitle("Login Failed");
                builder.setMessage("Invalid access link");
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(LoginActivity.this);
                try {
                    dialog.show();
                } catch (WindowManager.BadTokenException e) {
                }
            }
        }
    }

    private class ResetPasswordTask extends AsyncTaskEx<Void, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            email.setEnabled(false);
            sendAccessLinkBtn.setEnabled(false);
            connectingMsg.setText("Requesting Access Link");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(Void... arg0) {
            return NetworkConnection.getInstance().request_password(email.getText().toString());
        }

        @Override
        public void onPostExecute(JSONObject result) {
            email.setEnabled(true);
            sendAccessLinkBtn.setEnabled(true);
            progressBar.setIndeterminate(false);
            connecting.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            try {
                if (result != null && result.has("success") && result.getBoolean("success")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                    builder.setTitle("Access Link");
                    builder.setMessage("We've sent you an access link.  Check your email and follow the instructions to sign in.");
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loginHintClickListener.onClick(null);
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setOwnerActivity(LoginActivity.this);
                    try {
                        dialog.show();
                    } catch (WindowManager.BadTokenException e) {
                    }
                    return;
                }
            } catch (JSONException e) {
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            builder.setTitle("Password Reset Failed");
            builder.setMessage("Unable to request a password reset.  Please try again later.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(LoginActivity.this);
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
            }
        }
    }

    private void trimHost() {
        if(NetworkConnection.IRCCLOUD_HOST.startsWith("http://"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(7);
        if(NetworkConnection.IRCCLOUD_HOST.startsWith("https://"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(8);
        if(NetworkConnection.IRCCLOUD_HOST.endsWith("/"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(0, NetworkConnection.IRCCLOUD_HOST.length() - 1);
    }

    private class EnterpriseConfigTask extends AsyncTaskEx<Void, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            host.setEnabled(false);
            connectingMsg.setText("Connecting");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(Void... arg0) {
            try {
                return NetworkConnection.getInstance().fetchJSON("https://" + NetworkConnection.IRCCLOUD_HOST + "/config");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            host.setEnabled(true);
            progressBar.setIndeterminate(false);
            connecting.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            try {
                if (result != null && result.get("enterprise") instanceof JSONObject) {
                    NetworkConnection.IRCCLOUD_HOST = result.getString("api_host");
                    trimHost();

                    SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.commit();

                    if(result.getString("auth_mechanism").equals("internal"))
                        loginSignupHint.getChildAt(1).setVisibility(View.VISIBLE);
                    else
                        loginSignupHint.getChildAt(1).setVisibility(View.GONE);

                    if(((JSONObject) result.get("enterprise")).has("fullname"))
                        ((TextView)findViewById(R.id.enterpriseHintText)).setText(((JSONObject) result.get("enterprise")).getString("fullname"));

                    if(NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
                        NetworkConnection.getInstance().logout();
                        new AccessLinkTask().execute("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/access-link?" + getIntent().getData().getEncodedQuery().replace("&mobile=1","") + "&format=json");
                        setIntent(new Intent(LoginActivity.this, LoginActivity.class));
                    } else {
                        loginHintClickListener.onClick(null);
                    }
                    return;
                }
            } catch (JSONException e) {
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            builder.setTitle("Connection Failed");
            builder.setMessage("Please check your host and try again shortly, or contact your system administrator for assistance.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(LoginActivity.this);
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
            }
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000).show();
            }
            return false;
        }
        return true;
    }
}
