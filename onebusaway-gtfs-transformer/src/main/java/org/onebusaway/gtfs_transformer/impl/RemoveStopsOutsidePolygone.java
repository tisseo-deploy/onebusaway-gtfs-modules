package org.onebusaway.gtfs_transformer.impl;

import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.geom.*;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs_transformer.factory.EntityRetentionGraph;
import org.onebusaway.gtfs_transformer.services.GtfsTransformStrategy;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class RemoveStopsOutsidePolygone implements GtfsTransformStrategy {
    private final Logger log = LoggerFactory.getLogger(RemoveStopsOutsidePolygone.class);

    @CsvField(optional = true)
    private String polygone;

    public void setPolygone(String polygone) {
        this.polygone = polygone;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /*
     * example:
     *  {"op":"transform","class":"org.onebusaway.gtfs_transformer.impl.RemoveStopsOutsidePolygone","polygone":wkt_polygone ..."}
     */

     @Override
    public void run(TransformContext transformContext, GtfsMutableRelationalDao gtfsMutableRelationalDao) {
        Geometry geometry = buildPolygone(polygone);
        EntityRetentionGraph graph = new EntityRetentionGraph(gtfsMutableRelationalDao);
        graph.setRetainBlocks(false);
        // browse all stops and retain only those inside polygone/multiPolygone
        if (geometry.isValid() && !geometry.isEmpty()){
            for (Stop stop : gtfsMutableRelationalDao.getAllStops()) {
                if (insidePolygon(geometry,stop.getLon(),stop.getLat())){
                    graph.retain(stop, true);
                }
            }
        }

        // remove non retained objects
        for (Class<?> entityClass : GtfsEntitySchemaFactory.getEntityClasses()) {
            List<Object> objectsToRemove = new ArrayList<Object>();
            for (Object entity : gtfsMutableRelationalDao.getAllEntitiesForType(entityClass)) {
                if (!graph.isRetained(entity)){
                    objectsToRemove.add(entity);
                    }
                }
            for (Object toRemove : objectsToRemove){
                gtfsMutableRelationalDao.removeEntity((IdentityBean<Serializable>) toRemove);
                }
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
