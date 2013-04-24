package endUseWindow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;

public class Circuit extends Source{
	String sourceID;
	String sourceName;
	String notes;
	
	
	public Circuit(Site site,String sourceID,String sourceName,String notes){
		super(site, sourceID, sourceName, "Circuit", "");
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.notes = (notes==null?"":notes);
		
	}
	
	/*
	boolean equals(Appliance otherSource){
		if (this.sourceName.equals(otherSource.sourceName)
				&& this.sourceType.equals(otherSource.sourceType)
				&& this.measurementType.equals(otherSource.measurementType)
				&& this.location.equals(otherSource.location)){
			return true;
		}
		else{
			return false;
		}
	}*/
	
	public boolean isValid(){
		boolean isValid = false;
		if (sourceID.matches("^\\d{1,10}$") || sourceID.equals("")){
			if (sourceName.matches("^[\\w\\s\\-\\+\\&\\?\\(\\)/]{1,20}$")){
				isValid = true;
			}
			else{
				JOptionPane.showMessageDialog(null,"The Source Name '"+sourceName+"' is invalid.\r\nMax 16 alphanumeric characters.","Circuit Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Source ID '"+sourceID+"' is invalid.\r\nNumeric characters.","Circuit Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}	
	
	public static String addCircuit(Statement MySQL_Statement,LogWindow logWindow,String siteID,String circuitSourceID) throws SQLException{
		String circuitID = "";
		
		try{
			MySQL_Statement.executeUpdate("INSERT INTO circuits (site_id,source_id) VALUES("+siteID+",'"+circuitSourceID+"')");
			ResultSet new_circuit_id = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
			new_circuit_id.next();
			circuitID = new_circuit_id.getString("current_id");
		} catch(SQLException sE){
			Source.removeSource(MySQL_Statement,siteID,circuitSourceID);
			sE.printStackTrace();
			logWindow.println("Error occured when writing temperature information.");
			throw new SQLException();
		}

		return circuitID;
	}
}
