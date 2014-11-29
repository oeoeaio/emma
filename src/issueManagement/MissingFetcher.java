package issueManagement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MissingFetcher implements Runnable{
	
	private final Connection dbConn;
	private final String siteID;
	private final String sourceID;
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	MissingFetcher(Connection dbConn,String siteID,String sourceID){
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		this.siteID = siteID;
		this.sourceID = sourceID;
	}
	
	public void run(){
		String selectGapsSQL = "SELECT " +
				"issue_id," +
				"issues.site_id AS site_id," +
				"issues.source_id AS source_id," +
				"start_date," +
				"end_date," +
				"MAX(IF(date_time=DATE_ADD(start_date,INTERVAL -2 MINUTE),value,NULL)) AS two_prior," +
				"MAX(IF(date_time=DATE_ADD(start_date,INTERVAL -1 MINUTE),value,NULL)) AS one_prior," +
				"MAX(IF(date_time=DATE_ADD(end_date,INTERVAL 1 MINUTE),value,NULL)) AS one_post," +
				"MAX(IF(date_time=DATE_ADD(end_date,INTERVAL 2 MINUTE),value,NULL)) AS two_post " +
				"FROM issues " +
				"LEFT JOIN data_sa ON " +
				"data_sa.site_id = issues.site_id AND " +
				"data_sa.source_id = issues.source_id AND " +
				"data_sa.date_time BETWEEN DATE_ADD(issues.start_date,INTERVAL -2 MINUTE) " +
				"AND DATE_ADD(issues.end_date,INTERVAL 2 MINUTE) " +
				"WHERE issue_type = 'MissingValue' " +
				"AND site_id = " + siteID + " " +
				"AND source_id = " + sourceID + " " +
				"GROUP BY issue_id,issues.site_id,issues.source_id,start_date,end_date";
		try{
			ResultSet gapsRS = dbConn.createStatement().executeQuery(selectGapsSQL);
			if (gapsRS.next()){
				
			}
		}
		catch(SQLException sE){
			sE.printStackTrace();
		}
		
	}
}
