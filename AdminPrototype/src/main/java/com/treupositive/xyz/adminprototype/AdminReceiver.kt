package com.treupositive.xyz.adminprototype

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by YellowCataclysm on 07.08.2017.
 */

// This class do nothing for now
public class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
    }
}