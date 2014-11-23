package net.paluche.widigo

import android.database.sqlite._
import android.database.Cursor
import android.content.Context
import android.content.ContentValues
import android.graphics.Color
import android.location.Location

import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

import macroid.FullDsl._

class WidigoActivity(val polylineOptions: PolylineOptions,
  val activityID: Int,
  val activityType: Int,
  val pointList: List[WidigoActivityPoint] )

class WidigoActivityPoint (
  val timestamp: Long,
  val latitude: Double,
  val longitude: Double,
  val hasAltitude: Boolean,
  val altitude: Double,
  val hasSpeed: Boolean,
  val speed: Float)

class DbHelper(context: Context) extends SQLiteOpenHelper(context,
  "Widigo.db", null, 1) {
  import WidigoUtils._
  /*
   * Activity table
   */
  // Table description
  val tableName:              String = "activityPoints"

  // Columns name definition
  val columnNameKey:          String = "key"
  val columnNameTimestamp:    String = "timestamp"
  val columnNameActivityID:   String = "activityID"
  val columnNameActivityType: String = "activityType"
  val columnNameLatitude:     String = "latitude"
  val columnNameLongitude:    String = "longitude"
  val columnNameAltitude:     String = "altitude"
  val columnNameSpeed:        String = "speed"

  // Columns index definition
  val columnIndexTimestamp:    Int = 0
  val columnIndexActivityID:   Int = 1
  val columnIndexActivityType: Int = 2
  val columnIndexLatitude:     Int = 3
  val columnIndexLongitude:    Int = 4
  val columnIndexAltitude:     Int = 5
  val columnIndexSpeed:        Int = 6

  // SQLite helpers
  val intType:                String = " INTEGER"
  val realType:               String = " REAL"
  val commaSep:               String = ", "

  // Columns that interrest us in the request
  val projection: Array[String] = Array(columnNameTimestamp,
    columnNameActivityID, columnNameActivityType, columnNameLatitude,
    columnNameLongitude, columnNameAltitude, columnNameSpeed)

 def onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE " + tableName       + " (" +
      columnNameKey   + " PRIMARY KEY"  + commaSep +
      columnNameTimestamp    + intType  + commaSep +
      columnNameActivityType + intType  + commaSep +
      columnNameActivityID   + intType  + commaSep +
      columnNameLatitude     + realType + commaSep +
      columnNameLongitude    + realType + commaSep +
      columnNameAltitude     + realType + commaSep +
      columnNameSpeed        + realType + " )")
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP TABLE " + tableName + ";");
    onCreate(db);
  }

  /*
   * Helper
   */
  private def extractActivityPoint(cursor: Cursor) = new WidigoActivityPoint(
    cursor.getLong(columnIndexTimestamp),
    cursor.getDouble(columnIndexLatitude),
    cursor.getDouble(columnIndexLongitude),
    cursor.isNull(columnIndexAltitude),
    cursor.getFloat(columnIndexAltitude),
    cursor.isNull(columnIndexSpeed),
    cursor.getFloat(columnIndexSpeed))

  // Timestamp arguments are the UTC time in milliseconds since January 1, 1970
  private def getActivitiesDataByDate(startTimestamp: Long,
    endTimestamp: Long): Cursor = {
    val db: SQLiteDatabase  = this.getReadableDatabase();

    db.query(
      tableName,            // The table name
      projection,           // We want all the columns except the key
      columnNameTimestamp + " <= ? AND " +
      columnNameTimestamp + ">= ?",
      Array(                // Argument for the WHERE clause
        startTimestamp.toString,
        endTimestamp.toString),
      null,                 // Don't group the rows
      null,                 // Don't filter by group rows
      columnNameTimestamp + " DESC")
  }

  private def getLastkey(): java.lang.Long = {
    val db: SQLiteDatabase  = this.getReadableDatabase();
    val cursor: Cursor = db.rawQuery("SELECT MAX(" + columnNameKey +
      ") FROM " + tableName, null)
    cursor.moveToFirst
    if (cursor.getCount == 0)
      return 0
    val ret: Long = cursor.getInt(0)
    cursor.close
    ret
  }

  /*
   * Add data
   */
  def addActivityEntry(location: Location, activityType: Integer,
    activityID: Integer): Long = {
    val db: SQLiteDatabase = this.getWritableDatabase();
    var values: ContentValues = new ContentValues()

    values.put(columnNameKey, getLastkey())
    values.put(columnNameTimestamp, location.getTime.asInstanceOf[java.lang.Long])
    values.put(columnNameActivityType, activityType)
    values.put(columnNameActivityID, activityID)
    values.put(columnNameLatitude, location.getLatitude)
    values.put(columnNameLongitude, location.getLongitude)

    if (location.hasAltitude)
      values.put(columnNameAltitude, location.getAltitude)
    else
      values.putNull(columnNameAltitude)

    if (location.hasSpeed)
      values.put(columnNameSpeed, location.getSpeed.toDouble)
    else
      values.putNull(columnNameSpeed)

    return db.insert(tableName, null, values)
  }


  /*
   * Getting data from the database
   */
  def getLastActivityIdAndType(): WidigoActivity =
  {
    val db: SQLiteDatabase  = this.getReadableDatabase();
    var cursor: Cursor =
    db.rawQuery("SELECT MAX(" + columnNameTimestamp + "), " +
      columnNameTimestamp    + commaSep +
      columnNameActivityID   + commaSep +
      columnNameActivityType + commaSep +
      columnNameSpeed        +
      " FROM " + tableName,
      null)

    cursor.moveToFirst

    if (cursor.getCount == 0)
      return null

    var activityId:   Int = cursor.getInt(columnIndexActivityID)
    var activityType: Int = cursor.getInt(columnIndexActivityType)

    logD"getLastActivityIdAndType: ID ${activityId}, type ${activityType}"

    new WidigoActivity(null,
      activityId,
      activityType,
      null)
  }

  def getActivitiesByDate(startTimestamp: Long,
    endTimestamp: Long): List[WidigoActivity] = {

    // Get datas
    var cursor: Cursor = getActivitiesDataByDate(startTimestamp, endTimestamp)

    cursor.moveToFirst
    if (cursor.getCount == 0)
      return null

    // Convert database datas into a List of activities
    var activities: List[WidigoActivity] = List()

    do {
      var polylineOpt: PolylineOptions         = new PolylineOptions()
      var pointList: List[WidigoActivityPoint] = List()
      var activityID                           = cursor.getInt(columnIndexActivityID)
      var activityType                         = cursor.getInt(columnIndexActivityType)

      // Set the color according to the type of activity
      activityType match {
        case DetectedActivity.IN_VEHICLE => polylineOpt.color(Color.BLACK)
        case DetectedActivity.ON_BICYCLE => polylineOpt.color(Color.BLUE)
        case DetectedActivity.ON_FOOT    => polylineOpt.color(Color.CYAN)
        case DetectedActivity.STILL      => polylineOpt.color(Color.RED)
        case DetectedActivity.UNKNOWN | DetectedActivity.TILTING | _
        => polylineOpt.color(Color.WHITE)
      }

      // Geodesic reprenstation
      polylineOpt.geodesic(true)

      // Width of the line
      polylineOpt.width(3)

      do{
        // Add point to Polyline
        polylineOpt.add(new LatLng(cursor.getDouble(columnIndexLatitude),
          cursor.getDouble(columnIndexLongitude)))

        // Add pointdata to pointList
        pointList = pointList ++ List(extractActivityPoint(cursor))

      } while(cursor.moveToNext() && activityID == cursor.getInt(columnIndexActivityID))

      // Add the activity to the activities list.
      (new WidigoActivity(
        polylineOpt,
        activityID,
        activityType,
        pointList)) :: activities
    } while (cursor.moveToNext())

    cursor.close
    return activities
  }

  /*
   * Remove an Activity
   */
  def removeActivitybyID(activityID: Integer) {
    val db: SQLiteDatabase = this.getWritableDatabase();
    db.delete(
      tableName,
      columnNameActivityID + " = ?",
      Array(activityID.toString))
  }
}
