package com.treupositive.xyz.adminprototype

import com.treupositive.xyz.adminprototype.DeviceControl
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.Bean
import org.androidannotations.annotations.EActivity
import org.androidannotations.annotations.ViewById

@EActivity
open class MainActivity : AppCompatActivity() {

    private lateinit var mAppsAdapter: AppsAdapter
    private lateinit var mRestrictionsAdapter: UserRestrictionsAdapter
    private var mPopup: PopupWindow? = null

    @Bean
    protected lateinit var control: DeviceControl

    @ViewById protected lateinit var mRecyclerView: RecyclerView
    @ViewById protected lateinit var mWipeButton: Button
    @ViewById protected lateinit var mLockButton: Button
    @ViewById protected lateinit var mNullException: Button
    @ViewById protected lateinit var mExit: Button
    @ViewById protected lateinit var mButtonsLayout: LinearLayout

    @ViewById protected lateinit var navigation: BottomNavigationView

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.actions_tab_item -> {
                mButtonsLayout.visibility = View.VISIBLE
                mRecyclerView.visibility = View.GONE
                return@OnNavigationItemSelectedListener true
            }
            R.id.apps_hiding_tab_item -> {
                mButtonsLayout.visibility = View.GONE
                mRecyclerView.adapter = mAppsAdapter
                mRecyclerView.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_restrictions_tab_item -> {
                mButtonsLayout.visibility = View.GONE
                mRecyclerView.adapter = mRestrictionsAdapter
                mRecyclerView.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @AfterViews
    protected fun initialize() {
        mAppsAdapter = AppsAdapter(this)
        mRestrictionsAdapter = UserRestrictionsAdapter(this)

        mRecyclerView.layoutManager = LinearLayoutManager(this)

        val isAdminActive = control.isDeviceAdminActive() and control.isDeviceOwner()

        mWipeButton.isEnabled = isAdminActive
        mWipeButton.setOnClickListener { control.Util.wipe() }

        mLockButton.isEnabled = isAdminActive
        mLockButton.setOnClickListener { control.Util.lock() }

        mNullException.setOnClickListener { throw NullPointerException() }

        mExit.setOnClickListener { System.exit(1); }
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    inner class AppsAdapter(var mContext: Context): RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {
        var mInflater: LayoutInflater = LayoutInflater.from(mContext)
        private var mAppsList: List<ApplicationInfo> = control.Apps.installedApps
        private val mAppsByName: Map<String, ApplicationInfo> = emptyMap()

        init {
            val abn: MutableMap<String, ApplicationInfo> = hashMapOf()
            mAppsList.forEach { abn[it.packageName] = it }
        }

        val onSwitchCheckedListener = CompoundButton.OnCheckedChangeListener {
            button, isChecked ->
            if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                val info = mAppsByName[button.text] ?: return@OnCheckedChangeListener
                control.Apps.setAppHidden( info, isChecked )
            } else {
                Toast.makeText(mContext,"Not a device owner(Apps hide)",Toast.LENGTH_SHORT).show()
                if( isChecked ) button.isChecked = false
            }

        }

        override fun getItemCount(): Int = mAppsList.size

        override fun onBindViewHolder(holder: AppViewHolder?, position: Int) {
            val app = mAppsList[position]
            holder?.mAppSwitch?.text = app.packageName
            if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                holder?.mAppSwitch?.isChecked = control.Apps.isAppHidden( app )
            } else holder?.mAppSwitch?.isChecked = false
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AppViewHolder {
            val layout = mInflater.inflate( R.layout.switch_list_item, parent, false )
            return AppViewHolder(layout)
        }

        inner class AppViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var mAppSwitch: Switch = itemView.findViewById(R.id.switch_item_switch) as Switch
            init {
                mAppSwitch.setOnCheckedChangeListener(onSwitchCheckedListener)
            }
        }
    }

    inner class UserRestrictionsAdapter(var mContext: Context): RecyclerView.Adapter<UserRestrictionsAdapter.URViewHolder>() {
        var mInflater: LayoutInflater = LayoutInflater.from(mContext)
        private var mRestrictionsList: List<UserRestrictionInfo> = control.User.restrictionsInfo.values.toList()
        private var mRestrictionsByName: Map<String, UserRestrictionInfo> = emptyMap()
        init {
            val rbn: MutableMap<String, UserRestrictionInfo> = ArrayMap<String, UserRestrictionInfo>(mRestrictionsList.size)
            mRestrictionsList.forEach { rbn[it.name] = it }
            mRestrictionsByName = rbn
        }

        val onSwitchCheckedListener = CompoundButton.OnCheckedChangeListener {
            button, isChecked ->
                if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                    val info = mRestrictionsByName[button.text]
                    if( info != null )
                        control.User.setRestrictionEnabled( info.key, isChecked )
                } else {
                    Toast.makeText(mContext,"Not a device owner(User restrictions)",Toast.LENGTH_SHORT).show()
                    if( isChecked ) button.isChecked = false
                }

        }

        val onHelpButtonClickListener = View.OnClickListener {
            view: View? ->
            if( view == null ) return@OnClickListener

            val parentView = view.parent as RelativeLayout
            val switch = parentView.findViewById( R.id.switch_item_switch ) as Switch? ?: return@OnClickListener

            val r = mRestrictionsByName[switch.text] ?: return@OnClickListener
            showPopup( mInflater, view, r.name, r.description  )
        }

        override fun getItemCount(): Int = mRestrictionsList.size

        override fun onBindViewHolder(holder: URViewHolder?, position: Int) {
            val info = mRestrictionsList[position]
            holder?.mSwitch?.text = info.name
            if( control.isDeviceAdminActive() and control.isDeviceOwner() ) {
                holder?.mSwitch?.isChecked = control.User.isRestrictionEnabled(info.name)
            } else holder?.mSwitch?.isChecked = false
            holder?.mHelpButton?.visibility = View.VISIBLE
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): URViewHolder {
            val layout = mInflater.inflate( R.layout.switch_list_item, parent, false )
            return URViewHolder(layout)
        }

        inner class URViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var mSwitch: Switch = itemView.findViewById(R.id.switch_item_switch) as Switch
            var mHelpButton: Button = itemView.findViewById(R.id.help_button) as Button
            init {
                mSwitch.setOnCheckedChangeListener(onSwitchCheckedListener)
                mHelpButton.setOnClickListener(onHelpButtonClickListener)
            }
        }
    }

    fun showPopup( inflater: LayoutInflater, btn: View, title: String, msg: String) {
        if( mPopup == null ) {
            val popupView = inflater.inflate(R.layout.help_popup, null )
            mPopup = PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT )
            if(Build.VERSION.SDK_INT>=21) mPopup!!.elevation = 5.0f
            val closeBtn = popupView.findViewById(R.id.help_popup_close_button) as ImageButton?
            closeBtn?.setOnClickListener { mPopup!!.dismiss() }
        }
        else
            mPopup!!.dismiss()

        val titleTV = mPopup?.contentView?.findViewById(R.id.popup_title_text) as TextView?
        titleTV?.text = title

        val messageTV = mPopup?.contentView?.findViewById(R.id.popup_message_text) as TextView?
        messageTV?.text = msg

        mPopup!!.showAsDropDown( btn )
    }
}
