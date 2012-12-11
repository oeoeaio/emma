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

public class RawDataExport implements Runnable{
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat monthDateFormatter = new SimpleDateFormat("yyyy-MM");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final LogWindow logWindow;
	private final Connection dbConn;
	private final LinkedList<Source> sourceList;
	private final LinkedList<long[]> customStartAndEndDates;
	private final LinkedList<Integer> frequencies;
	private final boolean multipleSites;
	private final long startDate;
	private final long endDate;
	
	
	
	RawDataExport(LogWindow logWindow,Connection dbConn,LinkedList<Source> sourceList,LinkedList<long[]> customStartAndEndDates,LinkedList<Integer> frequencies,boolean multipleSites,long startDate,long endDate){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		monthDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.logWindow = logWindow;
		this.dbConn = dbConn;
		this.sourceList = sourceList;
		this.customStartAndEndDates = customStartAndEndDates;
		this.frequencies = frequencies;
		this.multipleSites = multipleSites;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastAvgAnalysisSave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./RawData_"+(multipleSites?"MultiSite":"Site"+sourceList.get(0).getSite().getSiteName())+"_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
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
					
					int rowsRequired = (int)((endDate-startDate)/(1000*60*minFreq));
					//System.out.println(1);
					//System.out.println(sqlDateFormatter.format(startDate)+" "+sqlDateFormatter.format(endDate));
					
					Double[][] rawData = new Double [rowsRequired][sourceList.size()];
					//Double[][] countData = new Double [rowsRequired][sourceList.size()];
					long rowDates[] = new long[rowsRequired];
					String rowDateStrings[] = new String[rowsRequired];
	
					Calendar rollDate = new GregorianCalendar();
					rollDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					rollDate.setTimeInMillis(startDate);
					
					for (int i=0;i<rowDates.length;i++){
						rollDate.add(Calendar.MINUTE, minFreq);
						rowDates[i] = rollDate.getTimeInMillis();
						rowDateStrings[i] = sqlDateFormatter.format(rowDates[i]);
					}
	
					String blockString = "";
					String groupByString = "";
					blockString = "UNIX_TIMESTAMP(DATE(date_time)) AS blockDate_ts,HOUR(date_time) AS blockHour,MINUTE(date_time) AS blockMinute";
					groupByString = "GROUP BY blockDate_ts,blockHour,blockMinute";
					
					/*if (samplePeriod == "Ten Minutely"){
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
					}*/
	
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
	
	
								String valueString = "ROUND(data_sa.value,3) AS rawData, ROUND(SUM(IF(data_sa.value IS NOT NULL,1,0)*(files.frequency/60)),1) AS pointCount";
								String joinString = "LEFT JOIN files USING (file_id)";
								

								//System.out.println(valueString);
								//System.out.println(joinString);
								//System.out.println(circuitFrequency);
	
	
								if (minDateString.equals("") == false && maxDateString.equals("") == false && valueString.equals("") == false){ //max sure required variables are in place
									String getDataSQL =  "SELECT "+blockString+","+valueString+" FROM data_sa "+joinString+" WHERE data_sa.site_id = "+sourceList.get(i).getSite().getSiteID()+" AND data_sa.source_id = "+sourceList.get(i).getSourceID()+" AND data_sa.date_time BETWEEN '"+minDateString+"' AND '"+maxDateString+"' "+groupByString;
									System.out.println(getDataSQL);
									Statement getData_statement = dbConn.createStatement();
									ResultSet getDataRS = getData_statement.executeQuery(getDataSQL);
	
									int rowCounter = 0;
	
									Calendar dbDate = new GregorianCalendar();
									dbDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
									dbDate.setTimeInMillis(startDate);
									while (getDataRS.next()){
										dbDate.setTimeInMillis(getDataRS.getLong("blockDate_ts")*1000);
										dbDate.add(Calendar.MINUTE,getDataRS.getInt("blockHour")*60+getDataRS.getInt("blockMinute"));
										while (rowDates[rowCounter]<dbDate.getTimeInMillis()){
											rawData[rowCounter][i] = null;
											//countData[rowCounter][i] = null;
											rowCounter++;
										}
	
										rawData[rowCounter][i] = getDataRS.getDouble("rawData");
	
										if(getDataRS.wasNull()){rawData[rowCounter][i] = null;}
	
										//countData[rowCounter][i] = getDataRS.getDouble("pointCount");
										//if (getDataRS.wasNull()){countData[rowCounter][i] = null;}
	
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
							//double weightedAverageSums = (averagedData[j][i]==null? 0 : averagedData[j][i]*(countData[j][i]==null? 0 : countData[j][i]) );
							//double countSum = (countData[j][i]==null? 0 : countData[j][i]);
							double rawDataPoint = rawData[j][i]==null? 0 : rawData[j][i];
							while (i+1 < sourceList.size() && sourceList.get(i+1).equals(sourceList.get(i))){
								legitCount += (rawData[j][i+1]==null?0:1);
								if (rawData[j][i+1]!=null){//find first number that is not null for a particular source
									rawDataPoint = rawData[j][i];
									break;
								}
								//weightedAverageSums += (averagedData[j][i+1]==null? 0 : averagedData[j][i+1]*(countData[j][i+1]==null? 0 : countData[j][i+1]) );
								//countSum += (countData[j][i+1]==null? 0 : countData[j][i+1]);
								i++;
							}

							//csvWriter.append((legitCount==0 ? "null" : new DecimalFormat("#.###").format(weightedAverageSums/countSum) ));
							csvWriter.append((legitCount==0 ? "null" : new DecimalFormat("#.###").format(rawDataPoint) ));

							//csvWriter.append((averagedData[j][i]==null ? "null" : Double.toString(averagedData[j][i])));
							//if (includeCount){csvWriter.append(","+(countData[j][i]==null ? "null" : (samplePeriod.equals("Monthly")? Double.toString(Math.round(countData[j][i]/1440)) : Double.toString(countData[j][i]) )));}
							//if (doStdDev){csvWriter.append(","+(stdDevData[j][i]==null ? "null" : Double.toString(stdDevData[j][i])));}


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
