/*
 * This service receives ActivityRecognition and Location updates. It receives
 * updates in the background, even if the main Activity is not visible.
 * Inspired from the google demonstration code, available at:
 * http://developer.android.com/training/location/activity-recognition.html
 *
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paluche.widigo

import com.google.android.gms.location._

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.NotificationCompat

import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks

import macroid.FullDsl._

import scala.util.control._

class ActivityRecognitionIntentService
extends IntentService("WidigoIntentService") with ConnectionCallbacks {

  import WidigoUtils._

  // Preferences
  var prefs: SharedPreferences = null

  // Location
  var locationClient: LocationClient = null

  override def onHandleIntent(intent: Intent) {
    // Get a handle to the repository
    prefs = getApplicationContext.getSharedPreferences(SHARED_PREFERENCES,
      Context.MODE_PRIVATE)

    if (prefs.getBoolean(KEY_TRACKING_ON, false)) {
      // Tracking activated

      // If the intent contains an Activity update
      if (ActivityRecognitionResult.hasResult(intent)) {
        // Get the update
        var result: ActivityRecognitionResult = ActivityRecognitionResult.extractResult(intent)

        // Get the most probable activity from the list of activities in the update
        var currentActivity: DetectedActivity = result.getMostProbableActivity()

        // Get the confidence percentage for the most probable activity
        var currentActivityConfidence: Int = currentActivity.getConfidence()

        // Get the type of activity
        var currentActivityType: Int = currentActivity.getType()

        // Check if the tracking is activated for this kind of activity
        if (!(currentActivityType match {
            case DetectedActivity.TILTING | DetectedActivity.UNKNOWN =>
            false
            case DetectedActivity.STILL      => prefs.getBoolean(
              KEY_TRACKING_STILL,      false)
            case DetectedActivity.ON_FOOT    => prefs.getBoolean(
              KEY_TRACKING_ON_FOOT,    false)
            case DetectedActivity.WALKING    => prefs.getBoolean(
              KEY_TRACKING_WALKING,    false)
            case DetectedActivity.RUNNING    => prefs.getBoolean(
              KEY_TRACKING_RUNNING,    false)
            case DetectedActivity.IN_VEHICLE => prefs.getBoolean(
              KEY_TRACKING_IN_VEHICLE, false)
            case DetectedActivity.ON_BICYCLE => prefs.getBoolean(
              KEY_TRACKING_ON_BICYCLE, false)
          }))
        return

        if (!prefs.contains(KEY_PREVIOUS_ACTIVITY_TYPE) ||
          !prefs.contains(KEY_PREVIOUS_ACTIVITY_ID)) {

          // This is the first type an activity has been detected. Store the type
          // and initialise the ID

          // Retrieve from the database the type and ID of the last
          // activity saved
          var dbHelper: DbHelper = new DbHelper(this)
          var widigoActivity: WidigoActivity = dbHelper.getLastActivityIdAndType()
          dbHelper.close

          var editor: Editor = prefs.edit()
          if (widigoActivity == null) {
            editor.putInt(KEY_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN)
            editor.putInt(KEY_PREVIOUS_ACTIVITY_ID,   0)

          } else {
            editor.putInt(KEY_PREVIOUS_ACTIVITY_TYPE, widigoActivity.activityType)
            editor.putInt(KEY_PREVIOUS_ACTIVITY_ID,   widigoActivity.activityID)
          }
          editor.commit()
        }

        var previousActivityType: Int = prefs.getInt(KEY_PREVIOUS_ACTIVITY_TYPE,
          DetectedActivity.UNKNOWN)

        // Activity changed
        if (previousActivityType != currentActivityType) {
          if (!isMoving(previousActivityType) && isMoving(currentActivityType)) {
            //Start location updates service
            startService(new Intent(this, classOf[LocationService]))

          } else if (isMoving(previousActivityType) && !isMoving(currentActivityType)) {
            // Stop location updates service
            stopService(new Intent(this, classOf[LocationService]))
          }

          // Increase Activity identifier
          var editor: Editor = prefs.edit()
          editor.putInt(KEY_PREVIOUS_ACTIVITY_ID,
            prefs.getInt(KEY_PREVIOUS_ACTIVITY_ID, -1) + 1)


          // Retrieve the last Location known
          // And push the data in the database
          // This will be done in the onConnect callback since we only use the
          // location Client for it.
          locationClient = new LocationClient(this, this, null)
          locationClient.connect
        }
      }
    }
  }

  /*
   * Connection callbacks related functions
   */
  override def onConnected(bundle: Bundle) {
    // Get a handle to the datas

    val currentLocation: Location = locationClient.getLastLocation()

    if (currentLocation != null) {
      var dbHelper: DbHelper = new DbHelper(this)
      // Push to database
      dbHelper.addActivityEntry(
        currentLocation,
        prefs.getInt(KEY_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN),
        prefs.getInt(KEY_PREVIOUS_ACTIVITY_ID, 0))
      dbHelper.close
    } else {
      // TODO display a notification for that the user activate the GPS
    }

    locationClient.disconnect()
  }

  override def onDisconnected() {}

  /**
    * Determine if an activity means that the user is moving.
    *
    * @param type The type of activity the user is doing (see DetectedActivity constants)
    * @return true if the user seems to be moving from one location to another, otherwise false
    */
  def isMoving(activityType: Int): Boolean = activityType match {
    // These types mean that the user is probably not moving
    case DetectedActivity.STILL | DetectedActivity.TILTING | DetectedActivity.UNKNOWN => false
    case _ => true
  }


  // Functions that follows are unused
  /**
    * Post a notification to the user. The notification prompts the user to click it to
    * open the device's GPS settings
    */
  def sendNotification {
    // Create a notification builder that's compatible with platforms >= version 4
    var builder: NotificationCompat.Builder =
    new NotificationCompat.Builder(getApplicationContext())

    // Set the title, text, and icon
    builder.setContentTitle("Widigo")
    .setContentText("Click to turn on GPS or swipe to ignore")
    //.setSmallIcon(R.drawable.ic_notification)

    // Get the Intent that starts the Location settings panel
    .setContentIntent(getContentIntent())

    // Get an instance of the Notification Manager
    var notifyManager: NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

    // Build the notification and post it
    notifyManager.notify(0, builder.build())
  }

  /**
    * Get a content Intent for the notification
    *
    * @return A PendingIntent that starts the device's Location Settings panel.
    */
  def getContentIntent(): PendingIntent = {

    // Set the Intent action to open Location Settings
    var gpsIntent: Intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    // Create a PendingIntent to start an Activity
    return PendingIntent.getActivity(getApplicationContext(), 0, gpsIntent,
      PendingIntent.FLAG_UPDATE_CURRENT)
  }

  /**
    * Map detected activity types to strings
    *
    * @param activityType The detected activity type
    * @return A user-readable name for the type
    */
  def getNameFromType(activityType: Int): String = activityType match {
    case DetectedActivity.IN_VEHICLE => "in_vehicle"
    case DetectedActivity.ON_BICYCLE => "on_bicycle"
    case DetectedActivity.ON_FOOT    => "on_foot"
    case DetectedActivity.STILL      => "still"
    case DetectedActivity.UNKNOWN    => "unknown"
    case DetectedActivity.TILTING    => "tilting"
    case _                           => "unknown"
  }
}
