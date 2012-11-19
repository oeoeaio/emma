package outputs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;
import endUseWindow.Source;

public class TimeOfDayAnalysis implements Runnable{
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final LogWindow logWindow;
	private final Connection dbConn;
	private final LinkedList<Source> sourceList;
	private final LinkedList<long[]> customStartAndEndDates;
	private final LinkedList<String> rangeTitles;
	private final long startDate;
	private final long endDate;
	private final int samplePeriod;
	private final String analysisType;
	private final String splitType;
	private final boolean multipleSites;
	
	
	TimeOfDayAnalysis(LogWindow logWindow,Connection dbConn,LinkedList<Source> sourceList,LinkedList<long[]> customStartAndEndDates,LinkedList<String> rangeTitles,long startDate,long endDate,int samplePeriod,String analysisType,String splitType,boolean multipleSites){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.logWindow = logWindow;
		this.dbConn = dbConn;
		this.sourceList = sourceList;
		this.customStartAndEndDates = customStartAndEndDates;
		this.rangeTitles = rangeTitles;
		this.startDate = startDate;
		this.endDate = endDate;
		this.samplePeriod = samplePeriod;
		this.analysisType = analysisType;
		this.splitType = splitType;
		this.multipleSites = multipleSites;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastTODAnalysisSave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./"+(analysisType.equals("normal")?"TimeOfDay":"CircuitKnownTimeOfDay")+"_"+(multipleSites?"MultiSite":"Site"+sourceList.get(0).getSite().getSiteName())+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastTODAnalysisSave", fileChooser.getSelectedFile().getParent());			
			File fileToWrite = fileChooser.getSelectedFile();
			
			Date startProcessTime = new Date();
				try{
					if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
					FileWriter csvWriter = new FileWriter(fileToWrite);//open the file for writing
					
					//Headers
					logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
					
					if (splitType.equals("none")){
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
						
						csvWriter.append((samplePeriod>=1440?"Date,":"Time,"));
						for (int i=0;i<sourceList.size();i++){
							csvWriter.append(sourceList.get(i).getSourceName());
							if (i<sourceList.size()-1){
								csvWriter.append(",");
							}
						}
						csvWriter.append("\r\n");
					}
					/*else if (splitType.equals("monthly")){
						csvWriter.append("Site,Source,"+(samplePeriod>=1440?"Date,":"Time,"));
						Source firstSource = sourceList.get(0);
						for (int i=0;i<rangeTitles.size() && sourceList.get(i).equals(firstSource);i++){
							csvWriter.append(rangeTitles.get(i));
							if (i<sourceList.size()-1){
								csvWriter.append(",");
							}
						}
					}*/
					
					
					logWindow.println("Done.");
					
					logWindow.println("Attempting to extract data from database and write to file...\r\n");					

					try {						
						int periodsRequired = (int)(1440/samplePeriod);
						Double[][] timeOfDayData = new Double [periodsRequired][sourceList.size()];
						Double[][] countData = new Double [periodsRequired][sourceList.size()];
						int[] rowDayMinutes = new int[periodsRequired];
						String[] rowTimeStrings = new String[periodsRequired];
						
						int rollTime = 0;
						for (int i=0;i<rowTimeStrings.length;i++){
							//add sample period on at the beginning for 10 minute and hour cases so that the first point represents the first block FOLLOWING the start date
							//sample period is added afterwards for daily (so date is represented by actual day, rather than following day)
							rollTime += samplePeriod;
							rowDayMinutes[i] = rollTime;
							long rowHours = TimeUnit.HOURS.convert(rollTime, TimeUnit.MINUTES);
							long rowMins = rollTime-(rowHours*60);
							if (rowHours == 24){rowHours = 0;};
							rowTimeStrings[i] = (rowHours<10?"0"+rowHours:rowHours)+":"+(rowMins<10?"0"+rowMins:rowMins);
						}
						
						if (splitType.equals("monthly")){
							csvWriter.append("Site,Source,Month,");
							for (int i=0;i<rowTimeStrings.length;i++){
								csvWriter.append(rowTimeStrings[i]);
								csvWriter.append(",");
							}
							for (int i=0;i<rowTimeStrings.length;i++){
								csvWriter.append(rowTimeStrings[i]);
								if (i<rowTimeStrings.length-1){
									csvWriter.append(",");
								}
							}
							csvWriter.append("\r\n");
							csvWriter.append("Site,Source,Month,");
							for (int i=0;i<rowTimeStrings.length;i++){
								csvWriter.append("Value");
								csvWriter.append(",");
							}
							for (int i=0;i<rowTimeStrings.length;i++){
								csvWriter.append("Count");
								if (i<rowTimeStrings.length-1){
									csvWriter.append(",");
								}
							}
							csvWriter.append("\r\n");
						}
						
						String blockString = "";
						String groupByString = "";
						if (samplePeriod == 1){
							blockString = "HOUR(date_time) AS blockHour,MINUTE(date_time) AS blockMinute";
							groupByString = "GROUP BY blockHour,blockMinute";
						}
						else if (samplePeriod == 10){
							blockString = "CASE WHEN HOUR(date_time)+MINUTE(date_time)/60 > 23+(5/6) THEN 0 ELSE CASE WHEN MINUTE(date_time) > 50 THEN CEIL(HOUR(date_time)+MINUTE(date_time)/60) ELSE FLOOR(HOUR(date_time)+MINUTE(date_time)/60) END END AS blockHour,CASE WHEN MINUTE(date_time) > 50 THEN 0 ELSE CEIL(MINUTE(date_time)/10)*10 END AS blockMinute";
							groupByString = "GROUP BY blockHour,blockMinute";
						}
						else if (samplePeriod == 60){
							blockString = "CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN 0 ELSE CEIL(HOUR(date_time)+MINUTE(date_time)/60) END AS blockHour";
							groupByString = "GROUP BY blockHour";
						}
						else{
							logWindow.println("ERROR: Sample period not recognised, data will not be processed");
						}

						if (blockString.equals("") == false && groupByString.equals("") == false){ //required variables are present
							for (int i=0;i<sourceList.size();i++){
								logWindow.printString("Extracting Data for "+sourceList.get(i).getSourceName()+".....");

								//SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
								//SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
								//hourFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								//minuteFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));

								//String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
								//String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";

								String minDateString = csvDateFormatter.format(customStartAndEndDates.get(i)[0]);
								String maxDateString = csvDateFormatter.format(customStartAndEndDates.get(i)[1]);

								
								String avgTotString = "ROUND(AVG(value),3) AS analysisValue,COUNT(*) AS record_count, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
								String joinString = "LEFT JOIN files USING (file_id)";
								int circuitFrequency = 0;
								
								
								if (analysisType.equals("normal")){
									String sourceTypeSQL = "SELECT source_type,measurement_type FROM sources WHERE site_id = "+sourceList.get(i).getSite().getSiteID()+" AND source_id = "+sourceList.get(i).getSourceID();
									ResultSet sourceTypeRS = dbConn.createStatement().executeQuery(sourceTypeSQL);
									
									if (sourceTypeRS.next()){ //change conversion factor
										if (sourceTypeRS.getString("source_type").equals("Light") && sourceTypeRS.getString("measurement_type").equals("OnTime")){
											avgTotString = "ROUND(AVG(data_sa.value*(lights.wattage/files.frequency)),3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
											joinString = "LEFT JOIN files ON files.file_id = data_sa.file_id LEFT JOIN lights ON lights.source_id = data_sa.source_id";
										}	
									}
								}
								else if (analysisType.equals("circuitKnown")){
									avgTotString = "ROUND(IFNULL(AVG(appliance_load.sumvalue),0)+IFNULL(AVG(light_load.sumvalue),0),3) AS analysisValue, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";								
									
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
								

								if (minDateString.equals("") == false && maxDateString.equals("") == false && avgTotString.equals("") == false){ //max sure required variables are in place
									String getDataSQL =  "SELECT "+blockString+","+avgTotString+" FROM data_sa "+joinString+" WHERE data_sa.site_id = "+sourceList.get(i).getSite().getSiteID()+" AND data_sa.source_id = "+sourceList.get(i).getSourceID()+" AND data_sa.date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString;
									System.out.println(getDataSQL);
									Statement getData_statement = dbConn.createStatement();
									ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);

									int rowCounter = 0;

									int dbTime = 0;
									while (getDataRS.next()){
										if (samplePeriod == 10 || samplePeriod == 1){
											dbTime = getDataRS.getInt("blockHour")*60+getDataRS.getInt("blockMinute");
											while (rowDayMinutes[rowCounter]<dbTime){
												timeOfDayData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												rowCounter++;
											}
										}
										else if (samplePeriod == 60){
											dbTime = getDataRS.getInt("blockHour")*60;
											while (rowDayMinutes[rowCounter]<dbTime){
												timeOfDayData[rowCounter][i] = null;
												countData[rowCounter][i] = null;
												rowCounter++;
											}
										}

										timeOfDayData[rowCounter][i] = getDataRS.getDouble("analysisValue");
										
										if(getDataRS.wasNull()){timeOfDayData[rowCounter][i] = null;}
										
										countData[rowCounter][i] = getDataRS.getDouble("pointCount");
										
										if (getDataRS.wasNull()){countData[rowCounter][i] = null;}

										rowCounter++;
									}

									getDataRS.close();
									getData_statement.close();
									logWindow.println("Done.");
								}
							}
						}
						/*if (splitType.equals("monthly")){
							for (int i=0;i<sourceList.size();i++){
								Source currSource = sourceList.get(i);
								int currBaseIndex = i;
								for(int j=0;j<periodsRequired;j++){
									i = currBaseIndex;
									csvWriter.append(currSource.getSite().getSiteName()+","+currSource.getSourceName()+","+rowTimeStrings[j]+",");
									csvWriter.append((timeOfDayData[j][i]==null ? "null" : Double.toString(timeOfDayData[j][i])));
									while(i+1 < sourceList.size() && sourceList.get(i+1).equals(currSource)){
										i++;
										csvWriter.append(",");
										csvWriter.append((timeOfDayData[j][i]==null ? "null" : Double.toString(timeOfDayData[j][i])));
									}
									csvWriter.append("\r\n");
								}
							}
						}*/
						if (splitType.equals("monthly")){
							for (int i=0;i<sourceList.size();i++){
								csvWriter.append(sourceList.get(i).getSite().getSiteName()+","+sourceList.get(i).getSourceName()+","+rangeTitles.get(i)+",");
								for (int j=0;j<periodsRequired;j++){
									csvWriter.append((timeOfDayData[j][i]==null ? "null" : Double.toString(timeOfDayData[j][i])));
									csvWriter.append(",");
								}
								for (int j=0;j<periodsRequired;j++){
									csvWriter.append((countData[j][i]==null ? "null" : Double.toString(countData[j][i])));
									if (j<periodsRequired-1){
										csvWriter.append(",");
									}
								}
								csvWriter.append("\r\n");
							}
						}
						else if (splitType.equals("none")){
							for(int j=0;j<periodsRequired;j++){
								csvWriter.append(rowTimeStrings[j]+",");
								for (int i=0;i<sourceList.size();i++){
									csvWriter.append((timeOfDayData[j][i]==null ? "null" : Double.toString(timeOfDayData[j][i])));
									if (i<sourceList.size()-1){
										csvWriter.append(",");
									}
								}
								csvWriter.append("\r\n");
							}
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
