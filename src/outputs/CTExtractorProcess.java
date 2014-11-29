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

public class CTExtractorProcess implements Runnable{
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	LogWindow logWindow;
	Connection dbConn;
	ArrayList<Integer> modulesToWrite;
	int[] modChCount;
	boolean[][] mod_ch_states;
	int total_ch_count;
	int installation_id;
	int installation_id_INDEX;
	ArrayList<int[]> moduleList;
	Calendar startDate;
	Calendar endDate;
	
	
	CTExtractorProcess(LogWindow logWindow_P,Connection dbConn_P,ArrayList<Integer> modules_to_write_P,int[] mod_ch_count_P,boolean[][] mod_ch_states_P,int total_ch_count_P,int installation_id_P,int installation_id_INDEX_P,ArrayList<int[]> moduleList_P,Calendar startDate_P,Calendar endDate_P){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		logWindow = logWindow_P;
		dbConn = dbConn_P;
		modulesToWrite = modules_to_write_P;
		modChCount = mod_ch_count_P;
		mod_ch_states = mod_ch_states_P;
		total_ch_count = total_ch_count_P;
		installation_id = installation_id_P;
		installation_id_INDEX = installation_id_INDEX_P;
		moduleList = moduleList_P;
		startDate = startDate_P;
		endDate = endDate_P;		
	}
	
	public void run(){
		logWindow.setVisible(true);

		JFileChooser fChooser = new JFileChooser();
		String fileName = "./REMP_CTdump_House"+installation_id+"_"+csvDateFormatter.format(startDate.getTimeInMillis())+"_"+csvDateFormatter.format(endDate.getTimeInMillis())+".csv";
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
					
					//Headers
					logWindow.printString("Writing Headers: "+fileToWrite.getName()+"...");
					csvWriter.append("Installation ID "+installation_id+"\r\n");
					csvWriter.append("Date,");
					int columnCounter = 0;

					for (int i=0;i<modulesToWrite.size();i++){
						for (int j=0;j<6;j++){
							if (mod_ch_states[modulesToWrite.get(i)][j]){
								csvWriter.append(moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+"."+(j+1));
								columnCounter++;
								if (columnCounter<total_ch_count){
									csvWriter.append(",");
								}
							}
						}
					}
					csvWriter.append("\r\n");
					logWindow.println("Done.");
					
					logWindow.println("Attempting to extract data from database and write to file...\r\n");
					Calendar rollDate = startDate;

					Statement ctModule_statement = dbConn.createStatement();
					String ctModuleSQL = "";
					ResultSet ctModule_RS;
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
						for (int i=0;i<modulesToWrite.size();i++){
							
							ctModuleSQL = "SELECT c1.ch1,c2.ch2,c3.ch3,c4.ch4,c5.ch5,c6.ch6 FROM (SELECT '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AS date_time) AS date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch1 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 1) AS c1 ON c1.date_time = date_time.date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch2 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 2) AS c2 ON c2.date_time = date_time.date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch3 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 3) AS c3 ON c3.date_time = date_time.date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch4 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 4) AS c4 ON c4.date_time = date_time.date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch5 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 5) AS c5 ON c5.date_time = date_time.date_time LEFT JOIN ";
							ctModuleSQL = ctModuleSQL + "(SELECT date_time,raw AS ch6 FROM data_all WHERE installation_id = "+installation_id+" AND date_time = '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"' AND module_sn = "+moduleList.get(installation_id_INDEX)[modulesToWrite.get(i)]+" AND reading_type = 'CT' AND channel = 6) AS c6 ON c6.date_time = date_time.date_time ";
							
							//ctModuleSQL = "CALL `full_ct`('"+installation_id+"' ,'"+moduleList.get(installation_id_INDEX)[i]+"', '"+sqlDateFormatter.format(rollDate.getTimeInMillis())+"', '"+sqlDateFormatter.format(rollDate.getTimeInMillis()+60000)+"', '"+(mod_ch_states[i][0]?1:0)+"', '"+(mod_ch_states[i][1]?1:0)+"', '"+(mod_ch_states[i][2]?1:0)+"', '"+(mod_ch_states[i][3]?1:0)+"', '"+(mod_ch_states[i][4]?1:0)+"', '"+(mod_ch_states[i][5]?1:0)+"')";
							ctModule_RS = ctModule_statement.executeQuery(ctModuleSQL);
							
							//finishedRowExtractionDate = new Date();
							
							if (ctModule_RS.next()){
								for (int j=0;j<6;j++){
									if (mod_ch_states[modulesToWrite.get(i)][j]){
										writeValue = Double.toString(ctModule_RS.getDouble("ch"+(j+1)));
										if (ctModule_RS.getDouble("ch"+(j+1))<0){writeValue = "0";}
										if (ctModule_RS.wasNull()){writeValue = "NULL";}
										csvWriter.append(writeValue);
										columnCounter++;
										if (columnCounter<total_ch_count){
											csvWriter.append(",");
										}
									}
								}
							}
						}
						if (columnCounter==total_ch_count){ //if all channels written
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
	
}
