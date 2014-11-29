package endUseWindow;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class RoomEditPanel extends JPanel{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Room Edit Panel
	JPanel roomEditPanel = new JPanel();
	JLabel siteIDLabel = new JLabel("Site ID:");
	//JLabel roomIDLabel = new JLabel("Room ID:");
	JLabel roomNumberLabel = new JLabel("Room Number:");
	JLabel roomTypeLabel = new JLabel("Room Type:");
	JLabel areaLabel = new JLabel("Area:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	//JTextField roomIDInput = new JTextField(20); //room id
	JComboBox<String> roomNumberInput = new JComboBox<String>(new String[] {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50","51","52","53","54","55","56","57","58","59","60"}); //site name
	JComboBox<String> roomTypeInput = new JComboBox<String>(Room.getRoomTypes()); //room type
	JTextField areaInput = new JTextField(20); //area
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;

	public RoomEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new GridLayout(6,2));
		this.add(siteIDLabel);
		this.add(siteIDInput);
		//this.add(roomIDLabel);
		//this.add(roomIDInput);
		this.add(roomNumberLabel);
		this.add(roomNumberInput);
		this.add(roomTypeLabel);
		this.add(roomTypeInput);
		this.add(areaLabel);
		this.add(areaInput);
		this.add(notesLabel);
		this.add(notesInput);

		//applianceInput1.setEnabled(false);
		siteIDInput.setEnabled(false);
		//roomIDInput.setEnabled(false);
	}

	private void resetRoomEditForm(){
		siteIDInput.setText("");
		//roomIDInput.setText("");
		roomNumberInput.setSelectedIndex(0);
		roomTypeInput.setSelectedIndex(0);
		areaInput.setText("");
		notesInput.setText("");
	}
	
	public void addRoom(String siteID,String siteName){
		roomNumberInput.setEnabled(true);
		siteIDInput.setText(siteID+": "+siteName);
		roomNumberInput.requestFocus();
		
		
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet maxRoomRS = MySQL_Statement.executeQuery("SELECT max(room_number)+1 AS next_room FROM rooms WHERE site_id = "+siteID);
			if (maxRoomRS.next()){
				roomNumberInput.setSelectedItem(maxRoomRS.getString("next_room"));
			}
		} catch(SQLException sE){
			sE.printStackTrace();
		}

		int response = JOptionPane.showOptionDialog(null,this,"Adding new room.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
		if (response == JOptionPane.YES_OPTION || response == JOptionPane.NO_OPTION){ 
			Room newRoom = new Room(null,roomNumberInput.getSelectedItem().toString(),roomTypeInput.getSelectedItem().toString(),areaInput.getText(),notesInput.getText());
			if (newRoom.isValid()){
				try{
					Statement MySQL_Statement = dbConn.createStatement();
					try{
						String newSourceSQL = "INSERT INTO rooms (site_id,room_number,room_type,area,notes) VALUES("+siteIDInput.getText().split(": ")[0]+","+roomNumberInput.getSelectedItem()+",'"+roomTypeInput.getSelectedItem().toString()+"',"+(areaInput.getText().equals("")?"NULL":areaInput.getText())+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
						MySQL_Statement.executeUpdate(newSourceSQL);
	
						resetRoomEditForm();
						if (response == JOptionPane.NO_OPTION){ //"Add Another" was selected
							addRoom(siteID,siteName);
						}
	
					} catch(SQLException sE){
						if (sE.getErrorCode()==1062){
							JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing room for this site.","Error",JOptionPane.ERROR_MESSAGE);
						}
						else{
							sE.printStackTrace();
							JOptionPane.showMessageDialog(null,"Error occured when writing room information.","Error",JOptionPane.ERROR_MESSAGE);
						}
						addRoom(siteID,siteName);
					}
				} catch(SQLException sE){
					sE.printStackTrace();
				}

			}
			else{
				addRoom(siteID,siteName);
			}
		}
		else{
			//Do Nothing.
		}

		resetRoomEditForm();	

	}
	
	private void editRoom(String siteID,String siteName,String roomID){
		try{	
			roomNumberInput.setEnabled(false);
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet roomData = MySQL_Statement.executeQuery("SELECT * FROM rooms WHERE site_id = "+siteID+" AND room_id = "+roomID); //retrieves data pertaining to selected site

			if (roomData.next()){ //if retrieved a new id
				siteIDInput.setText(siteID+": "+siteName);
				roomNumberInput.setSelectedItem(roomData.getString("room_number"));
				roomTypeInput.setSelectedItem(roomData.getString("room_type"));
				areaInput.setText(roomData.getString("area"));
				notesInput.setText(roomData.getString("notes"));

				int response = JOptionPane.showConfirmDialog(null,this,"Editing room (Room ID: "+roomID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Room newRoom = new Room(null,roomNumberInput.getSelectedItem().toString(),roomTypeInput.getSelectedItem().toString(),areaInput.getText(),notesInput.getText());
					if (newRoom.isValid()){
						try{
							String updRoomSQL = "UPDATE rooms SET room_number="+roomNumberInput.getSelectedItem().toString()+",room_type='"+roomTypeInput.getSelectedItem().toString()+"',area="+(areaInput.getText().equals("")?"NULL":areaInput.getText())+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+siteID+" AND room_id = "+roomID; //adds specified information into the database
							MySQL_Statement.executeUpdate(updRoomSQL);
						} catch(SQLException sE){
							if (sE.getErrorCode()==1062){
								JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing room for this site.","Error",JOptionPane.ERROR_MESSAGE);
							}
							else{
								sE.printStackTrace();
								JOptionPane.showMessageDialog(null,"Error occured when writing room information.","Error",JOptionPane.ERROR_MESSAGE);
							}
							editRoom(siteID,siteName,roomID);
						}
					}
					else{
						editRoom(siteID,siteName,roomID);
					}
				}
				resetRoomEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for room (Source ID: "+roomID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch (SQLException sE){
			sE.printStackTrace();
		} 
	}
	
	private void questionRemoveRoom(String siteID,String roomID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+roomID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeRoom(siteID,roomID);
		}
	}
	
	private void removeRoom(String siteID,String roomID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM rooms WHERE siteID = "+siteID+" AND source_id = "+roomID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified room (Source ID: "+roomID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}
	}
	
	public JPanel getRoomButtonPanel(SiteTable siteTable,RoomTable roomTable){
		return new RoomButtonPanel(siteTable,roomTable);
	}
	
	//Button Panel
	public class RoomButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton roomAddB = new JButton("Add");
		JButton roomEditB = new JButton("Edit");
		JButton roomRemB = new JButton("Remove");
		
		SiteTable siteTable;
		RoomTable roomTable;
		
		RoomButtonPanel(SiteTable siteTable,RoomTable roomTable){
			this.siteTable = siteTable;
			this.roomTable = roomTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(roomAddB);
			this.add(roomEditB);
			this.add(roomRemB);
			
			roomAddB.addActionListener(this);
			roomEditB.addActionListener(this);
			roomRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			roomAddB.setEnabled(enabled);
			roomEditB.setEnabled(enabled);
			roomRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				String siteID = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				String siteName = siteTable.getValueAt(siteTable.getSelectedRow(),1).toString();
				
				if (aE.getSource().equals(roomAddB)){
					addRoom(siteID,siteName);
				}
				else if (aE.getSource().equals(roomEditB)){
					if (roomTable.roomListModel.isSelectionEmpty()==false){
						String roomID = roomTable.getValueAt(roomTable.getSelectedRow(),0).toString();
						editRoom(siteID,siteName,roomID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(roomRemB)){
					if (roomTable.roomListModel.isSelectionEmpty()==false){
						String sourceID = roomTable.getValueAt(roomTable.getSelectedRow(),0).toString();
						questionRemoveRoom(siteID,sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				roomTable.update(dbConn, siteID);
			}
		}
	}
}
