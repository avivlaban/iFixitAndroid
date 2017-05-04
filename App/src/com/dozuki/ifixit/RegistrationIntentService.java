package com.dozuki.ifixit;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;

import java.io.IOException;

import sdk.insert.io.Insert;

/**
 * Created by Tsafrir on 09/04/17.
 */
public class RegistrationIntentService extends IntentService {

    public RegistrationIntentService() {
        super("bla");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String token = null;
        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (Exception e) {
            System.out.println("lld");
        }

        if (token != null) {
            Insert.setPushId(token);
        }
    }

}
