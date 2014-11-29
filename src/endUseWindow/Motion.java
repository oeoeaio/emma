package endUseWindow;

import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;


public class Motion extends Source{
	String sourceID;
	String sourceName;
	String roomID;
	String notes;

	
	
	public Motion(Site site,String sourceID,String sourceName,String roomID,String notes){
		super(site,sourceID,sourceName,"Motion","");
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.roomID = (roomID==null?"":roomID);
		this.notes = (notes==null?"":notes);
	}
	
	public boolean isValid(){
		boolean isValid = false;
		if (sourceID.matches("^\\d{1,10}$")){
			if (sourceName.matches("^[\\w\\s\\-\\(\\)/]{0,20}$")){
				if (roomID.matches("^\\d{1,10}$") || roomID.equals("")){
					if (notes.matches("^[\\w\\s]{0,255}$") || notes.equals("")){
						isValid = true;
					}
					else{
						JOptionPane.showMessageDialog(null,"The notes provided are invalid.\r\n Max 255 Alphnumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Room ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
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
	
	public static boolean addMotion(Statement MySQL_Statement,LogWindow logWindow,String siteID,Motion motion) throws SQLException{
		if (motion.isValid()){
			try{
				String updMotionSQL = "INSERT INTO motion (site_id,source_id,room_id,notes) VALUES("+siteID+","+motion.getSourceID()+","+(motion.getRoomID().equals("")?"NULL":motion.getRoomID())+","+(motion.getNotes().equals("")?"NULL":"'"+motion.getNotes()+"'")+")"; //adds specified information into the database
				MySQL_Statement.executeUpdate(updMotionSQL);
				
				return true;
			}catch(SQLException sE){
				Source.removeSource(MySQL_Statement,siteID,motion.sourceID);
				sE.printStackTrace();
				logWindow.println("Error occured when writing motion information.");
				throw new SQLException();
			}
		}
		else{
			Source.removeSource(MySQL_Statement,siteID,motion.sourceID);
			logWindow.println("Error occured when writing motion information.");
			throw new SQLException();
		}
	}
	
	public String getSourceID(){
		return sourceID;
	}
	
	public String getSourceName(){
		return sourceName;
	}
	
	public String getRoomID(){
		return roomID;
	}
	
	public String getNotes(){
		return notes;
	}
}
