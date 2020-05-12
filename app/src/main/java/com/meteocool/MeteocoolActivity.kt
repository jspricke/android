package com.meteocool

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.webkit.WebView
import android.widget.*
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener

import com.google.firebase.iid.FirebaseInstanceId
import com.meteocool.location.LocationUpdatesBroadcastReceiver
import com.meteocool.location.UploadLocation
import com.meteocool.location.WebAppInterface
import org.jetbrains.anko.doAsync

import com.meteocool.security.Validator
import com.meteocool.settings.SettingsFragment
import com.meteocool.utility.*
import com.meteocool.view.WebViewModel
import org.jetbrains.anko.defaultSharedPreferences


class MeteocoolActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener,
    WebFragment.WebViewClientListener {

    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(this, LocationUpdatesBroadcastReceiver::class.java)
            intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
            return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    private var  requestingLocationUpdates = false

    /**
     * The entry point to Google Play Services.
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private val webViewModel : WebViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                Log.d(TAG, token)
                Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
            })

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if(Validator.isLocationPermissionGranted(this)) {
            Log.d("Location", "Start Fused")
            requestLocationUpdates()
        }else{
            if(requestingLocationUpdates) {
                stopLocationRequests()
            }
        }

        setContentView(R.layout.activity_meteocool)
        val appVersion = findViewById<TextView>(R.id.app_version)
        appVersion.text = String.format("v %s", applicationContext.packageManager.getPackageInfo(packageName, 0).versionName)

        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, WebFragment()).commit()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        cancelNotifications()

        val drawerItems = listOf(NavDrawerItem(R.drawable.ic_map, getString(R.string.map_header)),
            NavDrawerItem(R.drawable.ic_file, getString(R.string.menu_documentation)))

        val drawerList : ListView = findViewById(R.id.drawer_menu)
        val navAdapter = NavDrawerAdapter(this, R.layout.menu_item, drawerItems)
        drawerList.adapter = navAdapter
        //TODO Maybe merge button in errorfragment with click listener in drawer -> try navigating to WebView
       /* drawerList.onItemClickListener = AdapterView.OnItemClickListener{
            handleWebViewNavigation()
        }  */
        addClickListenerTo(drawerList)
    }

    private fun addClickListenerTo(drawerList: ListView) {
        drawerList.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, pos, id ->
                Log.d(TAG, "{${webViewModel._url.value} + before change")
                supportFragmentManager.popBackStackImmediate()
                val selectedItem = adapterView.adapter.getItem(pos) as NavDrawerItem
                when(selectedItem.menuHeading) {
                    getString(R.string.map_header)->{
                        val lastState = defaultSharedPreferences.getString("map_url", null)
                        if (lastState != null) {
                            webViewModel._url.value = lastState
                        } else {
                            webViewModel._url.value = WebViewModel.MAP_URL
                        }
                    }
                    getString(R.string.menu_documentation) ->{
                        webViewModel._url.value = WebViewModel.DOC_URL
                    }

                }
                Log.d(TAG, "{${webViewModel._url.value} + after change")
            }
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
   private fun requestLocationUpdates() {
        try {
                Log.i(TAG, "Starting location updates")
                mFusedLocationClient.requestLocationUpdates(
                    locationRequest, pendingIntent
                )
            requestingLocationUpdates = true

        } catch (e: SecurityException) {
            e.printStackTrace()
        }

    }

    private fun stopLocationRequests(){
            Log.i(TAG, "Stopping location updates")
        val token = defaultSharedPreferences.getString("fb", "no token")!!
        doAsync {
            NetworkUtility.sendPostRequest(JSONUnregisterNotification(token), NetworkUtility.POST_UNREGISTER_TOKEN)
        }
            mFusedLocationClient.removeLocationUpdates(pendingIntent)

        requestingLocationUpdates = false
    }



    override fun onStart() {
        super.onStart()



        val token = defaultSharedPreferences.getString("fb", "no token")!!
        doAsync {
            NetworkUtility.sendPostRequest(
                JSONClearPost(
                    token,
                    "foreground"
                ),
                NetworkUtility.POST_CLEAR_NOTIFICATION
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cancelNotifications()
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onConnected(p0: Bundle?) {
        Log.i(TAG, "GoogleApiClient connected")
    }

    override fun onConnectionSuspended(p0: Int) {
        val text = "Connection suspended"
        Log.w(TAG, "$text: Error code: $p0")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        val text = "Exception while connecting to Google Play services"
        Log.w(TAG, text + ": " + connectionResult.errorMessage)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when(key){
            "map_mode", "map_rotate" -> {
                Log.i(TAG, "Preference value $key was updated to ${sharedPreferences!!.getBoolean(key, false)} ")
                val webAppInterface = WebAppInterface(this)
                webAppInterface.requestSettings()
            }
            "map_zoom" -> {
                Log.i(TAG, "Preference value $key was updated to ${sharedPreferences!!.getBoolean(key, false)} ")
                if(Validator.isLocationPermissionGranted(this)) {
                    val webAppInterface = WebAppInterface(this)
                    webAppInterface.requestSettings()
                }else{
                    Validator.checkAndroidPermissions(this, this)
                    Toast.makeText(this,"Location permission not granted.", Toast.LENGTH_SHORT).show()
                }
            }
            "notification" -> {
                val isNotificationON = sharedPreferences!!.getBoolean(key, false)
                Log.i(TAG, "Preference value $key was updated to $isNotificationON ")
                if(isNotificationON && !requestingLocationUpdates){
                    requestLocationUpdates()
                }else{
                    if(requestingLocationUpdates) {
                        stopLocationRequests()
                    }
                }
            }
            "notification_intensity"->{
                val intensity = sharedPreferences!!.getString(key, "-1")!!.toInt()
                Log.i(TAG, "Preference value $key was updated to $intensity")
                UploadLocation().execute()
            }
            "notification_time"->{
                val time = sharedPreferences!!.getString(key, "-1")!!.toInt()
                Log.i(TAG, "Preference value $key was updated to $time")
            }
        }
    }

    private fun cancelNotifications(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(notificationManager.activeNotifications.isNotEmpty()) {
            notificationManager.cancelAll()
            val token = defaultSharedPreferences.getString("fb", "no token")!!
            doAsync {
                NetworkUtility.sendPostRequest(
                    JSONClearPost(
                        token,
                        "background"
                    ),
                    NetworkUtility.POST_CLEAR_NOTIFICATION
                )
            }
        }
    }

    private val locationRequest : LocationRequest?
        get(){
           return  LocationRequest.create()?.apply {
                interval = UPDATE_INTERVAL
                fastestInterval = FASTEST_UPDATE_INTERVAL
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                maxWaitTime = MAX_WAIT_TIME
            }
        }


    companion object{

        private val TAG = MeteocoolActivity::class.java.simpleName + "_location"

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL : Long = 15 * 60 * 1000

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value, but they may be less frequent.
         */
        private const val FASTEST_UPDATE_INTERVAL : Long = 5 * 60 * 1000

        /**
         * The max time before batched results are delivered by location services. Results may be
         * delivered sooner than this interval.
         */
        private const val MAX_WAIT_TIME = UPDATE_INTERVAL
    }

    override fun receivedWebViewError() {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, ErrorFragment()).addToBackStack("Test").commit()
    }


}



