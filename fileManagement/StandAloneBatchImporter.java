package fileManagement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import endUseWindow.Appliance;
import endUseWindow.Circuit;
import endUseWindow.Gas;
import endUseWindow.Humidity;
import endUseWindow.Light;
import endUseWindow.LogWindow;
import endUseWindow.Motion;
import endUseWindow.MySQLConnection;
import endUseWindow.Phase;
import endUseWindow.Room;
import endUseWindow.Site;
import endUseWindow.Source;
import endUseWindow.Temperature;
import endUseWindow.Water;

public class StandAloneBatchImporter implements Runnable{
	
	private LogWindow logWindow;
	private File batchFile;
	private MySQLConnection mySQLConnection;
	private List<String> fileHeaders;

	public StandAloneBatchImporter(MySQLConnection mySQLConnection,LogWindow logWindow,File batchFile){
		this.logWindow = logWindow;
		this.batchFile = batchFile;
		this.mySQLConnection = mySQLConnection;
	}
	
	public void run(){
		LinkedList<ArrayList<File>> fileList = new LinkedList<ArrayList<File>>();
		fileList.add(new ArrayList<File>());
		ArrayList<DataLine> batchInfo = getBatchInfo();
		if (batchInfo.size()>0){
			Statement MySQL_Statement;
			try {
				batchInfo = sortBatchInfo(batchInfo);
				Connection dbConn = mySQLConnection.getCopyConnection();
				MySQL_Statement = dbConn.createStatement();

				String existingSiteID = null;
				String existingSourceID = null;
				
				Site existingSite = null;
				Source existingSource = null; 

				StandAloneValidator standAloneValidator = new StandAloneValidator(mySQLConnection,logWindow);
				Thread validatorThread = new Thread(standAloneValidator);
				validatorThread.start();
				
				for (int i=0;i<batchInfo.size();i++){
					try {
						//create new test site and test source
						Site testSite = new Site(null,batchInfo.get(i).data[0],batchInfo.get(i).data[1],batchInfo.get(i).data[2],batchInfo.get(i).data[3],batchInfo.get(i).data[4]);
						Source testSource = new Source(testSite,null,batchInfo.get(i).data[5],batchInfo.get(i).data[6],batchInfo.get(i).data[7]);
						DataFile dataFile = new DataFile();
						//if test site and source do not match existing test site and source, write data for previous source
						/*if (fileList.size() > 0 && fileList.peek().size()>0 && (!existingSite.getSiteName().equals(testSite.getSiteName()) || !existingSource.getSourceName().equals(testSource.getSourceName()))){
							//write data for previous source
							logWindow.println("Attempting to write files for source "+existingSourceID+" at site "+existingSiteID+" "+fileList.peek().size()+""+fileList.peek().get(0).getName());
							standAloneValidator.addFile(dataFile);
							Thread validateFiles = new Thread(new StandAloneValidator(mySQLConnection,fileList.poll(),logWindow,existingSiteID,existingSourceID));
							validateFiles.start();
							try {
								validateFiles.join();
							} catch (InterruptedException e) {
							}
							fileList.add(new ArrayList<File>());
							logWindow.println("\r\n");
						}*/

						//process site info for current record
						if (existingSite==null || existingSiteID==null || !existingSite.getSiteName().equals(testSite.getSiteName())){ //if existing site needs to be updated - a true here does not mean that there is an information conflict, it merely serves to reduce the number of requests to the MySQL database (one per distinct site and one per distinct source)

							String checkSiteSQL = "SELECT * FROM sites WHERE site_name = '"+testSite.getSiteName()+"'";
							ResultSet existingSiteInfo = MySQL_Statement.executeQuery(checkSiteSQL);
							existingSiteID = null;
							existingSite = null;
							if (existingSiteInfo.next()){
								existingSiteID = existingSiteInfo.getString("site_id");
								dataFile.siteID = existingSiteID;
								existingSite = new Site(existingSiteID,existingSiteInfo.getString("site_name"),existingSiteInfo.getString("given_name"),existingSiteInfo.getString("surname"),existingSiteInfo.getString("suburb"),existingSiteInfo.getString("state"));

								if (testSite.equalTo(existingSite)==false){
									logWindow.printString("WARNING: site "+testSite.getSiteName()+" mismatch. ");
									SiteClashWindow siteClashWindow = new SiteClashWindow();
									int selectedOption = siteClashWindow.getSelectedOption(existingSiteID,existingSite,testSite);
									if (selectedOption==1){
										logWindow.println("RESOLVED: Existing information was correct.");
									}
									else if(selectedOption==2){
										logWindow.println("RESOLVED: Conflicting information was correct.");
										String changeExistingInfoSQL = "UPDATE sites SET given_name="+(testSite.getGivenName().equals("")?"NULL":"'"+testSite.getGivenName()+"'")+",surname="+(testSite.getSurname().equals("")?"NULL":"'"+testSite.getSurname().replace("'","\\'")+"'")+",suburb="+(testSite.getSuburb().equals("")?"NULL":"'"+testSite.getSuburb()+"'")+",state="+(testSite.getState().equals("")?"NULL":"'"+testSite.getState()+"'")+" WHERE site_id = "+existingSourceID;
										MySQL_Statement.executeUpdate(changeExistingInfoSQL);
										existingSite = testSite;
									}
									else{
										logWindow.println("UNRESOLVED: Record ignored.");
									}
								}
								else{
									//all is well
								}
							}
							else{
								String addSiteSQL = "INSERT INTO sites (site_name,given_name,surname,suburb,state) VALUES("+(testSite.getSiteName().equals("")?"NULL":"'"+testSite.getSiteName()+"'")+","+(testSite.getGivenName().equals("")?"NULL":"'"+testSite.getGivenName()+"'")+","+(testSite.getSurname().equals("")?"NULL":"'"+testSite.getSurname().replace("'","\\'")+"'")+","+(testSite.getSuburb().equals("")?"NULL":"'"+testSite.getSuburb()+"'")+","+(testSite.getState().equals("")?"NULL":"'"+testSite.getState()+"'")+")";
								MySQL_Statement.executeUpdate(addSiteSQL);
								ResultSet new_site_id = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
								new_site_id.next();
								existingSiteID = new_site_id.getString("current_id");
								dataFile.siteID = existingSiteID;
								existingSite = new Site(existingSiteID,testSite.getSiteName(),testSite.getGivenName(),testSite.getSurname(),testSite.getSuburb(),testSite.getState());

								logWindow.println("Added new site ID "+existingSiteID);
							}
							existingSiteInfo.close();
						}
						else{ //if we get to here, sites should be the same, as they are not null and have the same name
							dataFile.siteID = existingSiteID;
							if (testSite.equalTo(existingSite)==false){ //if they don't match, resolve by prompting user 
								logWindow.printString("WARNING: site "+testSite.getSiteName()+" mismatch. ");
								SiteClashWindow siteClashWindow = new SiteClashWindow();
								int selectedOption = siteClashWindow.getSelectedOption(existingSiteID,existingSite,testSite);
								if (selectedOption==1){
									logWindow.println("RESOLVED: Existing information was correct.");
								}
								else if(selectedOption==2){
									logWindow.println("RESOLVED: Conflicting information was correct.");
									String changeExistingInfoSQL = "UPDATE sites SET given_name="+(testSite.getGivenName().equals("")?"NULL":"'"+testSite.getGivenName()+"'")+",surname="+(testSite.getSurname().equals("")?"NULL":"'"+testSite.getSurname().replace("'","\\'")+"'")+",suburb="+(testSite.getSuburb().equals("")?"NULL":"'"+testSite.getSuburb()+"'")+",state="+(testSite.getState().equals("")?"NULL":"'"+testSite.getState()+"'")+" WHERE site_id = "+existingSourceID;
									MySQL_Statement.executeUpdate(changeExistingInfoSQL);
									existingSite = testSite;
								}
								else{
									logWindow.println("UNRESOLVED: Record ignored.");
								}
							}
							else{
								//all is well
							}
						}

						//process source info for current record
						if (existingSource==null || existingSourceID==null || !existingSource.getSourceName().equals(testSource.getSourceName()) || !existingSource.getSite().equalTo(testSource.getSite())){ //if existing source needs to be updated - a true here does not mean that there is an information conflict, it merely serves to reduce the number of requests to the MySQL database (one per distinct site and one per distinct source)

							String checkSourceSQL = "SELECT * FROM sources WHERE site_id = "+existingSiteID+" AND source_name = '"+testSource.getSourceName()+"'";
							ResultSet existingSourceInfo = MySQL_Statement.executeQuery(checkSourceSQL);
							existingSourceID = null;
							
							if (existingSourceInfo.next()){ //if source name for current exists in the database already
								existingSourceID = existingSourceInfo.getString("source_id");
								dataFile.sourceID = existingSourceID;
								dataFile.sourceType = existingSourceInfo.getString("source_type");
								dataFile.measurementType = existingSourceInfo.getString("measurement_type");
								existingSource = new Source(existingSite,existingSourceID,existingSourceInfo.getString("source_name"),existingSourceInfo.getString("source_type"),existingSourceInfo.getString("measurement_type"));
								
								//now test to make sure the test source matches existing information
								if (testSource.equalTo(existingSource)==false){ //sources don't match
									logWindow.printString("WARNING: source "+testSource.getSourceName()+" mismatch at site "+existingSiteID+". ");
									SourceClashWindow sourceClashWindow = new SourceClashWindow();
									int selectedOption = sourceClashWindow.getSelectedOption(existingSourceID,existingSource,testSource);
									if (selectedOption==1){
										logWindow.println("RESOLVED: Existing information was correct.");
										File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
										if (newFile.isFile() && newFile.canRead()){	
											dataFile.fileName = newFile.getName();
											dataFile.file = newFile;
											standAloneValidator.addFile(dataFile);	
										}
										else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
									}
									else if(selectedOption==2){
										logWindow.println("RESOLVED: Conflicting information was correct.");
										String changeExistingInfoSQL = "UPDATE sources SET source_type="+(testSource.getSourceType().equals("")?"NULL":"'"+testSource.getSourceType()+"'")+",measurement_type="+(testSource.getMeasurementType().equals("")?"NULL":"'"+testSource.getMeasurementType())+"' WHERE source_id = "+existingSourceID;
										MySQL_Statement.executeUpdate(changeExistingInfoSQL);
										existingSource = testSource;
										File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
										if (newFile.isFile() && newFile.canRead()){	
											dataFile.fileName = newFile.getName();
											dataFile.file = newFile;
											standAloneValidator.addFile(dataFile);	
										}
										else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
									}
									else{
										logWindow.println("UNRESOLVED: Record ignored.");
									}
								}
								else{ //sources are matching
									File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
									if (newFile.isFile() && newFile.canRead()){	
										dataFile.fileName = newFile.getName();
										dataFile.file = newFile;
										standAloneValidator.addFile(dataFile);	
									}
									else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
								}
							}
							else{ //if no record for this source currently exists, add it to the database, and change the existingSource to the new id
								try{
									existingSourceID = Source.addSource(logWindow, true, MySQL_Statement, existingSite, testSource, dataFile.fileName, false);
									dataFile.sourceID = existingSourceID;
									dataFile.sourceType = testSource.getSourceType();
									dataFile.measurementType = testSource.getMeasurementType();
									existingSource = testSource;
									
									
									String roomID = "";
									try{
										roomID = Room.addRoom(MySQL_Statement, logWindow, existingSiteID, existingSourceID, batchInfo.get(i).get("room_number"), batchInfo.get(i).get("room_type"));
									} catch(SQLException sE){
										//non fatal, so allow to continue
										logWindow.println("Warning: unable to match specified room '"+batchInfo.get(i).get("room_number")+"'");
									}
									
									String circuitID = "";
									try{
										if (batchInfo.get(i).get("circuit_name").length()>0){
											ResultSet circuitIDRS = MySQL_Statement.executeQuery("SELECT circuit_id FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id = "+existingSiteID+" AND sources.source_name = '"+batchInfo.get(i).get("circuit_name")+"'");
											if (circuitIDRS.next()){
												circuitID = circuitIDRS.getString("circuit_id");
											}
											else{// add new circuit
												Source testCircuitSource = new Source(existingSite,null,batchInfo.get(i).get("circuit_name"),"Circuit","ActPower");
												String testCircuitSourceID = Source.addSource(logWindow, true, MySQL_Statement, existingSite, testCircuitSource, dataFile.fileName, false);
												
												
												circuitID = Circuit.addCircuit(MySQL_Statement, logWindow, existingSiteID, testCircuitSourceID);
											}
										}
									} catch(SQLException sE){
										//non fatal, so allow to continue
										logWindow.println("Warning: unable to match specified circuit '"+batchInfo.get(i).get("circuit_name")+"'");
									}
									
									boolean sourceAdded = false;
									
									if (testSource.getSourceType().equals("Appliance")){
										Appliance testAppliance = new Appliance(existingSite,existingSourceID,testSource.getSourceName(),circuitID,roomID,batchInfo.get(i).get("appliance_group"),batchInfo.get(i).get("appliance_type"),batchInfo.get(i).get("brand"),batchInfo.get(i).get("model"),batchInfo.get(i).get("appliance_serial"),"","","","","","","","","","","","","","","","","","","","");
										if (Appliance.addAppliance(MySQL_Statement, logWindow, existingSiteID, testAppliance)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Circuit")){
										if (Circuit.addCircuit(MySQL_Statement, logWindow, existingSiteID, existingSourceID).matches("^\\d{1,10}$")){ //returns a valid circuitID
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Gas")){
										Gas testGas = new Gas(existingSite,existingSourceID,testSource.getSourceName(),"");
										if (Gas.addGas(MySQL_Statement, logWindow, existingSiteID, testGas)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Humidity")){
										Humidity testHumidity = new Humidity(existingSite,existingSourceID,testSource.getSourceName(),"","");
										if (Humidity.addHumidity(MySQL_Statement, logWindow, existingSiteID, testHumidity)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Motion")){
										Motion testMotion = new Motion(existingSite,existingSourceID,testSource.getSourceName(),"","");
										if (Motion.addMotion(MySQL_Statement, logWindow, existingSiteID, testMotion)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Phase")){
										Phase testPhase = new Phase(existingSite,existingSourceID,testSource.getSourceName(),"");
										if (Phase.addPhase(MySQL_Statement, logWindow, existingSiteID, testPhase)){ //returns a valid circuitID
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Light")){
										Light testLight = new Light(existingSite,existingSourceID,testSource.getSourceName(),circuitID,roomID,batchInfo.get(i).get("wattage"),null);
										if (Light.addLight(MySQL_Statement, logWindow, existingSiteID, testLight)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Temperature")){
										Temperature testTemperature = new Temperature(existingSite,existingSourceID,testSource.getSourceName(),roomID,null);
										if (Temperature.addTemperature(MySQL_Statement, logWindow, existingSiteID, testTemperature)){
											sourceAdded = true;
										}
									}
									else if(testSource.getSourceType().equals("Water")){
										Water testWater = new Water(existingSite,existingSourceID,testSource.getSourceName(),"");
										if (Water.addWater(MySQL_Statement, logWindow, existingSiteID, testWater)){
											sourceAdded = true;
										}
									}
									
									if (sourceAdded){
										File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
										if (newFile.isFile() && newFile.canRead()){	
											dataFile.fileName = newFile.getName();
											dataFile.file = newFile;
											standAloneValidator.addFile(dataFile);	
										}
										else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
									}
									else{
										Source.removeSource(MySQL_Statement, existingSiteID, existingSourceID);
									}
									
								}catch(SQLException sE){
									JOptionPane.showMessageDialog(null,"Error occured while adding a new source: '"+testSource.getSourceName()+"' for file '"+batchInfo.get(i).data[8]+"'.\r\nSource will not be added at this point. File will be ignored.","Error",JOptionPane.ERROR_MESSAGE);
									logWindow.println("Error occured while adding a new source: '"+testSource.getSourceName()+"' for file '"+batchInfo.get(i).data[8]+"'.\r\nSource will not be added at this point. File will be ignored.\r\n");
								}
							}
							existingSourceInfo.close();
						}
						else{ //if we get to here, sources should be the same, as they are not null and have the same name 
							dataFile.sourceID = existingSourceID;
							dataFile.sourceType = testSource.getSourceType();
							dataFile.measurementType = testSource.getMeasurementType();
							if (testSource.equalTo(existingSource)==false){ //if they don't match, resolve
								logWindow.printString("WARNING: source "+testSource.getSourceName()+" mismatch at site "+existingSiteID+". ");
								SourceClashWindow sourceClashWindow = new SourceClashWindow();
								int selectedOption = sourceClashWindow.getSelectedOption(existingSourceID,existingSource,testSource);
								if (selectedOption==1){ //keep source information
									logWindow.println("RESOLVED: Existing information was correct.");
									File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
									if (newFile.isFile() && newFile.canRead()){	
										dataFile.fileName = newFile.getName();
										dataFile.file = newFile;
										standAloneValidator.addFile(dataFile);	
									}
									else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
								}
								else if(selectedOption==2){ //change source information
									logWindow.println("RESOLVED: Conflicting information was correct.");
									String changeExistingInfoSQL = "UPDATE sources SET source_type="+(testSource.getSourceType().equals("")?"NULL":"'"+testSource.getSourceType()+"'")+",measurement_type="+(testSource.getMeasurementType().equals("")?"NULL":"'"+testSource.getMeasurementType()+"'")+" WHERE source_id = "+existingSourceID;
									MySQL_Statement.executeUpdate(changeExistingInfoSQL);
									existingSource = testSource;
									File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
									if (newFile.isFile() && newFile.canRead()){	
										dataFile.fileName = newFile.getName();
										dataFile.file = newFile;
										standAloneValidator.addFile(dataFile);	
									}
									else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
								}
								else{ //ignore record
									logWindow.println("UNRESOLVED: Record ignored.");
								}
							}
							else{ //sources are matching
								File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+batchInfo.get(i).data[8]);
								if (newFile.isFile() && newFile.canRead()){	
									dataFile.fileName = newFile.getName();
									dataFile.file = newFile;
									standAloneValidator.addFile(dataFile);	
								}
								else{ logWindow.println("ERROR: '"+newFile.getName()+"' is not a valid file name or is not a readable file. Record will be ignored."); }
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				/*if (fileList.size() > 0 && fileList.peek().size()>0){
					logWindow.println("Attempting to write files for source "+existingSourceID+" at site "+existingSiteID);
					Thread validateFiles = new Thread(new StandAloneValidator(mySQLConnection,logWindow));
					validateFiles.start();
					try {
						validateFiles.join();
					} catch (InterruptedException e) {
					}
					fileList.add(new ArrayList<File>());
				}*/
				synchronized(standAloneValidator.fileList){
					standAloneValidator.moreFilesComing = false;
					standAloneValidator.fileList.notify();
				}
				try {
					validatorThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logWindow.println("\r\nFinished sending all files to writer.");

			} catch (SQLException e) {
				e.printStackTrace();
			} 
		}
	}
	
	ArrayList<DataLine> getBatchInfo(){
		ArrayList<DataLine> batchInfo = new ArrayList<DataLine>();
		BufferedReader inputStream = null;
		try {
			inputStream = new BufferedReader(new FileReader(batchFile));
			String line = "";
			String[] splitLine = inputStream.readLine().split(",");
			if (splitLine.length >= 9 && splitLine[0].equals("site_name") && splitLine[1].equals("given_name") && splitLine[2].equals("surname") && splitLine[3].equals("suburb") && splitLine[4].equals("state") && splitLine[5].equals("source_name") && splitLine[6].equals("source_type") && splitLine[7].equals("measurement_type") && splitLine[8].equals("file_name")){
				fileHeaders = Arrays.asList(splitLine);
				while ((line = inputStream.readLine()) != null) {
					splitLine = line.split(",");
					if (!splitLine[8].endsWith(".txt")){splitLine[8] += ".txt";} //add extension to file name if absent
					//TODO could make this test more strict
					if (Arrays.asList(Source.getSourceTypeList()).contains(splitLine[6])){
						if (Arrays.asList(Source.getMeasurementList()).contains(splitLine[7])){
							File newFile = new File(batchFile.getAbsolutePath().substring(0,batchFile.getAbsolutePath().lastIndexOf("\\"))+"\\"+splitLine[8]);
							if(newFile.isFile() && newFile.canRead()){
								batchInfo.add(new DataLine(splitLine));
							}
							else{
								logWindow.println("Error: Could not read file '"+splitLine[8]+"' at site "+splitLine[0]+" for source "+splitLine[5]+"\r\nRecord will not be processed.");
							}
						}
						else{
							logWindow.println("Error: Unknown Measurement Type '"+splitLine[7]+"' at site "+splitLine[0]+" for source "+splitLine[5]+"\r\nRecord will not be processed.");
						}
					}
					else{
						logWindow.println("Error: Unknown Source Type '"+splitLine[6]+"' at site "+splitLine[0]+" for source "+splitLine[5]+"\r\nRecord will not be processed.");
					}
				
				}
			}
			else{
				logWindow.println("Unexpected file format. File will not be written.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if (inputStream != null){
				try {
					inputStream.close();
				} catch (IOException err) {
					System.err.println("IOException: "+err.getMessage());
				}
			}
		}
		return batchInfo;
	}
	
	ArrayList<DataLine> sortBatchInfo(ArrayList<DataLine> batchInfo){		
		Collections.sort(batchInfo, new Comparator<DataLine>(){

			public int compare(DataLine arg0, DataLine arg1) {
				if(arg0.data[0].compareTo(arg1.data[0])>0){ //site name
					return 1;
				}
				else if (arg0.data[0].compareTo(arg1.data[0])<0){
					return -1;
				}
				else{
					if (arg0.data[5].compareTo(arg1.data[5])>0){ //source name
						return 1;
					}
					else if (arg0.data[5].compareTo(arg1.data[5])<0){
						return -1;
					}
					else{
						if (arg0.data[8].compareTo(arg1.data[8])>0){ //file name
							return 1;
						}
						else if (arg0.data[8].compareTo(arg1.data[8])<0){
							return -1;
						}
						else{
							return 0;
						}
					}
				}
			}
		});
		
		return batchInfo;
	}
	
	class DataLine {
		String[] data;
		
		DataLine(String[] data){
			this.data = data;
		}
		
		String get(String element){
			int index = -1;
			if ((index = fileHeaders.lastIndexOf(element)) != -1){
				if (data.length>index){
					return data[index];
				}
				else{
					return "";
				}
			}
			else{
				return "";
			}
		}
	}
}
