package net.paluche.Widigo

import macroid.LogTag

object WidigoUtils {

  // Used to track what type of request is in process
  val REQUEST_TYPE_ADD_UPDATES    = 0
  val REQUEST_TYPE_REMOVE_UPDATES = 1
  val REQUEST_TYPE_CONNECT_CLIENT = 2

  implicit val logTag = LogTag("Widigo")

  /*
   * Define a request code to send to Google Play services
   * This code is returned in Activity.onActivityResult
   */
  val CONNECTION_FAILURE_RESOLUTION_REQUEST: Int = 9000

  // Location Updates constants
  val UPDATE_INTERVAL_IN_MILLISECONDS       = 60000
  val FAST_INTERVAL_CEILING_IN_MILLISECONDS = 1000

  // ent actions and extras for sending information from the IntentService to the Activity
  val ACTION_CONNECTION_ERROR: String =
    "net.paluche.Widigo.ACTION_CONNECTION_ERROR"

  val ACTION_REFRESH_STATUS_LIST: String =
    "net.paluche.Widigo.ACTION_REFRESH_STATUS_LIST"

  val CATEGORY_LOCATION_SERVICES: String =
    "net.paluche.Widigo.CATEGORY_LOCATION_SERVICES"

  val EXTRA_CONNECTION_ERROR_CODE: String =
    "net.paluche.Widigo.EXTRA_CONNECTION_ERROR_CODE"

  val EXTRA_CONNECTION_ERROR_MESSAGE: String =
    "net.paluche.Widigo.EXTRA_CONNECTION_ERROR_MESSAGE"

  // Constants used to establish the activity update erval
  val  MILLISECONDS_PER_SECOND : Int = 1000

  val  DETECTION_INTERVAL_SECONDS: Int  = 20

  val  DETECTION_INTERVAL_MILLISECONDS: Int  =
    MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS

  // Shared Preferences repository name
  /*
   * Shared Preferences keys
   */
  val SHARED_PREFERENCES: String =
    "net.paluche.Widigo.SHARED_PREFERENCES"

  // Key in the repository for the previous activity
  val KEY_PREVIOUS_ACTIVITY_TYPE: String  =
    "net.paluche.Widigo.KEY_PREVIOUS_ACTIVITY_TYPE"
  val KEY_PREVIOUS_ACTIVITY_ID: String =
    "net.paluche.Widigo.KEY_PREVIOUS_ACTIVITY_ID"

  // Tracking preferences
  val KEY_TRACKING_ON: String =
    "net.paluche.Widigo.KEY_TRACKING_ON"
  val KEY_TRACKING_STILL: String =
    "net.paluche.Widigo.KEY_TRACKING_STILL"
  val KEY_TRACKING_WALKING: String =
    "net.paluche.Widigo.KEY_TRACKING_WALKING"
  val KEY_TRACKING_RUNNING: String =
    "net.paluche.Widigo.KEY_TRACKING_RUNNING"
  val KEY_TRACKING_IN_VEHICULE: String =
    "net.paluche.Widigo.KEY_TRACKING_IN_VEHICULE"
  val KEY_TRACKING_ON_BICYCLE: String =
    "net.paluche.Widigo.KEY_TRACKING_ON_BICYCLE"

  // My tracks to display preferences
  val KEY_MY_TRACKS_START_DATE: String =
    "net.paluche.Widigo.KEY_MY_TRACKS_START_DATE"
  val KEY_MY_TRACKS_STOP_DATE: String =
    "net.paluche.Widigo.KEY_MY_TRACKS_STOP_DATE"

  // Constants for constructing the log file name
  // TODO remove this
  val LOG_FILE_NAME_PREFIX: String = "activityrecognition"
  val LOG_FILE_NAME_SUFFIX: String = ".log"

  // Keys in the repository for storing the log file info
  val KEY_LOG_FILE_NUMBER: String =
    "net.paluche.Widigo.KEY_LOG_FILE_NUMBER"
  val KEY_LOG_FILE_NAME: String =
    "net.paluche.Widigo.KEY_LOG_FILE_NAME"
}
