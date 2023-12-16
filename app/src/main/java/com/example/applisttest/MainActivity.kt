package com.example.applisttest

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.applisttest.ui.theme.ApplistTestTheme
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.shizuku.shared.BuildConfig


class MainActivity : ComponentActivity() {
    private var shizuku_enabled = false
    private fun onRequestPermissionsResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        // Do stuff based on the result and the request code
        shizuku_enabled = granted
    }
    // https://github.com/LibChecker/LibChecker/pull/821/files
    private val iPackageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
    }

    private val REQUEST_PERMISSION_RESULT_LISTENER =
        OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            this.onRequestPermissionsResult(
                requestCode,
                grantResult
            )
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        try{
            checkPermission((Int.MIN_VALUE..Int.MAX_VALUE).random())
        }
        catch (e: IllegalStateException) {
            Toast.makeText(this, "Shizuku not installed.", Toast.LENGTH_SHORT).show()
        }
        setContent {
            ApplistTestTheme {
                // A surface container using the 'background' color from the theme
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Shizuku: ${checkPermission((Int.MIN_VALUE..Int.MAX_VALUE).random())}")
                    MessageList(messages = getUserProfiles().map { it.toString() })
                    // https://stackoverflow.com/questions/45203973/nested-foreach-how-to-distinguish-between-inner-and-outer-loop-parameters
                    getUserProfiles().forEach {profile ->
                        Text("UserHandle: $profile")
                        MessageList(messages = getInstalledPackages(0, getIdByUserHandle(profile)).map {pkg -> pkg.packageName })
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }
    private fun checkPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return false
        }
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            false
        } else {
            // Request the permission
            Shizuku.requestPermission(code)
            false
        }
    }

    private fun getPackages(): List<String> {
        return packageManager.getInstalledPackages(0).map { it.packageName }
    }

    private fun getUserProfiles(): List<UserHandle> {
        // https://stackoverflow.com/questions/14749504/android-usermanager-check-if-user-is-owner-admin
        val context = applicationContext
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.userProfiles
    }

    private fun getIdByUserHandle(userHandle: UserHandle): Int {
        return HiddenApiBypass.invoke(UserHandle::class.java, userHandle, "getIdentifier"/*, args*/) as Int
    }

    private fun getUserIds(): List<Int> {
        val profiles = getUserProfiles()
        val ids = profiles.map {
            getIdByUserHandle(it)
        }
        return ids
    }

    private fun getPackagesForUser(userHandle: UserHandle): List<String> {
        try{
            if(!checkPermission((Int.MIN_VALUE..Int.MAX_VALUE).random())){
                return listOf()
            }
        }
        catch (e: IllegalStateException) {
            Toast.makeText(this, "Shizuku not installed.", Toast.LENGTH_SHORT).show()
            return listOf()
        }
        val PACKAGE_MANAGER: IPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        // PACKAGE_MANAGER.getInstalledPackagesAsUser(0, getIdByUserHandle(userHandle))
        // val pkg1 = HiddenApiBypass.invoke(PackageManager::class.java, PACKAGE_MANAGER, "getInstalledPackagesAsUser", 0, getIdByUserHandle(userHandle)) as List<PackageInfo>
        Log.d("ApplistTest", "getPackagesForUser: ${PACKAGE_MANAGER.javaClass.methods.map { it.name }}")
        return listOf()
    }

    private fun atLeastT(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }

    private fun getInstalledPackages(flags: Int, userId: Int): List<PackageInfo> {
        return if (atLeastT()) {
            iPackageManager.getInstalledPackages(flags.toLong(), userId)
        } else {
            iPackageManager.getInstalledPackages(flags, userId)
        }.list
    }

}

@Composable
fun MessageList(messages: List<String>) {
    Column {
        messages.forEach { message ->
            Text(text = message)
        }
    }
}