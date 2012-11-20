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

public class AverageAnalysis implements Runnable{
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat monthDateFormatter = new SimpleDateFormat("yyyy-MM");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final LogWindow logWindow;
	private final Connection dbConn;
	private final LinkedList<Source> sourceList;
	private final LinkedList<long[]> customStartAndEndDates;
	private final boolean multipleSites;
	private final long startDate;
	private final long endDate;
	private final String samplePeriod;
	private final String analysisType;
	private final boolean includeCount;
	private final boolean doStdDev;
	
	
	
	AverageAnalysis(LogWindow logWindow,Connection dbConn,LinkedList<Source> sourceList,LinkedList<long[]> customStartAndEndDates,LinkedList<Integer> frequencies,boolean multipleSites,long startDate,long endDate,String samplePeriod,String analysisType,boolean includeCount,boolean doStdDev){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		monthDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.logWindow = logWindow;
		this.dbConn = dbConn;
		this.sourceList = sourceList;
		this.customStartAndEndDates = customStartAndEndDates;
		this.multipleSites = multipleSites;
		this.startDate = startDate;
		this.endDate = endDate;
		this.samplePeriod = samplePeriod;
		this.analysisType = analysisType;
		this.includeCount = includeCount;
		this.doStdDev = doStdDev;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastAvgAnalysisSave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./"+(analysisType.equals("avg")?"AvgAnalysis":(analysisType.equals("lightSum")?"LightOnTime":"CircuitKnown"))+"_"+(multipleSites?"MultiSite":"Site"+sourceList.get(0).getSite().getSiteName())+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastAvgAnalysisSave", fileChooser.getSelectedFile().getParent());
			File fileToWrite = fileChooser.getSelectedFile();
				
			Date startProcessTime = new Date();
			try{				
				if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
				FileWriter csvWriter = new FileWriter(fileToWrite);//open the file for writing
	
				//Headers
				logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
				if (multipleSites){
					csvWriter.append("Sites: Many,");
					
					for (int i=0;i<sourceList.size();i++){
						csvWriter.append(sourceList.get(i).getSite().getSiteName());
						if(includeCount){csvWriter.append(","+sourceList.get(i).getSite().getSiteName());}
						if(doStdDev){csvWriter.append(","+sourceList.get(i).getSite().getSiteName());}
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
					if(includeCount){csvWriter.append(","+sourceList.get(i).getSourceName());}
					if(doStdDev){csvWriter.append(","+sourceList.get(i).getSourceName());}
					if (i<sourceList.size()-1){
						csvWriter.append(",");
					}
					while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){ //chew up duplicate 
						i++;
					}
				}
				
				if(includeCount || doStdDev){
					csvWriter.append("\r\nDate,");
					for (int i=0;i<sourceList.size();i++){
						csvWriter.append((analysisType.equals("lightSum")?"OnTime" : "Average"));
						if(includeCount){csvWriter.append(",Count");}
						if(doStdDev){csvWriter.append(",StdDev");}
						if (i<sourceList.size()-1){
							csvWriter.append(",");
						}
						while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){ //chew up duplicate 
							i++;
						}
					}
				}
	
	
				csvWriter.append("\r\n");
				csvWriter.close();
				logWindow.println("Done.");
	
				logWindow.println("Attempting to extract data from database and write to file...\r\n");					
	
				try {					
					int rowsRequired = 0;
					if (samplePeriod.equals("Ten Minutely")){rowsRequired = (int)((endDate-startDate)/(1000*60*10));}
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
					//System.out.println(1);
					//System.out.println(sqlDateFormatter.format(startDate)+" "+sqlDateFormatter.format(endDate));
					//System.out.println(rowsRequired);
					
					Double[][] averagedData = new Double [rowsRequired][sourceList.size()];
					Double[][] countData = new Double [rowsRequired][sourceList.size()];
					Double[][] stdDevData = new Double [rowsRequired][sourceList.size()];
					long rowDates[] = new long[rowsRequired];
					String rowDateStrings[] = new String[rowsRequired];
	
					Calendar rollDate = new GregorianCalendar();
					rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					rollDate.setTimeInMillis(startDate);
					if (samplePeriod.equals("Monthly")){
						rollDate.set(Calendar.DAY_OF_MONTH, 1);
						rollDate.set(Calendar.HOUR_OF_DAY, 0);
						rollDate.set(Calendar.MINUTE, 0);
						rollDate.set(Calendar.SECOND, 0);
					}
					
					for (int i=0;i<rowDates.length;i++){
						//add sample period on at the beginning for 10 minute and hour cases so that the first point represents the first block FOLLOWING the start date
						//sample period is added afterwards for daily (so date is represented by actual day, rather than following day)
						/*if (samplePeriod == 1){
							rollDate.add(Calendar.MINUTE, 1);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}*/
						if (samplePeriod == "Ten Minutely"){
							rollDate.add(Calendar.MINUTE, 10);
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
	
					String blockString = "";
					String groupByString = "";
					/*if (samplePeriod == 1){
						blockString = "UNIX_TIMESTAMP(DATE(date_time)) AS blockDate_ts,HOUR(date_time) AS blockHour,MINUTE(date_time) AS blockMinute";
						groupByString = "GROUP BY blockDate_ts,blockHour,blockMinute";
					}*/
					if (samplePeriod == "Ten Minutely"){
						blockString = "UNIX_TIMESTAMP(CASE WHEN HOUR(date_time)+MINUTE(date_time)/60 > 23+(5/6) THEN DATE(DATE_ADD(date_time,INTERVAL 1 HOUR)) ELSE DATE(date_time) END) AS blockDate_ts,CASE WHEN HOUR(date_time)+MINUTE(date_time)/60 > 23+(5/6) THEN 0 ELSE CASE WHEN MINUTE(date_time) > 50 THEN CEIL(HOUR(date_time)+MINUTE(date_time)/60) ELSE FLOOR(HOUR(date_time)+MINUTE(date_time)/60) END END AS blockHour,CASE WHEN MINUTE(date_time) > 50 THEN 0 ELSE CEIL(MINUTE(date_time)/10)*10 END AS blockMinute";
						groupByString = "GROUP BY blockDate_ts,blockHour,blockMinute";
					}
					else if (samplePeriod.equals("Hourly")){
						blockString = "UNIX_TIMESTAMP(CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN DATE(DATE_ADD(date_time,INTERVAL 1 HOUR)) ELSE DATE(date_time) END) AS blockDate_ts,CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN 0 ELSE CEIL(HOUR(date_time)+MINUTE(date_time)/60) END AS blockHour";
						groupByString = "GROUP BY blockDate_ts,blockHour";
					}
					else if (samplePeriod.equals("Daily")){
						blockString = "UNIX_TIMESTAMP(DATE(DATE_SUB(date_time, INTERVAL 1 MINUTE))) AS blockDate_ts";
						groupByString = "GROUP BY blockDate_ts";
					}
					else if (samplePeriod.equals("Monthly")){
						blockString = "UNIX_TIMESTAMP(STR_TO_DATE(DATE_FORMAT(DATE_SUB(date_time, INTERVAL 1 MINUTE),'%Y-%m-01'),'%Y-%m-%d')) AS blockDate_ts";
						groupByString = "GROUP BY blockDate_ts";
					}
					else{
						logWindow.println("ERROR: Sample period not recognised, channels will not be processed");
					}
	
					if (blockString.equals("") == false && groupByString.equals("") == false){ //required variables are present
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
	
	
								String valueString = "ROUND(AVG(data_sa.value),3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
								String joinString = "LEFT JOIN files USING (file_id)";
								//String valueString = "ROUND(SUM(data_sa.value*(files.frequency/60))/SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),3) AS analysisValue";
								
								if(doStdDev){
									valueString += ",ROUND(STDDEV(data_sa.value),3) AS standardDev";
								}
								int circuitFrequency = 0;
	
								if (analysisType.equals("avg")){
									String sourceTypeSQL = "SELECT source_type,measurement_type FROM sources WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID();
									ResultSet sourceTypeRS = dbConn.createStatement().executeQuery(sourceTypeSQL);
	
									if (sourceTypeRS.next()){ //change conversion factor
										if (sourceTypeRS.getString("source_type").equals("Light") && sourceTypeRS.getString("measurement_type").equals("OnTime")){
											//valueString = "@file_id:=file_id AS file_id,@source_id:=source_id AS source_id,@freq:=(SELECT frequency FROM files WHERE file_id = @file_id) AS freq,@wattage:=(SELECT wattage FROM lights WHERE source_id = @source_id) AS wattage,ROUND(AVG(value*(@wattage/@freq)),3) AS analysisValue";
											valueString = "ROUND(AVG(data_sa.value*(lights.wattage/files.frequency)),3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount, ROUND(STDDEV(data_sa.value*(lights.wattage/files.frequency)),3) AS standardDev";
											joinString = "LEFT JOIN files ON files.file_id = data_sa.file_id LEFT JOIN lights ON lights.source_id = data_sa.source_id";
										}
									}
								}
								else if (analysisType.equals("lightSum")){ //if analysis type is Light sum
									valueString = "ROUND(SUM(value)/60,3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
	
									String sourceTypeSQL = "SELECT source_type,measurement_type FROM sources WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID();
									ResultSet sourceTypeRS = dbConn.createStatement().executeQuery(sourceTypeSQL);
	
									if (sourceTypeRS.next()){ //change conversion factor
										//if (sourceTypeRS.getString("source_type").equals("Light") && sourceTypeRS.getString("measurement_type").equals("ActPower")){
										//	valueString = "@file_id:=file_id AS file_id,@freq:=(SELECT frequency FROM files WHERE file_id = @file_id) AS freq,SUM(IF(value > 0.1,@freq/60,0)) AS analysisValue";
										//}
										if (sourceTypeRS.getString("source_type").equals("Light") && sourceTypeRS.getString("measurement_type").equals("ActPower")){
											valueString = "IF(lights.wattage IS NULL,NULL,ROUND(SUM(LEAST(value,lights.wattage)*(files.frequency/lights.wattage))/60,3)) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
											joinString = "LEFT JOIN files ON files.file_id = data_sa.file_id LEFT JOIN lights ON lights.source_id = data_sa.source_id";
										}	
									}
								}
								else if (analysisType.equals("circuitKnown")){
									valueString = "ROUND(IFNULL(AVG(appliance_load.sumvalue),0)+IFNULL(AVG(light_load.sumvalue),0),3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";								
	
									String circuitFreqSQL = "SELECT MAX(frequency) AS freq FROM files WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID()+" AND (start_date <='"+maxDateString+"') AND (end_date >= '"+minDateString+"')";
									//System.out.println(circuitFreqSQL);
									ResultSet circuitFreqRS = dbConn.createStatement().executeQuery(circuitFreqSQL);
	
									if (circuitFreqRS.next()){
										circuitFrequency = circuitFreqRS.getInt("freq");
									}
	
									if (circuitFrequency != 0){
										joinString = "LEFT JOIN (SELECT FROM_UNIXTIME(CEIL(UNIX_TIMESTAMP(data_sa.date_time)/"+circuitFrequency+")*"+circuitFrequency+") AS round_date,SUM(data_sa.value/("+circuitFrequency+"/files.frequency)) AS sumvalue FROM data_sa LEFT JOIN files ON data_sa.file_id = files.file_id WHERE data_sa.site_id = "+sourceList.get(i).getSite().getSiteID()+" AND data_sa.source_id IN (SELECT source_id FROM appliances WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND circuit_id = (SELECT circuit_id FROM circuits WHERE source_id = "+sourceList.get(i).getSourceID()+")) GROUP BY round_date) AS appliance_load ON appliance_load.round_date = data_sa.date_time";
										joinString = joinString + " LEFT JOIN files USING (file_id) LEFT JOIN (SELECT FROM_UNIXTIME(CEIL(UNIX_TIMESTAMP(data_sa.date_time)/"+circuitFrequency+")*"+circuitFrequency+") AS round_date,IF(sources.measurement_type='OnTime',SUM(data_sa.value*(lights.wattage/IF(files.frequency>="+circuitFrequency+",files.frequency,"+circuitFrequency+"))),SUM(data_sa.value/("+circuitFrequency+"/files.frequency))) AS sumvalue FROM data_sa LEFT JOIN files ON data_sa.file_id = files.file_id LEFT JOIN lights ON lights.source_id = data_sa.source_id LEFT JOIN sources ON data_sa.source_id = sources.source_id WHERE data_sa.site_id = "+sourceList.get(i).getSite().getSiteID()+" AND data_sa.source_id IN (SELECT source_id FROM lights WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND circuit_id = (SELECT circuit_id FROM circuits WHERE source_id = "+sourceList.get(i).getSourceID()+")) GROUP BY round_date) AS light_load ON light_load.round_date = data_sa.date_time";
									}
								}
								//System.out.println(valueString);
								//System.out.println(joinString);
								//System.out.println(circuitFrequency);
	
	
								if (minDateString.equals("") == false && maxDateString.equals("") == false && valueString.equals("") == false && (!analysisType.equals("circuitResidual") || (analysisType.equals("circuitResidual") && circuitFrequency!=0))){ //max sure required variables are in place
									String getDataSQL =  "SELECT "+blockString+","+valueString+" FROM data_sa "+joinString+" WHERE data_sa.site_id = "+sourceList.get(i).getSite().getSiteID()+" AND data_sa.source_id = "+sourceList.get(i).getSourceID()+" AND data_sa.date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString;
									System.out.println(getDataSQL);
									Statement getData_statement = dbConn.createStatement();
									ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);
	
									int rowCounter = 0;
	
									Calendar dbDate = new GregorianCalendar();
									dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
									dbDate.setTimeInMillis(startDate);
									while (getDataRS.next()){
										if (samplePeriod == "Ten Minutely" || samplePeriod == "Minutely"){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											dbDate.add(Calendar.MINUTE,getDataRS.getInt("blockHour")*60+getDataRS.getInt("blockMinute"));
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												averagedData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												if (doStdDev){stdDevData[rowCounter][i] = null;}
												rowCounter++;
											}
										}
										else if (samplePeriod.equals("Hourly")){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											dbDate.add(Calendar.HOUR,getDataRS.getInt("blockHour"));
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												averagedData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												if (doStdDev){stdDevData[rowCounter][i] = null;}
												rowCounter++;
											}
										}
										else if (samplePeriod.equals("Daily")){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												averagedData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												if (doStdDev){stdDevData[rowCounter][i] = null;}
												rowCounter++;
											}
										}
										else if (samplePeriod.equals("Monthly")){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												averagedData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												if (doStdDev){stdDevData[rowCounter][i] = null;}
												rowCounter++;
											}
										}
	
										averagedData[rowCounter][i] = getDataRS.getDouble("analysisValue");
	
										if(getDataRS.wasNull()){averagedData[rowCounter][i] = null;}
	
										countData[rowCounter][i] = getDataRS.getDouble("pointCount");
										if (getDataRS.wasNull()){countData[rowCounter][i] = null;}
	
										if (doStdDev){
											stdDevData[rowCounter][i] = getDataRS.getDouble("standardDev");
											if (getDataRS.wasNull()){stdDevData[rowCounter][i] = null;}
										}
	
	
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
						if (analysisType.equals("avg")){
							for (int i=0;i<sourceList.size();i++){
								
								int legitCount = (averagedData[j][i]==null?0:1);
								double weightedAverageSums = (averagedData[j][i]==null? 0 : averagedData[j][i]*(countData[j][i]==null? 0 : countData[j][i]) );
								double weightedStdDevs = (stdDevData[j][i]==null? 0 : stdDevData[j][i]*(countData[j][i]==null? 0 : countData[j][i]) );
								double countSum = (countData[j][i]==null? 0 : countData[j][i]);
								while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){
									legitCount += (averagedData[j][i+1]==null?0:1);
									weightedAverageSums += (averagedData[j][i+1]==null? 0 : averagedData[j][i+1]*(countData[j][i+1]==null? 0 : countData[j][i+1]) );
									weightedStdDevs += (stdDevData[j][i+1]==null? 0 : stdDevData[j][i+1]*(countData[j][i+1]==null? 0 : countData[j][i+1]) );
									countSum += (countData[j][i+1]==null? 0 : countData[j][i+1]);
									i++;
								}
								
								csvWriter.append((legitCount==0 ? "null" : new DecimalFormat("#.###").format(weightedAverageSums/countSum) ));
								if (includeCount){csvWriter.append(","+(legitCount==0 ? "null" : (samplePeriod.equals("Monthly")? new DecimalFormat("#.#").format(countSum/1440) : new DecimalFormat("#.#").format(countSum) )));}
								if (doStdDev){csvWriter.append(","+(legitCount==0 ? "null" : new DecimalFormat("#.###").format(weightedStdDevs/countSum) ));}
								
								
								//csvWriter.append((averagedData[j][i]==null ? "null" : Double.toString(averagedData[j][i])));
								//if (includeCount){csvWriter.append(","+(countData[j][i]==null ? "null" : (samplePeriod.equals("Monthly")? Double.toString(Math.round(countData[j][i]/1440)) : Double.toString(countData[j][i]) )));}
								//if (doStdDev){csvWriter.append(","+(stdDevData[j][i]==null ? "null" : Double.toString(stdDevData[j][i])));}
								
								
								if (i<sourceList.size()-1){
									csvWriter.append(",");
								}
							}
						}
						else if (analysisType.equals("lightsum")){
							for (int i=0;i<sourceList.size();i++){
								
								int legitCount = (averagedData[j][i]==null?0:1);
								double sums = (averagedData[j][i]==null? 0 : averagedData[j][i]);
								double countSum = (countData[j][i]==null? 0 : countData[j][i]);
								while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){
									legitCount += (averagedData[j][i+1]==null?0:1);
									sums += (averagedData[j][i+1]==null? 0 : averagedData[j][i+1]);
									countSum += (countData[j][i+1]==null? 0 : countData[j][i+1]);
									i++;
								}
								
								csvWriter.append((legitCount==0 ? "null" : (samplePeriod.equals("Monthly")? new DecimalFormat("#.#").format(sums/1440) : new DecimalFormat("#.###").format(sums) ) ));
								if (includeCount){csvWriter.append(","+(legitCount==0 ? "null" : (samplePeriod.equals("Monthly")? new DecimalFormat("#.#").format(countSum/1440) : new DecimalFormat("#.#").format(countSum) )));}
								
								
								//csvWriter.append((averagedData[j][i]==null ? "null" : Double.toString(averagedData[j][i])));
								//if (includeCount){csvWriter.append(","+(countData[j][i]==null ? "null" : (samplePeriod.equals("Monthly")? Double.toString(Math.round(countData[j][i]/1440)) : Double.toString(countData[j][i]) )));}
								//if (doStdDev){csvWriter.append(","+(stdDevData[j][i]==null ? "null" : Double.toString(stdDevData[j][i])));}
								
								
								if (i<sourceList.size()-1){
									csvWriter.append(",");
								}
							}
						}
						else{
							for (int i=0;i<sourceList.size();i++){

								csvWriter.append((averagedData[j][i]==null ? "null" : Double.toString(averagedData[j][i])));
								if (includeCount){csvWriter.append(","+(countData[j][i]==null ? "null" : (samplePeriod.equals("Monthly")? Double.toString(Math.round(countData[j][i]/1440)) : Double.toString(countData[j][i]) )));}
								if (doStdDev){csvWriter.append(","+(stdDevData[j][i]==null ? "null" : Double.toString(stdDevData[j][i])));}
								
								if (i<sourceList.size()-1){
									csvWriter.append(",");
								}
							}
						}
						csvWriter.append("\r\n");
					}
	
				} catch (SQLException sE) {
					sE.printStackTrace();
				}
	
				Date endProcessTime = new Date();
				logWindow.println("Total Extraction Time: "+getTimeString(endProcessTime.getTime()-startProcessTime.getTime())+"\r\n");
				csvWriter.close();
			}	catch (IOException e){
				JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
			}
		}
		else{ //File not selected or invalid file
			logWindow.println("No valid file selected.");
		}
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
