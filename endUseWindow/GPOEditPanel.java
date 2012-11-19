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

public class GPOEditPanel extends JPanel {
	private static final long serialVersionUID = 2465730538321066498L;
	
	//GPO Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel gpoIDLabel = new JLabel("GPO ID:");
	JLabel gpoNameLabel = new JLabel("GPO Name:");
	JLabel circuitIDLabel = new JLabel("Circuit ID:");
	JLabel roomIDLabel = new JLabel("Room ID:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField gpoIDInput = new JTextField(20); //gpo id
	JTextField gpoNameInput = new JTextField(20); //gpo name
	JComboBox circuitIDInput = new JComboBox(); //circuit id
	JComboBox roomIDInput = new JComboBox(); //room id
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public GPOEditPanel(Connection dbConn){
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
		this.add(gpoIDLabel);
		this.add(gpoIDInput);
		this.add(gpoNameLabel);
		this.add(gpoNameInput);
		this.add(circuitIDLabel);
		this.add(circuitIDInput);
		this.add(roomIDLabel);
		this.add(roomIDInput);
		this.add(notesLabel);
		this.add(notesInput);
		
		siteIDInput.setEnabled(false);
		gpoIDInput.setEnabled(false);
	}

	private void resetGPOEditForm(){
		siteIDInput.setText("");
		gpoIDInput.setText("");
		gpoNameInput.setText("");
		circuitIDInput.removeAllItems();
		roomIDInput.removeAllItems();
		notesInput.setText("");
	}
	
	private void addGPO(String siteID,String siteName){
		//PREPARE NEW SOURCE
		String newGPOID = null;
		if (siteIDInput.getText().equals("") && gpoIDInput.getText().equals("")){
			//set sites
			siteIDInput.setText(siteID+": "+siteName);

			try{
				//get new gpo
				Statement MySQL_Statement = dbConn.createStatement();
				MySQL_Statement.executeUpdate("INSERT INTO gpos (site_id) VALUES("+siteID+")");//creates new row in sources
				ResultSet new_gpo_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 

				if (new_gpo_id_result.next()){ //if retrieved a new id
					newGPOID = new_gpo_id_result.getString(1);
				}
				gpoIDInput.setText(newGPOID);

				//get room types
				ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_type FROM rooms WHERE site_id ="+siteID);//gets relevant rooms
				if (roomIDsSQL.next()){
					roomIDsSQL.beforeFirst();
					while (roomIDsSQL.next()){ //if got some rooms
						roomIDInput.addItem(roomIDsSQL.getString("room_id")+": "+roomIDsSQL.getString("room_type"));
					}
				}
				else{
					roomIDInput.addItem("No Rooms");
					roomIDInput.addItem("Add New Room");
				}

				//get circuits
				ResultSet circuitIDsSQL = MySQL_Statement.executeQuery("SELECT circuit_id,source_name FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id ="+siteID);//gets relevant circuits
				if (circuitIDsSQL.next()){
					circuitIDsSQL.beforeFirst();
					while (circuitIDsSQL.next()){ //if got some circuits
						circuitIDInput.addItem(circuitIDsSQL.getString("circuit_id")+": "+circuitIDsSQL.getString("source_name"));
					}
				}
				else{
					circuitIDInput.addItem("No Circuits");
					circuitIDInput.addItem("Add New Circuit");
				}
			} catch (SQLException sE){
				sE.printStackTrace();
			} 
		}
		else{ //this happens when an error has occurred with the previous source add attempt
			newGPOID = gpoIDInput.getText();
		}

		//GET USER RESPONSE
		if (newGPOID != null){
			gpoNameInput.requestFocus();

			int response = JOptionPane.showOptionDialog(null,this,"Adding new GPO.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){ //"Done" was selected
				GPO newGPO = new GPO(newGPOID,gpoNameInput.getText(),circuitIDInput.getSelectedItem().toString().split(": ")[0],roomIDInput.getSelectedItem().toString().split(": ")[0],notesInput.getText());
				if (newGPO.isValid()){
					try{
						Statement MySQL_Statement = dbConn.createStatement();
						try{
							String updGPOSQL = "UPDATE gpos SET gpo_name='"+gpoNameInput.getText()+"',circuit_id="+circuitIDInput.getSelectedItem().toString().split(": ")[0]+",room_id="+roomIDInput.getSelectedItem().toString().split(": ")[0]+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+siteID+" AND gpo_id = "+newGPOID; //adds specified information into the database
							MySQL_Statement.executeUpdate(updGPOSQL);
	
							resetGPOEditForm();
	
							//restart, because user clicked "Add another"
							if (response==JOptionPane.NO_OPTION){
								addGPO(siteID,siteName);
							}
						}catch(SQLException sE){
							if (sE.getErrorCode()==1062){
								JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing GPO for this site.","Error",JOptionPane.ERROR_MESSAGE);
							}
							else{
								sE.printStackTrace();
								JOptionPane.showMessageDialog(null,"Error occured when writing gpo information.","Error",JOptionPane.ERROR_MESSAGE);
							}
						}
					} catch (SQLException sE){
						sE.printStackTrace();
					} 
				}
				else{
					addGPO(siteID,siteName);
				}
			}
			else{
				removeGPO(siteID,newGPOID); //incomplete source
			}
			resetGPOEditForm();
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}

	}
	
	private void editGPO(String siteID,String siteName,String gpoID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			
			if(siteIDInput.getText().equals("") || gpoIDInput.getText().equals("") || roomIDInput.getItemCount()==0 || circuitIDInput.getItemCount()==0){
				System.out.println("lala");
				//get room types
				ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_type FROM rooms WHERE site_id ="+siteID);//gets relevant rooms
				if (roomIDsSQL.next()){
					roomIDsSQL.beforeFirst();
					while (roomIDsSQL.next()){ //if got some rooms
						roomIDInput.addItem(roomIDsSQL.getString("room_id")+": "+roomIDsSQL.getString("room_type"));
					}
				}
				else{
					roomIDInput.addItem("No Rooms");
					roomIDInput.addItem("Add New Room");
				}

				//get circuits
				ResultSet circuitIDsSQL = MySQL_Statement.executeQuery("SELECT circuit_id,source_name FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id ="+siteID);//gets relevant circuits
				if (circuitIDsSQL.next()){
					circuitIDsSQL.beforeFirst();
					while (circuitIDsSQL.next()){ //if got some circuits
						circuitIDInput.addItem(circuitIDsSQL.getString("circuit_id")+": "+circuitIDsSQL.getString("source_name"));
					}
				}
				else{
					circuitIDInput.addItem("No Circuits");
					circuitIDInput.addItem("Add New Circuit");
				}

				siteIDInput.setText(siteID+": "+siteName);
				gpoIDInput.setText(gpoID);
				ResultSet gpoData = MySQL_Statement.executeQuery("SELECT sources.source_name,rooms.room_type,gpos.* FROM gpos LEFT JOIN rooms ON rooms.room_id = gpos.room_id LEFT JOIN circuits ON circuits.circuit_id = gpos.circuit_id LEFT JOIN sources ON sources.source_id = circuits.source_id WHERE gpos.site_id ="+siteID+" AND gpo_id = "+gpoID); //retrieves data pertaining to selected site
				if (gpoData.next()){
					System.out.println(gpoData.getString("gpo_name")+" lkdfh");
					gpoNameInput.setText(gpoData.getString("gpo_name"));
					circuitIDInput.setSelectedItem(gpoData.getString("circuit_id")+": "+gpoData.getString("source_name"));
					roomIDInput.setSelectedItem(gpoData.getString("room_id")+": "+gpoData.getString("room_type"));
					notesInput.setText(gpoData.getString("notes"));
				}
			}

			if (!siteIDInput.getText().equals("") && !gpoIDInput.getText().equals("") && roomIDInput.getItemCount()>0 && circuitIDInput.getItemCount()>0){ //if retrieved a new id


				int response = JOptionPane.showConfirmDialog(null,this,"Editing gpo (GPO ID: "+gpoID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					GPO newGPO = new GPO(gpoID,gpoNameInput.getText(),circuitIDInput.getSelectedItem().toString().split(": ")[0],roomIDInput.getSelectedItem().toString().split(": ")[0],notesInput.getText());
					if (newGPO.isValid()){
						try{
							String updGPOSQL = "UPDATE gpos SET gpo_name='"+gpoNameInput.getText()+"',circuit_id="+circuitIDInput.getSelectedItem().toString().split(": ")[0]+",room_id="+roomIDInput.getSelectedItem().toString().split(": ")[0]+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+siteID+" AND gpo_id = "+gpoID; //adds specified information into the database
							System.out.println(updGPOSQL);
							MySQL_Statement.executeUpdate(updGPOSQL);

							resetGPOEditForm();
						}catch(SQLException sE){
							if (sE.getErrorCode()==1062){
								JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing GPO for this site.","Error",JOptionPane.ERROR_MESSAGE);
							}
							else{
								sE.printStackTrace();
								JOptionPane.showMessageDialog(null,"Error occured when writing gpo information.","Error",JOptionPane.ERROR_MESSAGE);
							}
							editGPO(siteID,siteName,gpoID);
						}
					}
					else{
						editGPO(siteID,siteName,gpoID);
					}
				}
				resetGPOEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for gpo (Source ID: "+gpoID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch (SQLException sE){
			sE.printStackTrace();
		} 
	}
	
	private void questionRemoveGPO(String siteID,String gpoID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+gpoID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeGPO(siteID,gpoID);
		}
	}
	
	private void removeGPO(String siteID,String gpoID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM gpos WHERE site_id = "+siteID+" AND gpo_id = "+gpoID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified source (Source ID: "+gpoID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}
	}
	
	public JPanel getGPOButtonPanel(SiteTable siteTable,GPOTable gpoTable){
		return new GPOButtonPanel(siteTable,gpoTable);
	}
	
	//Button Panel
	private class GPOButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton gpoAddB = new JButton("Add");
		JButton gpoEditB = new JButton("Edit");
		JButton gpoRemB = new JButton("Remove");

		SiteTable siteTable;
		GPOTable gpoTable;
		
		GPOButtonPanel(SiteTable siteTable,GPOTable gpoTable){
			this.siteTable = siteTable;
			this.gpoTable = gpoTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(gpoAddB);
			this.add(gpoEditB);
			this.add(gpoRemB);
			
			gpoAddB.addActionListener(this);
			gpoEditB.addActionListener(this);
			gpoRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			gpoAddB.setEnabled(enabled);
			gpoEditB.setEnabled(enabled);
			gpoRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				String siteID = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				String siteName = siteTable.getValueAt(siteTable.getSelectedRow(),1).toString();
				
				if (aE.getSource().equals(gpoAddB)){
					addGPO(siteID,siteName);
				}
				else if (aE.getSource().equals(gpoEditB)){
					if (gpoTable.gpoListModel.isSelectionEmpty()==false){
						String sourceID = gpoTable.getValueAt(gpoTable.getSelectedRow(),0).toString();
						editGPO(siteID,siteName,sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(gpoRemB)){
					if (gpoTable.gpoListModel.isSelectionEmpty()==false){
						String sourceID = gpoTable.getValueAt(gpoTable.getSelectedRow(),0).toString();
						questionRemoveGPO(siteID,sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				gpoTable.update(dbConn,siteID);
			}
		}
	}
}
