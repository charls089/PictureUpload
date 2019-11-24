package com.kobbi.picture.upload

import android.app.Activity
import android.widget.Toast

class BackPressedCloser(private val mActivity: Activity) {

    private var mBackKeyPressedTime: Long = 0
    private lateinit var mToast: Toast

    fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime > mBackKeyPressedTime + 2000) {
            mBackKeyPressedTime = currentTime
            mToast = Toast.makeText(mActivity, R.string.info_exit_message, Toast.LENGTH_SHORT)
            mToast.show()
        } else {
            mActivity.finish()
            mToast.cancel()
        }
    }
}