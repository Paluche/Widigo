/*
 * The Location Listener cannot work with an IntentService, we need to have a
 * Service for it.
 */
package net.paluche.widigo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.location.Location
import android.os.Bundle
import android.os.IBinder

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener
import com.google.android.gms.location._

class LocationService extends Service with LocationListener
    with ConnectionCallbacks
    with OnConnectionFailedListener {
  import WidigoUtils._

  var locationClient:   LocationClient  = null
  var locationRequest:  LocationRequest = null

  /*
   * Service related function
   */
  override def onBind(intent: Intent): IBinder = null

  override def onStart(intent: Intent, startId: Int) {
    // set location request
    locationRequest = LocationRequest.create()
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS)

    locationClient = new LocationClient(this, this, this)
    locationClient.connect
  }

  override def onDestroy() {
    locationClient.removeLocationUpdates(this)
    locationClient.disconnect
  }

  /*
   * LocationListener related function
   */
  def onLocationChanged(currentLocation: Location) {
    // Get a handle on the preferences of the application
    var prefs: SharedPreferences = getApplicationContext.getSharedPreferences(
              SHARED_PREFERENCES, Context.MODE_PRIVATE)
    if (prefs.contains(KEY_PREVIOUS_ACTIVITY_TYPE) &&
      prefs.contains(KEY_PREVIOUS_ACTIVITY_ID)) {

      // Get a handle on the database of the application
      var dbHelper: DbHelper = new DbHelper(this)

      // Push to database
      dbHelper.addActivityEntry(
        currentLocation,
        prefs.getInt(KEY_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN),
        prefs.getInt(KEY_PREVIOUS_ACTIVITY_ID, -1))
    }
  }

  def onProviderDisabled(provider: String) {
    // TODO pop a notification for asking the user to put back on the GPS
  }

  /*
   * Connection callback related functions
   */
  override def onConnected(bundle: Bundle) {
    // Start Location request updates
    locationClient.requestLocationUpdates(locationRequest, this)
  }

  override def onDisconnected() {
  }

  /*
   * OnConnectionFailedListener related functions
   */
  override def onConnectionFailed(connectionResult: ConnectionResult) { }
}
