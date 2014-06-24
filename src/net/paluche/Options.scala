package net.paluche.widigo

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._
import macroid.util.Ui

import android.app.Activity
import android.app.ActionBar
import android.widget.{Switch, TextView, CheckBox, Button, DatePicker}
import android.os.Bundle

class TrackingOption(var trackingOn: Boolean, var trackingStill: Boolean,
  var trackingWalking: Boolean, var trackingRunning: Boolean,
  var trackingInVehicle: Boolean, var trackingOnBicycle : Boolean)

// For tests
object OptionTest {
  var trackingOn:        Boolean = false
  var trackingStill:     Boolean = true
  var trackingWalking:   Boolean = true
  var trackingRunning:   Boolean = true
  var trackingInVehicle: Boolean = true
  var trackingOnBicycle: Boolean = true
}

class TrackingOptionActivity extends Activity
with Contexts[Activity]
with IdGeneration {
  import OptionTest._

  var trackingOnSwitch:  Switch   = null
  var stillCheckbox:     CheckBox = null
  var walkingCheckbox:   CheckBox = null
  var runningCheckbox:   CheckBox = null
  var inVehicleCheckbox: CheckBox = null
  var onBicycleCheckbox: CheckBox = null

  lazy val trackingOptionLayout = l[VerticalLinearLayout](
    w[Switch]   <~ id(Id.trackingOnSwitch) <~ text("Tracking On"),
    w[TextView] <~ text("\n  Specify Activities"),
    w[CheckBox] <~ id(Id.stillCheckBox)     <~ text("Still")      <~ On.click(saveTrackingOption),
    w[CheckBox] <~ id(Id.walkingCheckBox)   <~ text("Walking")    <~ On.click(saveTrackingOption),
    w[CheckBox] <~ id(Id.runningCheckBox)   <~ text("Running")    <~ On.click(saveTrackingOption),
    w[CheckBox] <~ id(Id.inVehicleCheckBox) <~ text("In vehicle") <~ On.click(saveTrackingOption),
    w[CheckBox] <~ id(Id.onBicycleCheckBox) <~ text("On bicycle") <~ On.click(saveTrackingOption))

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    setContentView(trackingOptionLayout.get)

    val actionBar: ActionBar = getActionBar

    actionBar.setDisplayOptions(
      ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE,
      ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM |
      ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE)

    // Get the different switch and checkboxes
    trackingOnSwitch  = findViewById(Id.trackingOnSwitch).asInstanceOf[Switch]
    stillCheckbox     = findViewById(Id.stillCheckBox).asInstanceOf[CheckBox]
    walkingCheckbox   = findViewById(Id.walkingCheckBox).asInstanceOf[CheckBox]
    runningCheckbox   = findViewById(Id.runningCheckBox).asInstanceOf[CheckBox]
    inVehicleCheckbox = findViewById(Id.inVehicleCheckBox).asInstanceOf[CheckBox]
    onBicycleCheckbox = findViewById(Id.onBicycleCheckBox).asInstanceOf[CheckBox]

    // Set the display according
    val trackingOption:TrackingOption = getTrackingOption()

    trackingOnSwitch.setChecked(trackingOption.trackingOn)
    stillCheckbox.setChecked(trackingOption.trackingStill)
    walkingCheckbox.setChecked(trackingOption.trackingWalking)
    runningCheckbox.setChecked(trackingOption.trackingRunning)
    inVehicleCheckbox.setChecked(trackingOption.trackingInVehicle)
    onBicycleCheckbox.setChecked(trackingOption.trackingOnBicycle)
  }

  override def onStop() {
    saveTrackingOption
    super.onStop
  }

  // FIXME The end of the class is designed for tests. The final
  // implementation is to be done.
  def getTrackingOption(): TrackingOption = new TrackingOption(
    trackingOn, trackingStill, trackingWalking, trackingRunning,
    trackingInVehicle, trackingOnBicycle)

  lazy val saveTrackingOption = {
    //  TrackingOption = new TrackingOption(trackingOnSwitch.isChecked,
      //    stillCheckbox.isChecked, walkingCheckbox.isChecked,
      //    runningCheckbox.isChecked, inVehicleCheckbox.isChecked,
      //    onBicycleCheckbox.isChecked)
    trackingOn  = trackingOnSwitch.isChecked
    trackingStill     = stillCheckbox.isChecked
    trackingWalking   = walkingCheckbox.isChecked
    trackingRunning   = runningCheckbox.isChecked
    trackingInVehicle = inVehicleCheckbox.isChecked
    trackingOnBicycle = onBicycleCheckbox.isChecked
    toast("Tracking options saved") <~ fry
  }
}

class MyTracksOptionActivity extends Activity with Contexts[Activity] {
  val myTracksOptionLayout = l[VerticalLinearLayout](
    l[HorizontalLinearLayout](
      w[TextView] <~ text("Start Date"),
      w[DatePicker]),
    l[HorizontalLinearLayout](
      w[TextView] <~ text("Stop Date"),
      w[DatePicker]))

    override def onCreate(b: Bundle) {
      super.onCreate(b)
      setContentView(myTracksOptionLayout.get)

      val actionBar: ActionBar = getActionBar

      actionBar.setDisplayOptions(
        ActionBar.DISPLAY_HOME_AS_UP,
        ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM |
        ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE)
    }
  }


