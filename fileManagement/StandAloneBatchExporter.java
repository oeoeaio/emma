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
			outputStream.append("site_name,given_name,surname,suburb,state,source_name,source_type,brand,model,serial,location,file_name\r\n");
			String fetchBatch = "SELECT sites.site_name AS site_name,sites.given_name AS given_name,sites.surname AS surname,sites.suburb AS suburb,sites.state AS state,sources.source_name AS source_name,sources.source_type AS source_type,sources.brand AS brand,sources.model AS model,sources.serial AS serial,sources.location AS location,files.file_name AS file_name FROM sites LEFT JOIN sources ON sources.site_id=sites.site_id LEFT JOIN files ON files.source_id = sources.source_id";
			ResultSet batchData = dbConn.createStatement().executeQuery(fetchBatch);
			logWindow.println("Done.");
			logWindow.printString("Writing data to file...");
			while (batchData.next()){
				outputStream.append(batchData.getString("site_name")+","+batchData.getString("given_name")+","+batchData.getString("surname")+","+batchData.getString("suburb")+","+batchData.getString("state")+","+batchData.getString("source_name")+","+batchData.getString("source_type")+","+batchData.getString("brand")+","+batchData.getString("model")+","+batchData.getString("serial")+","+batchData.getString("location")+","+batchData.getString("file_name")+"\r\n");
			}
			logWindow.println("Done.");
			
			batchData.close();
			outputStream.close();
			logWindow.println("Finished extraction to file "+exportFile.getName());

			
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Could not retrieve batch information from database.", "Fetching batch info failed", JOptionPane.ERROR_MESSAGE);
		} catch (IOException iOE){
			JOptionPane.showMessageDialog(null, "Could not access file "+exportFile.getName()+" for writing.\r\nPlease ensure that you have permission to write to this location.", "Writing batch file failed", JOptionPane.ERROR_MESSAGE);
		}
	}
}
