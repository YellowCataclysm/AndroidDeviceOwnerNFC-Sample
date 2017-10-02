package com.treupositive.xyz.adminprototype

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.logging.Logger

/**
 * Created by YellowCataclysm on 07.08.2017.
 */

// This class do nothing for now
class AdminReceiver : DeviceAdminReceiver() {
    private val tag = "RRAdminReceiver"
    private var control: DeviceControl? = null

    private fun getControl( context: Context? ): DeviceControl {
        if( control == null ) control = DeviceControl_.getInstance_(context)
        return control!!
    }

    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
        control = DeviceControl_.getInstance_(context)
    }

    override fun onPasswordChanged(context: Context?, intent: Intent?) {
        super.onPasswordChanged(context, intent)
        getControl(context).util.dropPassword()
    }

    override fun onPasswordFailed(context: Context?, intent: Intent?) {
        super.onPasswordFailed(context, intent)
        getControl(context).util.dropPassword()
    }

    override fun onPasswordSucceeded(context: Context?, intent: Intent?) {
        super.onPasswordSucceeded(context, intent)
        getControl(context).util.dropPassword()
    }
}