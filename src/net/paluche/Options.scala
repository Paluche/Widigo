package net.paluche.Widigo

import macroid._
import macroid.contrib.Layouts._
import macroid.FullDsl._
import macroid.util.Ui

import android.app.Activity
import android.app.ActionBar
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.widget.{Switch, TextView, CheckBox, Button, DatePicker}

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

import scala.util.control._

object Options {
  var prefs: SharedPreferences = null
}

class TrackingOption(var trackingOn: Boolean, var trackingStill: Boolean,
  var trackingWalking: Boolean, var trackingRunning: Boolean,
  var trackingInVehicle: Boolean, var trackingOnBicycle : Boolean)

class TrackingOptionActivity extends Activity
with Contexts[Activity]
with IdGeneration {
  import Options._
  import WidigoUtils._

  var trackingOnSwitch:  Switch   = null
  var stillCheckBox:     CheckBox = null
  var walkingCheckBox:   CheckBox = null
  var runningCheckBox:   CheckBox = null
  var inVehicleCheckBox: CheckBox = null
  var onBicycleCheckBox: CheckBox = null

  lazy val trackingOptionLayout = l[VerticalLinearLayout](
    w[Switch]   <~ id(Id.trackingOnSwitch) <~ text("Tracking On"),
    w[TextView] <~ text("\n  Specify Activities"),
    w[CheckBox] <~ id(Id.stillCheckBox)     <~ text("Still"),
    w[CheckBox] <~ id(Id.walkingCheckBox)   <~ text("Walking"),
    w[CheckBox] <~ id(Id.runningCheckBox)   <~ text("Running"),
    w[CheckBox] <~ id(Id.inVehicleCheckBox) <~ text("In vehicle"),
    w[CheckBox] <~ id(Id.onBicycleCheckBox) <~ text("On bicycle"))

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
  def getTrackingOption(): TrackingOption = {
    if (prefs == null)
      prefs = getApplicationContext.getSharedPreferences(
      SHARED_PREFERENCES,
      Context.MODE_PRIVATE)

    // Check if the Keys exist
    if ((!prefs.contains(KEY_TRACKING_ON))        ||
      (!prefs.contains(KEY_TRACKING_STILL))       ||
      (!prefs.contains(KEY_TRACKING_WALKING))     ||
      (!prefs.contains(KEY_TRACKING_RUNNING))     ||
      (!prefs.contains(KEY_TRACKING_IN_VEHICULE)) ||
      (!prefs.contains(KEY_TRACKING_ON_BICYCLE))) {
      var editor: Editor = prefs.edit()

      if (!prefs.contains(KEY_TRACKING_ON))
        editor.putBoolean(KEY_TRACKING_ON, false)

      if (!prefs.contains(KEY_TRACKING_STILL))
        editor.putBoolean(KEY_TRACKING_STILL, true)

      if (!prefs.contains(KEY_TRACKING_WALKING))
        editor.putBoolean(KEY_TRACKING_WALKING, true)

      if (!prefs.contains(KEY_TRACKING_RUNNING))
        editor.putBoolean(KEY_TRACKING_RUNNING, true)

      if (!prefs.contains(KEY_TRACKING_IN_VEHICULE))
        editor.putBoolean(KEY_TRACKING_IN_VEHICULE, true)

      if (!prefs.contains(KEY_TRACKING_ON_BICYCLE))
        editor.putBoolean(KEY_TRACKING_ON_BICYCLE, true)

      editor.commit()
    }

    new TrackingOption(
      prefs.getBoolean(KEY_TRACKING_ON, false),
      prefs.getBoolean(KEY_TRACKING_STILL, false),
      prefs.getBoolean(KEY_TRACKING_WALKING, false),
      prefs.getBoolean(KEY_TRACKING_RUNNING, false),
      prefs.getBoolean(KEY_TRACKING_IN_VEHICULE, false),
      prefs.getBoolean(KEY_TRACKING_ON_BICYCLE, false))
  }

  lazy val saveTrackingOption = {
    var editor: Editor = prefs.edit()
    editor.putBoolean(KEY_TRACKING_ON, trackingOnSwitch.isChecked)
    editor.putBoolean(KEY_TRACKING_STILL, stillCheckBox.isChecked)
    editor.putBoolean(KEY_TRACKING_WALKING, walkingCheckBox.isChecked)
    editor.putBoolean(KEY_TRACKING_RUNNING, runningCheckBox.isChecked)
    editor.putBoolean(KEY_TRACKING_IN_VEHICULE, inVehicleCheckBox.isChecked)
    editor.putBoolean(KEY_TRACKING_ON_BICYCLE, onBicycleCheckBox.isChecked)
    editor.commit()
  }
}

class MyTracksOption(var startDate: Long, var stopDate: Long)

class MyTracksOptionActivity extends Activity
with Contexts[Activity]
with IdGeneration {
  import Options._
  import WidigoUtils._

  var startDateButton: Button = null
  var stopDateButton: Button  = null

  var dateFormat: SimpleDateFormat = null

  var dateDialogLayout = w[DatePicker]
  lazy val datePicker: DatePicker = dateDialogLayout.get.asInstanceOf[DatePicker]

  def startDateButtonClick: Ui[Any] = {
    var start = Calendar.getInstance()

    var myTracksOption: MyTracksOption = getMyTracksOption()
    start.setTimeInMillis(myTracksOption.startDate)

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
    w[Button]   <~ id(Id.stopDateButton) <~ On.click(stopDatePickerDialog))

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
    try {
      dateFormat = DateFormat.getDateTimeInstance().asInstanceOf[SimpleDateFormat]
    } catch {
      case NonFatal(exc) => logE"Internat error: date formatting exeption."
    }

    // Get the preferences.
    if (prefs == null)
      prefs = getApplicationContext.getSharedPreferences(
      SHARED_PREFERENCES,
      Context.MODE_PRIVATE)
    var myTracksOption: MyTracksOption = getMyTracksOption()

    dateFormat.applyPattern("yyyy-MM-dd")
    startDateButton.setText(dateFormat.format(new Date(myTracksOption.startDate * 1000)))
    stopDateButton.setText(dateFormat.format(new Date(myTracksOption.stopDate * 1000)))
  }

  def getMyTracksOption(): MyTracksOption = {
    // Check if the Keys exist
    if ((!prefs.contains(KEY_MY_TRACKS_START_DATE)) ||
      (!prefs.contains(KEY_MY_TRACKS_STOP_DATE))) {
      var editor: Editor = prefs.edit()

      if (!prefs.contains(KEY_MY_TRACKS_START_DATE))
        // Default: January 1, 2014
        editor.putLong(KEY_MY_TRACKS_START_DATE, 1388530800)

      if (!prefs.contains(KEY_MY_TRACKS_STOP_DATE))
        // Default: invalid value to keep the stop at the current day.
        editor.putLong(KEY_MY_TRACKS_STOP_DATE, -1)

      editor.commit()
    }

    var stopDate: Long = prefs.getLong(KEY_MY_TRACKS_STOP_DATE,
      -1)

    if (stopDate == -1)
      stopDate = (new Date).getTime()/1000

    new MyTracksOption(prefs.getLong(KEY_MY_TRACKS_START_DATE,
      1388530800), stopDate)
  }

  def saveMyTracksOption(datePickerId: Int) {
    var myTracksOption: MyTracksOption = getMyTracksOption()

    // Update date preferences
    if (datePickerId == Id.startDatePicker) {
      myTracksOption.startDate = (new GregorianCalendar(datePicker.getYear,
        datePicker.getMonth,
        datePicker.getDayOfMonth)).getGregorianChange.getTime
    } else if (datePickerId == Id.stopDatePicker) {
      myTracksOption.stopDate = (new GregorianCalendar(datePicker.getYear,
        datePicker.getMonth,
        datePicker.getDayOfMonth)).getGregorianChange.getTime
    }

    // Update display
    startDateButton.setText(dateFormat.format(new Date(myTracksOption.startDate * 1000)))
    stopDateButton.setText(dateFormat.format(new Date(myTracksOption.stopDate * 1000)))
  }
}
