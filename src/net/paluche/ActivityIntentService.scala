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
package net.paluche.Widigo

//import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.ActivityRecognitionResult

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.provider.Settings
import android.support.v4.app.NotificationCompat

import macroid.FullDsl._

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import scala.util.control._

class ActivityRecognitionIntentService
    extends IntentService("WidigoActivity") {
  import WidigoUtils._

  var prefs:      SharedPreferences = null
  var dateFormat: SimpleDateFormat  = null

  override def onHandleIntent(intent: Intent) {
    // Get a handle to the repository
    prefs = getApplicationContext.getSharedPreferences(
              SHARED_PREFERENCES, Context.MODE_PRIVATE)

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

      // Check to see if the repository contains a previous activity
      if (!prefs.contains(KEY_PREVIOUS_ACTIVITY_TYPE)) {
        // This is the first type an activity has been detected. Store the type
        var editor: Editor = prefs.edit()
        editor.putInt(KEY_PREVIOUS_ACTIVITY_TYPE, currentActivityType)
        editor.commit()

        // Push to database
        // TODO

        if (isMoving(currentActivityType)) {
            //Start location updates intent
            // TODO
          }
      } else {

        var previousActivityType: Int = prefs.getInt(KEY_PREVIOUS_ACTIVITY_TYPE,
          DetectedActivity.UNKNOWN)

        // Activity changed
        if (previousActivityType != currentActivityType) {
          if (isMoving(previousActivityType) && !isMoving(currentActivityType)) {
            //Start location updates intent
            // TODO
          } else if (!isMoving(previousActivityType) && isMoving(currentActivityType)) {
            // Stop location updates intent
            // TODO
          }
          // Push to database
          // TODO
        }
      }
    }
  }

  // UNUSED FOR NOW
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

  // UNUSED FOR NOW
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

  // UNUSED
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
