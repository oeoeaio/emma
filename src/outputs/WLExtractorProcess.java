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
import java.util.TimeZone;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;

public class WLExtractorProcess implements Runnable{
	
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
	
	
	WLExtractorProcess(LogWindow logWindow_P,Connection dbConn_P,boolean[] chStates_P,int totalChCount_P,int installation_id_P,int installation_id_INDEX_P,Calendar startDate_P,Calendar endDate_P){
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
	}
	
	public void run(){
		logWindow.setVisible(true);

		JFileChooser fChooser = new JFileChooser();
		String fileName = "./REMP_WLdump_House"+installation_id+"_"+csvDateFormatter.format(startDate.getTimeInMillis())+"_"+csvDateFormatter.format(endDate.getTimeInMillis())+".csv";
		fChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fChooser.showSaveDialog(fChooser);
		while (fChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fChooser.showSaveDialog(fChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			File fileToWrite = fChooser.getSelectedFile();
			
			Date startProcessTime = new Date();
				try{
					if (fileToWrite.exists()){fileToWrite.delete();} //remove the file if it already exists
					FileWriter csvWriter = new FileWriter(fileToWrite);//open the file for writing
					
					Statement getHeader_statement = dbConn.createStatement();
					
					String countSPsSQL =  "SELECT COUNT(*) AS spCount FROM header_log WHERE installation_id = "+installation_id+" AND date_time BETWEEN (SELECT MAX(date_time) FROM header_log WHERE installation_id = "+installation_id+" AND date_time < '"+sqlDateFormatter.format(startDate.getTimeInMillis())+"') AND '"+sqlDateFormatter.format(endDate.getTimeInMillis())+"'";
					ResultSet countSPsRS = getHeader_statement.executeQuery(countSPsSQL);
					
					int spCount = 0;
					if (countSPsRS.next()){
						spCount = countSPsRS.getInt("spCount");
					}
					
					String headerValuesSQL =  "SELECT date_time,wl_ch_names,wl_sensor_chs FROM header_log WHERE installation_id = "+installation_id+" AND date_time BETWEEN (SELECT MAX(date_time) FROM header_log WHERE installation_id = "+installation_id+" AND date_time < '"+sqlDateFormatter.format(startDate.getTimeInMillis())+"') AND '"+sqlDateFormatter.format(endDate.getTimeInMillis())+"' ORDER BY date_time";
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
									logWindow.println("WARNING: Channel Type changes in channel "+(i+1)+".");
								}
								else if (channelNames[k][i].equals(channelNames[0][i]) == false){
									logWindow.println("WARNING: Channel Name changes in channel "+(i+1)+". Change is from "+channelNames[0][i]+" to "+channelNames[k][i]);
								}
							}
						}
					}
					
					String[] chDataTypes = new String[48];
					
					
					
					
					
					//Headers
					logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
					csvWriter.append("Installation ID "+installation_id+"\r\n");
					csvWriter.append("Date,");
					int columnCounter = 0;
					
					for (int i=0;i<chStates.length;i++){
						if (chStates[i]){
							csvWriter.append("ch"+(i+1));
							columnCounter++;
							if (columnCounter<totalChCount){
								csvWriter.append(",");
							}
							
							//determine channel data type (ie converted or raw)
							chDataTypes[i] = getDataType(channelTypes[0][i]);
						}
					}
					csvWriter.append("\r\n");
					logWindow.println("Done.");
					
					logWindow.println("Attempting to extract data from database and write to file...\r\n");
					Calendar rollDate = startDate;

					Statement wlModule_statement = dbConn.createStatement();
					String wlModuleSQL;
					String wlModuleSQL_PT1;
					String wlModuleSQL_PT2;
					ResultSet wlModule_RS;
					String writeValue;
					Date startRowDate = new Date();
					//Date finishedRowExtractionDate = new Date();
					logWindow.printString("Processing: "+dateFormatter.format(rollDate.getTimeInMillis()+60000)+"...");
					while (rollDate.getTimeInMillis()<=endDate.getTimeInMillis()){				
						columnCounter = 0;
						if (csvDateFormatter.format(rollDate.getTimeInMillis()).equals(csvDateFormatter.format(rollDate.getTimeInMillis()+60000))==false){
							logWindow.println("Done: "+getTimeString(new Date().getTime()-startRowDate.getTime()));
							//System.out.println(Double.toString(((double)((double)(finishedRowExtractionDate.getTime()-startRowDate.getTime())/((double)(finishedRowExtractionDate.getTime()-startRowDate.getTime())+(double)(new Date().getTime()-startRowDate.getTime()))))));
							
							startRowDate = new Date();
							logWindow.printString("Processing: "+dateFormatter.format(rollDate.getTimeInMillis()+60000)+"...");
						}
						csvWriter.append(sqlDateFormatter.format(rollDate.getTimeInMillis())+",");
						columnCounter = 0;
						wlModuleSQL = "";
						wlModuleSQL_PT1 = "SELECT ";
						wlModuleSQL_PT2 = "";
						for (int i=0;i<chStates.length;i++){ //Build SQL query
							if (chStates[i]){
								columnCounter++;
								if (columnCounter<totalChCount){
									wlModuleSQL_PT1 = wlModuleSQL_PT1 + "c"+(i+1)+".ch"+(i+1)+",";
									wlModuleSQL_PT2 = wlModuleSQL_PT2 + "(SELECT date_time,"+chDataTypes[i]+" AS ch"+(i+1)+" FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND reading_type = 'WL' AND channel = "+(i+1)+") AS c"+(i+1)+" ON c"+(i+1)+".date_time = date_time.date_time LEFT JOIN ";
								}
								else{
									wlModuleSQL_PT1 = wlModuleSQL_PT1 + "c"+(i+1)+".ch"+(i+1);
									wlModuleSQL_PT2 = wlModuleSQL_PT2 + "(SELECT date_time,"+chDataTypes[i]+" AS ch"+(i+1)+" FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND reading_type = 'WL' AND channel = "+(i+1)+") AS c"+(i+1)+" ON c"+(i+1)+".date_time = date_time.date_time ";
								}
							}
						}
						wlModuleSQL = wlModuleSQL_PT1 + " FROM (SELECT '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AS date_time) AS date_time LEFT JOIN " + wlModuleSQL_PT2;

						wlModule_RS = wlModule_statement.executeQuery(wlModuleSQL);	
						
						columnCounter = 0;
						
						if (wlModule_RS.next()){
							for (int i=0;i<chStates.length;i++){
								if (chStates[i]){
									writeValue = Double.toString(wlModule_RS.getDouble("ch"+(i+1)));
									if (wlModule_RS.wasNull()){writeValue = "NULL";}
									csvWriter.append(writeValue);
									columnCounter++;
									if (columnCounter<totalChCount){
										csvWriter.append(",");
									}
								}
							}
						}
						if (columnCounter==totalChCount){ //if all channels written
							csvWriter.append("\r\n");
							csvWriter.flush();
							rollDate.add(Calendar.MINUTE, 1);
						}
						else{
							System.out.println("Error: not all channels were written");
							break;
						}
					}
					logWindow.println("Retrieval of data complete.\r\n");
					logWindow.println("Finished Writing.\r\n");
					Date endProcessTime = new Date();
					logWindow.println("Total Extraction Time: "+getTimeString(endProcessTime.getTime()-startProcessTime.getTime())+"\r\n");
					csvWriter.close();
				}	catch (IOException e){
					JOptionPane.showMessageDialog(logWindow,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
				}	catch(SQLException sE){
					sE.printStackTrace();
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
	
	String getDataType(String channelType){
		String dataType = "raw";
		if (channelType.equals("00")){ //0 : temperature
			dataType = "raw";
		}
		else if (channelType.equals("01")){ //1 : humidity
			dataType = "raw";
		}
		else if (channelType.equals("02")){ //2 : volts
			dataType = "raw";
		}
		else if (channelType.equals("03")){ //3 : amps
			dataType = "raw";
		}
		else if (channelType.equals("04")){ //4 : Active Power
			dataType = "raw";
		}
		else if (channelType.equals("05")){ //5 : Apparent Power
			dataType = "raw";
		}
		else if (channelType.equals("06")){ //6 : Light
			dataType = "raw";
		}
		else if (channelType.equals("07")){ //7 : Average temperature
			dataType = "raw";
		}
		else if (channelType.equals("80")){
			dataType = "converted";
		}
		else if (channelType.equals("81")){
			dataType = "converted";
		}
		else if (channelType.equals("90") || channelType.equals("91")){
			dataType = "converted";
		}
		else{
			dataType = "raw";
		}
		return dataType;
	}
	
}
