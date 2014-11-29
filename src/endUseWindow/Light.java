package endUseWindow;

import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;


public class Light extends Source{
	String sourceID;
	String sourceName;
	String circuitID;
	String roomID;
	String wattage;
	String notes;

	
	
	public Light(Site site,String sourceID,String sourceName,String circuitID,String roomID,String wattage,String notes){
		super(site,sourceID,sourceName,"Light","");
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.circuitID = (circuitID==null?"":circuitID);
		this.roomID = (roomID==null?"":roomID);
		this.wattage = (wattage==null?"":wattage);
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
		if (sourceID.matches("^\\d{1,10}$")){
			if (sourceName.matches("^[\\w\\s\\-\\(\\)/]{0,20}$")){
				if (circuitID.matches("^\\d{1,10}$") || circuitID.equals("")){
					if (roomID.matches("^\\d{1,10}$") || roomID.equals("")){
						if (wattage.matches("^\\d{1,3}[.]{0,1}\\d{0,1}$") || wattage.equals("")){
							isValid = true;
						}
						else{
							JOptionPane.showMessageDialog(null,"The wattage provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
						}
					}
					else{
						JOptionPane.showMessageDialog(null,"The Room ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Circuit ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The Source Name provided is invalid.\r\nMax 16 Alphnumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Source ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}	
	
	public static boolean addLight(Statement MySQL_Statement,LogWindow logWindow,String siteID,Light light) throws SQLException{
		if (light.isValid()){
			try{
				String updLightSQL = "INSERT INTO lights (site_id,source_id,circuit_id,room_id,wattage) VALUES("+siteID+","+light.getSourceID()+","+(light.getCircuitID().equals("")?"NULL":light.getCircuitID())+","+(light.getRoomID().equals("")?"NULL":light.getRoomID())+","+(light.getWattage().equals("")?"NULL":light.getWattage())+")"; //adds specified information into the database
				MySQL_Statement.executeUpdate(updLightSQL);

				return true;
			}catch(SQLException sE){
				Source.removeSource(MySQL_Statement,siteID,light.sourceID);
				sE.printStackTrace();
				logWindow.println("Error occured when writing light information.");
				throw new SQLException();
			}
		}
		else{
			Source.removeSource(MySQL_Statement,siteID,light.sourceID);
			logWindow.println("Error occured when writing light information.");
			throw new SQLException();
		}
	}
	
	public String getSourceID(){
		return sourceID;
	}
	
	public String getSourceName(){
		return sourceName;
	}
	
	public String getCircuitID(){
		return circuitID;
	}
	
	public String getRoomID(){
		return roomID;
	}
	
	public String getWattage(){
		return wattage;
	}
	
	public String getNotes(){
		return notes;
	}
}
