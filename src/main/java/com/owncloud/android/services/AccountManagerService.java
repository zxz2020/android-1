package com.owncloud.android.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class AccountManagerService extends Service {

    public AccountManagerService() { }

    /** Command to the service to display a message */
    private static final int MSG_REQUEST_PASSWORD = 1;
    private static final int MSG_RESPONSE_PASSWORD = 2;
    private static final int MSG_CREATE_NEW_ACCOUNT = 3;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REQUEST_PASSWORD:
                    AccountManager accountManager = AccountManager.get(AccountManagerService.this);
                    Account[] accounts = accountManager.getAccountsByType(MainApp.getAccountType());

                    if(accounts.length == 0) { // No accounts present yet
                        addNewAccount();
                    } else {
                        Message resp = Message.obtain(null, MSG_RESPONSE_PASSWORD);
                        Bundle bResp = new Bundle();
                        bResp.putSerializable("accounts", convertAccountsToMap(accountManager, accounts));
                        resp.setData(bResp);
                        try {
                            msg.replyTo.send(resp);
                        } catch (RemoteException e) {
                            Toast.makeText(AccountManagerService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_CREATE_NEW_ACCOUNT:
                    addNewAccount();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }


    private void addNewAccount() {
        Intent dialogIntent = new Intent(AccountManagerService.this, FileDisplayActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // TODO make the activity start the "add new account" dialog automatically...
        startActivity(dialogIntent);
    }

    private ArrayList<HashMap<String, String>> convertAccountsToMap(AccountManager accountManager, Account[] accounts) {
        ArrayList<HashMap<String, String>> usernames = new ArrayList<>();
        for (Account account : accounts) {
            String password = accountManager.getPassword(account);
            String username = account.name.split("@")[0];
            String server = account.name.split("@")[1];

            HashMap<String, String> user = new HashMap<>();
            user.put("username", username);
            user.put("password", password);
            user.put("url", server); // TODO how do we detect http / https here?

            usernames.add(user);
        }
        return usernames;
    }
}
