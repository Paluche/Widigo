/*
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

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener
import com.google.android.gms.location.ActivityRecognitionClient

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log

import macroid.FullDsl._

/**
  * Class for connecting to Location Services and activity recognition updates.
  * <b>
  * Note: Clients must ensure that Google Play services is available before requesting updates.
  * </b> Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to check.
  *
  *
  * To use a DetectionRequester, instantiate it and call requestUpdates(). Everything else is done
  * automatically.
  *
  */
class DetectionRequester(var context: Context) extends ConnectionCallbacks
    with OnConnectionFailedListener {
  import WidigoUtils._

  // Stores the PendingIntent used to send activity recognition events back to the app
  var activityRecognitionPendingIntent: PendingIntent = null

  // Stores the current instantiation of the activity recognition client
  var activityRecognitionClient: ActivityRecognitionClient = null

  /**
    * Returns the current PendingIntent to the caller.
    *
    * @return The PendingIntent used to request activity recognition updates
    */
  def getRequestPendingIntent(): PendingIntent = {
    return activityRecognitionPendingIntent
  }

  /**
    * Sets the PendingIntent used to make activity recognition update requests
    * @param intent The PendingIntent
    */
  def setRequestPendingIntent(intent: PendingIntent) {
    activityRecognitionPendingIntent = intent
  }

  /**
    * Start the activity recognition update request process by
    * getting a connection.
    */
  def requestUpdates() {
    requestConnection()
  }

  /**
    * Make the actual update request. This is called from onConnected().
    */
  def continueRequestActivityUpdates() {
    /*
     * Request updates, using the default detection interval.
     * The PendingIntent sends updates to ActivityRecognitionIntentService
     */
    getActivityRecognitionClient().requestActivityUpdates(
      DETECTION_INTERVAL_MILLISECONDS,
      createRequestPendingIntent())

    // Disconnect the client
    requestDisconnection()
  }

  /**
    * Request a connection to Location Services. This call returns immediately,
    * but the request is not complete until onConnected() or onConnectionFailure() is called.
    */
  def requestConnection() {
    getActivityRecognitionClient().connect()
  }

  /**
    * Get the current activity recognition client, or create a new one if necessary.
    * This method facilitates multiple requests for a client, even if a previous
    * request wasn't finished. Since only one client object exists while a connection
    * is underway, no memory leaks occur.
    *
    * @return An ActivityRecognitionClient object
    */
  def getActivityRecognitionClient(): ActivityRecognitionClient = {
    if (activityRecognitionClient == null) {

      activityRecognitionClient =
      new ActivityRecognitionClient(context, this, this)
    }
    return activityRecognitionClient
  }

  /**
    * Get the current activity recognition client and disconnect from Location Services
    */
  def requestDisconnection() {
    getActivityRecognitionClient().disconnect()
  }

  /*
   * Called by Location Services once the activity recognition client is connected.
   *
   * Continue by requesting activity updates.
   */
  override def onConnected(arg0: Bundle) {
    // If debugging, log the connection
    logD"Connected from Location Services"

    // Continue the process of requesting activity recognition updates
    continueRequestActivityUpdates()
  }

  /*
   * Called by Location Services once the activity recognition client is disconnected.
   */
  override def onDisconnected() {
    // In debug mode, log the disconnection
    logD"Disconnected from Location Services"

    // Destroy the current activity recognition client
    activityRecognitionClient = null
  }

  /**
    * Get a PendingIntent to send with the request to get activity recognition updates. Location
    * Services issues the Intent inside this PendingIntent whenever a activity recognition update
    * occurs.
    *
    * @return A PendingIntent for the IntentService that handles activity recognition updates.
    */
  def createRequestPendingIntent(): PendingIntent = {

    // If the PendingIntent already exists
    if (null != getRequestPendingIntent()) {

      // Return the existing intent
      return activityRecognitionPendingIntent

      // If no PendingIntent exists
    } else {
      // Create an Intent pointing to the IntentService
      var intent: Intent = new Intent(context, classOf[ActivityRecognitionIntentService])

      /*
       * Return a PendingIntent to start the IntentService.
       * Always create a PendingIntent sent to Location Services
       * with FLAG_UPDATE_CURRENT, so that sending the PendingIntent
       * again updates the original. Otherwise, Location Services
       * can't match the PendingIntent to requests made with it.
       */
      var pendingIntent: PendingIntent = PendingIntent.getService(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT)

      setRequestPendingIntent(pendingIntent)
      return pendingIntent
    }

  }

  /*
   * Implementation of OnConnectionFailedListener.onConnectionFailed
   * If a connection or disconnection request fails, report the error
   * connectionResult is passed in from Location Services
   */
  override def onConnectionFailed(connectionResult: ConnectionResult) {
    /*
     * Google Play services can resolve some errors it detects.
     * If the error has a resolution, try sending an Intent to
     * start a Google Play services activity that can resolve
     * error.
     */
    if (connectionResult.hasResolution()) {

      try {
        connectionResult.startResolutionForResult(context.asInstanceOf[Activity],
          CONNECTION_FAILURE_RESOLUTION_REQUEST)

        /*
         * Thrown if Google Play services canceled the original
         * PendingIntent
         */
      } catch {
        case e: SendIntentException => ??? // display an error or log it here.
      }

      /*
       * If no resolution is available, display Google
       * Play service error dialog. This may direct the
       * user to Google Play Store if Google Play services
       * is out of date.
       */
    } else {
      var dialog: Dialog = GooglePlayServicesUtil.getErrorDialog(
        connectionResult.getErrorCode(),
        context.asInstanceOf[Activity],
        CONNECTION_FAILURE_RESOLUTION_REQUEST)
      if (dialog != null) {
        dialog.show()
      }
    }
  }
}

