package dev.putra.app.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.WebSettings.RenderPriority
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.putra.dev.webview.R
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec


class MainActivity : Activity(), EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {
    private lateinit var mContext: Context
    internal var mLoaded = false

    // set your custom url here
    internal val URL by lazy { intent.extras?.getString(EXTRA_URL).orEmpty() }

    //for attach files
    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    internal var doubleBackToExitPressedOnce = false

    //AdView adView;
    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askPermissionTask()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mContext = this
        mWebView = findViewById<View>(R.id.webview) as WebView
        prgs = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        layoutWebview = findViewById<View>(R.id.layout_webview) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout

        //request for show website
        requestForWebview()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }
    }


    private fun requestForWebview() {

        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                prgs.visibility = View.VISIBLE
                //viewSplash.getBackground().setAlpha(145);
                mWebView.visibility = View.VISIBLE
            }, 3000)

        } else {
            mWebView.visibility = View.VISIBLE
            prgs.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        /** Layout of webview screen View  */
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            prgs.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE

            return
        }
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.setRenderPriority(RenderPriority.HIGH)
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.setAppCacheEnabled(true)
        mWebView.settings.databaseEnabled = true
        //mWebView.getSettings().setDatabasePath(
        //        this.getFilesDir().getPath() + this.getPackageName() + "/databases/");

        // this force use chromeWebClient
        mWebView.settings.setSupportMultipleWindows(false)
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {

                Log.d(TAG, "URL: " + url!!)
                if (internetCheck(mContext)) {
                    // If you wnat to open url inside then use
                    view.loadUrl(url);

                    // if you wanna open outside of app
                    /*if (url.contains(URL)) {
                        view.loadUrl(url)
                        return false
                    }else {
                        // Otherwise, give the default behavior (open in browser)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    }*/
                } else {
                    prgs.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }

                return true
            }

            /* @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(internetCheck(mContext)) {
                    mWebView.setVisibility(View.VISIBLE);
                    layoutNoInternet.setVisibility(View.GONE);
                    //view.loadUrl(url);
                }else{
                    prgs.setVisibility(View.GONE);
                    mWebView.setVisibility(View.GONE);
                    layoutSplash.setVisibility(View.GONE);
                    layoutNoInternet.setVisibility(View.VISIBLE);
                }
                return false;
            }*/

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (prgs.visibility == View.GONE) {
                    prgs.visibility = View.VISIBLE
                }
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mLoaded = true
                if (prgs.visibility == View.VISIBLE)
                    prgs.visibility = View.GONE

                // check if layoutSplash is still there, get it away!
                Handler().postDelayed({
                    //viewSplash.getBackground().setAlpha(255);
                }, 2000)
            }
        }

        //file attach request
        mWebView.webChromeClient = object : WebChromeClient() {
            @SuppressLint("QueryPermissionsNeeded")
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback
                var type = "*/*"
                for (acceptTypes in fileChooserParams.acceptTypes) {
                    val splitTypes = acceptTypes.split(", ?+")
                            .toTypedArray() // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                    for (acceptType in splitTypes) {
                        println("TIPE: $acceptType")
                        type = when (acceptType) {
                            "image/jpg", "image/jpeg", "image/png" -> "image/*"
                            "application/pdf" -> "application/pdf"
                            else -> "*/*"
                        }
                    }
                }
                var takePictureIntent: Intent? = null
                if (type == "image/*") {
                    takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                        // Create the File where the photo should go
                        var photoFile: File? = null
                        try {
                            photoFile = createImageFile()
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                        } catch (ex: IOException) {
                            // Error occurred while creating the File
                            Log.e(TAG, "Unable to create Image File", ex)
                        }

                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile))
                        } else {
                            takePictureIntent = null
                        }
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = type

                val intentArray: Array<Intent?>
                intentArray = if (takePictureIntent != null) {
                    arrayOf(takePictureIntent)
                } else {
                    arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Pilihan Upload")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
    }

    /**
     * Convenience method to set some generic defaults for a
     * given WebView
     */
    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();

        // Enable Javascript
        settings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            settings.setDisplayZoomControls(false);
        }

        // Enable remote debugging via chrome://inspect
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // We set the WebViewClient to ensure links are consumed by the WebView rather
        // than passed to a browser if it can
        mWebView.setWebViewClient(new WebViewClient());
    }*/

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            val yes = getString(R.string.yes)
            val no = getString(R.string.no)
            Toast.makeText(
                    this,
                    getString(
                            R.string.returned_from_app_settings_to_activity,
                            if (hasPermissionCam()) yes else no,
                    ),
                    Toast.LENGTH_LONG
            )
                    .show()
        }

        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                mCameraPhotoPath?.let {
                    GlobalScope.launch {
                        Compressor.compress(this@MainActivity, Uri.parse(it).toFile()) {
                            default(quality = 70)
                            size(2_097_152)
                        }.let {file->
                            results = arrayOf(file.toUri())
                        }
                    }
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        runBlocking {
            delay(1500L)
        }
        mFilePathCallback?.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

    private fun showAdMob() {
        /** Layout of AdMob screen View  */
        /*layoutFooter = (LinearLayout) findViewById(R.id.layout_footer);
          adView = (AdView) findViewById(R.id.adMob);
          try {
           if(internetCheck(mContext)){
               //initializeAdMob();
           }else{
               Log.d("---------","--no internet-");
           }
       }catch (Exception ex){
           Log.d("-----------", ""+ex);
       }*/
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    companion object {

        const val EXTRA_URL = "EXTRA_URL"
        const val RC_PERM = 123
        const val FCR = 1

        internal var TAG = "---MainActivity"
        val INPUT_FILE_REQUEST_CODE = 1
        val EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION"


        //for security
        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun generateKey(): SecretKey {
            val random = SecureRandom()
            val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
            //random.nextBytes(key);
            return SecretKeySpec(key, "AES")
        }

        /*@Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidParameterSpecException::class, IllegalBlockSizeException::class, BadPaddingException::class, UnsupportedEncodingException::class)
        fun encryptMsg(message: String, secret: SecretKey): ByteArray {
            var cipher: Cipher? = null
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher!!.init(Cipher.ENCRYPT_MODE, secret)
            return cipher.doFinal(message.toByteArray(charset("UTF-8")))
        }

        @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidParameterSpecException::class, InvalidAlgorithmParameterException::class, InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class, UnsupportedEncodingException::class)
        fun decryptMsg(cipherText: ByteArray, secret: SecretKey): String {
            var cipher: Cipher? = null
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher!!.init(Cipher.DECRYPT_MODE, secret)
            return String(cipher.doFinal(cipherText), charset("UTF-8"))
        }*/


        /**** Initial AdMob  */
        /**
         * private void initializeAdMob() {
         * Log.d("----","Initial Call");
         * adView.setVisibility(View.GONE);
         * AdRequest adRequest = new AdRequest.Builder()
         * .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
         * //.addTestDevice("F901B815E265F8281206A2CC49D4E432")
         * .build();
         * adView.setAdListener(new AdListener() {
         * @Override
         * public void onAdLoaded() {
         * super.onAdLoaded();
         * runOnUiThread(new Runnable() {
         * @Override
         * public void run() {
         * adView.setVisibility(View.VISIBLE);
         * Log.d("----","Visible");
         * }
         * });
         * }
         * });
         * adView.loadAd(adRequest);
         * }
         */
        /**
         * public static void showAlertDialog(Context mContext, String mTitle, String mBody, int mImage){
         * android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(mContext);
         * builder.setCancelable(true);
         * builder.setIcon(mImage);
         * if(mTitle.length()>0)
         * builder.setTitle(mTitle);
         * if(mBody.length()>0)
         * builder.setTitle(mBody);
         *
         * builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
         * @Override
         * public void onClick(DialogInterface dialog, int which) {
         * dialog.dismiss();
         * }
         * });
         *
         * builder.create().show();
         * } */

        fun internetCheck(context: Context): Boolean {
            var available = false
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (connectivity != null) {
                val networkInfo = connectivity.allNetworkInfo
                if (networkInfo != null) {
                    for (i in networkInfo.indices) {
                        if (networkInfo[i].state == NetworkInfo.State.CONNECTED) {
                            available = true
                            break
                        }
                    }
                }
            }
            return available
        }
    }

    @AfterPermissionGranted(RC_PERM)
    fun askPermissionTask() {
        if (!hasPermissionCam() || !hasPermissionStorage()) {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                    this, getString(R.string.rationale), RC_PERM,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasPermissionCam(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)
    }

    private fun hasPermissionStorage(): Boolean {
        return EasyPermissions.hasPermissions(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d("TAG", "onPermissionsGranted:" + requestCode + ":" + perms.size)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {
        Log.d("TAG", "onRationaleAccepted:$requestCode")
    }

    override fun onRationaleDenied(requestCode: Int) {
        Log.d("TAG", "onRationaleDenied:$requestCode")
    }

}