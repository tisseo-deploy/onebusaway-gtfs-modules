package org.onebusaway.gtfs_transformer.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;

import org.json.JSONObject;
import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopLocation;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs_transformer.factory.EntityRetentionGraph;
import org.onebusaway.gtfs_transformer.services.GtfsTransformStrategy;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RemoveEntitiesOutsidePolygone implements GtfsTransformStrategy {
    private final Logger log = LoggerFactory.getLogger(RemoveEntitiesOutsidePolygone.class);

    @CsvField(optional = true)
    private String polygone;

    public void setPolygone(String polygone) {
        this.polygone = polygone;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

     @Override
    public void run(TransformContext transformContext, GtfsMutableRelationalDao gtfsMutableRelationalDao) {
        Geometry geometry = buildPolygone(polygone);
        // ArrayList<Stop> stopsToRemove = new ArrayList<>();
        // ArrayList<StopTime> stopTimesToRemove = new ArrayList<>();
        // ArrayList<Trip> tripsToRemove = new ArrayList<>();
        // RemoveEntityLibrary removeEntityLibrary = new RemoveEntityLibrary();
        // HashMap<String, StopTime> referenceStopTimes = new HashMap<>();
        // browse all stops and retain only those inside polygone/multiPolygone
        Map<Object, List<Object>> tripsWithStopsOutsidePolygone = new HashMap<>();

        Integer iterationST = 0;
        // loop all trips
        for (Trip trip : gtfsMutableRelationalDao.getAllTrips()){
            if (trip.getId().getId().equals("OCESN55594R3128108:2024-05-08T00:33:19Z"))
                log.info("route : Toulouse - Guzet "+trip.getId());
            for (StopTime stopTime: gtfsMutableRelationalDao.getStopTimesForTrip(trip)){
                Stop stop = gtfsMutableRelationalDao.getStopForId(stopTime.getStop().getId());
                if (! insidePolygon(geometry,stop.getLon(),stop.getLat())){
                    List<Object> stopsWithSequence = new ArrayList<>();
                    stopsWithSequence.add(new Object[] {stopTime.getStopSequence(), stop.getId().getId()});
                    updateDict(tripsWithStopsOutsidePolygone,trip.getId().getId(),stopsWithSequence);
                    iterationST+=1;
                }
            }
        }
        for (Map.Entry<Object,List<Object>> entry:  tripsWithStopsOutsidePolygone.entrySet()){
            Object firstObject = entry.getValue().get(0);
            Object[] firstArray = (Object[]) firstObject;
            String fromStopId = (String) firstArray[1]; // get from stop id
            String toStopId = null; // get to stop id
             // Process the last object if there is more than one object in the list
             if (entry.getValue().size() > 1) {
                Object lastObject = entry.getValue().get(entry.getValue().size() - 1);
                Object[] lastArray = (Object[]) lastObject;
                toStopId = (String) lastArray[1];
            }
            // call trim trip operation
            String line = "{\"op\":\"trim_trip\", \"match\":{\"file\":\"trips.txt\", \"trip_id\":\""+entry.getKey()+"\"},\"from_stop_id\":\""+fromStopId+"\"}";
            try {
                 JSONObject json = new JSONObject(line);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        log.info("tripswithStop outside polygone "+tripsWithStopsOutsidePolygone.size(),Integer.toString(iterationST));
        //
        // if (geometry.isValid() && !geometry.isEmpty()){
        //     for (Stop stop : gtfsMutableRelationalDao.getAllStops()) {
        //         if (! insidePolygon(geometry,stop.getLon(),stop.getLat())){
        //             for (StopTime stopTime : gtfsMutableRelationalDao.getStopTimesForStop(stop)){
        //                 log.info("stop "+stop.getId()+ " trip id : "+stopTime.getTrip().getId().getId());
        //                 tripsOutsidePolygoneDict.put(stopTime.getTrip().getId().getId()+";"+stop.getId(),stopTime.getTrip().getRoute().getShortName() + ";"+stopTime.getTrip().getRoute().getLongName());
        //                 if (!tripsToRemove.contains(stopTime.getTrip()))
        //                     tripsToRemove.add(stopTime.getTrip());
        //                 if (!stopTimesToRemove.contains(stopTime))
        //                     stopTimesToRemove.add(stopTime);
        //                 if (!stopsToRemove.contains(stop))
        //                     stopsToRemove.add(stop);
        //             }
        //         }
        //     }
        // }


    }   
      // Method to update the dictionary with multiple values for a key
      private static void updateDict(Map<Object, List<Object>> dictionary, String key, List<Object> values) {
        // If the key already exists, append the values to its existing list
        if (dictionary.containsKey(key)) {
            List<Object> existingValues = dictionary.get(key);
            existingValues.addAll(values);
        }
        // If the key is new, add it to the dictionary with the list of values
        else {
            dictionary.put(key, values);
        }
    }
    
    /*
     * create polygone/multiPolygone from 'polygone' variable in json file
        * return Geometry variable
        * return null if an exception is encountered when parsing the wkt string
     */
    private Geometry buildPolygone(String wktPolygone) {
        WKTReader reader = new WKTReader();
        try{
            return  reader.read(wktPolygone);
        } catch (ParseException e){
            String message = String.format("Error parsing WKT string : %s", e.getMessage());
            log.error(message);
            return null;
        }
        
    }
    /*
     * insidePolygone returns boolean variable
        * true: if polygone contains point
        * false if point is outside polygone
     */
    private boolean insidePolygon(Geometry geometry, double lon, double lat) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        return geometry.contains(point);
    }

}
