package net.paluche.Widigo

import android.database.sqlite._
import android.content.Context
import android.content.ContentValues

import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.LatLng

object ActivityHAndler {
  def isMoving(activityType: Int): Boolean = activityType match {
    // These types mean that the user is probably not moving
    case DetectedActivity.STILL | DetectedActivity.TILTING | DetectedActivity.UNKNOWN => false
    case _ => true
  }

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

// Data format for the SQL base

// Activity format in the SQLite base.
// We will save a list of points, each point belong to an activity number and
// an activity type. After we have the information about the location. And
// finally the timestamp (UTC time in milliseconds since January 1, 1970).
class ActivityPoint (val ID: Int,
                     val timestamp: Long,
                     val activityType: Int,
                     val activityID: Int,
                     val latitude: Double,
                     val longitude: Double,
                     val hasAltitude: Boolean,
                     val altitude: Double,
                     val hasSpeed: Boolean,
                     val speed: Float)

class DbHelper(context: Context) extends
    SQLiteOpenHelper(context, "Widigo.db", null, 1) {

  // Table description
  val tableName:              String = " activityPoints"
  val columnNameTimestamp:    String = " timestamp"
  val columnNameActivityType: String = " activityType"
  val columnNameActivityID:   String = " activityID"
  val columnNameLatitude:     String = " latitude"
  val columnNameLongitude:    String = " longitude"
  val columnNameAltitude:     String = " altitude"
  val columnNameSpeed:        String = " speed"

  val intType:                String = " INTEGER"
  val realType:               String = " REAL"
  val commaSep:               String = ","


  def SQL_CREATE_ENTRIES: String = "CREATE TABLE WIDIGO " + tableName +
  " (" + columnNameTimestamp    + intType   + " PRIMARY KEY" + commaSep +
         columnNameActivityType + intType   + commaSep +
         columnNameActivityID   + intType   + commaSep +
         columnNameLatitude     + realType  + commaSep +
         columnNameLongitude    + realType  + commaSep +
         columnNameAltitude     + realType  + commaSep +
         columnNameSpeed + " )"



  def onCreate(db: SQLiteDatabase) {
    db.execSQL(SQL_CREATE_ENTRIES)
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

  }

  def genReqCreate(activityPoint: ActivityPoint): String =
  ""

  def addEntry(db:SQLiteDatabase, activityPoint: ActivityPoint): Long = {
    var values: ContentValues = new ContentValues()

    values.put(columnNameTimestamp,    activityPoint.timestamp)
    values.put(columnNameActivityType, activityPoint.activityType)
    values.put(columnNameActivityID,   activityPoint.activityID)
    values.put(columnNameLatitude,     activityPoint.latitude)
    values.put(columnNameLongitude,    activityPoint.longitude)
    if (activityPoint.hasAltitude)
      values.put(columnNameAltitude,     activityPoint.altitude)
    else
      values.put(columnNameAltitude, null)
    if (activityPoint.hasSpeed)
      values.put(columnNameSpeed,     activityPoint.speed)
    else
      values.put(columnNameSpeed, null)


    return db.insert( tableName, null, values)
  }

}

/*
  About the confidence level, if the confidence in the activity is
    - between 75 and 100, this is the
 */

class ActivityHandler {

  def handleActivities(list: List[DetectedActivity]) {

    val confidenceMax: Int = 0

    for (activity: DetectedActivity <- list ) {

    }

  }

}
