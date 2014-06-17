/*
 * This module handles the location updates, under the form of a IntentService
 * We need to have these updates to run in background. The service is activated
 * by the Activities recognition service, which also runs in background.
 */
package net.paluche.widigo

import android.app.IntentService
import android.content.Intent

class LocationUpdates extends IntentService("WidigoLocation") {

  override def onHandleIntent(intent: Intent) {

  }
}
