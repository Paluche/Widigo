package net.paluche.Widigo

import android.database.sqlite._
import android.database.Cursor
import android.content.Context
import android.content.ContentValues
import android.graphics.Color

import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions


class Activity(val polylineOptions: PolylineOptions,
               val activityID: Int,
               val activityType: Int,
               val pointList: List[ActivityPoint] )

class ActivityPoint (val timestamp: Long,
                     val latitude: Double,
                     val longitude: Double,
                     val hasAltitude: Boolean,
                     val altitude: Double,
                     val hasSpeed: Boolean,
                     val speed: Float)

class DbHelper(context: Context) extends
    SQLiteOpenHelper(context, "Widigo.db", null, 1) {

  /*
   * Activity table
   */
  // Table description
  val tableName:              String = " activityPoints"

  // Columns name definition
  val columnNameTimestamp:    String = " timestamp"
  val columnNameActivityID:   String = " activityID"
  val columnNameActivityType: String = " activityType"
  val columnNameLatitude:     String = " latitude"
  val columnNameLongitude:    String = " longitude"
  val columnNameAltitude:     String = " altitude"
  val columnNameSpeed:        String = " speed"

  // Columns index definition
  val columnIndexTimestamp:    Int = 0
  val columnIndexActivityID:   Int = 1
  val columnIndexActivityType: Int = 2
  val columnIndexLatitude:     Int = 3
  val columnIndexLongitude:    Int = 4
  val columnIndexAltitude:     Int = 5
  val columnIndexSpeed:        Int = 6

  // Data needed when we make a request (all of them)
  val projection: Array[String] = Array(tableName, columnNameTimestamp,
    columnNameActivityType, columnNameActivityID, columnNameLatitude,
    columnNameLongitude, columnNameAltitude, columnNameSpeed)

  // SQLite helpers
  val intType:                String = " INTEGER"
  val realType:               String = " REAL"
  val commaSep:               String = ","

  def onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE WIDIGO " + tableName +
      " (" + columnNameTimestamp    + intType   + " PRIMARY KEY" + commaSep +
      columnNameActivityType + intType   + commaSep +
      columnNameActivityID   + intType   + commaSep +
      columnNameLatitude     + realType  + commaSep +
      columnNameLongitude    + realType  + commaSep +
      columnNameAltitude     + realType  + commaSep +
      columnNameSpeed + " )")
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // I don't know what to do
  }

  /*
   * Add data
   */
  def addActivityEntry(activityPoint: ActivityPoint, activityType: Integer,
      activityID: Integer): Long = {
    val db: SQLiteDatabase = this.getWritableDatabase();
    var values: ContentValues = new ContentValues()

    values.put(columnNameTimestamp, activityPoint.timestamp.asInstanceOf[java.lang.Long])
    values.put(columnNameActivityType, activityType)
    values.put(columnNameActivityID, activityID)
    values.put(columnNameLatitude, activityPoint.latitude)
    values.put(columnNameLongitude, activityPoint.longitude)

    if (activityPoint.hasAltitude)
      values.put(columnNameAltitude, activityPoint.altitude)
    else
      values.putNull(columnNameAltitude)

    if (activityPoint.hasSpeed)
      values.put(columnNameSpeed, activityPoint.speed.toDouble)
    else
      values.putNull(columnNameSpeed)

    return db.insert( tableName, null, values)
  }

  /*
   * Getting data from the database
   */

  private def getLastActivitPoint(): ActivityPoint = {
    val db: SQLiteDatabase  = this.getReadableDatabase();
    var cursor: Cursor = db.query(
      tableName,
      projection,
      columnNameTimestamp,
      Array("MAX"), // FIXME I don't konw the correct syntax
      null,
      null,
      null)

    extractActivityPoint(cursor)
  }
  private def extractActivityPoint(cursor: Cursor) = {
    new ActivityPoint(
      cursor.getLong(columnIndexTimestamp),
      cursor.getDouble(columnIndexLatitude),
      cursor.getDouble(columnIndexLongitude),
      cursor.isNull(columnIndexAltitude),
      cursor.getFloat(columnIndexAltitude),
      cursor.isNull(columnIndexSpeed),
      cursor.getFloat(columnIndexSpeed))
  }

  // Timestamp arguments are the UTC time in milliseconds since January 1, 1970
  private def getActivitiesDataByDate(startTimestamp: Long,
      endTimestamp: Long): Cursor = {
    val db: SQLiteDatabase  = this.getReadableDatabase();

    db.query(
      tableName,            // The table name
      projection,           // Columns we will get
      columnNameTimestamp,  // Columns for the WHERE clause
      Array(                // Argument for the WHERE clause
        ">= " + startTimestamp.toString + " & ",
        "<= " + endTimestamp.toString),
      null,                 // Don't group the rows
      null,                 // Don't filter by group rows
      columnNameTimestamp + " DESC")
    }

  def getActivitiesByDate(startTimestamp: Long,
      endTimestamp: Long): List[Activity] = {

    // Get datas
    var cursor: Cursor = getActivitiesDataByDate(startTimestamp, endTimestamp)

    // Convert database datas into a List of activities
    var activities: List[Activity] = List()

    do {
      var polylineOpt: PolylineOptions   = new PolylineOptions()
      var pointList: List[ActivityPoint] = List()
      var activityID                     = cursor.getInt(columnIndexActivityID)
      var activityType                   = cursor.getInt(columnIndexActivityType)

      // Set the color according to the type of activity
      activityType match {
        case DetectedActivity.IN_VEHICLE => polylineOpt.color(Color.BLACK)
        case DetectedActivity.ON_BICYCLE => polylineOpt.color(Color.BLUE)
        case DetectedActivity.ON_FOOT    => polylineOpt.color(Color.CYAN)
        case DetectedActivity.STILL      => polylineOpt.color(Color.RED)
        case DetectedActivity.UNKNOWN | DetectedActivity.TILTING | _
          => polylineOpt.color(Color.WHITE)
      }

      do{
        // Add point to Polyline
        polylineOpt.add(new LatLng(cursor.getDouble(columnIndexLatitude),
          cursor.getDouble(columnIndexLongitude)))

        // Add pointdata to pointList
        pointList = pointList ++ List(extractActivityPoint(cursor))

      } while(cursor.moveToNext() && activityID == cursor.getInt(columnIndexActivityID))

      // Add the activity to the activities list.
      (new Activity(
        polylineOpt,
        activityID,
        activityType,
        pointList)) :: activities
    } while (cursor.moveToNext())

    cursor.close
    return activities
  }

  // Remove an Activity
  def removeActivitybyID(db: SQLiteDatabase, activityID: Integer) {
    db.delete(
      tableName,
      columnNameActivityID + " LIKE ?",
      Array(activityID.toString))
  }
}
