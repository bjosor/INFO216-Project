# INFO216-Project

Instructions for the weather app

The project was made in IntelliJ and is probably safest to compile there.  All dependencies should be loaded in by Maven. It runs on JDK 8

The UI provides a rudimentary way of interacting and observing the data. 
Just pressing the search button will try to show all available SensorStations. If the “Search Observations” box is checked it will also show all observations. This is limited to 5000 entries in the sparql query to avoid crashing the program with to many entries.

The “place” searchbox will match for name, municipality or county on any particular SensorStation.

Check the “Search Observations” checkbox to make use of the related fields. It will match for observations of the specified element between the dates specified. If no local data is found it will attempt to request it from frost. For this to work id, element and both date fields must be filled out and the id must be complete and correspond to a specific station.

We only have some data stored locally as the api database is to big to effectively download all of. The program will search the local database first, but if no results are found it will attempt to query the frost api for the relevant data. This data is then stored in the local file for later use.

In case of the frost api being unavailable or otherwise not working, we have stored some data locally already. It will also be easier to use these as it is a bit impractical to figure out which stations offer which data. We noticed this a bit late and could not implement a ui for this in time. Here are some examples of locally available data:

SN4110 Lørenskog
Has sum(precipitation_amount P1D) data available between 01.01.2016 up to today.
