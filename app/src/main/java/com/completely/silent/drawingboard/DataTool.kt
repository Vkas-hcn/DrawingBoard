package com.completely.silent.drawingboard

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.completely.silent.drawingboard.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface ViewPager2Provider {
    fun getViewPager2(): ViewPager2
}
object DataTool {
    var mirrorImage = 0
    var isFirstKey = "svsdfv"
    var isFirstMainKey = "wefwef"
    //图片列表
    val imageList = listOf(
        R.drawable.ic_mirrir_1,
        R.drawable.ic_mirrir_2,
        R.drawable.ic_mirrir_3,
        R.drawable.ic_mirrir_4,
        R.drawable.ic_mirrir_5,
        R.drawable.ic_mirrir_6,
        R.drawable.ic_mirrir_7,
        R.drawable.ic_mirrir_8,
        R.drawable.ic_mirrir_9,
    )
    private fun openHomeScreenSettings2(mActivity: MainActivity) {
        val intent = Intent()
        intent.setAction("android.settings.HOME_SETTINGS")
        if (mActivity.packageManager.queryIntentActivities(intent, 0).size > 0) {
            mActivity.startActivity(intent)
            openSystemSettings(mActivity)
        } else {
            Toast.makeText(mActivity, "The settings page cannot be opened", Toast.LENGTH_SHORT).show()
        }
    }

    fun openHomeScreenSettings(mActivity: MainActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = mActivity.getSystemService(RoleManager::class.java)
            mActivity.startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 100)
            openSystemSettings(mActivity)
        }else{
            openHomeScreenSettings2(mActivity)
        }
    }
    private fun openSystemSettings(mActivity: MainActivity) {
        mActivity.lifecycleScope.launch(Dispatchers.Main) {
            delay(400)
            val intent = Intent(mActivity, DialogActivity::class.java)
            mActivity.startActivity(intent)
        }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager? = getRoleManager(context)
            return roleManager?.isRoleHeld(RoleManager.ROLE_HOME)?:false
        }else{
            return isDefaultLauncher2(context)
        }
        return false
    }
    private fun isDefaultLauncher2(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val packageManager = context.packageManager
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            val defaultLauncherPackage = resolveInfo.activityInfo.packageName
            val currentPackage = context.packageName
            return defaultLauncherPackage == currentPackage
        }
        return false
    }

    var mRoleManager: RoleManager?=null
    fun getRoleManager(context: Context): RoleManager? {
        var roleManager: RoleManager? = mRoleManager
        if (roleManager==null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            }
            mRoleManager = roleManager
        }
        return mRoleManager
    }
}