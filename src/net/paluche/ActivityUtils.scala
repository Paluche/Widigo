package net.paluche.widigo

import macroid.LogTag

object ActivityUtils {

  // Used to track what type of request is in process
  val REQUEST_TYPE_ADD_UPDATES    = 0
  val REQUEST_TYPE_REMOVE_UPDATES = 1
  val REQUEST_TYPE_CONNECT_CLIENT = 2

  implicit val logTag = LogTag("ActivitySample")

  /*
   * Define a request code to send to Google Play services
   * This code is returned in Activity.onActivityResult
   */
  val CONNECTION_FAILURE_RESOLUTION_REQUEST: Int = 9000

  // ent actions and extras for sending information from the IntentService to the Activity
  val ACTION_CONNECTION_ERROR: String =
    "com.example.android.activityrecognition.ACTION_CONNECTION_ERROR"

  val  ACTION_REFRESH_STATUS_LIST: String =
    "com.example.android.activityrecognition.ACTION_REFRESH_STATUS_LIST"

  val  CATEGORY_LOCATION_SERVICES: String =
    "com.example.android.activityrecognition.CATEGORY_LOCATION_SERVICES"

  val  EXTRA_CONNECTION_ERROR_CODE: String =
    "com.example.android.activityrecognition.EXTRA_CONNECTION_ERROR_CODE"

  val  EXTRA_CONNECTION_ERROR_MESSAGE: String =
    "com.example.android.activityrecognition.EXTRA_CONNECTION_ERROR_MESSAGE"

  // Constants used to establish the activity update erval
  val  MILLISECONDS_PER_SECOND : Int = 1000

  val  DETECTION_INTERVAL_SECONDS: Int  = 20

  val  DETECTION_INTERVAL_MILLISECONDS: Int  =
    MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS

  // Shared Preferences repository name
  val SHARED_PREFERENCES: String =
    "com.example.android.activityrecognition.SHARED_PREFERENCES"

  // Key in the repository for the previous activity
  val KEY_PREVIOUS_ACTIVITY_TYPE: String  =
    "com.example.android.activityrecognition.KEY_PREVIOUS_ACTIVITY_TYPE"

  // Constants for constructing the log file name
  val LOG_FILE_NAME_PREFIX: String = "activityrecognition"
  val LOG_FILE_NAME_SUFFIX: String = ".log"

  // Keys in the repository for storing the log file info
  val KEY_LOG_FILE_NUMBER: String =
    "com.example.android.activityrecognition.KEY_LOG_FILE_NUMBER"
  val KEY_LOG_FILE_NAME: String =
    "com.example.android.activityrecognition.KEY_LOG_FILE_NAME"
}
