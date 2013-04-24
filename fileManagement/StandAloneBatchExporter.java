package fileManagement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import endUseWindow.LogWindow;

public class StandAloneBatchExporter extends Thread {
	
	File exportFile = null;
	Connection dbConn = null;
	LogWindow logWindow = null;
	
	public StandAloneBatchExporter(File exportFile,Connection dbConn,LogWindow logWindow){
		this.exportFile = exportFile;
		this.dbConn = dbConn;
		this.logWindow = logWindow;
	}
	
	public void run(){
		try {
			logWindow.println("Starting batch file extraction process...");
			BufferedWriter outputStream = new BufferedWriter(new FileWriter(exportFile));
			logWindow.printString("Extracting data...");
			outputStream.append("site_name,given_name,surname,suburb,state,source_name,source_type,measurement_type,file_name,room_number,room_type,appliance_group,appliance_type,brand,model,serial_no\r\n");
			
			//Appliances
			logWindow.println("Fetching appliances...");
			String applianceBatch = "SELECT sites.site_name AS site_name,sites.given_name AS given_name,sites.surname AS surname,sites.suburb AS suburb,sites.state AS state,sources.source_name AS source_name,sources.source_type AS source_type,sources.measurement_type AS measurement_type,files.file_name AS file_name,rooms.room_number AS room_number,rooms.room_type AS room_type,appliances.appliance_group AS appliance_group,appliances.appliance_type,appliances.brand AS brand,appliances.model AS model,appliances.serial_no AS serial_no FROM sites LEFT JOIN sources ON sources.site_id=sites.site_id LEFT JOIN files ON files.source_id = sources.source_id LEFT JOIN appliances ON sources.source_id = appliances.source_id LEFT JOIN rooms ON rooms.room_id = appliances.room_id WHERE sources.source_type = 'Appliance'";
			ResultSet applianceBatchData = dbConn.createStatement().executeQuery(applianceBatch);
			logWindow.println("Done.");
			logWindow.printString("Writing data to file...");
			while (applianceBatchData.next()){
				outputStream.append(applianceBatchData.getString("site_name")+","+applianceBatchData.getString("given_name")+","+applianceBatchData.getString("surname")+","+applianceBatchData.getString("suburb")+","+applianceBatchData.getString("state")+","+applianceBatchData.getString("source_name")+","+applianceBatchData.getString("source_type")+","+applianceBatchData.getString("measurement_type")+","+applianceBatchData.getString("file_name")+","+(applianceBatchData.getString("room_number")==null?"":applianceBatchData.getString("room_number"))+","+(applianceBatchData.getString("room_type")==null?"":applianceBatchData.getString("room_type"))+","+(applianceBatchData.getString("appliance_group")==null?"":applianceBatchData.getString("appliance_group"))+","+(applianceBatchData.getString("appliance_type")==null?"":applianceBatchData.getString("appliance_type"))+","+(applianceBatchData.getString("brand")==null?"":applianceBatchData.getString("brand"))+","+(applianceBatchData.getString("model")==null?"":applianceBatchData.getString("model"))+","+(applianceBatchData.getString("serial_no")==null?"":applianceBatchData.getString("serial_no"))+"\r\n");
			}
			logWindow.println("Done.");
			applianceBatchData.close();
			
			//Temps
			logWindow.println("Fetching temperatures...");
			String temperatureBatch = "SELECT sites.site_name AS site_name,sites.given_name AS given_name,sites.surname AS surname,sites.suburb AS suburb,sites.state AS state,sources.source_name AS source_name,sources.source_type AS source_type,sources.measurement_type AS measurement_type,files.file_name AS file_name,rooms.room_number AS room_number,rooms.room_type AS room_type FROM sites LEFT JOIN sources ON sources.site_id=sites.site_id LEFT JOIN files ON files.source_id = sources.source_id LEFT JOIN temperatures ON sources.source_id = temperatures.source_id LEFT JOIN rooms ON rooms.room_id = temperatures.room_id WHERE sources.source_type = 'Temperature'";
			ResultSet temperatureBatchData = dbConn.createStatement().executeQuery(temperatureBatch);
			logWindow.println("Done.");
			logWindow.printString("Writing data to file...");
			while (temperatureBatchData.next()){
				outputStream.append(temperatureBatchData.getString("site_name")+","+temperatureBatchData.getString("given_name")+","+temperatureBatchData.getString("surname")+","+temperatureBatchData.getString("suburb")+","+temperatureBatchData.getString("state")+","+temperatureBatchData.getString("source_name")+","+temperatureBatchData.getString("source_type")+","+temperatureBatchData.getString("measurement_type")+","+temperatureBatchData.getString("file_name")+","+(temperatureBatchData.getString("room_number")==null?"":temperatureBatchData.getString("room_number"))+","+(temperatureBatchData.getString("room_type")==null?"":temperatureBatchData.getString("room_type"))+",,,,,\r\n");
			}
			logWindow.println("Done.");
			temperatureBatchData.close();
			
			
			outputStream.close();
			logWindow.println("Finished extraction to file "+exportFile.getName());

			
		} catch (SQLException sE) {
			JOptionPane.showMessageDialog(null, "Could not retrieve batch information from database.", "Fetching batch info failed", JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		} catch (IOException iOE){
			JOptionPane.showMessageDialog(null, "Could not access file "+exportFile.getName()+" for writing.\r\nPlease ensure that you have permission to write to this location.", "Writing batch file failed", JOptionPane.ERROR_MESSAGE);
			iOE.printStackTrace();
		}
	}
}
