package tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;

public class MissingSummaryAnalysis implements Runnable{
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final LogWindow logWindow;
	private final Connection dbConn;
	private final int[] selectedSources;
	private final String[] sourceNames;
	private final int siteID;
	private final String siteName;
	private final long startDate;
	private final long endDate;
	private final int samplePeriod;
	//private final String analysisType;
	
	
	
	MissingSummaryAnalysis(LogWindow logWindow,Connection dbConn,int[] selectedSources,String[] sourceNames,int siteID,String siteName,long startDate,long endDate,int samplePeriod){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.logWindow = logWindow;
		this.dbConn = dbConn;
		this.selectedSources = selectedSources;
		this.sourceNames = sourceNames;
		this.siteID = siteID;
		this.siteName = siteName;
		this.startDate = startDate;
		this.endDate = endDate;
		this.samplePeriod = samplePeriod;
		//this.analysisType = analysisType;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastMissingSummarySave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./"+"MissingSummary_Site"+siteName+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastMissingSummarySave", fileChooser.getSelectedFile().getParent());
			File fileToWrite = fileChooser.getSelectedFile();
				
			Date startProcessTime = new Date();
			try{
				if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
				FileWriter csvWriter = new FileWriter(fileToWrite);//open the file for writing				
	
				try {						
					int colsRequired = (int)((endDate-startDate)/(1000*60*samplePeriod));
					Double[][] averagedData = new Double [colsRequired][selectedSources.length];
					long rowDates[] = new long[colsRequired];
					String rowDateStrings[] = new String[colsRequired];
	
					Calendar rollDate = new GregorianCalendar();
					rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					rollDate.setTimeInMillis(startDate);
					for (int i=0;i<rowDates.length;i++){
						//add sample period on at the beginning for 10 minute and hour cases so that the first point represents the first block FOLLOWING the start date
						//sample period is added afterwards for daily (so date is represented by actual day, rather than following day)
						if (samplePeriod == 1){
							rollDate.add(Calendar.MINUTE, 1);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod == 10){
							rollDate.add(Calendar.MINUTE, 10);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod == 60){
							rollDate.add(Calendar.HOUR_OF_DAY, 1);
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
						}
						else if (samplePeriod == 1440){
							rowDates[i] = rollDate.getTimeInMillis();
							rowDateStrings[i] = csvDateFormatter.format(rowDates[i]);
							rollDate.add(Calendar.DAY_OF_MONTH, 1);
						}
					}
					
					//Headers
					logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
					csvWriter.append("Site: "+siteName+"\r\n");
					csvWriter.append("Source Name,Meter SN (most recent),");
					String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
					String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";
					
					
					//Get Meter Serial Numbers
					String meterSerials[] = new String[sourceNames.length];
					Statement getSerial_Statement = dbConn.createStatement();
					ResultSet getSerialRS;
					for (int i=0;i<meterSerials.length;i++){
						String getDataSQL =  "SELECT DISTINCT meter_sn FROM files WHERE site_id = "+siteID+" AND source_id = "+selectedSources[i]+" AND start_date <= '"+maxDateString+"' AND end_date >= '"+minDateString+"' ORDER BY end_date DESC";
						getSerialRS = getSerial_Statement.executeQuery(getDataSQL);
						if (getSerialRS.next()){
							meterSerials[i] = getSerialRS.getString("meter_sn");
							if (getSerialRS.wasNull()){
								meterSerials[i] = "N/A";
							}
						}
						else{
							meterSerials[i] = "N/A";
						}
					}
					
					
					/*for (int i=0;i<selectedSources.length;i++){
						csvWriter.append(sourceNames[i]+"");
						if (i<selectedSources.length-1){
							csvWriter.append(",");
						}
					}*/
					
					
					for (int i=0;i<colsRequired;i++){
						csvWriter.append(rowDateStrings[i]);
						if (i<colsRequired-1){
							csvWriter.append(",");
						}
					}

					csvWriter.append("\r\n");
					
					logWindow.println("Done.");
					
	
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
						logWindow.println("ERROR: Sample period not recognised, channels will not be processed");
					}
	
					if (blockString.equals("") == false && groupByString.equals("") == false){ //required variables are present
						for (int i=0;i<selectedSources.length;i++){
							logWindow.printString("Extracting Data for "+sourceNames[i]+".....");
	
							SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
							SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
							hourFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
							minuteFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
	
							//String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
							//String maxDateString = csvDateFormatter.format(endDate)+" 00:00:00";
	
	
							String valueString = "ROUND(1-(COUNT(*)/"+samplePeriod+"),2) AS analysisValue"; //percentage of each day
							String joinString = "";
	
							if (minDateString.equals("") == false && maxDateString.equals("") == false && valueString.equals("") == false){ //max sure required variables are in place
								String getDataSQL =  "SELECT "+blockString+","+valueString+" FROM data_sa "+joinString+" WHERE data_sa.site_id = "+siteID+" AND data_sa.source_id = "+selectedSources[i]+" AND data_sa.date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' AND value IS NOT NULL "+groupByString;
								//System.out.println(getDataSQL);
								Statement getData_statement = dbConn.createStatement();
								ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);
	
								int rowCounter = 0;
	
								Calendar dbDate = new GregorianCalendar();
								dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								dbDate.setTimeInMillis(startDate);
								while (getDataRS.next()){
									if (samplePeriod == 10 || samplePeriod == 1){
										dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
										dbDate.add(Calendar.MINUTE,getDataRS.getInt("blockHour")*60+getDataRS.getInt("blockMinute"));
										while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
											averagedData[rowCounter][i] = 1.0;
											rowCounter++;
										}
									}
									else if (samplePeriod == 60){
										dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
										dbDate.add(Calendar.HOUR,getDataRS.getInt("blockHour"));
										while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
											averagedData[rowCounter][i] = 1.0;
											rowCounter++;
										}
									}
									else if (samplePeriod == 1440){
										dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
										while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
											averagedData[rowCounter][i] = 1.0;
											rowCounter++;
										}
									}
	
									averagedData[rowCounter][i] = getDataRS.getDouble("analysisValue");
									
									if(getDataRS.wasNull()){averagedData[rowCounter][i] = 1.0;}
									
	
									rowCounter++;
								}
	
								getDataRS.close();
								getData_statement.close();
								logWindow.println("Done.");
							}
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
					
					for (int i=0;i<selectedSources.length;i++){
						csvWriter.append(sourceNames[i]+","+meterSerials[i]+",");
						for(int j=0;j<colsRequired;j++){
							csvWriter.append((averagedData[j][i]==null ? "1.0" : Double.toString(averagedData[j][i])));
							if (j<colsRequired-1){
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
