package missingPlotter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import endUseWindow.Source;
import fileManagement.DataPoint;

public class MissingGrabber {
	
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	MissingGrabber(){
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
	}

	
	ArrayList<ArrayList<DataPoint>> getData(Connection dbConn,String siteID, Source[] sources, long startDate, long endDate, int samplePeriod){
		ArrayList<ArrayList<DataPoint>> allDataPoints = new ArrayList<ArrayList<DataPoint>>();
		
		try {
			int colsRequired = (int)((endDate-startDate)/(1000*60*samplePeriod));
			Double[][] averagedData = new Double [colsRequired][sources.length];
			long rowDates[] = new long[colsRequired];
			//String rowDateStrings[] = new String[colsRequired];

			Calendar rollDate = new GregorianCalendar();
			rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			rollDate.setTimeInMillis(startDate);
			for (int i=0;i<rowDates.length;i++){
				//add sample period on at the beginning for 10 minute and hour cases so that the first point represents the first block FOLLOWING the start date
				//sample period is added afterwards for daily (so date is represented by actual day, rather than following day)
				if (samplePeriod == 1){
					rollDate.add(Calendar.MINUTE, 1);
					rowDates[i] = rollDate.getTimeInMillis();
				}
				else if (samplePeriod == 10){
					rollDate.add(Calendar.MINUTE, 10);
					rowDates[i] = rollDate.getTimeInMillis();
				}
				else if (samplePeriod == 60){
					rollDate.add(Calendar.HOUR_OF_DAY, 1);
					rowDates[i] = rollDate.getTimeInMillis();
				}
				else if (samplePeriod == 1440){
					rowDates[i] = rollDate.getTimeInMillis();
					rollDate.add(Calendar.DAY_OF_MONTH, 1);
				}
			}

			String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
			String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";

			String blockString = "";
			String groupByString = "";
			if (samplePeriod == 1){
				blockString = "UNIX_TIMESTAMP(DATE(date_time)) AS blockDate_ts,HOUR(date_time) AS blockHour,MINUTE(date_time) AS blockMinute";
				groupByString = "GROUP BY blockDate_ts,blockHour,blockMinute";
			}
			else if (samplePeriod == 10){
				blockString = "UNIX_TIMESTAMP(CASE WHEN HOUR(date_time)+MINUTE(date_time)/60 > 23+(5/6) THEN DATE(DATE_ADD(date_time,INTERVAL 1 HOUR)) ELSE DATE(date_time) END) AS blockDate_ts,CASE WHEN HOUR(date_time)+MINUTE(date_time)/60 > 23+(5/6) THEN 0 ELSE CASE WHEN MINUTE(date_time) > 50 THEN CEIL(HOUR(date_time)+MINUTE(date_time)/60) ELSE FLOOR(HOUR(date_time)+MINUTE(date_time)/60) END END AS blockHour,CASE WHEN MINUTE(date_time) > 50 THEN 0 ELSE CEIL(MINUTE(date_time)/10)*10 END AS blockMinute";
				groupByString = "GROUP BY blockDate_ts,blockHour,blockMinute";
			}
			else if (samplePeriod == 60){
				blockString = "UNIX_TIMESTAMP(CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN DATE(DATE_ADD(date_time,INTERVAL 1 HOUR)) ELSE DATE(date_time) END) AS blockDate_ts,CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN 0 ELSE CEIL(HOUR(date_time)+MINUTE(date_time)/60) END AS blockHour";
				groupByString = "GROUP BY blockDate_ts,blockHour";
			}
			else if (samplePeriod == 1440){
				blockString = "UNIX_TIMESTAMP(DATE(DATE_SUB(date_time, INTERVAL 1 MINUTE))) AS blockDate_ts";
				groupByString = "GROUP BY blockDate_ts";
			}
			else{
				JOptionPane.showMessageDialog(null, "Error: Sample period not recognised","Error",JOptionPane.ERROR_MESSAGE);
			}

			if (blockString.equals("") == false && groupByString.equals("") == false){ //required variables are present
				for (int i=0;i<sources.length;i++){
					ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
					//logWindow.printString("Extracting Data for "+sources.get[i]+".....");

					SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
					SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
					hourFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					minuteFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));

					//String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
					//String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";


					String valueString = "ROUND(1-(COUNT(*)/"+samplePeriod+"),2) AS value"; //percentage of each day
					String joinString = "";

					if (minDateString.equals("") == false && maxDateString.equals("") == false && valueString.equals("") == false){ //max sure required variables are in place
						String getDataSQL =  "SELECT "+blockString+","+valueString+" FROM data_sa "+joinString+" WHERE data_sa.site_id = "+siteID+" AND data_sa.source_id = "+sources[i].getSourceID()+" AND data_sa.date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' AND value IS NOT NULL "+groupByString;
						//System.out.println(getDataSQL);
						Statement getData_statement = dbConn.createStatement();
						ResultSet getPointsRS = getData_statement.executeQuery(getDataSQL);

						int rowCounter = 0;

						Calendar dbDate = new GregorianCalendar();
						dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						dbDate.setTimeInMillis(startDate);
						while (getPointsRS.next()){
							if (samplePeriod == 10 || samplePeriod == 1){
								dbDate.setTimeInMillis(getPointsRS.getLong("blockDate_ts")*1000);
								dbDate.add(Calendar.MINUTE,getPointsRS.getInt("blockHour")*60+getPointsRS.getInt("blockMinute"));
								while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
									Long currentDate = rowDates[rowCounter];
									dataPoints.add(new DataPoint(currentDate,1.0));
									rowCounter++;
								}
							}
							else if (samplePeriod == 60){
								dbDate.setTimeInMillis(getPointsRS.getLong("blockDate_ts")*1000);
								dbDate.add(Calendar.HOUR,getPointsRS.getInt("blockHour"));
								while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
									Long currentDate = rowDates[rowCounter];
									dataPoints.add(new DataPoint(currentDate,1.0));
									rowCounter++;
								}
							}
							else if (samplePeriod == 1440){
								dbDate.setTimeInMillis(getPointsRS.getLong("blockDate_ts")*1000);
								while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
									Long currentDate = rowDates[rowCounter];
									dataPoints.add(new DataPoint(currentDate,1.0));
									rowCounter++;
								}
							}

							Long currentDate = getPointsRS.getLong("blockDate_ts")*1000;
							dataPoints.add(new DataPoint(currentDate,getPointsRS.getDouble("value")));
							
							if(getPointsRS.wasNull()){averagedData[rowCounter][i] = 1.0;}
							

							rowCounter++;
						}
						dbDate.setTimeInMillis(endDate);
						//if missing data up to endDate
						while (rowCounter < rowDates.length && rowDates[rowCounter]<dbDate.getTimeInMillis()){
							Long currentDate = rowDates[rowCounter];
							dataPoints.add(new DataPoint(currentDate,1.0));
							rowCounter++;
						}

						getPointsRS.close();
						getData_statement.close();
						//logWindow.println("Done.");
					}
					allDataPoints.add(dataPoints);
				}
			}
			
			/*for(int j=0;j<colsRequired;j++){
				csvWriter.append(rowDateStrings[j]+",");
				for (int i=0;i<selectedSources.length;i++){
					csvWriter.append((averagedData[j][i]==null ? "1.0" : Double.toString(averagedData[j][i])));
					if (i<selectedSources.length-1){
						csvWriter.append(",");
					}
				}
				csvWriter.append("\r\n");
			}*/
		} catch (SQLException sE){
			allDataPoints.clear();
		}
		
		return allDataPoints;
	}
}
