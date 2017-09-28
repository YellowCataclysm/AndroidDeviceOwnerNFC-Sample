package com.treupositive.xyz.adminprototype

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.Bean
import org.androidannotations.annotations.EActivity
import org.androidannotations.annotations.ViewById

@EActivity
open class MainActivity : AppCompatActivity() {

    @Bean
    protected lateinit var control: DeviceControl

    @ViewById protected lateinit var mAppRecyclerView: RecyclerView
    @ViewById protected lateinit var mRestrictionsSwitch: Switch
    @ViewById protected lateinit var mWipeButton: Button
    @ViewById protected lateinit var mLockButton: Button
    @ViewById protected lateinit var mNullException: Button
    @ViewById protected lateinit var mExit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @AfterViews
    protected fun initialize() {
        mAppRecyclerView.layoutManager = LinearLayoutManager(this)
        mAppRecyclerView.adapter = AppsAdapter(this)

        val isAdminActive = control.isDeviceAdminActive() and control.isDeviceOwner()
        mRestrictionsSwitch.isEnabled = isAdminActive
        mRestrictionsSwitch.setOnCheckedChangeListener { btn, isChecked -> control.User.setOppressionEnabled(isChecked) }

        mWipeButton.isEnabled = isAdminActive
        mWipeButton.setOnClickListener { control.Util.wipe() }

        mLockButton.isEnabled = isAdminActive
        mLockButton.setOnClickListener { control.Util.lock() }

        mNullException.setOnClickListener { throw NullPointerException() }

        mExit.setOnClickListener { System.exit(1); }

    }

    inner class AppsAdapter(var mContext: Context): RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {
        var mInflater: LayoutInflater = LayoutInflater.from(mContext)
        private var mAppsList: List<ApplicationInfo> = control.Apps.installedApps

        override fun getItemCount(): Int { return mAppsList.size }

        override fun onBindViewHolder(holder: AppViewHolder?, position: Int) {
            val app = mAppsList[position]
            holder?.mAppSwitch?.text = app.packageName
            if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                holder?.mAppSwitch?.isChecked = control.Apps.isAppHidden( app )
            } else holder?.mAppSwitch?.isChecked = false
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AppViewHolder {
            val layout = mInflater.inflate( R.layout.app_list_item, parent, false )
            return AppViewHolder(layout)
        }

        inner class AppViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var mAppSwitch: Switch = itemView.findViewById(R.id.app_item_switch) as Switch
            init {
                mAppSwitch.setOnCheckedChangeListener { button, isChecked ->
                    if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                        control.Apps.setAppHidden( mAppsList.get(adapterPosition), isChecked )
                    } else Toast.makeText(mContext,"Not a device owner(Apps hide)",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
