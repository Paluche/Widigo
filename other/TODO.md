Notes about the project
=======================

#Google Maps or OpenStreetMap?
I have a preference for the Map of OSM, but GMaps seems to be easy to use, or
at least more popular.

#Interesting Features
Step counter embedded as Android sensor

#TODO
- Think about layouts.
- Receiving Activity updates.
- Get a layout with the google Maps.
- Get a menu, where we select whch activities we save.

#The datas
Save the datas under the format of tracks containing for the Activities:
    - IN_VEHICLE, ON_BICYCLE and ON_FOOT
      a list of points with the time
    - STILL
      the position time of start and time of end
    - UNKNOW and TITLING
      nothing, we will not save the datas in this case.

#The Intent Service
It is the service that will run all the times. It will trigger on detected
activities. If we are moving we activate the location update.

# What We do with the datas.
Display the tracks.
Have a menu displaying the tracks saved between two dates.

#The options:
##Tracking options
Allows the user to activate / de-activate the tracking and also select for
which kind of activities we track and save.

##My tracks options
Here we will select wich tracks to display we will have two sections allowing
to select the tracks between two dates and the list of the saved tracks.
Allowing to delete the tracks.
