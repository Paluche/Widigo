package net.paluche.widigo

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._
import macroid.util.Ui

import android.app.Activity
import android.app.ActionBar
import android.widget.{Switch, TextView, CheckBox, Button, DatePicker}
import android.os.Bundle

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Calendar

import scala.util.control._

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

  var startDate: Long = (new Date(114, 0, 1)).getTime//(new GregorianCalendar(14, 1, 1)).getGregorianChange.getTime
  var stopDate: Long = (new Date()).getTime()
}

class TrackingOptionActivity extends Activity
with Contexts[Activity]
with IdGeneration {
  import OptionTest._

  var trackingOnSwitch:  Switch   = null
  var stillCheckBox:     CheckBox = null
  var walkingCheckBox:   CheckBox = null
  var runningCheckBox:   CheckBox = null
  var inVehicleCheckBox: CheckBox = null
  var onBicycleCheckBox: CheckBox = null

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

    // Set the display according
    val trackingOption:TrackingOption = getTrackingOption()

    // Get the different switch and checkboxes
    trackingOnSwitch  = findViewById(Id.trackingOnSwitch).asInstanceOf[Switch]
    stillCheckBox     = findViewById(Id.stillCheckBox).asInstanceOf[CheckBox]
    walkingCheckBox   = findViewById(Id.walkingCheckBox).asInstanceOf[CheckBox]
    runningCheckBox   = findViewById(Id.runningCheckBox).asInstanceOf[CheckBox]
    inVehicleCheckBox = findViewById(Id.inVehicleCheckBox).asInstanceOf[CheckBox]
    onBicycleCheckBox = findViewById(Id.onBicycleCheckBox).asInstanceOf[CheckBox]

    trackingOnSwitch.setChecked(trackingOption.trackingOn)
    stillCheckBox.setChecked(trackingOption.trackingStill)
    walkingCheckBox.setChecked(trackingOption.trackingWalking)
    runningCheckBox.setChecked(trackingOption.trackingRunning)
    inVehicleCheckBox.setChecked(trackingOption.trackingInVehicle)
    onBicycleCheckBox.setChecked(trackingOption.trackingOnBicycle)
  }

  override def onStop() {
    saveTrackingOption
    super.onStop
  }

  // FIXME The end of the class is designed for tests. The final
  // implementation is to be done.
  // TODO get the preferences
  def getTrackingOption(): TrackingOption = new TrackingOption(
    trackingOn, trackingStill, trackingWalking, trackingRunning,
    trackingInVehicle, trackingOnBicycle)

  lazy val saveTrackingOption = {
    //  TrackingOption = new TrackingOption(trackingOnSwitch.isChecked,
    //    stillCheckBox.isChecked, walkingCheckBox.isChecked,
    //    runningCheckBox.isChecked, inVehicleCheckBox.isChecked,
    //    onBicycleCheckBox.isChecked)
    // TODO save in preferences
    trackingOn        = trackingOnSwitch.isChecked
    trackingStill     = stillCheckBox.isChecked
    trackingWalking   = walkingCheckBox.isChecked
    trackingRunning   = runningCheckBox.isChecked
    trackingInVehicle = inVehicleCheckBox.isChecked
    trackingOnBicycle = onBicycleCheckBox.isChecked
    toast("Tracking options saved") <~ fry
  }
}

class MyTracksOptionActivity extends Activity
with Contexts[Activity]
with IdGeneration {
  import OptionTest._

  var startDateButton: Button = null
  var stopDateButton: Button  = null

  var dateFormat: SimpleDateFormat = null

  var dateDialogLayout = w[DatePicker]
  lazy val datePicker: DatePicker = dateDialogLayout.get.asInstanceOf[DatePicker]

  def startDateButtonClick: Ui[Any] = {
    var start = Calendar.getInstance()
    start.setTimeInMillis(startDate)

    dateDialogLayout.get.asInstanceOf[DatePicker].init(start.get(Calendar.YEAR),
      start.get(Calendar.MONTH), start.get(Calendar.DATE), null)

    dialog(dateDialogLayout) <~ title("Start date") <~
      positiveYes(Ui(saveMyTracksOption(Id.startDatePickerDialog))) <~ speak
  }

  lazy val stopDatePickerDialog = dialog(w[DatePicker] <~
    id(Id.stopDatePicker)) <~ title("Stop date") <~
    positiveYes(Ui(saveMyTracksOption(Id.stopDatePickerDialog))) <~ speak

  lazy val myTracksOptionLayout = l[VerticalLinearLayout](
      w[TextView] <~ text("Start Date:"),
      w[Button]   <~ id(Id.startDateButton) <~ On.click(startDateButtonClick),
      w[TextView] <~ text("\nStop Date:"),
      w[Button]   <~ id(Id.stopDateButton) <~ On.click(stopDatePickerDialog)
    )

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    setContentView(myTracksOptionLayout.get)

    val actionBar: ActionBar = getActionBar
    actionBar.setDisplayOptions(
      ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE,
      ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM |
      ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE)

    startDateButton = findViewById(Id.startDateButton).asInstanceOf[Button]
    stopDateButton  = findViewById(Id.stopDateButton).asInstanceOf[Button]
    // Set the date slot to the saved date from preferences.
    // TODO get the preferences.
    try {
      dateFormat = DateFormat.getDateTimeInstance().asInstanceOf[SimpleDateFormat]
    } catch {
      case NonFatal(exc) => logE"Internat error: date formatting exeption."
    }

    dateFormat.applyPattern("yyyy-MM-dd")
    startDateButton.setText(dateFormat.format(new Date(startDate)))
    stopDateButton.setText(dateFormat.format(new Date(stopDate)))
  }

  def saveMyTracksOption(datePickerId: Int) {
    // TODO retrieve dates preferences
    // Update date preferences
    if (datePickerId == Id.startDatePicker) {
      startDate = (new GregorianCalendar(datePicker.getYear, datePicker.getMonth,
        datePicker.getDayOfMonth)).getGregorianChange.getTime
    } else if (datePickerId == Id.stopDatePicker) {
      stopDate = (new GregorianCalendar(datePicker.getYear, datePicker.getMonth,
        datePicker.getDayOfMonth)).getGregorianChange.getTime
    }

    // Update display
    startDateButton.setText(dateFormat.format(new Date(startDate)))
    stopDateButton.setText(dateFormat.format(new Date(stopDate)))
  }
}
