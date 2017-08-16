package com.treupositive.xyz.adminprototype

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.support.annotation.RequiresApi
import android.widget.Toast
import org.androidannotations.annotations.AfterInject
import org.androidannotations.annotations.EBean
import org.androidannotations.annotations.RootContext

/**
 * Created by YellowCataclysm on 07.08.2017.
 */

@EBean(scope = EBean.Scope.Singleton)
open class DeviceControl {

    // Main properties
    @RootContext
    protected lateinit var mRootContext: Context
    private lateinit var mPolicyManager: DevicePolicyManager
    private lateinit var mAdminComponent: ComponentName

    val Apps = AppsControl()
    val User = UserControl()
    val Util = Utilities()

    @AfterInject
    fun load() {
        mPolicyManager = mRootContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mAdminComponent = ComponentName(mRootContext, AdminReceiver::class.java)
        Apps.loadInstalledApps()
        User.setOppressionEnabled(true)
    }

    inner class AppsControl {
        private var mAppsLoaded = false
        private var mInstalledApps: List<ApplicationInfo> = ArrayList()
        val installedApps get() = mInstalledApps

        fun loadInstalledApps() {
            val pkm = mRootContext.packageManager
            val installedApps = pkm?.getInstalledApplications(
                    PackageManager.GET_META_DATA or PackageManager.GET_UNINSTALLED_PACKAGES)
            if (installedApps != null)
                this.mInstalledApps = installedApps
            else
                this.mInstalledApps = ArrayList()
            this.mInstalledApps = this.mInstalledApps.sortedWith(compareBy { it.packageName })
            mAppsLoaded = true
        }

        fun setAppHidden(app: ApplicationInfo, hide: Boolean) {
            mPolicyManager.setApplicationHidden(mAdminComponent, app.packageName, hide)
        }

        fun isAppHidden(app: ApplicationInfo): Boolean {
            return mPolicyManager.isApplicationHidden(mAdminComponent, app.packageName)
        }

        fun setUninstallBlocked(app: ApplicationInfo, block: Boolean = true) {
            mPolicyManager.setUninstallBlocked(mAdminComponent, app.packageName, block)
        }

        fun isUninstallBlocked(app: ApplicationInfo): Boolean {
            return mPolicyManager.isUninstallBlocked(mAdminComponent, app.packageName)
        }
    }

    inner class UserControl {

        private var mEnabled = false
        val isRestrictionsEnabled get() = mEnabled
        private val mRestrictionsList: List<String>
        init {
            val restrictions = mutableListOf(
                    UserManager.DISALLOW_ADD_USER,
                    UserManager.DISALLOW_APPS_CONTROL,
                    UserManager.DISALLOW_CONFIG_CREDENTIALS,
                    UserManager.DISALLOW_MODIFY_ACCOUNTS,
                    UserManager.DISALLOW_UNINSTALL_APPS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                restrictions.add(UserManager.DISALLOW_SET_WALLPAPER)
                restrictions.add(UserManager.DISALLOW_FUN)
            }
            mRestrictionsList = restrictions
        }

        fun setOppressionEnabled(on: Boolean = true) {
            try {
                for (r in mRestrictionsList)
                    if (on) mPolicyManager.addUserRestriction(mAdminComponent, r)
                    else mPolicyManager.clearUserRestriction(mAdminComponent, r)
                mEnabled = on
            } catch (e: SecurityException) {
                Toast.makeText(mRootContext, "Not an admin!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class Utilities {
        fun wipe() { mPolicyManager.wipeData(0) }
        fun lock() { mPolicyManager.lockNow() }

        @RequiresApi(Build.VERSION_CODES.N)
        fun reboot() { mPolicyManager.reboot(mAdminComponent) }
    }

    fun isDeviceAdminActive(): Boolean {
        return mPolicyManager.isAdminActive(mAdminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return mPolicyManager.isDeviceOwnerApp(mRootContext.packageName)
    }
}