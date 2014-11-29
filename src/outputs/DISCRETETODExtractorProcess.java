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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;

public class DISCRETETODExtractorProcess implements Runnable{
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	LogWindow logWindow;
	Connection dbConn;
	int[] modChCount;
	boolean[] chStates;
	int totalChCount;
	int installation_id;
	int installation_id_INDEX;
	Calendar startDate;
	Calendar endDate;
	String analysisPeriod;
	double[][] thresholdValues;
	String powerOrTime = "power";
	
	
	DISCRETETODExtractorProcess(LogWindow logWindow_P,Connection dbConn_P,boolean[] chStates_P,int totalChCount_P,int installation_id_P,int installation_id_INDEX_P,Calendar startDate_P,Calendar endDate_P,String analysisPeriod_P,double[][] thresholdValues_P){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		logWindow = logWindow_P;
		dbConn = dbConn_P;
		chStates = chStates_P;
		totalChCount = totalChCount_P;
		installation_id = installation_id_P;
		installation_id_INDEX = installation_id_INDEX_P;
		startDate = startDate_P;
		endDate = endDate_P;
		analysisPeriod = analysisPeriod_P;
		thresholdValues = thresholdValues_P;
	}
	
	public void run(){
		logWindow.setVisible(true);

		JFileChooser fChooser = new JFileChooser();
		String fileName = "./REMP_TimeOfDay_House"+installation_id+"_"+csvDateFormatter.format(startDate.getTimeInMillis())+"_"+csvDateFormatter.format(endDate.getTimeInMillis()-(1000*60*60*24))+".csv";
		fChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fChooser.showSaveDialog(fChooser);
		while (fChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fChooser.showSaveDialog(fChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			File fileToWrite = fChooser.getSelectedFile();
			
			Date startAllTime = new Date();
				try{
					if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
					FileWriter csvWriter = new FileWriter(fileToWrite);//open the file for writing
					
					ArrayList<Long[]> periodEnds = new ArrayList<Long[]>();
					if (analysisPeriod.equals("monthly")){ //set up periodEnds array
						Calendar rollDate = new GregorianCalendar();
						rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						rollDate.setTimeInMillis(startDate.getTimeInMillis()+(1000*60*60*24)); //add one day to startDate in case startDate is 01/xx/xxxx
						
						while (rollDate.get(Calendar.DAY_OF_MONTH)!=1 && rollDate.getTimeInMillis()<endDate.getTimeInMillis()){
							rollDate.add(Calendar.DAY_OF_MONTH, 1);
						}
						
						periodEnds.add(new Long[] {startDate.getTimeInMillis(),rollDate.getTimeInMillis()});
						Long prevRollDate = rollDate.getTimeInMillis();
						rollDate.add(Calendar.MONTH, 1);
						
						while (rollDate.getTimeInMillis()<endDate.getTimeInMillis()){
							if (rollDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) && rollDate.get(Calendar.MONTH) == endDate.get(Calendar.MONTH)){
								periodEnds.add(new Long[] {prevRollDate,rollDate.getTimeInMillis()});
								periodEnds.add(new Long[] {rollDate.getTimeInMillis(),endDate.getTimeInMillis()});
							}
							else{
								periodEnds.add(new Long[] {prevRollDate,rollDate.getTimeInMillis()});
							}
							prevRollDate = rollDate.getTimeInMillis();
							rollDate.add(Calendar.MONTH, 1);
						}
						
						if (rollDate.getTimeInMillis()==endDate.getTimeInMillis()){
							periodEnds.add(new Long[] {prevRollDate,endDate.getTimeInMillis()});
						}
						
						
					}
					else if (analysisPeriod.equals("selection")){
						periodEnds.add(new Long[] {startDate.getTimeInMillis(),endDate.getTimeInMillis()});
					}
					
					Calendar startDate_TEMP = startDate;
					Calendar endDate_TEMP = endDate;
					
					logWindow.println("Writing to file: "+fileToWrite.getName()+"...\r\n");
					
					for (int p=0;p<periodEnds.size();p++){
						
						Date startProcessTime = new Date();
						startDate_TEMP.setTimeInMillis(periodEnds.get(p)[0]);
						endDate_TEMP.setTimeInMillis(periodEnds.get(p)[1]);
						
						//Headers
						logWindow.printString("Writing Headers...");
						csvWriter.append("Installation ID "+installation_id);
						csvWriter.append(",,"+csvDateFormatter.format(startDate.getTimeInMillis())+"_"+csvDateFormatter.format(endDate.getTimeInMillis()-(1000*60*60*24)));
						csvWriter.append("\r\n");
						csvWriter.append(",");
						int columnCounter = 0;
						
						int modeCount[] = new int[48]; //holds the number of modes set for each channel
						for (int i=0;i<chStates.length;i++){
							if (chStates[i]){
								columnCounter++;
								modeCount[i] = getModeCount(i);
								for (int j=0;j<modeCount[i];j++){
									csvWriter.append("ch"+(i+1));
									if (columnCounter<totalChCount || j<modeCount[i]-1){
										csvWriter.append(",");
									}
								}
							}
						}
						csvWriter.append("\r\n");
						
						columnCounter = 0;
						csvWriter.append("Date,");
						for (int i=0;i<chStates.length;i++){
							if (chStates[i]){
								columnCounter++;
								for (int j=0;j<modeCount[i];j++){
									if (modeCount[i]==1){csvWriter.append("all");}
									else if (j==0){csvWriter.append("<="+thresholdValues[i][j]);}
									else if (j==(modeCount[i]-1)){csvWriter.append(">"+thresholdValues[i][j-1]);}
									else{csvWriter.append(thresholdValues[i][j-1]+"-"+thresholdValues[i][j]);}
									if (columnCounter<totalChCount || j<(modeCount[i]-1)){
										csvWriter.append(",");
									}
								}
							}
						}
						csvWriter.append("\r\n");
						
						logWindow.println("Done.");
						
						if (analysisPeriod.equals("monthly")){
							logWindow.printString("Attempting to extract data for "+csvDateFormatter.format(startDate_TEMP.getTimeInMillis())+" - "+csvDateFormatter.format(endDate_TEMP.getTimeInMillis())+"...");					
						}
						else{
							logWindow.printString("Attempting to extract data from database...");					
						}
						
						ArrayList<Double[][]> timeOfDayData = new ArrayList<Double[][]>();
						try {
							Statement getHeader_statement = dbConn.createStatement();
							String headerValuesSQL =  "SELECT wl_ch_names,wl_sensor_chs FROM header_log WHERE installation_id = "+installation_id+" ORDER BY date_time DESC";
							ResultSet headerValuesRS = getHeader_statement.executeQuery(headerValuesSQL);
							
							if (headerValuesRS.next()){
								Statement getData_Statement = dbConn.createStatement();
								
								for (int i=0;i<chStates.length;i++){ //Build SQL query
									if (chStates[i]){
										String channelName = headerValuesRS.getString("wl_ch_names").split(",")[i];
										String channelType = headerValuesRS.getString("wl_sensor_chs").split(",")[i];
										
										String dataType = ""; //raw of converted
										String readingTypeString = "";
										String avgTotString = "";
										
										//if (reading_type.equals("Wireless")){
										readingTypeString = "reading_type = 'WL'"; //wireless or ct
										
										int noOfDays = (int)((endDate.getTimeInMillis()-startDate.getTimeInMillis())/(1000*60*1440));
										
										if (channelType.equals("00") || channelType.equals("01") || channelType.equals("02") || channelType.equals("03") || channelType.equals("04") || channelType.equals("05") || channelType.equals("07")){
											dataType = "raw";
											if (powerOrTime.equals("time")){
												avgTotString = "COUNT("+dataType+")/"+noOfDays+" AS avgPwr"; //will return the sum of minutes in each ode rather than avg power
											}
											else{
												avgTotString = "ROUND(AVG("+dataType+"),3) AS avgPwr";
											}
										}
										else if (channelType.equals("90") || channelType.equals("91")  || channelType.equals("06")){
											dataType = "converted";
											if (powerOrTime.equals("time")){
												avgTotString = "COUNT("+dataType+")/"+noOfDays+" AS avgPwr"; //will return the sum of minutes in each ode rather than avg power
											}
											else{
												avgTotString = "ROUND(AVG("+dataType+"),3) AS avgPwr";
											}
										}
										else if (channelType.equals("80")){
											dataType = "converted";
											if (powerOrTime.equals("time")){
												avgTotString = "COUNT("+dataType+")/"+noOfDays+" AS avgPwr"; //will return the sum of minutes in each ode rather than avg power
											}
											else{
												avgTotString = "ROUND(SUM("+dataType+"),3) AS avgPwr";
											}
										}
										else if (channelType.equals("81")){
											if (channelName.matches("[lL][0-9]{1,3}[a-zA-Z]{0,1}")){ //light accumulator
												dataType = "converted";
												if (powerOrTime.equals("time")){
													avgTotString = "COUNT("+dataType+")/"+noOfDays+" AS avgPwr"; //will return the sum of minutes in each ode rather than avg power
												}
												else{
													avgTotString = "ROUND(AVG("+dataType+"),3) AS avgPwr";
												}
											}
											else if (channelName.equals("Lounge") || channelName.equals("Kitchen") || channelName.equals("Hallway1") || channelName.equals("Kitchen/Living") || channelName.equals("Bed 2")){ //motion sensor
												dataType = "converted";
												if (powerOrTime.equals("time")){
													avgTotString = "COUNT("+dataType+")/"+noOfDays+" AS avgPwr"; //will return the sum of minutes in each ode rather than avg power
												}
												else{
													avgTotString = "ROUND(SUM("+dataType+"),3) AS avgPwr";
												}
											}
											else{
												logWindow.println("ERROR: Unrecognised channel name \""+channelName+"\" for channel "+(i+1)+". Channel will be not be extracted.");
											}
										}
										else{
											logWindow.println("ERROR: Unrecognised channel type \""+channelType+"\" for channel "+(i+1)+". Channel will be not be extracted.");
										}
										//}
										//else{
										//	readingTypeString = "reading_type = 'CT' AND module_sn = "+module_sns[i];
										//	dataTypeString = "raw";
										//}
																				
										timeOfDayData.add(i,new Double [24][modeCount[i]]);
										
										//Define bands 
										String modeString = "";
										if (modeCount[i] == 1){
											modeString = "0 AS modeNo";
										}
										if (modeCount[i] == 2){
											modeString = "IF("+dataType+"<="+thresholdValues[i][0]+",0,1) AS modeNo";
										}
										else if (modeCount[i] == 3){
											modeString = "IF("+dataType+"<="+thresholdValues[i][0]+",0,IF("+dataType+"<="+thresholdValues[i][1]+",1,2)) AS modeNo";
										}
										else if (modeCount[i] == 4){
											modeString = "IF("+dataType+"<="+thresholdValues[i][0]+",0,IF("+dataType+"<="+thresholdValues[i][1]+",1,IF("+dataType+"<="+thresholdValues[i][2]+",2,3))) AS modeNo";
										}
										else if (modeCount[i] == 5){
											modeString = "IF("+dataType+"<="+thresholdValues[i][0]+",0,IF("+dataType+"<="+thresholdValues[i][1]+",1,IF("+dataType+"<="+thresholdValues[i][2]+",2,IF("+dataType+"<="+thresholdValues[i][3]+",3,4)))) AS modeNo";
										}
										
										//This SQL groups data by the hour (eg. 08:01:00-09:00:00)
										if (dataType.equals("") == false && readingTypeString.equals("") == false){
											String getDataSQL =  "SELECT CASE CEIL(HOUR(date_time)+MINUTE(date_time)/60) WHEN 0 THEN 24 ELSE CEIL(HOUR(date_time)+MINUTE(date_time)/60) END AS blockHour,"+avgTotString+","+modeString+" FROM data_all WHERE installation_id = "+installation_id+" AND "+readingTypeString+" AND channel = "+(i+1)+" AND date_time BETWEEN '"+csvDateFormatter.format(startDate_TEMP.getTimeInMillis())+" 00:01:00' AND '"+csvDateFormatter.format(endDate_TEMP.getTimeInMillis())+" 00:00:00' AND "+dataType+" IS NOT NULL GROUP BY blockHour,modeNo ORDER BY blockHour";
											System.out.println(getDataSQL);
											ResultSet getDataRS = getData_Statement.executeQuery(getDataSQL);
											int rowTracker = 0; //is equal to the hour minus one (hour-1)
											int dbMode = 0; //mode No. from DB
											int modeTracker = 0; //keeps track of what the mode No. should be
											int dbHour = 0; //keeps track of what the "hour" value associated with the current data point is
											while (getDataRS.next()){
												dbHour = getDataRS.getInt("blockHour");
												dbMode = getDataRS.getInt("modeNo");
												while (rowTracker<dbHour-1){
													while (modeTracker<modeCount[i]){
														timeOfDayData.get(i)[rowTracker][modeTracker] = null;
														modeTracker++;
													}
													rowTracker++;
													modeTracker = 0;
												}
												while (modeTracker<dbMode){
													timeOfDayData.get(i)[rowTracker][modeTracker] = null;
													modeTracker++;
												}				

												timeOfDayData.get(i)[rowTracker][modeTracker] = getDataRS.getDouble("avgPwr");
												
												if (modeTracker==(modeCount[i]-1)){
													rowTracker++;
													modeTracker = 0;
												}
												else{
													modeTracker++;
												}
											}
											getDataRS.close();
										}
									}
									else{
										timeOfDayData.add(i,new Double [0][0]); //must add an array to every index of timeOfDayData, these will be ignored however
									}
								}
								getData_Statement.close();
								
								logWindow.printString("Done.\r\nWriting data to file...");
								
								for (int hrIndex=0;hrIndex<24;hrIndex++){//write data to file
									csvWriter.append((hrIndex<9?"0":"")+(hrIndex+1)+":00:00,");
									columnCounter=0;
									for (int i=0;i<chStates.length;i++){
										if (chStates[i]){
											columnCounter++;
											for(int j=0;j<modeCount[i];j++){
												if (powerOrTime.equals("time")){
													csvWriter.append((timeOfDayData.get(i)[hrIndex][j] == null ? Integer.toString(0) : Double.toString(timeOfDayData.get(i)[hrIndex][j])));
												}
												else{
													csvWriter.append((timeOfDayData.get(i)[hrIndex][j] == null ? "null" : Double.toString(timeOfDayData.get(i)[hrIndex][j])));
												}
												if (columnCounter<totalChCount || j<modeCount[i]-1){
													csvWriter.append(",");
												}
											}
										}
									}
									csvWriter.append("\r\n");
									csvWriter.flush();
								}
								logWindow.println("Done.");
							}
							else{
								//TODO ERROR cannot extract headers
							}
							headerValuesRS.close();
							getHeader_statement.close();
						} catch (SQLException e) {
							//TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Date endProcessTime = new Date();
						
						if (analysisPeriod.equals("monthly")){
							csvWriter.append("\r\n");
							logWindow.println("Extraction Time for "+csvDateFormatter.format(startDate_TEMP.getTimeInMillis())+" - "+csvDateFormatter.format(endDate_TEMP.getTimeInMillis())+": "+getTimeString(endProcessTime.getTime()-startProcessTime.getTime())+"\r\n");
						}

					}
					
					Date endAllTime = new Date();
					logWindow.println("Total Extraction Time: "+getTimeString(endAllTime.getTime()-startAllTime.getTime())+"\r\n");

					
					csvWriter.close();
				}	catch (IOException e){
					JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
				}
		}
		else{ //File not selected or invalid file
			Thread runProcess = new Thread(new CTExtractor(dbConn,logWindow));
			runProcess.start();
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
	
	int getModeCount(int chIndex){
		int modeCount = 1;
		for (int i=0;i<thresholdValues[chIndex].length;i++){
			if (thresholdValues[chIndex][i]>0){
				modeCount++;
			}
			else{
				break;
			}
		}
		return modeCount;
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
