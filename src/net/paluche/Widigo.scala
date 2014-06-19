package net.paluche.widigo

import macroid._
import macroid.ActivityContext
import macroid.contrib.ExtraTweaks._
import macroid.contrib.Layouts._
import macroid.FullDsl._
import macroid.util.Ui
import macroid.contrib.Layouts._
import macroid.AppContext
import macroid.LogTag

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.async

import android.content.Context
import android.content.Intent
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.Activity
import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.view.Gravity
import android.widget.{ProgressBar, TextView, Button, LinearLayout, FrameLayout}
import android.provider.Settings
import android.content.Context
import android.util.Log
import android.location.Location
import android.graphics.Color

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesClient
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.location._
import com.google.android.gms.maps._
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng


import java.io.IOException

/*
 * This is a test app that connects to Google Play service and retrieve the
 * current position and display it on the screen when we push on a "start"
 * button.
 * This code is inspired from "retrieving the Current Location" code example
 * available at http://developer.android.com/training/location/retrieve-current.html
 */
object Widigo {

  // Constants
  val UPDATE_INTERVAL_IN_MILLISECONDS       = 10000//60000
  val FAST_INTERVAL_CEILING_IN_MILLISECONDS = 1



  // Display variables
  var status:    String = "???"

  // Intent for activity recognition needs
  var broadcastFilter:            IntentFilter          = null
  private var broadcastManager:   LocalBroadcastManager = null
  private var detectionRequester: DetectionRequester    = null
  private var detectionRemover:   DetectionRemover      = null

  // RequstType
  var requestType = -1

  var logFile: LogFile = null
}

class Widigo extends Activity with Contexts[Activity]
    with LocationListener
    with GooglePlayServicesClient.ConnectionCallbacks
    with GooglePlayServicesClient.OnConnectionFailedListener
    with IdGeneration {

  import Widigo._

  /*
   * Local variables
   */
  // Location
  var locationRequest:  LocationRequest  = null
  var locationClient:   LocationClient   = null
  var locationUpdatesResquested: Boolean = false

  // Layout
  var map: GoogleMap = null

  // Local position marker
  var marker: MarkerOptions = new MarkerOptions()

  /*
   * Activity related function
   */
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)

    // Layout
    val subLayout1 = l[FrameLayout](
      f[MapFragment].framed(Id.map, Tag.map)
        <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

    val subLayout2 = l[FrameLayout]()
      //w[ProgressBar])

    val layout = l[FrameLayout](
      subLayout1,
      subLayout2)


    setContentView(layout.get)

    // Requirements
    // Create a new global location parameters object
    locationRequest = LocationRequest.create()

    // Set the update interval
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)

    // Use high accuracy
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    // Set the interval ceiling to one minute
    locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS)

    // Create a new location client, using the enclosing class to
    // handle callbacks.
    locationClient = new LocationClient(this, this, this)

  }

  override def onStart {
    super.onStart

    // Get the google Map to do things on it
    map = (getFragmentManager().findFragmentById(Id.map))
              .asInstanceOf[MapFragment].getMap();
    locationClient.connect

    if (!servicesConnected) {
      logE"Cannot connect to fucking Google play services"
      return // Can't I do something better?
    }

    // Start the requests for activity recognition updates.
    //requestType = ActivityUtils.REQUEST_TYPE_ADD_UPDATES
    //detectionRequester.requestUpdates
  }

 override def onPause {
   // Stop listening to broadcasts when the Activity isn't visible.

   locationClient.requestLocationUpdates(locationRequest, this)

   super.onPause
 }

  /*
   * Connection callback related functions
   */
  override def onConnected(bundle: Bundle) {
    // Initialize the marker displaying the current location
    var currentLocation: Location = null
    while (currentLocation == null)
      currentLocation = locationClient.getLastLocation()

    val currentLatLng: LatLng = new LatLng(currentLocation.getLatitude, currentLocation.getLongitude)

    marker = marker.position(currentLatLng)
    map.addMarker(marker)

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16))

    // Start Location request updates
    locationClient.requestLocationUpdates(locationRequest, this)
    locationUpdatesResquested = true
  }

  override def onDisconnected() {
    if (locationUpdatesResquested)
      locationClient.removeLocationUpdates(this)
    locationUpdatesResquested = false
  }

  /*
   * On connection failed listener related funciton
   */
  override def onConnectionFailed(connectionResult: ConnectionResult) {
    if (connectionResult.hasResolution) {
      connectionResult.startResolutionForResult(this,
        ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST)
    }
  }

  /*
   * Location Listener related functions
   */
  override def onLocationChanged(currentLocation: Location) {
    // TODO diplay the location on the map
      marker.position(new LatLng(currentLocation.getLatitude,
                                        currentLocation.getLongitude))


  }

  /*
   * Other functions
   */
  def servicesConnected: Boolean = {
    requestType = ActivityUtils.REQUEST_TYPE_CONNECT_CLIENT
    var resultCode: Int = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)

    if (resultCode == ConnectionResult.SUCCESS) {
      status = "Google Play services is available"
      logD"${status}"
      return true
    } else {
      // Display an error dialog
      GooglePlayServicesUtil.showErrorDialogFragment(resultCode, this, 0)
      return false
    }
  }

  // Handle results returned to the FragmentActivity by Google Play Services
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = {
    if (requestCode == ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        requestType match {
          case ActivityUtils.REQUEST_TYPE_CONNECT_CLIENT => {
            status = "Error resolved, please re-try operation"
            logD"${status}"
          }

          case ActivityUtils.REQUEST_TYPE_ADD_UPDATES =>
          detectionRequester.requestUpdates()

          case ActivityUtils.REQUEST_TYPE_REMOVE_UPDATES =>
          detectionRemover.removeUpdates(detectionRequester.getRequestPendingIntent)

          case _ => {
            status = "Unable to resolve"
            logD"${status}"
          }
        }
      }
    } else {
      logD"Received unknown activity request code ${requestCode} in onActivityResult"
    }
  }

}

/*
 class Widigo extends Activity with Contexts[Activity]
 // with LocationListener
 with GooglePlayServicesClient.ConnectionCallbacks
 with GooglePlayServicesClient.OnConnectionFailedListener
 with IdGeneration {

   import Widigo._
   import Layout._

   def activityPrintBox(name: Spanned) =
   text(name) + TextSize.large +
   Tweak[TextView]{ _.setGravity(Gravity.CENTER) } +
   lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f)

   override def onCreate(bundle: Bundle) {
     super.onCreate(bundle)

     // Layout
     val layout = l[FrameLayout](
       //w[TextView] <~ text(status) <~ wire(statusTextBox),
       f[MapFragment].framed(Id.map, Tag.map) <~
       lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
     )
   setContentView(layout.get)

   // Requirements
   // Create a new global location parameters object
   locationRequest = LocationRequest.create()

   // Set the update interval
   locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)

   // Use high accuracy
   locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

   // Set the interval ceiling to one minute
   locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS)

   // Create a new location client, using the enclosing class to
   // handle callbacks.
   locationClient = new LocationClient(this, this, this)

   status = "Ready"
   (statusTextBox <~ text(status)).run

   // Set the broadcast receiver intent filer
   broadcastManager = LocalBroadcastManager.getInstance(this)

   // Create a new Intent filter for the broadcast receiver
   broadcastFilter = new IntentFilter(ActivityUtils.ACTION_REFRESH_STATUS_LIST)
   broadcastFilter.addCategory(ActivityUtils.CATEGORY_LOCATION_SERVICES);

   // Get detection requester and remover objects
   detectionRequester = new DetectionRequester(this)
   detectionRemover   = new DetectionRemover(this)

   // Create a new LogFile object
   logFile = LogFile.getInstance(this)
 }

 // Register the broadcast receiver and update the log of activity updates
 override def onResume() {
   super.onResume();

   // Register the broadcast receiver
   broadcastManager.registerReceiver(
     updateListReceiver,
     broadcastFilter);

   // Load updated activity history
   updateActivityHistory();
 }
 // Display the activity detection history stored in the
 // log file
 def updateActivityHistory() {
   // Try to load data from the history file
   try {
     // Load log file records into the List
     var activityDetectionHistory = logFile.loadLogFile()

     // Clear the adapter of existing data
     //mStatusAdapter.clear()

     // Add each element of the history to the adapter
     for (activity: Spanned <- activityDetectionHistory)
       runUi(logBox <~ addViews(List(w[TextView] <~ activityPrintBox(activity))))

     // If the number of loaded records is greater than the max log size
     // And delete the old log file
     if ((mStatusAdapter.getCount() > MAX_LOG_SIZE) &&
       !logFile.removeLogFiles()) {
       // Log an error if unable to delete the log file
       logE"Log file deletion error"
     }

     // Trigger the adapter to update the display
     //mStatusAdapter.notifyDataSetChanged()

   } catch {
     // If an error occurs while reading the history file
     case e: IOException =>
     logE"${e.getMessage}"
   }
 }

 var updateListReceiver: BroadcastReceiver = new BroadcastReceiver() {
   override def onReceive(context: Context, intent: Intent) {

     // When an Intent is received from the update listener IntentService, update
     // the displayed log.
     updateActivityHistory();
   }
 }
}
*/
/*
 * Actually I don't want the requests to stop

 override def onStop {

   if (locationClient.isConnected)
     stopPeriodicUpdates
   locationClient.disconnect
   super.onStop
 }


 def startPeriodicUpdates {
   locationClient.requestLocationUpdates(locationRequest, this)
 }

 def stopPeriodicUpdates {
   locationClient.removeLocationUpdates(this)
 }

 def startUpdates {
   if (servicesConnected)
     startPeriodicUpdates
 }

 def stopUpdates {
   if (servicesConnected)
     stopPeriodicUpdates
 }

 def getLocation {
   if (servicesConnected) {
     val currentLocation: Location = locationClient.getLastLocation()
     if (currentLocation == null) {
       lazy val goToSettings = {
         lazy val intent: Intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
         this.startActivity(intent)
       }
       val dialogView = l[VerticalLinearLayout] (
         w[TextView] <~ text("Please enable GPS in settings"),
         w[Button] <~ text("Go to Settings") <~ On.click(Ui(goToSettings)))
       (dialog(dialogView) <~ title("GPS needed") <~ speak).run

     } else {
       onLocationChanged(currentLocation)
     }
   }
 }

 override def onRestart()

 override def onPause()

 override def onStop()

 override def onDestroy()
 */
