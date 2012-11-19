package dataPlotter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import fileManagement.DataPoint;

public class DataGrabber {
	
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	DataGrabber(){
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
	}

	
	ArrayList<DataPoint> getData(Connection dbConn,String siteID, String sourceID, long startDate, long endDate){
		ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
		try {
			String dataPointsSQL = "SELECT UNIX_TIMESTAMP(date_time) AS unixts,value FROM data_sa WHERE site_id = "+siteID+" AND source_id = "+sourceID+" AND date_time BETWEEN '"+sqlDateFormatter.format(startDate)+"' AND '"+sqlDateFormatter.format(endDate)+"'";
			ResultSet dataPointsRS = dbConn.createStatement().executeQuery(dataPointsSQL);
			
			while(dataPointsRS.next()){
				Long currentDate = dataPointsRS.getLong("unixts")*1000;
				dataPoints.add(new DataPoint(currentDate,dataPointsRS.getDouble("value")));					
			}
		
		} catch (SQLException e) {
			dataPoints.clear();
		};
		
		return dataPoints;
	}
}
