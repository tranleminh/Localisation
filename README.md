# This Location tracker application is created within the scope of my internship at IMT Mines Ales on the subject of Burnout Syndrome Detection using Smartphones' data
# Current Version : 1.00

#How it works :

Based greatly on the Google's Location API, this app is capable of computing the duration where user stays in a same location, a same address. Because of the smartphone's sensor precision, it can show different addresses eventhough the smartphone is not moving. That is why this app does not predict correctly the user's current location but put all the locations in a certain radius in a same address group.

There are three buttons on the UI : Start/Stop, Debug and View Record
- Once clicking the Start button, the app will start to record users' location and address. It can automatically detect when user changes location, the app stop recording data and insert it into the database. While functioning, the Start button becomes Stop button, and will stop the tracker when clicked
- The Debug button shows the distance between different address groups
- The View Record button is used to display all the recorded data.

# Functionalities by version :
1.00 :
- Locations and addresses are detected using Google's Location API
- The tracker has a precision parameter of 53 meters, so it groups all the locations in a radius of 53m together for duration calculation.
- Using SQLite Database
- In the database, are stored information on Address Group, the location's longitude, latitude, an address and the duration. When displaying database, only address group, address and duration are shown. This address is indeed one of the address belonging to an address group, and was stored for solely research purpose and will be removed soon.
- Everytime the app is launched, it automatically creates a table containing address groups and their central location. When the tracker detects a new location, it checks if this location is within the radius of the old location then the location stays unchanged. Else, using the address groups table initially created, it calculates the distance between that new location to the stored address groups and checks if the minimal distance to a group is smaller than the precision parameter then that new location belongs to that existed address group, otherwise create a new address group with that new location as the central location. 
- Data displayed in textual form contained in an alert dialog
- There is not yet a functionality to deduct the user's home location and workplace, which will be added in an upcoming update.

# Changelogs :

1.00 : 
- Localisation's prototype released
