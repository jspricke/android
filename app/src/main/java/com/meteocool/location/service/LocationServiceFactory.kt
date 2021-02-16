package com.meteocool.location.service

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability



class LocationServiceFactory {

    companion object {

        fun getLocationService(context : Context) : ForegroundService {
             return if(isGooglePlayServicesAvailable(context)) {
                 FusedLocationService(context)
             }else{
                 FusedLocationService(context) //TODO Implement LocationManager
             }
        }

        private fun isGooglePlayServicesAvailable(context : Context): Boolean {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
            if (status != ConnectionResult.SUCCESS) {
                return false
            }
            return true
        }
    }
}