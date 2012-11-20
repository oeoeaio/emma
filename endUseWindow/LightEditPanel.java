package endUseWindow;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class LightEditPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Appliance Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel sourceIDLabel = new JLabel("Source ID:");
	JLabel sourceNameLabel = new JLabel("Source Name:");
	JLabel circuitLabel = new JLabel("Circuit:");
	JLabel roomLabel = new JLabel("Room:");
	JLabel sourceTypeLabel = new JLabel("Source Type:");
	JLabel measurementTypeLabel = new JLabel("MeasurementType:");
	JLabel wattageLabel = new JLabel("Wattage:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField sourceIDInput = new JTextField(20); //source id
	JTextField sourceNameInput = new JTextField(20); //source name
	JComboBox<String> circuitInput = new JComboBox<String>(); //circuit id
	JComboBox<String> roomInput = new JComboBox<String>(); //room id
	JComboBox<String> sourceTypeInput = new JComboBox<String>(Source.getSourceTypeList()); //source type
	JComboBox<String> measurementTypeInput = new JComboBox<String>(Source.getMeasurementList()); //measurement type
	JTextField wattageInput = new JTextField(20); //wattage
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public LightEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new GridLayout(16,2));
	//	this.setLayout(new BorderLayout());
		
	//	JPanel splitPanel = new JPanel();
	//	/splitPanel.setLayout(new GridLayout(1,3,20,0));
		
	//	JPanel panel1 = new JPanel(new GridLayout(16,2));
	//	JPanel panel2 = new JPanel(new GridLayout(16,2));

	//	splitPanel.add(panel1);
	//	splitPanel.add(panel2);
		
		this.add(siteIDLabel);
		this.add(siteIDInput);
		this.add(sourceIDLabel);
		this.add(sourceIDInput);
		this.add(sourceNameLabel);
		this.add(sourceNameInput);
		this.add(circuitLabel);
		this.add(circuitInput);
		this.add(roomLabel);
		this.add(roomInput);
		this.add(sourceTypeLabel);
		this.add(sourceTypeInput);
		this.add(measurementTypeLabel);
		this.add(measurementTypeInput);
		this.add(wattageLabel);
		this.add(wattageInput);
		this.add(notesLabel);
		this.add(notesInput);
		
		
		//this.add(splitPanel,BorderLayout.CENTER);
		
		siteIDInput.setEnabled(false);
		sourceIDInput.setEnabled(false);
		
		roomInput.addActionListener(this);
		sourceTypeInput.addActionListener(this);
		
		sourceTypeInput.setSelectedItem("Light");
		sourceTypeInput.setEnabled(false);
	}

	private void resetLightEditForm(){
		siteIDInput.setText("");
		sourceIDInput.setText("");
		sourceNameInput.setText("");
		circuitInput.removeAllItems();
		roomInput.removeAllItems();
		sourceTypeInput.setSelectedItem("Light");
		measurementTypeInput.setSelectedIndex(0);
		wattageInput.setText("");
		notesInput.setText("");
	}
	
	private void addLight(Site site){

		//PREPARE NEW SOURCE
		String newSourceID = null;
		ArrayList<String> circuitIDList = new ArrayList<String>();
		ArrayList<String> roomIDList = new ArrayList<String>();
		
		if (siteIDInput.getText().equals("") || sourceIDInput.getText().equals("") || circuitInput.getItemCount()==0 || roomInput.getItemCount()==0){
			//set sites
			siteIDInput.setText(site.siteID+": "+site.siteName);
			
			try{
				//get new source
				Statement MySQL_Statement = dbConn.createStatement();
				MySQL_Statement.executeUpdate("INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.siteID+",NULL,'Light','OnTime')");//creates new row in sources
				ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
	
				if (new_id_result.next()){ //if retrieved a new id
					newSourceID = new_id_result.getString(1);
				}
				sourceIDInput.setText(newSourceID);
	
				//create new light
				//MySQL_Statement.executeUpdate("INSERT INTO appliances (site_id,source_id) VALUES("+siteID+","+newSourceID+")");//creates new row in sources
				try{
					//get room types
					ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_number,room_type FROM rooms WHERE site_id ="+site.siteID+" ORDER BY room_number ASC");//gets relevant rooms
					if (roomIDsSQL.next()){
						roomIDsSQL.beforeFirst();
						while (roomIDsSQL.next()){ //if got some rooms
							roomInput.addItem(roomIDsSQL.getString("room_number")+": "+roomIDsSQL.getString("room_type"));
							roomIDList.add(roomIDsSQL.getString("room_id"));
						}
					}
					else{
						roomInput.addItem("No Rooms");
						roomInput.addItem("Add New Room");
					}
		
					//get circuits
					ResultSet circuitIDsSQL = MySQL_Statement.executeQuery("SELECT circuit_id,source_name FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id ="+site.siteID);//gets relevant circuits
					if (circuitIDsSQL.next()){
						circuitIDsSQL.beforeFirst();
						while (circuitIDsSQL.next()){ //if got some circuits
							circuitInput.addItem(circuitIDsSQL.getString("source_name"));
							circuitIDList.add(circuitIDsSQL.getString("circuit_id"));
						}
					}
					else{
						circuitInput.addItem("No Circuits");
						circuitInput.addItem("Add New Circuit");
					}
				} catch(SQLException sE){
					sE.printStackTrace();
					removeLight(site,newSourceID); //incomplete source
				}
			} catch (SQLException sE){
				JOptionPane.showMessageDialog(null,"Error occured when creating new source.","Error",JOptionPane.ERROR_MESSAGE);
			} 
		}
		else{ //this happens when an error has occurred with the previous source add attempt
			newSourceID = sourceIDInput.getText();
		}

		//GET USER RESPONSE
		if (!siteIDInput.getText().equals("") && !sourceIDInput.getText().equals("") && circuitInput.getItemCount()>0 && roomInput.getItemCount()>0){
			sourceNameInput.requestFocus();

			int response = JOptionPane.showOptionDialog(null,this,"Adding new light.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){
				Source newSource = new Source(site,newSourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
				if (newSource.isValid()){
					Light newLight = new Light(site,newSourceID,sourceNameInput.getText(),circuitIDList.get(circuitInput.getSelectedIndex()),roomIDList.get(roomInput.getSelectedIndex()),wattageInput.getText(),notesInput.getText());
					if (newLight.isValid()){
						try{
							Statement MySQL_Statement = dbConn.createStatement();
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+site.siteID+" AND source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updLightSQL = "INSERT INTO lights (site_id,source_id,circuit_id,room_id,wattage,notes) VALUES("+site.siteID+","+newSourceID+","+circuitIDList.get(circuitInput.getSelectedIndex())+","+roomIDList.get(roomInput.getSelectedIndex())+","+(wattageInput.getText().equals("")?"NULL":wattageInput.getText())+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
									MySQL_Statement.executeUpdate(updLightSQL);
	
									resetLightEditForm();
	
									//restart, because user clicked "Add another"
									if (response==JOptionPane.NO_OPTION){
										addLight(site);
									}
								}catch(SQLException sE){
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing light information.","Error",JOptionPane.ERROR_MESSAGE);
									addLight(site);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								addLight(site);
							}
						} catch (SQLException sE){
							sE.printStackTrace();
						} 
					}
					else{
						addLight(site);
					}
				}
				else{
					addLight(site);
				}
			}
			else{
				removeLight(site,newSourceID); //incomplete source
			}
			resetLightEditForm();
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void editLight(Source source){	
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			ArrayList<String> circuitIDList = new ArrayList<String>();
			ArrayList<String> roomIDList = new ArrayList<String>();
			
			if (siteIDInput.getText().equals("") || sourceIDInput.getText().equals("") || circuitInput.getItemCount()==0 || roomInput.getItemCount()==0){
				//get room types
				ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_number,room_type FROM rooms WHERE site_id ="+source.site.siteID);//gets relevant rooms
				if (roomIDsSQL.next()){
					roomIDsSQL.beforeFirst();
					while (roomIDsSQL.next()){ //if got some rooms
						roomInput.addItem(roomIDsSQL.getString("room_number")+": "+roomIDsSQL.getString("room_type"));
						roomIDList.add(roomIDsSQL.getString("room_id"));
					}
				}
				else{
					roomInput.addItem("No Rooms");
					roomInput.addItem("Add New Room");
				}
				
				//get circuits
				ResultSet circuitIDsSQL = MySQL_Statement.executeQuery("SELECT circuit_id,source_name FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id ="+source.site.siteID);//gets relevant circuits
				if (circuitIDsSQL.next()){
					circuitIDsSQL.beforeFirst();
					while (circuitIDsSQL.next()){ //if got some circuits
						circuitInput.addItem(circuitIDsSQL.getString("source_name"));
						circuitIDList.add(circuitIDsSQL.getString("circuit_id"));
					}
				}
				else{
					circuitInput.addItem("No Circuits");
					circuitInput.addItem("Add New Circuit");
				}

				siteIDInput.setText(source.site.siteID+": "+source.site.siteName);
				sourceIDInput.setText(source.sourceID);
				ResultSet lightData = MySQL_Statement.executeQuery("SELECT sources.source_name,sources.source_type,sources.measurement_type,rooms.room_number,rooms.room_type,lights.* FROM lights LEFT JOIN sources ON sources.source_id = lights.source_id LEFT JOIN rooms ON lights.room_id = rooms.room_id WHERE lights.site_id ="+source.site.siteID+" AND lights.source_id = "+source.sourceID); //retrieves data pertaining to selected site
				if (lightData.next()){
					sourceNameInput.setText(lightData.getString("source_name"));
					circuitInput.setSelectedIndex(circuitIDList.lastIndexOf(lightData.getString("circuit_id")));
					roomInput.setSelectedItem(lightData.getString("room_number")+": "+lightData.getString("room_type"));
					sourceTypeInput.setSelectedItem(lightData.getString("source_type"));
					measurementTypeInput.setSelectedItem(lightData.getString("measurement_type"));
					wattageInput.setText(lightData.getString("wattage"));
					notesInput.setText(lightData.getString("notes"));
				}
			}
						
			if (!siteIDInput.getText().equals("") && !sourceIDInput.getText().equals("") && circuitInput.getItemCount()>0 && roomInput.getItemCount()>0){				
	
				int response = JOptionPane.showConfirmDialog(null,this,"Editing light (Source ID: "+source.sourceID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Source newSource = new Source(source.site,source.sourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
					if (newSource.isValid()){
						Light newLight = new Light(source.site,source.sourceID,sourceNameInput.getText(),circuitIDList.get(circuitInput.getSelectedIndex()),roomIDList.get(roomInput.getSelectedIndex()),wattageInput.getText(),notesInput.getText());
						if (newLight.isValid()){
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updLightSQL = "UPDATE lights SET circuit_id="+circuitIDList.get(circuitInput.getSelectedIndex())+",room_id="+roomIDList.get(roomInput.getSelectedIndex())+",wattage="+(wattageInput.getText().equals("")?"NULL":wattageInput.getText())+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
									MySQL_Statement.executeUpdate(updLightSQL);
								}catch(SQLException sE){
									JOptionPane.showMessageDialog(null,"Error occured when writing light information.","Error",JOptionPane.ERROR_MESSAGE);
									editLight(source);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								editLight(source);
							}
						}
						else{
							editLight(source);
						}
					}
					else{
						editLight(source);
					}
				}
				resetLightEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for light (Source ID: "+source.sourceID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch(SQLException sE){
			sE.printStackTrace();
		}
	}
	
	private void questionRemoveLight(Site site,String sourceID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+sourceID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeLight(site,sourceID);
		}
	}
	
	private void removeLight(Site site,String sourceID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+site.siteID+" AND source_id = "+sourceID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified source (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}

	}
	
	/*void fillGPOs(String siteID){
		gpoInput.removeAllItems();
		try{
			//fill gpos
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet gpoIDsSQL = MySQL_Statement.executeQuery("SELECT gpo_id,gpo_name FROM gpos WHERE site_id ="+siteID+" AND room_id = "+roomIDInput.getSelectedItem().toString().split(": ")[0]);//gets relevant gpos
			if (gpoIDsSQL.next()){
				gpoIDsSQL.beforeFirst();
				while (gpoIDsSQL.next()){ //if got some rooms
					gpoInput.addItem(gpoIDsSQL.getString("gpo_id")+": "+gpoIDsSQL.getString("gpo_name"));
				}
			}
			else{
				gpoInput.addItem("No GPOs");
				gpoInput.addItem("Add New GPO");
			}
		}catch(SQLException sE){
			gpoInput.addItem("DB Error");
		}
	}*/

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(sourceTypeInput)){
			measurementTypeInput.removeAllItems();
			if(sourceTypeInput.getSelectedItem().toString().equals("Appliance")){
				measurementTypeInput.addItem("AppEnergy");
				measurementTypeInput.addItem("AppPower");
				measurementTypeInput.addItem("ActEnergy");
				measurementTypeInput.addItem("ActPower");
				measurementTypeInput.addItem("Volts");
				measurementTypeInput.addItem("Amps");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Circuit")){
				measurementTypeInput.addItem("ActPower");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Gas")){
				measurementTypeInput.addItem("Pulse");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Humidity")){
				measurementTypeInput.addItem("Humidity");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Light")){
				measurementTypeInput.addItem("OnTime");
				measurementTypeInput.addItem("LightLevel");
				measurementTypeInput.addItem("AppEnergy");
				measurementTypeInput.addItem("AppPower");
				measurementTypeInput.addItem("ActEnergy");
				measurementTypeInput.addItem("ActPower");
				measurementTypeInput.addItem("Volts");
				measurementTypeInput.addItem("Amps");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Motion")){
				measurementTypeInput.addItem("Motion");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Temperature")){
				measurementTypeInput.addItem("Temp");
				measurementTypeInput.addItem("AvgTemp");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Water")){
				measurementTypeInput.addItem("Pulse");
			}
		}
		else if (aE.getSource().equals(roomInput) && roomInput.getItemCount()>0 && roomInput.getSelectedItem().toString().equals("Add New Room") && siteIDInput.getText().toString().split(": ")[0].matches("^\\d{1,10}$")){
			RoomEditPanel roomEditPanel = new RoomEditPanel(dbConn);
			String[] split = siteIDInput.getText().toString().split(": ");
			roomEditPanel.addRoom(split[0],split[1]);
		}
		//else if (aE.getSource().equals(roomIDInput) && roomIDInput.getItemCount()>0 && !roomIDInput.getItemAt(0).equals("No Rooms")){
		//	fillGPOs(siteIDInput.getText().split(": ")[0]);
		//}
	}
	
	public JPanel getLightButtonPanel(SiteTable siteTable,LightTable lightTable){
		return new LightButtonPanel(siteTable,lightTable);
	}
	
	//Button Panel
	public class LightButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton lightAddB = new JButton("Add");
		JButton lightEditB = new JButton("Edit");
		JButton lightRemB = new JButton("Remove");

		SiteTable siteTable;
		LightTable lightTable;
		
		LightButtonPanel(SiteTable siteTable,LightTable lightTable){
			this.siteTable = siteTable;
			this.lightTable = lightTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(lightAddB);
			this.add(lightEditB);
			this.add(lightRemB);
			
			lightAddB.addActionListener(this);
			lightEditB.addActionListener(this);
			lightRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			lightAddB.setEnabled(enabled);
			lightEditB.setEnabled(enabled);
			lightRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				Site site = siteTable.siteList.get(siteTable.getSelectedRow());
				
				if (aE.getSource().equals(lightAddB)){
					addLight(site);
				}
				else if (aE.getSource().equals(lightEditB)){
					if (lightTable.lightListModel.isSelectionEmpty()==false){
						Source source = lightTable.lightList.get(lightTable.getSelectedRow());
						editLight(source);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(lightRemB)){
					if (lightTable.lightListModel.isSelectionEmpty()==false){
						Source source = lightTable.lightList.get(lightTable.getSelectedRow());
						questionRemoveLight(site,source.sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				lightTable.update(dbConn, site);
			}
		}
	}
}
