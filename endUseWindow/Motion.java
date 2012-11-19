package endUseWindow;

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
			if (sourceName.matches("^[\\w\\s\\-\\(\\)/]{0,16}$")){
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
