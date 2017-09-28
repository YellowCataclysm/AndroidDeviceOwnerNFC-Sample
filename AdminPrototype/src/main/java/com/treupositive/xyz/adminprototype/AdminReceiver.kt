package com.treupositive.xyz.adminprototype

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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

    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
        mDPM = getManager( context )
    }

    override fun onPasswordChanged(context: Context?, intent: Intent?) {
        super.onPasswordChanged(context, intent)
        dropPassword()
    }

    override fun onPasswordFailed(context: Context?, intent: Intent?) {
        super.onPasswordFailed(context, intent)
        dropPassword()
    }

    override fun onPasswordSucceeded(context: Context?, intent: Intent?) {
        super.onPasswordSucceeded(context, intent)
        dropPassword()
    }

    private fun dropPassword() {
        clearPasswordPolicies()
        var flags = DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flags = flags or DevicePolicyManager.RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT
        mDPM.resetPassword("", flags)
    }

    private fun clearPasswordPolicies() {
        with(mDPM) {
            setPasswordExpirationTimeout(mComponentName, 0)
            setPasswordHistoryLength(mComponentName, 0)
            setPasswordMinimumLength(mComponentName, 0)
            setPasswordMinimumLetters(mComponentName, 0)
            setPasswordMinimumLowerCase(mComponentName, 0)
            setPasswordMinimumNonLetter(mComponentName, 0)
            setPasswordMinimumNumeric(mComponentName, 0)
            setPasswordMinimumSymbols(mComponentName, 0)
            setPasswordMinimumUpperCase(mComponentName, 0)
            setPasswordQuality(mComponentName, 0)
        }
    }
}