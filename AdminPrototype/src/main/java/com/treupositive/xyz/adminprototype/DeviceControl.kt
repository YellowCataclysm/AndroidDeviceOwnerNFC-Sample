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

/***
 * name - Readable name to show in UI
 * key - System defined name
 */

@EBean(scope = EBean.Scope.Singleton)
open class DeviceControl {

    // Main properties
    @RootContext
    protected lateinit var mRootContext: Context
    private lateinit var mPolicyManager: DevicePolicyManager
    private lateinit var mAdminComponent: ComponentName

    lateinit var apps: AppsControl
    lateinit var user: UserControl
    lateinit var util: Utilities

    @AfterInject
    fun load() {
        mPolicyManager = mRootContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mAdminComponent = ComponentName(mRootContext, AdminReceiver::class.java)

        apps = AppsControl()
        apps.loadInstalledApps()

        user = UserControl()
        user.setOppressionEnabled(true)

        util = Utilities()
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
        val restrictionsInfo get() = mRestrictionInfos
        private val mAllRestrictionsList: List<String>
        private val mRestrictionInfos: Map<String, UserRestrictionInfo>
        init {
            mRestrictionInfos = mapOf(
                    Pair(
                            UserManager.DISALLOW_ADD_USER,
                            UserRestrictionInfo(
                                    name = "DISALLOW ADD USER",
                                    key = UserManager.DISALLOW_ADD_USER,
                                    description = "Ограничивает возможность добавления пользователя/аккаунта в систему",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_ADD_USER ))
                    ),
                    Pair(
                            UserManager.DISALLOW_ADD_USER,
                            UserRestrictionInfo(
                                    name = "DISALLOW ADD USER",
                                    key = UserManager.DISALLOW_ADD_USER,
                                    description = "Ограничивает возможность добавления пользователя/аккаунта в систему",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_ADD_USER ))
                    ),
                    Pair(
                            UserManager.DISALLOW_APPS_CONTROL,
                            UserRestrictionInfo(
                                    name = "DISALLOW APPS CONTROL",
                                    key = UserManager.DISALLOW_APPS_CONTROL,
                                    description = "Ограничивает возможность управления приложениями",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_APPS_CONTROL ))
                    ),
                    Pair(
                            UserManager.DISALLOW_CONFIG_CREDENTIALS,
                            UserRestrictionInfo(
                                    name = "DISALLOW CONFIG CREDENTIALS",
                                    key = UserManager.DISALLOW_CONFIG_CREDENTIALS,
                                    description = "Запрещает указание личных данных для аккаунтов",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_CONFIG_CREDENTIALS ))
                    ),
                    Pair(
                            UserManager.DISALLOW_MODIFY_ACCOUNTS,
                            UserRestrictionInfo(
                                    name = "DISALLOW MODIFY ACCOUNTS",
                                    key = UserManager.DISALLOW_MODIFY_ACCOUNTS,
                                    description = "Ограничивает возможность изменения существующих пользователей/аккаунтов",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_MODIFY_ACCOUNTS ))
                    ),
                    Pair(
                            UserManager.DISALLOW_UNINSTALL_APPS,
                            UserRestrictionInfo(
                                    name = "DISALLOW UNINSTALL APPS",
                                    key = UserManager.DISALLOW_UNINSTALL_APPS,
                                    description = "Запрещает удаление приложений пользователем",
                                    enabled = __isRestrictionEnabled( UserManager.DISALLOW_UNINSTALL_APPS ))
                    )
            )
            mAllRestrictionsList = mRestrictionInfos.keys.toList()
        }

        fun setOppressionEnabled(on: Boolean = true) {
            try {
                for (r in mAllRestrictionsList)
                    if (on) mPolicyManager.addUserRestriction(mAdminComponent, r)
                    else mPolicyManager.clearUserRestriction(mAdminComponent, r)
                mEnabled = on
            } catch (e: SecurityException) {
                Toast.makeText(mRootContext, "Not an admin!", Toast.LENGTH_SHORT).show()
            }
        }

        fun setRestrictionEnabled( restriction: String, enabled: Boolean = true ) : Boolean {
            if( !mAllRestrictionsList.contains( restriction ) )
                throw IllegalArgumentException("isRestrictionEnabled -> Restriction $restriction is not supported")
            if (enabled)
                mPolicyManager.addUserRestriction(mAdminComponent, restriction)
            else
                mPolicyManager.clearUserRestriction(mAdminComponent, restriction)
            mRestrictionInfos[restriction]!!.enabled = enabled
            return true
        }

        fun isRestrictionEnabled( restriction: String ) : Boolean {
            if( !mAllRestrictionsList.contains( restriction ) )
                throw IllegalArgumentException("isRestrictionEnabled -> Restriction $restriction is not supported")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                return __isRestrictionEnabled(restriction)
            return mRestrictionInfos[restriction]!!.enabled
        }

        private fun __isRestrictionEnabled( restriction: String ) : Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                return try {
                    mPolicyManager.getUserRestrictions( mAdminComponent ).getBoolean( restriction, false )
                } catch (e: SecurityException) { false }
            return false
        }
    }

    inner class Utilities {
        fun wipe() { mPolicyManager.wipeData(0) }
        fun lock() { mPolicyManager.lockNow() }

        @RequiresApi(Build.VERSION_CODES.N)
        fun reboot() { mPolicyManager.reboot(mAdminComponent) }

        fun dropPassword() {
            try {
                clearPasswordPolicies()
                var flags = DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    flags = flags or DevicePolicyManager.RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT
                mPolicyManager.resetPassword("", flags)
            } catch ( e: SecurityException ) {
                Toast.makeText(mRootContext, "Cannot drop password - ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        fun clearPasswordPolicies() {
            with(mPolicyManager) {
                setPasswordHistoryLength(mAdminComponent, 0)
                setPasswordMinimumLength(mAdminComponent, 0)
                setPasswordMinimumLetters(mAdminComponent, 0)
                setPasswordMinimumLowerCase(mAdminComponent, 0)
                setPasswordMinimumNonLetter(mAdminComponent, 0)
                setPasswordMinimumNumeric(mAdminComponent, 0)
                setPasswordMinimumSymbols(mAdminComponent, 0)
                setPasswordMinimumUpperCase(mAdminComponent, 0)
                setPasswordQuality(mAdminComponent, 0)
            }
        }
    }

    fun isDeviceAdminActive(): Boolean {
        return mPolicyManager.isAdminActive(mAdminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return mPolicyManager.isDeviceOwnerApp(mRootContext.packageName)
    }
}

