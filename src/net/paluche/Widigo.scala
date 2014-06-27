package net.paluche.widigo

import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.view.Gravity
import android.view.Menu
import android.widget.{CheckBox, Switch, Space, ListView, DatePicker}
import android.widget.{TextView, Button, LinearLayout, FrameLayout}

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesClient
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.location._
import com.google.android.gms.maps._
import com.google.android.gms.maps.model.{LatLng, MarkerOptions, Polyline}
import com.google.android.gms.maps.model.PolylineOptions

import java.io.IOException
import java.util.Date

import macroid._
import macroid.FullDsl._
import macroid.ActivityContext
import macroid.contrib.ExtraTweaks._
import macroid.util.Ui
import macroid.contrib.Layouts._
import macroid.AppContext
import macroid.LogTag

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.async

class Widigo extends Activity with Contexts[Activity]
    with LocationListener
    with GooglePlayServicesClient.ConnectionCallbacks
    with GooglePlayServicesClient.OnConnectionFailedListener
    with IdGeneration {

  import WidigoUtils._
  // Activity detection handler
  private var detectionRequester:       DetectionRequester    = null
  private var detectionRemover:         DetectionRemover      = null
  private var statusActivityDetection:  Int                   = STATUS_ACTIVITY_DETECTION_UNKNOWN

  private var requestType:              Int                   = -1

  // Location
  private var locationRequest:          LocationRequest       = null
  private var locationClient:           LocationClient        = null
  private var locationClientConnected:  Boolean               = false
  private var locationUpdatesRequested: Boolean               = false

  // Datas of the application
  private var prefs:                    SharedPreferences     = null
  private var dbHelper:                 DbHelper              = null

  // Map
  var map:                              GoogleMap             = null
  // Local position marker
  var marker:                           MarkerOptions         = null
  // List of activities actually displayed
  var widigoActivities:                 List[WidigoActivity]  = null


  /*
   * Buttons actions
   */
  // Content view.
  lazy val trackingOptionButton = {
    startActivity(new Intent(this, classOf[TrackingOptionActivity]))
  }

  lazy val myTracksOptionButton = {
    startActivity(new Intent(this, classOf[MyTracksOptionActivity]))
  }

  /*
   * Layouts
   */
  val optionButtonLayout = l[HorizontalLinearLayout](
    w[Button] <~ text("Tracking Option") <~ On.click(Ui(trackingOptionButton)),
    w[Button] <~ text("My Tracks") <~ On.click(Ui(myTracksOptionButton)))

  val homeLayout = l[FrameLayout](f[MapFragment].framed(Id.map, Tag.map)
    <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

  /*
   * Activity related function
   */
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)

    setContentView(homeLayout.get)

    // Initializes the ActionBar
    var actionBar: ActionBar = getActionBar
    // Hide the title of the app to leave more place for options buttons
    actionBar.setCustomView(optionButtonLayout.get)
    actionBar.setDisplayOptions(
      ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO,
      ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO |
      ActionBar.DISPLAY_HOME_AS_UP  | ActionBar.DISPLAY_SHOW_TITLE)

    // Location
    // Create a new global location parameters object
    locationRequest = LocationRequest.create()

    // Set the update interval
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS)

    // Create a new location client, using the enclosing class to
    // handle callbacks.
    if (locationClient == null)
      locationClient = new LocationClient(this, this, this)

    requestType = REQUEST_TYPE_CONNECT_CLIENT
    locationClient.connect

    // Get a handle to the preferences and the database of the Application
    prefs = getApplicationContext.getSharedPreferences(SHARED_PREFERENCES,
      Context.MODE_PRIVATE)
    dbHelper = new DbHelper(this)

    // Get detection requester and remover objects
    detectionRequester = new DetectionRequester(this)
    detectionRemover   = new DetectionRemover(this)
  }

  override def onStart {
    super.onStart

    // Get the google Map to do things on it
    map = (getFragmentManager().findFragmentById(Id.map))
    .asInstanceOf[MapFragment].getMap();

    if (!servicesConnected) {
      logE"Cannot connect to Google play services"
      return // Can't I do something better?
    }

    if (!locationUpdatesRequested && locationClientConnected) {
      locationClient.requestLocationUpdates(locationRequest, this)
      locationUpdatesRequested = true
    }
  }

  // Register the broadcast receiver and update the log of activity updates
  override def onResume() {
    super.onResume();

    // We launch this code when we start the app and we are back from the
    // Options activities.
    updateTracesOnMap()

    if (locationClientConnected) {
      var currentLocation = locationClient.getLastLocation()
      updateMarkerOnMap(new LatLng(currentLocation.getLatitude, currentLocation.getLongitude))
    }
    if (prefs.getBoolean(KEY_TRACKING_ON, false)) {
      if (statusActivityDetection != STATUS_ACTIVITY_DETECTION_REQUESTED) {
        // Start the requests for activity recognition updates.
        requestType = REQUEST_TYPE_ADD_UPDATES
        statusActivityDetection = STATUS_ACTIVITY_DETECTION_REQUESTED
        if (servicesConnected)
          detectionRequester.requestUpdates()
      }
    } else {
      if (statusActivityDetection != STATUS_ACTIVITY_DETECTION_REMOVED) {
        requestType = REQUEST_TYPE_REMOVE_UPDATES
        statusActivityDetection = STATUS_ACTIVITY_DETECTION_REMOVED
        if (servicesConnected)
        detectionRemover.removeUpdates(detectionRequester.getRequestPendingIntent)
      }
    }
  }

  override def onStop() {
    // The activity is no longer visible
    if (locationUpdatesRequested)
      locationClient.removeLocationUpdates(this)
    locationUpdatesRequested = false
    super.onPause
  }

  /*
   * Connection callback related functions
   */
  override def onConnected(bundle: Bundle) {
    locationClientConnected = true

    // Initialize the marker displaying the current location
    var currentLocation: Location = null
    currentLocation = locationClient.getLastLocation()

    val currentLatLng: LatLng = new LatLng(currentLocation.getLatitude, currentLocation.getLongitude)

    updateMarkerOnMap(currentLatLng)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16))

    if (!locationUpdatesRequested && locationClientConnected) {
      locationClient.requestLocationUpdates(locationRequest, this)
      locationUpdatesRequested = true
    }
  }

  override def onDisconnected() {
    locationClientConnected = false
    if (locationUpdatesRequested)
      locationClient.removeLocationUpdates(this)
    locationUpdatesRequested = false
  }

  /*
   * On connection failed listener related funciton
   */
  override def onConnectionFailed(connectionResult: ConnectionResult) {
    if (connectionResult.hasResolution) {
      connectionResult.startResolutionForResult(this,
        CONNECTION_FAILURE_RESOLUTION_REQUEST)
    }
  }

  /*
   * Location Listener related functions
   */
  override def onLocationChanged(currentLocation: Location) {
    updateMarkerOnMap(new LatLng(currentLocation.getLatitude,
      currentLocation.getLongitude))
  }

  /*
   * Other functions
   */
  def servicesConnected: Boolean = {
    var resultCode: Int = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)

    if (resultCode == ConnectionResult.SUCCESS) {
      logD"Google Play services is available"
      return true
    } else {
      // Display an error dialog
      GooglePlayServicesUtil.showErrorDialogFragment(resultCode, this, 0)
      return false
    }
  }

  // Handle results returned to the FragmentActivity by Google Play Services
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = {
    if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        requestType match {
          case REQUEST_TYPE_CONNECT_CLIENT => locationClient.connect()

          case REQUEST_TYPE_ADD_UPDATES => detectionRequester.requestUpdates()

          case REQUEST_TYPE_REMOVE_UPDATES =>
          detectionRemover.removeUpdates(detectionRequester.getRequestPendingIntent)

          case _ => logD"Unable to resolve"
        }
      }
    } else {
      logD"Received unknown activity request code ${requestCode} in onActivityResult"
    }
  }

  // Update the traces on the Map
  def updateTracesOnMap() {
    map.clear

    marker           = null
    widigoActivities = dbHelper.getActivitiesByDate(
      prefs.getLong(KEY_MY_TRACKS_START_DATE, 0),
      prefs.getLong(KEY_MY_TRACKS_STOP_DATE, (new Date()).getTime))

    if (widigoActivities != null) {
      for (widigoActivity: WidigoActivity <- widigoActivities) {
        map.addPolyline(widigoActivity.polylineOptions)
      }
    }
  }

  def updateMarkerOnMap(latLng: LatLng) {
    if (marker == null) {
      marker = new MarkerOptions()
      marker = marker.position(latLng)
      map.addMarker(marker)
    } else {
      marker = marker.position(latLng)
    }

    if (widigoActivities == null) {
      widigoActivities = List(new WidigoActivity(
        (new PolylineOptions).add(latLng),
        -1,
        DetectedActivity.UNKNOWN,
        null))
    } else {
      // Get the polylineOption of the last activity of the list Activities
      // and add the point. This operation should be done also by the Location
      // Service in the database, in this case we do not draw again the full
      // polylines.
      widigoActivities.last.polylineOptions.add(latLng)
    }
  }
}

/*
 // On onCreate
 // Set the broadcast receiver intent filer
 broadcastManager = LocalBroadcastManager.getInstance(this)

 // Create a new Intent filter for the broadcast receiver
 broadcastFilter = new IntentFilter(ACTION_REFRESH_STATUS_LIST)
 broadcastFilter.addCategory(CATEGORY_LOCATION_SERVICES);

 }

 // Display the activity detection history stored in the

 var updateListReceiver: BroadcastReceiver = new BroadcastReceiver() {
   override def onReceive(context: Context, intent: Intent) {

     // When an Intent is received from the update listener IntentService, update
     // the displayed log.
     updateActivityHistory();
   }
 }
}


// Pop-up dialod for GPS settings

lazy val goToSettings = {
  lazy val intent: Intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
  this.startActivity(intent)
}

val dialogView = l[VerticalLinearLayout] (
  w[TextView] <~ text("Please enable GPS in settings"),
  w[Button] <~ text("Go to Settings") <~ On.click(Ui(goToSettings)))
(dialog(dialogView) <~ title("GPS needed") <~ speak).run
*/
