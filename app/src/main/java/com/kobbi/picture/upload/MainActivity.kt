package com.kobbi.picture.upload

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val UPLOAD_REQUEST_CODE = 111
        private const val LOAD_IMG_VIEW_REQUEST_CODE = 222
    }

    private val mBackPressedCloser by lazy { BackPressedCloser(this) }
    private val mWebView: WebView by lazy { findViewById<WebView>(R.id.wv_main) }
    private val mProgressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.pb_load) }
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_img_choose.setOnClickListener {
            imageChooser(LOAD_IMG_VIEW_REQUEST_CODE)
        }
//        init()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("####", "onActivityResult() --> $requestCode, resultCode : $resultCode, data : $data")
        when (requestCode) {
            UPLOAD_REQUEST_CODE -> {
                Log.e("####", "onActivityResult() --> data : $data")
                Log.e("####", "onActivityResult() --> uri : ${data?.data}")
                val uri = getResultUri(data)
                val uriArr = if (uri != null) arrayOf(uri) else null
                mFilePathCallback?.onReceiveValue(uriArr)
                mFilePathCallback = null
            }
            LOAD_IMG_VIEW_REQUEST_CODE -> {
                Log.e("####", "onActivityResult() --> data : $data")
                Log.e("####", "onActivityResult() --> uri : ${data?.data}")
                getResultUri(data)?.let { uri ->
                    iv_load_img.setImageBitmap(
                        BitmapFactory.decodeStream(
                            applicationContext.contentResolver.openInputStream(
                                uri
                            )
                        )
                    )
                }
            }
        }

    }

    override fun onBackPressed() {
        mBackPressedCloser.onBackPressed()
    }

    private fun init() {
        mWebView.webViewClient = WebViewClient()
        mWebView.webChromeClient = ChromeClient()
        mWebView.settings.run {
            javaScriptEnabled = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            domStorageEnabled = true
        }
        mWebView.loadUrl("http://172.30.1.43:8080/file/home")
    }

    private fun getResultUri(data: Intent?): Uri? {
        var resultUri: Uri? = null
        if (data != null) {
            data.data?.let { uri ->
                resultUri = if (!uri.toString().startsWith("content://"))
                    Uri.fromFile(File(uri.toString()))
                else
                    uri
            }
        } else {
            Log.e("####", "mCameraPhotoPath : $mCameraPhotoPath")
            if (mCameraPhotoPath != null) {
                resultUri = mCameraPhotoPath
            }
        }
        Log.e("####", "resultUri : $resultUri")
        if (resultUri != null) {
            val file = File("${cacheDir}/tmp_img_${System.currentTimeMillis()}.jpg")
            applicationContext.contentResolver.openInputStream(resultUri!!).use { ois ->
                val bitmap = BitmapFactory.decodeStream(ois)
                Log.e("####","bitmap.byteCount : ${bitmap.allocationByteCount}")
                file.createNewFile()
                file.outputStream().use { fos ->
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                Log.e("####", "file : $file, size : ${file.length()}")
            }
            if (resultUri == mCameraPhotoPath) {
                Log.e("####", "delete uri")
                applicationContext.contentResolver.delete(resultUri!!, null, null)
                mCameraPhotoPath = null
            }
            return Uri.fromFile(file)
        }
        return null
    }

    inner class ChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            mProgressBar.run {
                progress = newProgress
                visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            Log.e("####", "onShowFileChooser() --> filePathCallback : $filePathCallback")
            mFilePathCallback?.onReceiveValue(null)
            mFilePathCallback = filePathCallback
            imageChooser(UPLOAD_REQUEST_CODE)
            return true
        }
    }

    private fun imageChooser(requestCode: Int) {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val resolveActivity = pictureIntent.resolveActivity(packageManager)
        Log.e("####", "resolveActivity : $resolveActivity")
        resolveActivity?.run {
            val fileUri = createImageFile()
            Log.e("####", "call createImageFile() : $fileUri")
            if (fileUri != null) {
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            }
        }
        val selectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, selectionIntent)
            putExtra(Intent.EXTRA_TITLE, "Choose action type")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pictureIntent))
        }
        startActivityForResult(chooserIntent, requestCode)
    }

    private fun createImageFile(): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "tmp_img_$timestamp"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        Log.e("####", "createImageFile() --> contentValues : $contentValues")
        val resolver = applicationContext.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        mCameraPhotoPath = uri
        Log.e("####", "createImageFile() --> uri : $uri")
        return uri
    }
}
