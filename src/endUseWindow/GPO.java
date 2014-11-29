package endUseWindow;

import javax.swing.JOptionPane;

public class GPO {
	String gpoID;
	String gpoName;
	String circuitID;
	String roomID;
	String notes;
	
	
	public GPO(String gpoID,String gpoName,String circuitID,String roomID,String notes){
		this.gpoID = (gpoID==null?"":gpoID);
		this.gpoName = (gpoName==null?"":gpoName);
		this.circuitID = (circuitID==null?"":circuitID);
		this.roomID = (roomID==null?"":roomID);
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
		if (gpoID.matches("^\\d{1,10}$")){
			if (gpoName.matches("^[\\w\\s]{0,16}$")){
				if (circuitID.matches("^\\d{1,10}$")){
					if (roomID.matches("^\\d{1,10}$")){
						isValid = true;
					}
					else{
						JOptionPane.showMessageDialog(null,"The Room ID provided is invalid.\r\nMax 10 numeric characters.","GPO Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Circuit ID provided is invalid.\r\nMax 10 numeric characters.","GPO Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The GPO Name provided is invalid.\r\nMax 10 alphanumeric characters.","GPO Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The GPO ID provided is invalid.\r\nNumeric characters.","GPO Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}	
}
