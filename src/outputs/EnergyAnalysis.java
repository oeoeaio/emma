package outputs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;
import endUseWindow.Source;

public class EnergyAnalysis implements Runnable{
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat monthDateFormatter = new SimpleDateFormat("yyyy-MM");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final LogWindow logWindow;
	private final Connection dbConn;
	private final LinkedList<Source> sourceList;
	private final LinkedList<long[]> customStartAndEndDates;
	private final LinkedList<Integer> frequencies;
	private final String samplePeriod;
	private final boolean multipleSites;
	private final long startDate;
	private final long endDate;
	
	EnergyAnalysis(LogWindow logWindow,Connection dbConn,LinkedList<Source> sourceList,LinkedList<long[]> customStartAndEndDates,LinkedList<Integer> frequencies,String samplePeriod,boolean multipleSites,long startDate,long endDate){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		monthDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.logWindow = logWindow;
		this.dbConn = dbConn;
		this.sourceList = sourceList;
		this.customStartAndEndDates = customStartAndEndDates;
		this.frequencies = frequencies;
		this.samplePeriod = samplePeriod;
		this.multipleSites = multipleSites;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastDailyEnergySave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./EnergyAnalysis_"+(multipleSites?"MultiSite":"Site"+sourceList.get(0).getSite().getSiteName())+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+"-"+samplePeriod+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastDailyEnergySave", fileChooser.getSelectedFile().getParent());
			File fileToWrite = fileChooser.getSelectedFile();
				
			Date startProcessTime = new Date();
			FileWriter csvWriter = null;
			try{				
				if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
				 csvWriter = new FileWriter(fileToWrite);//open the file for writing
	
				//Headers
				logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
				if (multipleSites){
					csvWriter.append("Sites: Many,");
					
					for (int i=0;i<sourceList.size();i++){
						csvWriter.append(sourceList.get(i).getSite().getSiteName());
						if (i<sourceList.size()-1){
							csvWriter.append(",");
						}
						while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){ //chew up duplicate 
							i++;
						}
					}
					csvWriter.append("\r\n");
					
				}
				else{csvWriter.append("Site: "+sourceList.get(0).getSite().getSiteName()+"\r\n");}
				csvWriter.append("Date,");
	
				for (int i=0;i<sourceList.size();i++){
					csvWriter.append(sourceList.get(i).getSourceName());
					if (i<sourceList.size()-1){
						csvWriter.append(",");
					}
					while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){ //chew up duplicate 
						i++;
					}
				}
	
				csvWriter.append("\r\n");
				logWindow.println("Done.");
	
				logWindow.println("Attempting to extract data from database and write to file...\r\n");					
	
				try {					
					int minFreq = 0;
					
					for (int i=0;i<frequencies.size();i++){
						if (frequencies.get(i) < minFreq || minFreq == 0){
							minFreq = frequencies.get(i)/60;
						}
					}
					
					int rowsRequired = 0;
					if (samplePeriod.equals("Ten Minutely")){rowsRequired = (int)((endDate-startDate)/(1000*60*10));}
					else if (samplePeriod.equals("Half Hourly")){rowsRequired = (int)((endDate-startDate)/(1000*60*30));}
					else if (samplePeriod.equals("Hourly")){rowsRequired = (int)((endDate-startDate)/(1000*60*60));}
					else if (samplePeriod.equals("Daily")){rowsRequired = (int)((endDate-startDate)/(1000*60*1440));}
					else if (samplePeriod.equals("Monthly")){
						Calendar startCal = new GregorianCalendar();
						startCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						startCal.setTimeInMillis(startDate);
						Calendar endCal = new GregorianCalendar();
						endCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						endCal.setTimeInMillis(endDate);
						endCal.add(Calendar.MINUTE,-1); //if end date is first day of month, avoids a null value for that month in which no data will actually be returned.
						rowsRequired = ((endCal.get(Calendar.YEAR)-startCal.get(Calendar.YEAR))*12)+(endCal.get(Calendar.MONTH)-startCal.get(Calendar.MONTH)+1);
						//rowsRequired = (int)((endDate-startDate)/(1000*60*60));
					}
					
					Double[][] rawData = new Double [rowsRequired][sourceList.size()];
					//Double[][] countData = new Double [rowsRequired][sourceList.size()];
					long rowDates[] = new long[rowsRequired];
					String rowDateStrings[] = new String[rowsRequired];
	
					Calendar rollDate = new GregorianCalendar();
					rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					rollDate.setTimeInMillis(startDate);
					
					for (int i=0;i<rowDates.length;i++){
						if (samplePeriod == "Ten Minutely"){
							rollDate.add(Calendar.MINUTE, 10);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod == "Half Hourly"){
							rollDate.add(Calendar.MINUTE, 30);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod.equals("Hourly")){
							rollDate.add(Calendar.HOUR_OF_DAY, 1);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod.equals("Daily")){
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = csvDateFormatter.format(rowDates[i]);
							rollDate.add(Calendar.DAY_OF_MONTH, 1);
						}
						else if (samplePeriod.equals("Monthly")){
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = monthDateFormatter.format(rowDates[i]);
							System.out.println(i+" "+rowDateStrings[i]);
							rollDate.add(Calendar.MONTH, 1);
						}
					}
	
					String dateGroupString = "";
					String groupByString = "";
					
					if (samplePeriod == "Ten Minutely"){
						dateGroupString = "FROM_UNIXTIME(CEIL(UNIX_TIMESTAMP(date_time)/600)*600) AS date_group";
					}
					else if (samplePeriod == "Half Hourly"){
						dateGroupString = "FROM_UNIXTIME(CEIL(UNIX_TIMESTAMP(date_time)/1800)*1800) AS date_group";
					}
					else if (samplePeriod == "Hourly"){
						dateGroupString = "FROM_UNIXTIME(CEIL(UNIX_TIMESTAMP(date_time)/3600)*3600) AS date_group";
					}
					else {
						dateGroupString = "DATE(DATE_ADD(date_time,INTERVAL -1 MINUTE)) AS date_group";
					}
					
					groupByString = "GROUP BY date_group";
	
					if (dateGroupString.equals("") == false && groupByString.equals("") == false){ //required variables are present
						for (int i=0;i<sourceList.size();i++){
							logWindow.printString("Extracting Data for "+sourceList.get(i).getSourceName()+".....");

							try{
								
								SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
								SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
								hourFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								minuteFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
	
	
								//String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
								//String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";
	
								String minDateString = sqlDateFormatter.format(customStartAndEndDates.get(i)[0]);
								String maxDateString = sqlDateFormatter.format(customStartAndEndDates.get(i)[1]);
								
								System.out.println("Dates: "+minDateString+" "+maxDateString);
	
								String valueString = "UNIX_TIMESTAMP(max_dates.date_group) AS date_group_ts,UNIX_TIMESTAMP(min_time) AS min_time_ts,UNIX_TIMESTAMP(max_time) AS max_time_ts,min_time_value,max_time_value,min_value,max_value";
								String joinString = "(SELECT data_sa.site_id,data_sa.source_id,date_group,max_time,value AS max_time_value FROM (SELECT site_id,source_id,"+dateGroupString+",MAX(date_time) AS max_time FROM data_sa WHERE value IS NOT NULL AND site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID()+" AND date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString+") AS max_dates LEFT JOIN data_sa ON data_sa.site_id = max_dates.site_id ANd data_sa.source_id = max_dates.source_id ANd data_sa.date_time = max_dates.max_time) AS max_dates LEFT JOIN " +
										"(SELECT data_sa.site_id,data_sa.source_id,date_group,min_time,value AS min_time_value FROM (SELECT site_id,source_id,"+dateGroupString+",MIN(date_time) AS min_time FROM data_sa WHERE value IS NOT NULL AND site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID()+"  AND date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString+") AS min_dates LEFT JOIN data_sa ON data_sa.site_id = min_dates.site_id ANd data_sa.source_id = min_dates.source_id ANd data_sa.date_time = min_dates.min_time) AS min_dates USING(date_group) LEFT JOIN " +
										"(SELECT site_id,source_id,"+dateGroupString+",MIN(value) AS min_value,MAX(value) AS max_value FROM data_sa WHERE value IS NOT NULL AND site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID()+"  AND date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString+") AS max_min_values " +
										"USING(date_group)";
															

								//System.out.println(valueString);
								//System.out.println(joinString);
								//System.out.println(circuitFrequency);
	
	
								if (minDateString.equals("") == false && maxDateString.equals("") == false && valueString.equals("") == false){ //max sure required variables are in place
									String getDataSQL = "SELECT "+valueString+" FROM "+joinString+" ORDER BY date_group_ts";
									System.out.println(getDataSQL);
									Statement getData_statement = dbConn.createStatement();
									ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);
	
									int rowCounter = 0;
	
									Calendar dbDate = new GregorianCalendar();
									dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
									dbDate.setTimeInMillis(startDate);
									double prevMaxTimeValue = 0;
									long prevMaxDateTime = startDate-frequencies.get(i)*1000;
									double currMaxTimeValue;
									double currMinTimeValue;
									long currMaxDateTime;
									long currMinDateTime;
									double currMaxValue;
									double currMinValue;
									while (getDataRS.next()){
										dbDate.setTimeInMillis(getDataRS.getLong("date_group_ts")*1000);
										while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
											rawData[rowCounter][i] = null;
											rowCounter++;
										}
										
										currMinDateTime = getDataRS.getLong("min_time_ts")*1000; //minimum time where not null
										currMaxDateTime = getDataRS.getLong("max_time_ts")*1000; //maximum time where not null
										currMinTimeValue = getDataRS.getDouble("min_time_value"); //value at minimum time
										currMaxTimeValue = getDataRS.getDouble("max_time_value"); //value at maximum time
										currMinValue = getDataRS.getDouble("min_value"); //minimum value for day
										currMaxValue = getDataRS.getDouble("max_value"); //maximum value for day
										
										if (currMaxTimeValue<currMinTimeValue){ //if counter has gone past 6553.6
											String currMinDateString = sqlDateFormatter.format(currMinDateTime + frequencies.get(i)*1000);
											String currMaxDateString = sqlDateFormatter.format(currMaxDateTime + frequencies.get(i)*1000);
											
											// check to see if a full cycle occurred by looking for values between maxTimeValue and minTimeValue
											String cycleTestSQL = "SELECT date_time,value FROM data_sa WHERE value IS NOT NULL AND VALUE > "+currMaxTimeValue+" AND VALUE < "+currMinTimeValue+" AND site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID()+" AND date_time BETWEEN '"+currMinDateString+"' AND '"+currMaxDateString+"' LIMIT 1";
											Statement cycleTestStatement = dbConn.createStatement();
											ResultSet getMiddleDataRS = cycleTestStatement.executeQuery(cycleTestSQL);
											if (getMiddleDataRS.next()){ //multiple times over the top
												rawData[rowCounter][i] = getEnergyWithMultipleCycles(cycleTestStatement,sourceList.get(i).getSite().getSiteID(),sourceList.get(i).getSourceID(),currMinDateString,currMaxDateString,currMinTimeValue,currMaxTimeValue);
											}
											else{ // only once over the top, so do the normal thing
												rawData[rowCounter][i] = ( currMaxTimeValue - currMinTimeValue < 0 ? currMaxTimeValue + 6553.6 - currMinTimeValue : currMaxTimeValue - currMinTimeValue );
												//System.out.println(sqlDateFormatter.format(dbDate.getTimeInMillis())+" "+(rawData[rowCounter][i]));
											}
										}
										else if (currMaxValue>currMaxTimeValue || currMinValue<currMinTimeValue){ //if counter has gone past 6553.6 and initial point
											String currMinDateString = sqlDateFormatter.format(currMinDateTime + frequencies.get(i)*1000);
											String currMaxDateString = sqlDateFormatter.format(currMaxDateTime + frequencies.get(i)*1000);
											Statement cycleTestStatement = dbConn.createStatement();
											rawData[rowCounter][i] = getEnergyWithMultipleCycles(cycleTestStatement,sourceList.get(i).getSite().getSiteID(),sourceList.get(i).getSourceID(),currMinDateString,currMaxDateString,currMinTimeValue,currMaxTimeValue);
										}
										else{
											rawData[rowCounter][i] = ( currMaxTimeValue - currMinTimeValue < 0 ? currMaxTimeValue + 6553.6 - currMinTimeValue : currMaxTimeValue - currMinTimeValue );
											//System.out.println(sqlDateFormatter.format(dbDate.getTimeInMillis())+" "+(rawData[rowCounter][i]));
										}
										
										
										
										if (rowCounter > 0 && rawData[rowCounter-1][i] != null && currMinTimeValue != prevMaxTimeValue){
											long prevDist = dbDate.getTimeInMillis() - prevMaxDateTime;
											long currDist = currMinDateTime - dbDate.getTimeInMillis() - frequencies.get(i)*1000;
											
											// Initial algorithm worked for daily (ie. dbDate.getTimeInMillis() was start of the period) for minutely, date is at the end of the period
											if (samplePeriod.equals("Ten Minutely")){
												prevDist = (dbDate.getTimeInMillis() - 600000) - prevMaxDateTime;
												currDist = currMinDateTime - (dbDate.getTimeInMillis() - 600000) - frequencies.get(i)*1000;
											}
											else if (samplePeriod.equals("Half Hourly")){
												prevDist = (dbDate.getTimeInMillis() - 1800000) - prevMaxDateTime;
												currDist = currMinDateTime - (dbDate.getTimeInMillis() - 1800000) - frequencies.get(i)*1000;
											}
											else if (samplePeriod.equals("Hourly")){
												prevDist = (dbDate.getTimeInMillis() - 3600000) - prevMaxDateTime;
												currDist = currMinDateTime - (dbDate.getTimeInMillis() - 3600000) - frequencies.get(i)*1000;
											}
											
											double block = ( currMinTimeValue - prevMaxTimeValue < 0 ? currMinTimeValue + 6553.6 - prevMaxTimeValue : currMinTimeValue - prevMaxTimeValue);
											if (prevDist > 0 || currDist > 0){
												rawData[rowCounter-1][i] += Math.round((block*prevDist/(currDist+prevDist))*10)/10;
												rawData[rowCounter][i] += Math.round((block*currDist/(currDist+prevDist))*10)/10;
												//System.out.println(sqlDateFormatter.format(dbDate.getTimeInMillis()) + " block to add  " + block);
												//System.out.println(sqlDateFormatter.format(prevMaxDateTime)+" prev "+(block*prevDist/(currDist+prevDist)) + " " + prevDist);
												//System.out.println(sqlDateFormatter.format(currMinDateTime)+" post "+(block*currDist/(currDist+prevDist)) + " " + currDist);
											}
											else{ // add single period between prevMaxDateTime and currMinDateTime  
												rawData[rowCounter][i] += block;
											}
										}
										
										prevMaxDateTime = currMaxDateTime;
										prevMaxTimeValue = currMaxTimeValue;
										
										rowCounter++;
									}
	
									getDataRS.close();
									getData_statement.close();
									logWindow.println("Done.");
								}
							}
							catch(SQLException sE){
								sE.printStackTrace();
								StackTraceElement[] sTE = sE.getStackTrace();
								for (int c=0;c<sTE.length;c++){
									logWindow.println(sTE[c].toString());
								}
								throw sE;
							}
							catch(Exception e){
								e.printStackTrace();
								StackTraceElement[] sTE = e.getStackTrace();
								for (int c=0;c<sTE.length;c++){
									logWindow.println(sTE[c].toString());
								}
							}

						}
					}
					for(int j=0;j<rowsRequired;j++){
						csvWriter.append(rowDateStrings[j]+",");
						for (int i=0;i<sourceList.size();i++){

							int legitCount = (rawData[j][i]==null?0:1);

							double rawDataPoint = rawData[j][i]==null? 0 : rawData[j][i];
							while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){
								legitCount += (rawData[j][i+1]==null?0:1);
								if (rawData[j][i+1]!=null){//find first number that is not null for a particular source
									rawDataPoint = rawData[j][i];
									break;
								}
								i++;
							}

							csvWriter.append((legitCount==0 ? "null" : new DecimalFormat("#.###").format(rawDataPoint) ));



							if (i<sourceList.size()-1){
								csvWriter.append(",");
							}
						}
					
						csvWriter.append("\r\n");
					}
	
				} catch (SQLException sE) {
					sE.printStackTrace();
				}
	
				Date endProcessTime = new Date();
				logWindow.println("Total Extraction Time: "+getTimeString(endProcessTime.getTime()-startProcessTime.getTime())+"\r\n");
				
			}	catch (IOException e){
				JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
			} finally{
				if (csvWriter!=null){
					try {
						csvWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else{ //File not selected or invalid file
			logWindow.println("No valid file selected.");
		}
	}
	
	double getEnergyWithMultipleCycles(Statement cycleTestStatement,String siteID,String sourceID,String currMinDateString,String currMaxDateString,double currMinTimeValue,double currMaxTimeValue) throws SQLException{
		double energy = 0.0;
		
		
		String getDataSQL = "SELECT UNIX_TIMESTAMP(date_time) AS date_time_ts,value FROM data_sa WHERE value IS NOT NULL AND site_id = "+siteID+" AND source_id = "+sourceID+" AND date_time BETWEEN '"+currMinDateString+"' AND '"+currMaxDateString+"' ORDER BY date_time";
		ResultSet getDataRS = cycleTestStatement.executeQuery(getDataSQL);
		double prevValue = currMinTimeValue;
		while (getDataRS.next()){
			if (getDataRS.getDouble("value") < prevValue){
				if (energy == 0.0){ //if first cycle
					energy = energy + 6553.6 - currMinTimeValue;
				}
				else{
					energy = energy + 6553.6;
				}
			}
			prevValue = getDataRS.getDouble("value");
		}
		energy = energy + currMaxTimeValue;
		
		cycleTestStatement.close();
		return energy;
	}
	
	boolean moreData(ArrayList<ResultSet> results){
		boolean moreData = true;
		try{
			for (int i=0;i<results.size();i++){
				if (results.get(i).next()==false){
					moreData = false;
				}
			}
		} catch(SQLException sE){
			moreData = false;
		}
		return moreData;
	}
	
	String getTimeString(long timeInMillis){
		String timeString = "";
		long remainder = timeInMillis;
		int hours = (int)Math.floor(remainder/(1000*60*60));
		remainder = remainder - hours*(1000*60*60);
		int mins = (int)Math.floor(remainder/(1000*60));
		remainder = remainder - mins*(1000*60);
		double secs = (double)remainder/1000;
		if (hours>0){
			if (hours == 1){
				timeString = timeString.concat("1 hour");
			}
			else{
				timeString = timeString.concat(hours+" hours");
			}
		}
		if (hours>0 && mins>0){
			timeString = timeString.concat(", ");
		}
		if (mins>0){
			if (mins == 1){
				timeString = timeString.concat("1 minute");
			}
			else{
				timeString = timeString.concat(mins+" minutes");
			}
		}
		if (hours>0 || mins>0){
			timeString = timeString.concat(" and ");
		}
		timeString = timeString.concat(secs+" seconds");
		return timeString;
	}
	
}
