/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.example.android.location;

import com.google.android.gms.common.ConnectionResult;
import android.content.Context;
import android.content.res.Resources;

/**
 * Map error codes to error messages.
 */
class LocationServiceErrorMessages {

  def getErrorString(context: Context, errorCode: Int): String = {

    // Get a handle to resources, to allow the method to retrieve messages.
    val mResources: Resources = context.getResources()

    // Define a string to contain the error message
    errorCode match {
      case ConnectionResult.DEVELOPER_ERROR =>
      s"Connection failure error code ${errorCode}";
      case ConnectionResult.INTERNAL_ERROR =>
      "Google Play services is disabled"
      case ConnectionResult.INVALID_ACCOUNT =>
      "An internal error occurred"
      case ConnectionResult.LICENSE_CHECK_FAILED =>
      "The version of Google Play services on this device is not authentic"
      case ConnectionResult.NETWORK_ERROR =>
      "The specified account name is invalid"
      case ConnectionResult.RESOLUTION_REQUIRED =>
      "The app is not licensed to the user"
      case ConnectionResult.SERVICE_DISABLED =>
      "Connection failure error message: ${errorCode}"
      case ConnectionResult.SERVICE_INVALID =>
      "The application is misconfigured"
      case ConnectionResult.SERVICE_MISSING =>
      "Google Play services is missing"
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED =>
      "Google Play services is out of date"
      case ConnectionResult.SIGN_IN_REQUIRED =>
      "The user is not signed in"
      case _ =>
      "An unknown error occurred"
    }
  }
}
