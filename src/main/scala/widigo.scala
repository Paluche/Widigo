package net.paluche.widigo

// Macroid imports
import macroid._
import macroid.Logging._

import android.content.Context
import android.app.Activity
import android.view.Gravity

import android.location._
import android.os.Bundle

object Widigo {

  implicit val tag = LogTag("Widigo")

}

class Widigo extends Activity with Contexts[Activity] {

  import Widigo._

  lazy val locationManager =
    getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

  val locationListener = new LocationListener {
    // Callbacks
    override def onLocationChanged(location: Location) {
      logD"Location changed"
      // TODO
    }

    override def onStatusChanged(provider: String, status: Int,
      extras: Bundle) {
      logD"Status changed"
      // TODO
    }

    override def onProviderEnabled(provider: String) {
      logD"Provider enabled"
      // TODO
    }

    override def onProviderDisabled(provider: String) {
      logE"Provider disabled"
      // TODO
    }
  }

  // Start the Location Listener
  locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
    locationListener)
}
