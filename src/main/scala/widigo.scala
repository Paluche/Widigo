package net.paluche.widigo

import macroid._
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
import android.app.Activity
import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.{ScrollView, TextView, Button, LinearLayout}
import android.provider.Settings
import android.content.Context
import android.util.Log
import android.location.Location

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/*
 * This is a test app that connects to Google Play service and retrieve the
 * current position and display it on the screen when we push on a "start"
 * button.
 * This code is inspired from "retrieving the Current Location" code example
 * available at http://developer.android.com/training/location/retrieve-current.html
 */
object Widigo {
  // Implicit functions
  implicit val logTag = LogTag("Widigo")

  // Constants
  val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
  val UPDATE_INTERVAL_IN_MILLISECONDS       = 5
  val FAST_INTERVAL_CEILING_IN_MILLISECONDS = 1

  // Layout
  var stateTextBox     = slot[TextView]
  var statusTextBox    = slot[TextView]
  var latitudeTextBox  = slot[TextView]
  var longitudeTextBox = slot[TextView]

  var locationRequest: LocationRequest = null
  var locationClient: LocationClient   = null
  var updatesRequested: Boolean        = false
}

class Widigo extends FragmentActivity with Contexts[FragmentActivity]
    with LocationListener
    with GooglePlayServicesClient.ConnectionCallbacks
    with GooglePlayServicesClient.OnConnectionFailedListener
                                           {

  import Widigo._

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    val view = l[VerticalLinearLayout](
      w[Button]   <~ text("Start") <~ On.click(Ui(getLocation)),
      w[TextView] <~ text("Status:"),
      w[TextView] <~ text("??") <~ wire(statusTextBox),
      w[TextView] <~ text("State:"),
      w[TextView] <~ text("??") <~ wire(stateTextBox),
      w[TextView] <~ text("Latitude:"),
      w[TextView] <~ text("??") <~ wire(latitudeTextBox),
      w[TextView] <~ text("Longitude:"),
      w[TextView] <~ text("??") <~ wire(longitudeTextBox)
    )

    setContentView(view.get)

    // Create a new global location parameters object
    locationRequest = LocationRequest.create();

    /*
     * Set the update interval
     */
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

    // Use high accuracy
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    // Set the interval ceiling to one minute
    locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

    /*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     */
    locationClient = new LocationClient(this, this, this);
    (stateTextBox <~ text("Ready")).run
  }

  override def onStart {
    super.onStart
    locationClient.connect
  }

  override def onPause {
    super.onPause
  }

  override def onStop {
    locationClient.disconnect
    super.onStop
  }

  /*
   * Handle results returned to the FragmentActivity by Google Play Services
   */
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = {
    if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        logD"Error resolved, please re-try operation"
        runUi(
          stateTextBox <~ text("Client connected"),
          statusTextBox <~ text("Error resolved, please re-try operation"))
      } else {
        logD"Google Play services: unable to resolve connection error"
        runUi(
          stateTextBox <~ text("Client disconnected"),
          statusTextBox <~ text("Google Play services: unable to resolve connection error"))
      }
    } else {
      logD"Received unknown activity request code ${requestCode} in onActivityResult"
    }
  }

  override def onConnected(bundle: Bundle) {
    (statusTextBox <~ text("Client connected")).run
  }

  override def onDisconnected() {
    (statusTextBox <~ text("Client disconnected")). run
  }

  override def onLocationChanged(currentLocation: Location) {
    runUi(
      statusTextBox    <~ text("LocationUpdated"),
      latitudeTextBox  <~ text(s"${currentLocation.getLatitude}"),
      longitudeTextBox <~ text(s"${currentLocation.getLongitude}"))
  }

  override def onConnectionFailed(connectionResult: ConnectionResult) {
    if (connectionResult.hasResolution) {
      connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST)
    }
  }


  def servicesConnected: Boolean = {
    var resultCode: Int = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)

    if (resultCode == ConnectionResult.SUCCESS) {
      logD"Google Play services is available"
      (statusTextBox <~ text("Google Play services is available")).run
      return true
    } else {
      // Display an error dialog
      GooglePlayServicesUtil.showErrorDialogFragment(resultCode, this, 0)
      return false
    }
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
        runUi(
          statusTextBox    <~ text("Location retreived"),
          latitudeTextBox  <~ text(s"${currentLocation.getLatitude}"),
          longitudeTextBox <~ text(s"${currentLocation.getLongitude}"))
      }
    }
  }

  /*
   override def onRestart()

   override def onResume()

   override def onPause()

   override def onStop()

   override def onDestroy()
   */
}
