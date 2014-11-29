package endUseWindow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import javax.swing.JOptionPane;


public class Room {
	String roomID;
	String roomNumber;
	String roomType;
	String area;
	String notes;
	
	
	public Room(String roomID,String roomNumber,String roomType,String area,String notes){
		this.roomID = (roomID==null?"":roomID);
		this.roomNumber = (roomNumber==null?"":roomNumber);
		this.roomType = (roomType==null?"":roomType);
		this.area = (area==null?"":area);
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
		if (roomID.matches("^\\d{1,5}$") || roomID.equals("")){
			if (roomNumber.matches("^\\d{1,2}$")){
				if (Arrays.asList(getRoomTypes()).contains(roomType)){
					if (area.matches("^\\d{1,10}[.]{0,1}\\d{0,3}$") || area.equals("")){
						isValid = true;
					}
					else{
						JOptionPane.showMessageDialog(null,"The area provided is invalid.\r\nMust be a number.","Room Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Room Type provided is invalid.\r\nMust select from list.","Room Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The Room Number provided is invalid.\r\nMax two numeric characters","Room Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Room ID provided is invalid.\r\nNumeric characters.","Room Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}	
	
	public static String addRoom(Statement MySQL_Statement,LogWindow logWindow,String siteID,String sourceID,String roomNumber,String roomType) throws SQLException{
		String roomID = "";
		if (roomNumber.matches("^\\d{1,2}$")){
			ResultSet roomIDRS = MySQL_Statement.executeQuery("SELECT room_id FROM rooms WHERE site_id = "+siteID+" AND room_number = "+roomNumber);
			if (roomIDRS.next()){
				roomID = roomIDRS.getString("room_id");
			}
			else{// add new circuit
				if (Arrays.asList(Room.getRoomTypes()).contains(roomType)){ //check valid room type
					try{
						logWindow.printString("Warning: unable to match specified room '"+roomNumber+"'.\r\nAdding room of type '"+roomType+"' to database...");
						MySQL_Statement.executeUpdate("INSERT INTO rooms (site_id,room_number,room_type) VALUES("+siteID+","+roomNumber+",'"+roomType+"')");
						ResultSet new_room_id = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
						new_room_id.next();
						roomID = new_room_id.getString("current_id");
						logWindow.println("Done.");
					} catch(SQLException sE){
						//error here means should remove source
						sE.printStackTrace();
						logWindow.println("Failed.");
					}

				}
				else{
					logWindow.printString("Warning: unable to match specified room '"+roomNumber+"'.");
				}
			}
		}
		return roomID;
	}

	public static String[] getRoomTypes(){
		return new String[] {		
				"Bathroom",
				"Bedroom",
				"Dining",
				"Foyer-inside",
				"Garage",
				"Hallway",
				"Kitchen",
				"Kitchen/Living",
				"Laundry",
				"Living-other",
				"Lounge",
				"Other-inside",
				"Other-outside",
				"Outside-general",
				"Pantry",
				"Storage Room",
				"Study",
				"Toilet",
				"Verandah",
				"Walk-in Robe"
		};
	}
}
