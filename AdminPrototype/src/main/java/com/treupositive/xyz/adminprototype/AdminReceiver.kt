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
    private lateinit var mDPM : DevicePolicyManager
    private val mComponentName = ComponentName( javaClass.`package`.name, javaClass.name)
    private val tag = "RRAdminReceiver"
    private val LOG = Logger.getLogger(tag)
    private lateinit var control: DeviceControl

    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
        mDPM = getManager( context )
        control = DeviceControl_.getInstance_(context)
    }

    override fun onPasswordChanged(context: Context?, intent: Intent?) {
        super.onPasswordChanged(context, intent)
        control.util.dropPassword()
    }

    override fun onPasswordFailed(context: Context?, intent: Intent?) {
        super.onPasswordFailed(context, intent)
        control.util.dropPassword()
    }

    override fun onPasswordSucceeded(context: Context?, intent: Intent?) {
        super.onPasswordSucceeded(context, intent)
        control.util.dropPassword()
    }
}