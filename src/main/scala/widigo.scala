package net.paluche.widigo

import macroid._
import macroid.contrib.ExtraTweaks._
import macroid.contrib.Layouts._
import macroid.Logging._
import macroid.LogTag
import macroid.FullDsl._
import macroid.util.Ui
import macroid.contrib.Layouts._
import macroid.AppContext

import scala.language.postfixOps

import android.content.Context
import android.app.Activity
import android.app.Fragment
import android.app.Service
import android.content.Intent
import android.view.Gravity
import android.location._
import android.os.Bundle
import android.os.IBinder
import android.view.ViewGroup.LayoutParams
import android.widget.{ScrollView, TextView, Button, LinearLayout}
import android.provider.Settings
import android.content.Context

object Widigo {

  implicit val logTag = LogTag("Widigo")
  var locationBox  = slot[TextView]
  var statusBox    = slot[TextView]
  var isGPSEnable: Boolean = false
  var isNetworkEnable: Boolean = false
}

class GPS extends Service with LocationListener {
  import Widigo._

  lazy val locationManager = getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

  isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
  isNetworkEnable = locationManager.isProviderEnabled(
    LocationManager.NETWORK_PROVIDER)

  // Callbacks
  override def onLocationChanged(location: Location) {
    locationBox <~ text(location.toString)
  }

  override def onStatusChanged(provider: String, status: Int,
    extras: Bundle) {
    if (status == LocationProvider.OUT_OF_SERVICE)
      statusBox <~ text("Location out of Service")
    else if(status == LocationProvider.TEMPORARILY_UNAVAILABLE)
      statusBox <~ text("Location temporary unavailable")
    else
      statusBox <~ text("Location available")
  }

  override def onProviderEnabled(provider: String) {

  }

  override def onProviderDisabled(provider: String) {

  }

  override def onBind(arg0: Intent): IBinder  = null

  override def onCreate { locationManager.requestLocationUpdates(
    LocationManager.GPS_PROVIDER, 3000, 0, this)
  }


}

class Widigo extends Activity with Contexts[Activity] {

  import Widigo._

  lazy val gps = new GPS

  def checkRequirements {
    lazy val goToSettings = {
      lazy val intent: Intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
     this.startActivity(intent)

    }

    val dialogView = l[VerticalLinearLayout] (
        w[TextView] <~ text("Please enable GPS in settings"),
        w[Button] <~ text("Go to Settings") <~ On.click(Ui(goToSettings)))


    if (true/*sGPSEnable*/) {
      // Show a dialog box to request for GPS activation
      dialog(dialogView) <~ title("GPS disabled") <~ speak
    }
  }

  lazy val displayDialog = dialog("Test") <~ title("Fuck You") <~ speak
  lazy val displayToast  = toast("Test") <~ fry

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    val view = l[VerticalLinearLayout](
      w[Button] <~ text("Dialog") <~ On.click(displayDialog),
      w[Button] <~ text("Toast") <~ On.click(displayToast),

      w[TextView] <~ text("No location") <~ wire(locationBox))
    setContentView(view.get)

  }

}
