/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.usb;

import com.android.internal.app.ResolverActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.IUsbManager;
import android.hardware.UsbAccessory;
import android.hardware.UsbDevice;
import android.hardware.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;

/* Activity for choosing an application for a USB device or accessory */
public class UsbResolverActivity extends ResolverActivity {
    public static final String TAG = "UsbResolverActivity";
    public static final String EXTRA_RESOLVE_INFOS = "rlist";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(targetParcelable instanceof Intent)) {
            Log.w("UsbResolverActivity", "Target is not an intent: " + targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent)targetParcelable;
        ArrayList<ResolveInfo> rList = intent.getParcelableArrayListExtra(EXTRA_RESOLVE_INFOS);
        Log.d(TAG, "rList.size() " + rList.size());
        CharSequence title = getResources().getText(com.android.internal.R.string.chooseUsbActivity);
        super.onCreate(savedInstanceState, target, title, null, rList,
                true, /* Set alwaysUseOption to true to enable "always use this app" checkbox. */
                true  /* Set alwaysChoose to display activity when only one choice is available.
                         This is necessary because this activity is needed for the user to allow
                         the application permission to access the device */
                );
    }

    protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        try {
            IBinder b = ServiceManager.getService(USB_SERVICE);
            IUsbManager service = IUsbManager.Stub.asInterface(b);
            int uid = ri.activityInfo.applicationInfo.uid;
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // grant permission for the device
                service.grantDevicePermission(device, uid);
                // set or clear default setting
                if (alwaysCheck) {
                    service.setDevicePackage(device, ri.activityInfo.packageName);
                } else {
                    service.setDevicePackage(device, null);
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(
                        UsbManager.EXTRA_ACCESSORY);
                // grant permission for the accessory
                service.grantAccessoryPermission(accessory, uid);
                // set or clear default setting
                if (alwaysCheck) {
                    service.setAccessoryPackage(accessory, ri.activityInfo.packageName);
                } else {
                    service.setAccessoryPackage(accessory, null);
                }
            }

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "startActivity failed", e);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onIntentSelected failed", e);
        }
    }
}