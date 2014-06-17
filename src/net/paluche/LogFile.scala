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

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.text.Spanned
import android.text.SpannedString

import macroid.FullDsl._

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.List
import java.util.Locale

/**
  * Utility class that handles writing, reading and cleaning up files where we
  * log the activities that where detected by the activity detection service.
  */

/**
  * Singleton that ensures that only one logFileInstance of the LogFile exists at any time
  *
  * @param context A Context for the current app
  */
object LogFile {
  // Store an logFileInstance of the log file
  var logFileInstance: LogFile = null

  /**
    * Create an logFileInstance of log file, or return the current logFileInstance
    *
    * @param context A Context for the current app
    *
    * @return An logFileInstance of this class
    */
  def getInstance(context: Context): LogFile = {
    if (logFileInstance == null) {
      logFileInstance = new LogFile(context)
    }
    return logFileInstance
  }
}

class LogFile(context: Context) {
  // Store an object that can "print" to a file
  var activityWriter: PrintWriter = null

  // Store a File handle
  var logFile: File = null

  // Store the shared preferences repository handle
  var prefs: SharedPreferences = null

  // Store the current file number and name
  var fileNumber: Int = 0
  var fileName: String = null

  // Open the shared preferences repository
  prefs = context.getSharedPreferences(ActivityUtils.SHARED_PREFERENCES,
    Context.MODE_PRIVATE)

  // If it doesn't contain a file number, set the file number to 1
  if (!prefs.contains(ActivityUtils.KEY_LOG_FILE_NUMBER)) {
    fileNumber = 1

    // Otherwise, get the last-used file number and increment it.
  } else {
    var fileNum: Int = prefs.getInt(ActivityUtils.KEY_LOG_FILE_NUMBER, 0)
    fileNumber = fileNum + 1
  }

  // Get a repository editor logFileInstance
  var editor: Editor = prefs.edit()

  // Put the file number in the repository
  editor.putInt(ActivityUtils.KEY_LOG_FILE_NUMBER, fileNumber)

  // Create a timestamp
  var dateString: String = new SimpleDateFormat("yyyy_MM_dd", Locale.US).format(new Date())

  // Create the file name by sprintf'ing its parts into the filename string.
  fileName = s"${ActivityUtils.LOG_FILE_NAME_PREFIX}${dateString}${fileNumber+1}s{ActivityUtils.LOG_FILE_NAME_SUFFIX}"

  // Save the filename
  editor.putString(ActivityUtils.KEY_LOG_FILE_NAME, fileName)

  // Commit the updates
  editor.commit()

  // Create the log file
  logFile = createLogFile(fileName)

  /**
    * Log a message to the log file
    */
  def log(message: String) {

    // Start a print writer for the log file
    initLogWriter

    // Print a log message
    activityWriter.println(message)

    // Flush buffers
    activityWriter.flush()
  }

  /**
    * Loads data from the log file.
    */
  def loadLogFile(): List[Spanned] = {

    // Get a new List of spanned strings
    var content: List[Spanned] = new ArrayList[Spanned]()

    // If no log file exists yet, return the empty List
    if (!logFile.exists()) {
      return content
    }

    // Create a new buffered file reader based on the log file
    var reader: BufferedReader = new BufferedReader(new FileReader(logFile))

    // Get a String instance to hold input from the log file
    var line: String = reader.readLine

    /*
     * Read until end-of-file from the log file, and store the input line as a
     * spanned string in the List
     */
    while (line != null) {
      content.add(new SpannedString(line))
      line = reader.readLine
    }

    // Close the file
    reader.close()

    // Return the data from the log file
    return content
  }

  /**
    * Creates an object that writes human-readable lines of text to a file
    */
  def initLogWriter() {

    // Catch IO exceptions
    try {
      // If the writer is still open, close it
      if (activityWriter != null) {
        activityWriter.close()
      }
      // Create a new writer for the log file
      activityWriter = new PrintWriter(new FileWriter(logFile, true))

      // If an IO exception occurs, print a stack trace
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  /**
    * Delete log files
    */
  def removeLogFiles(): Boolean = {
    // Start with the "all files removed" flag set to true
    var removed: Boolean = true

    // Iterate through all the files in the app's file directory
    for (file: File <- context.getFilesDir().listFiles()) {

      // If unable to delete the file
      if (!file.delete()) {

        // Log the file's name
        logE"${file.getAbsolutePath()} : ${file.getName()}"

        // Note that not all files were removed
        removed = false
      }
    }

    // Return true if all files were removed, otherwise false
    return removed
  }

  /**
    * Returns a new file object for the specified filename.
    *
    * @return A File for the given file name
    */
  def createLogFile(filename: String): File = {

    // Create a new file in the app's directory
    var newFile: File = new File(context.getFilesDir(), filename)

    // Log the file name
    logD"${newFile.getAbsolutePath()} : ${newFile.getName()}"

    // return the new file handle
    return newFile

  }
}
