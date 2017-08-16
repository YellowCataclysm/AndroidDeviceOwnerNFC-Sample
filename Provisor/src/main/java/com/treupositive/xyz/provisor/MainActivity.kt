package com.treupositive.xyz.provisor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    //private val mMessageCallback = NdefMessageCallback()
    private val tag = "NFCProvisor"
    private val LOG = Logger.getLogger(tag)

    private val props = Properties()

    override fun onCreate(savedInstanceState: Bundle?) {
        println("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Log.i(tag, "Adapter = " + mNfcAdapter.toString())
        //Log.i(tag, "CB = " + mMessageCallback.toString())

        initMessage()

        //mNfcAdapter?.setNdefPushMessageCallback(mMessageCallback, this)
        mNfcAdapter?.setNdefPushMessage( initMessage(), this )
    }

    fun initMessage() : NdefMessage? {
        val digest = MessageDigest.getInstance("SHA-1")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        Log.i(tag, "Downloads: " + downloadsDir.path)
        val byteStream = ByteArrayOutputStream()
        try {
            if (downloadsDir != null) {
                val files = downloadsDir.listFiles()
                if( files.size != 1 ) return null
                val pi = this.packageManager.getPackageArchiveInfo(
                        files[0].absolutePath,
                        PackageManager.GET_RECEIVERS )
                if( pi == null ) Log.i(tag, "NULL PACKAGE INFO")
                val apkFile = files[0]

                val apkBytes = apkFile.readBytes()
                digest.update(apkBytes)
                val apkDigest = Base64.encodeToString(digest.digest(), Base64.URL_SAFE)

                Log.i(tag, "Package name: " + pi.packageName)
                pi.receivers.forEach { Log.i(tag, it.permission) }
                val adminReceiver = pi.receivers
                        .firstOrNull { it.permission == android.Manifest.permission.BIND_DEVICE_ADMIN }
                if( adminReceiver == null ) {
                    Log.e(tag, "No Admin Receiver found");
                    return null
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    val adminComponent = ComponentName(
                            adminReceiver.packageName,
                            adminReceiver.name)
                    props.setProperty(
                            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                            adminComponent.flattenToShortString() )
                    Log.i(tag, "Component: " + adminComponent.flattenToShortString())
                }

                props.setProperty(
                        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                        pi.packageName )

                props.setProperty(
                        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM,
                        apkDigest
                )

                props.setProperty(
                        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
                        "http://85.143.210.6/AdminPrototyped.apk"
                )

                props.store(byteStream, "")
                val r = NdefRecord.createMime(
                        DevicePolicyManager.MIME_TYPE_PROVISIONING_NFC,
                        byteStream.toByteArray())
                return NdefMessage(arrayOf(r))
            }
        } catch (e: Exception) {
            Log.e(tag, e.message)
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy")
        mNfcAdapter?.setNdefPushMessageCallback(null, this)
    }

//    inner class NdefMessageCallback: NfcAdapter.CreateNdefMessageCallback {
//        override fun createNdefMessage(event: NfcEvent?): NdefMessage {
//            Log.d(tag, "Create message")
//
//
////            props.setProperty(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID, "\"waflya\"")
////            props.setProperty(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD, "killwifi280494")
////            props.setProperty(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "WPA2");
//
//            //return NdefMessage(arrayOf(msg))
//        }
//
//    }
}
