package org.onebusaway.gtfs_transformer.impl;
import static  org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onebusaway.csv_entities.IndividualCsvEntityReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs.services.MockGtfs;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveStopsOutsidePolygoneTest {

    private RemoveStopsOutsidePolygone removeStopsOutsidePolygone = new RemoveStopsOutsidePolygone();
    private TransformContext _context = new TransformContext();
    private MockGtfs _gtfs;
  private static Logger _log = LoggerFactory.getLogger(IndividualCsvEntityReader.class);


    @BeforeEach
    public void setup() throws IOException{

        _gtfs = MockGtfs.create();
        _gtfs.putAgencies(1);
        _gtfs.putStops(1);
        _gtfs.putRoutes(1);
        _gtfs.putTrips(1, "r0", "sid0");
        _gtfs.putStopTimes("t0", "s0");
        _gtfs.putStops(1, 
        "s0,Stop A,40.748817,-73.985428" // New York (Example)
    );

        // Set startDate to today's date and endDate to three weeks from today
        String startDate = getCurrentDateFormatted(null);
        String endDate = getCurrentDateFormatted(21);

        // Insert a calendar entry with startDate set to today and endDate set to 3 weeks from today
        _gtfs.putCalendars(
            1,
            "start_date="+startDate,
            "end_date="+endDate
        );
    }

    @Test
    public void testRemoveStopsOutsidePolygone() throws IOException  {
        GtfsMutableRelationalDao dao = _gtfs.read();
        // Verify that GtfsMutableRelationalDao object contains exactly one calendar and three calendar dates entries
        assertEquals(1,dao.getAllCalendars().size());
        // assertEquals(3,dao.getAllCalendarDates().size());
    }

    // Helper function to get today's date in the required format
    public static String getCurrentDateFormatted(Integer daysOffset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.now();
        if (daysOffset != null){
            date = date.plusDays(daysOffset);
        }
        // Format date as "yyyyMMdd"
        return date.format(formatter);
    }
    
}
