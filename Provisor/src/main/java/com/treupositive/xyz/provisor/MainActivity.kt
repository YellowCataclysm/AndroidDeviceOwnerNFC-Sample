package com.treupositive.xyz.provisor

import android.Manifest
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
import android.Manifest.permission
import android.Manifest.permission.WRITE_CALENDAR
import android.content.Context
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private val _readExternalRequestCode = 1001

    private var mNfcAdapter: NfcAdapter? = null
    private val tag = "NFCProvisor"

    private val props = Properties()

    override fun onCreate(savedInstanceState: Bundle?) {
        println("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Log.i(tag, "Adapter = " + mNfcAdapter.toString())

        val canReadExternal = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        if( canReadExternal )
            mNfcAdapter?.setNdefPushMessage( initMessage(), this )
        else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    _readExternalRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if( requestCode == _readExternalRequestCode ) {
            if( grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                mNfcAdapter?.setNdefPushMessage( initMessage(), this )
        }
    }

    private fun initMessage() : NdefMessage? {
        val digest = MessageDigest.getInstance("SHA-1")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        Log.i(tag, "Downloads: " + downloadsDir.path)
        val byteStream = ByteArrayOutputStream()
        try {
            if (downloadsDir != null) {
                val files = downloadsDir.listFiles()
                Log.i(tag, "Listed files")
                files.forEach { Log.i(tag, it.name) }
                if(files.isEmpty()) return null
                val apkFile = files.firstOrNull {
                    it.name.startsWith("Admin", true) && it.name.endsWith("apk")
                } ?: return null
                Log.i(tag, "Using file $apkFile")
                val pi = this.packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.GET_RECEIVERS )
                if( pi == null ) Log.i(tag, "NULL PACKAGE INFO")

                val apkBytes = apkFile.readBytes()
                digest.update(apkBytes)
                val apkDigest = Base64.encodeToString(digest.digest(), Base64.URL_SAFE)
                Log.i(tag, "Digest: " + apkDigest)
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
                        "http://85.143.210.6/${apkFile.name}"
                )

                props.store(byteStream, "")
                Log.i(tag, "Result message $byteStream")
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
}
