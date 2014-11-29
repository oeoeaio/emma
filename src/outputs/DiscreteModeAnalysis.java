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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;

public class DiscreteModeAnalysis implements Runnable{
	
	final private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	final private SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	final private SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	final private LogWindow logWindow;
	final private Connection dbConn;
	final private int[] selectedSources;
	final private String[] sourceNames;
	final private int siteID;
	final private String siteName;
	final private long startDate;
	final private long endDate;
	final private int samplePeriod;
	final private double[][] thresholdValues;
	
	DiscreteModeAnalysis(LogWindow logWindow,Connection dbConn,int[] selectedSources,String[] sourceNames,int siteID,String siteName,long startDate,long endDate,int samplePeriod,double[][] thresholdValues){
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
		this.thresholdValues = thresholdValues;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastDiscreteModeAnalysisSave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./DiscreteMode_Site"+siteName+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastDiscreteModeAnalysisSave", fileChooser.getSelectedFile().getParent());
			File fileToWrite = fileChooser.getSelectedFile();
			
			Date startProcessTime = new Date();
				FileWriter csvWriter = null;
				try{
					if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
					csvWriter = new FileWriter(fileToWrite);//open the file for writing
					
					logWindow.println("Attempting to extract data from database and write to file...\r\n");					

					try {
						/*Statement getHeader_statement = dbConn.createStatement();
						
						String countSPsSQL =  "SELECT COUNT(*) AS spCount FROM header_log WHERE siteID = "+siteID+" AND date_time BETWEEN (SELECT MAX(date_time) FROM header_log WHERE siteID = "+siteID+" AND date_time < '"+sqlDateFormatter.format(startDate)+"') AND '"+sqlDateFormatter.format(endDate)+"'";
						ResultSet countSPsRS = getHeader_statement.executeQuery(countSPsSQL);
						
						int spCount = 0;
						if (countSPsRS.next()){
							spCount = countSPsRS.getInt("spCount");
						}
						
						String headerValuesSQL =  "SELECT date_time,wl_ch_names,wl_sensor_chs FROM header_log WHERE siteID = "+siteID+" AND date_time BETWEEN (SELECT MAX(date_time) FROM header_log WHERE siteID = "+siteID+" AND date_time < '"+sqlDateFormatter.format(startDate)+"') AND '"+sqlDateFormatter.format(endDate)+"' ORDER BY date_time";
						ResultSet headerValuesRS = getHeader_statement.executeQuery(headerValuesSQL);
						
						long[] headerDateTime = new long[spCount];
						String[][] channelTypes = new String[spCount][48];
						String[][] channelNames = new String[spCount][48];
						for (int i=0;headerValuesRS.next() && i<spCount;i++){
							headerDateTime[i] = headerValuesRS.getTimestamp("date_time").getTime();
							channelTypes[i] = headerValuesRS.getString("wl_sensor_chs").split(",");
							channelNames[i] = headerValuesRS.getString("wl_ch_names").split(",");
						}
						headerValuesRS.close();
						getHeader_statement.close();						
						
						for (int k=0;k<headerDateTime.length;k++){ //Cycle through sub periods and check for changes in channel name or channel type
							for (int i=0;i<chStates.length;i++){
								if (chStates[i] && k>0){
									if (channelTypes[k][i].equals(channelTypes[0][i]) == false){
										chStates[i] = false;
										logWindow.println("ERROR: Channel Type changes in channel "+(i+1)+". This channel will not be processed.");
									}
									else if (channelNames[k][i].equals(channelNames[0][i]) == false){
										logWindow.println("WARNING: Channel Name changes in channel "+(i+1)+". Change is from "+channelNames[0][i]+" to "+channelNames[k][i]);
									}
								}
							}
						}
						*/
						
						int rowsRequired = (int)((endDate-startDate+(1000*60*60*24))/(1000*60*samplePeriod));
						int colGroupsRequired = getColGroupsRequired();
						Double[][] powerAvgs = new Double [rowsRequired][colGroupsRequired];
						Double[][] totTimes = new Double [rowsRequired][colGroupsRequired];
						long rowDates[] = new long[rowsRequired];
						
						Calendar rollDate = new GregorianCalendar();
						rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						rollDate.setTimeInMillis(startDate);
						for (int i=0;i<rowDates.length;i++){
							//add sample period on at the beginning so that the first point represents the first block FOLLOWING the start date, except for whole days
							if (samplePeriod == 10){
								rollDate.add(Calendar.MINUTE, 10);
							}
							else if (samplePeriod == 60){
								rollDate.add(Calendar.HOUR_OF_DAY, 1);						
							}
							
							rowDates[i] = rollDate.getTimeInMillis();
							if (samplePeriod == 1440){ //afterwards because don't need to add a day on at the start
								rollDate.add(Calendar.DAY_OF_MONTH, 1);						
							}
						}
						
						String blockString = "";
						String groupByString = "";
						if (samplePeriod == 10){
							blockString = "UNIX_TIMESTAMP(DATE(date_time)) AS blockDate_ts,HOUR(date_time) AS blockHour,FLOOR(MINUTE(date_time)/10)*10 AS blockMinute";
							groupByString = "blockDate_ts,blockHour,blockMinute";
						}
						else if (samplePeriod == 60){
							blockString = "UNIX_TIMESTAMP(CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN DATE(DATE_ADD(date_time,INTERVAL 1 HOUR)) ELSE DATE(date_time) END) AS blockDate_ts,CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 24 THEN 0 ELSE CEIL(HOUR(date_time)+MINUTE(date_time)/60) END AS blockHour";
							groupByString = "blockDate_ts,blockHour";
						}
						else if (samplePeriod == 1440){
							blockString = "UNIX_TIMESTAMP(DATE(DATE_SUB(date_time, INTERVAL 1 MINUTE))) AS blockDate_ts";
							groupByString = "blockDate_ts";
						}
						else{
							logWindow.println("ERROR: Sample period not recognised, channels will not be processed");
						}
						System.out.println("1: "+blockString+" "+groupByString);
						if (blockString.equals("") == false && groupByString.equals("") == false){ //required variables are present
							for (int i=0;i<selectedSources.length;i++){
								int colGroupCounter = getColGroupPointer(i);
								int modeCount = getModeCount(i);

								SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
								SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
								hourFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								minuteFormat.setTimeZone(TimeZone.getTimeZone("GMT+10"));

								String minDateString = csvDateFormatter.format(startDate)+" 00:01:00";
								String maxDateString = csvDateFormatter.format(endDate+(24*60*60000))+" 00:00:00";
								
								String avgTotString = "COUNT(value) AS totTime, ROUND(AVG(value),3) AS avgPwr";

								//Define bands 
								String modeString = "";
								if (modeCount == 1){
									modeString = "0 AS modeNo";
								}
								if (modeCount == 2){
									modeString = "IF(value<="+thresholdValues[i][0]+",0,1) AS modeNo";
								}
								else if (modeCount == 3){
									modeString = "IF(value<="+thresholdValues[i][0]+",0,IF(value<="+thresholdValues[i][1]+",1,2)) AS modeNo";
								}
								else if (modeCount == 4){
									modeString = "IF(value<="+thresholdValues[i][0]+",0,IF(value<="+thresholdValues[i][1]+",1,IF(value<="+thresholdValues[i][2]+",2,3))) AS modeNo";
								}
								else if (modeCount == 5){
									modeString = "IF(value<="+thresholdValues[i][0]+",0,IF(value<="+thresholdValues[i][1]+",1,IF(value<="+thresholdValues[i][2]+",2,IF(value<="+thresholdValues[i][3]+",3,4)))) AS modeNo";
								}

								System.out.println("2: "+minDateString+" "+maxDateString+" ");
								if (!minDateString.equals("") && !maxDateString.equals("") && !avgTotString.equals("")){ //max sure required variables are in place
									String getDataSQL =  "SELECT "+blockString+","+avgTotString+","+modeString+" FROM data_sa WHERE site_id = "+siteID+" AND source_id = "+selectedSources[i]+" AND date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' GROUP BY "+groupByString+",modeNo";
									System.out.println(getDataSQL);
									Statement getData_statement = dbConn.createStatement();
									ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);

									int rowCounter = 0;

									Calendar dbDate = new GregorianCalendar();
									dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
									dbDate.setTimeInMillis(startDate);
									int dbMode = 0; //mode No. from DB
									int modeTracker = 0; //keeps track of what the mode No. should be
									while (getDataRS.next()){
										if (samplePeriod == 10){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											dbDate.add(Calendar.MINUTE,getDataRS.getInt("blockHour")*60+getDataRS.getInt("blockMinute"));//add hour and minute components to date
											dbMode = getDataRS.getInt("modeNo");
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												while (modeTracker<modeCount){
													totTimes[rowCounter][colGroupCounter+modeTracker] = null;
													powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
													modeTracker++;
												}
												rowCounter++;
												modeTracker = 0;
											}
											while (modeTracker<dbMode){
												totTimes[rowCounter][colGroupCounter+modeTracker] = null;
												powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
												modeTracker++;
											}
										}
										else if (samplePeriod == 60){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											dbDate.add(Calendar.HOUR,getDataRS.getInt("blockHour")); //add hour component to date
											dbMode = getDataRS.getInt("modeNo");
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												while (modeTracker<modeCount){
													totTimes[rowCounter][colGroupCounter+modeTracker] = null;
													powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
													modeTracker++;
												}
												rowCounter++;
												modeTracker = 0;
											}
											while (modeTracker<dbMode){
												totTimes[rowCounter][colGroupCounter+modeTracker] = null;
												powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
												modeTracker++;
											}
										}
										else if (samplePeriod == 1440){
											dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
											dbMode = getDataRS.getInt("modeNo");
											while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
												while (modeTracker<modeCount){
													totTimes[rowCounter][colGroupCounter+modeTracker] = null;
													powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
													modeTracker++;
												}
												rowCounter++;
												modeTracker = 0;
											}
											while (modeTracker<dbMode){
												totTimes[rowCounter][colGroupCounter+modeTracker] = null;
												powerAvgs[rowCounter][colGroupCounter+modeTracker] = null;
												modeTracker++;
											}
										}
										totTimes[rowCounter][colGroupCounter+modeTracker] = getDataRS.getDouble("totTime");
										powerAvgs[rowCounter][colGroupCounter+modeTracker] = getDataRS.getDouble("avgPwr");

										if (modeTracker==(modeCount-1)){
											rowCounter++;
											modeTracker = 0;
										}
										else{
											modeTracker++;
										}
									}


									//getDataRS.close();
									getData_statement.close();



								}
							}
						}
						
						//Headers
						logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
						csvWriter.append("Site : "+siteName+"\r\n");
						csvWriter.append(",");
						//int columnCounter = 0;
						
						for (int i=0;i<selectedSources.length;i++){
							int modeCount = getModeCount(i);
							for (int j=0;j<modeCount;j++){
								csvWriter.append(sourceNames[i]+","+sourceNames[i]); //one for pwr column, one for time column
								if (i<selectedSources.length-1 || j<modeCount-1){
									csvWriter.append(",");
								}
							}
						}
						csvWriter.append("\r\n");
						
						csvWriter.append("Date,");
						for (int i=0;i<selectedSources.length;i++){
							int modeCount = getModeCount(i);
							for (int j=0;j<modeCount;j++){
								if (modeCount==1){csvWriter.append("all,all");}
								else if (j==0){csvWriter.append("<="+thresholdValues[i][j]+",<="+thresholdValues[i][j]);}
								else if (j==(modeCount-1)){csvWriter.append(">"+thresholdValues[i][j-1]+",>"+thresholdValues[i][j-1]);}
								else{csvWriter.append(thresholdValues[i][j-1]+"-"+thresholdValues[i][j]+","+thresholdValues[i][j-1]+"-"+thresholdValues[i][j]);}
								if (i<selectedSources.length-1 || j<(modeCount-1)){
									csvWriter.append(",");
								}
							}
						}
						csvWriter.append("\r\n");
						
						csvWriter.append("Date,");
						for (int i=0;i<selectedSources.length;i++){
							int modeCount = getModeCount(i);
							for (int j=0;j<modeCount;j++){
								csvWriter.append("TimeInMode,AvPower"); //one for pwr column, one for time column
								if (i<selectedSources.length-1 || j<modeCount-1){
									csvWriter.append(",");
								}
							}
						}
						csvWriter.append("\r\n");
						
						logWindow.println("Done.");
						
						for(int i=0;i<rowsRequired;i++){
							csvWriter.append(sqlDateFormatter.format(rowDates[i])+",");
							for (int j=0;j<colGroupsRequired;j++){
								csvWriter.append((totTimes[i][j]==null ? Integer.toString(0) : Double.toString(totTimes[i][j])));
								csvWriter.append(",");
								csvWriter.append((powerAvgs[i][j]==null ? "null" : Double.toString(powerAvgs[i][j])));
								if (j<colGroupsRequired){
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
					JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
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
	
	int getModeCount(int sourceIndex){
		int modeCount = 1;
		for (int i=0;i<thresholdValues[sourceIndex].length;i++){
			if (thresholdValues[sourceIndex][i]>=0){
				modeCount++;
			}
			else{
				break;
			}
		}
		return modeCount;
	}
	
	int getColGroupsRequired(){
		int colGroupsRequired = 0;
		for (int i=0;i<selectedSources.length;i++){
			colGroupsRequired = colGroupsRequired + getModeCount(i);
		}
		System.out.println("Col Groups Required: "+colGroupsRequired);
		return colGroupsRequired;
	}
	
	int getColGroupPointer(int sourceIndex){
		int colGroupPointer = 0;
		for (int i=0;i<sourceIndex;i++){
			colGroupPointer = colGroupPointer + getModeCount(i);
		}
		return colGroupPointer;
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
