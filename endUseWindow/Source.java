package endUseWindow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.swing.JOptionPane;


public class Source {
	Site site;
	String sourceID;
	String sourceName;
	String sourceType;
	String measurementType;
	
	public Source(Site site,String sourceID,String sourceName,String sourceType,String measurementType){
		this.site = site;
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.sourceType = (sourceType==null?"":sourceType);
		this.measurementType = (measurementType==null?"":measurementType);
	}
	
	public boolean equalTo(Source otherSource){
		if (this.sourceName.equals(otherSource.sourceName)
				&& this.sourceType.equals(otherSource.sourceType)
				&& this.measurementType.equals(otherSource.measurementType)){
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isValid(){
		boolean isValid = false;
		if (sourceName.matches("^[\\w\\s\\-\\+\\&\\(\\)/]{1,16}$")){
			if (Arrays.asList(getSourceTypeList()).contains(sourceType)){
				if (Arrays.asList(getMeasurementList()).contains(measurementType)){
					isValid = true;
				}
				else{
					JOptionPane.showMessageDialog(null,"The measurement type provided is invalid.\r\nMax 30 alphanumeric chaacters.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{				
				JOptionPane.showMessageDialog(null,"The Source Type provided is invalid. Please select from the list.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Source Name provided is invalid.\r\nMax 16 alphanumeric chaacters.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		
		return isValid;
	}
	
	public static String addSource(Statement MySQL_Statement,String siteID,Source source) throws SQLException{
		String newSourceID = null;
		String addSourceSQL = "INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+siteID+","+(source.sourceName.equals("")?"NULL":"'"+source.sourceName+"'")+","+(source.sourceType.equals("")?"NULL":"'"+source.sourceType+"'")+","+(source.measurementType.equals("")?"NULL":"'"+source.measurementType+"'")+")";
		MySQL_Statement.executeUpdate(addSourceSQL);
		ResultSet new_source_id = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
		new_source_id.next();
		newSourceID = new_source_id.getString("current_id");
		//existingSource = testSource;

		return newSourceID;
	}
	
	public static void removeSource(Statement MySQL_Statement,String siteID,String sourceID){
		try{
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+siteID+" AND source_id = "+sourceID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(null,"An error occured when removing data for the specified source (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}

	}
	
	public static double getRangeMin(String sourceType,String measurementType){
		double rangeMin = 0.0;
		if(measurementType.equals("AppEnergy")){rangeMin = 0.0;}
		else if(measurementType.equals("AppPower")){rangeMin = 0.0;}
		else if(measurementType.equals("ActEnergy")){rangeMin = 0.0;}
		else if(measurementType.equals("ActPower")){rangeMin = 0.0;}
		else if(measurementType.equals("Volts")){rangeMin = 0.0;}
		else if(measurementType.equals("Amps")){rangeMin = 0.0;}
		else if(measurementType.equals("Pulse")){rangeMin = 0.0;} //gas and water
		else if(measurementType.equals("Humidity")){rangeMin = 0.0;}
		else if(measurementType.equals("OnTime")){rangeMin = 0.0;}
		else if(measurementType.equals("LightLevel")){rangeMin = 0.0;}
		else if(measurementType.equals("Temp")){rangeMin = -20.0;}
		else if(measurementType.equals("AvgTemp")){rangeMin = -20.0;}
		else {rangeMin = 0.0;}
		return rangeMin;
	}
	
	public static double getRangeMax(String sourceType,String measurementType,int frequency){
		double rangeMax = 2500.0;
		if(measurementType.equals("AppEnergy")){rangeMax = 65536.0;}
		else if(measurementType.equals("AppPower")){
			if (sourceType.equals("Circuit")){rangeMax = 8000.0;}
			else{rangeMax = 2500.0;}
		}
		else if(measurementType.equals("ActEnergy")){rangeMax = 65536.0;}
		else if(measurementType.equals("ActPower")){
			if (sourceType.equals("Circuit")){rangeMax = 8000.0;}
			else{rangeMax = 2500.0;}
		}
		else if(measurementType.equals("Volts")){rangeMax = 260.0;}
		else if(measurementType.equals("Amps")){rangeMax = 10.0;}
		else if(measurementType.equals("Pulse")){rangeMax = 6553.6;} //gas and water
		else if(measurementType.equals("Humidity")){rangeMax = 100.0;}
		else if(measurementType.equals("OnTime")){rangeMax = (double)frequency;}
		else if(measurementType.equals("LightLevel")){rangeMax = 255.0;}
		else if(measurementType.equals("Temp")){rangeMax = 60.0;}
		else if(measurementType.equals("AvgTemp")){rangeMax = 60.0;}
		else {rangeMax = 2500.0;}
		return rangeMax;
	}
	
	public Site getSite(){
		return site;
	}
	
	public String getSourceID(){
		return sourceID;
	}
	
	public String getSourceName(){
		return sourceName;
	}
	
	public String getSourceType(){
		return sourceType;
	}
	
	public String getMeasurementType(){
		return measurementType;
	}
	
	static final public String[] getSourceTypeList(){
		return new String[] {
			"Appliance",
			"Circuit",
			"Gas",
			"Humidity",
			"Light",
			"Motion",
			"Phase",
			"Temperature",
			"Water"
		};
	}
	
	static final public String[] getManagementList(){
		return new String[] {
			"Rooms",
			"GPOs",
			"Appliances",
			"Circuits",
			"Gas",
			"Humidity",
			"Lights",
			"Motion",
			"Phases",
			"Temperatures",
			"Water"
		};
	}
	
	static final public String[] getMeasurementList(){
		return new String[] {
			"ActEnergy",
			"AppEnergy",
			"OnTime",
			"Temp",
			"Humidity",
			"Pulse",
			"ActPower",
			"AppPower",
			"LightLevel",
			"Volts",
			"Amps",
			"AvgTemp"
		};
	}
}
