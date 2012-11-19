package endUseWindow;


import java.awt.BorderLayout;
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

public class ApplianceEditPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Appliance Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel sourceIDLabel = new JLabel("Source ID:");
	JLabel sourceNameLabel = new JLabel("Source Name:");
	JLabel circuitLabel = new JLabel("Circuit:");
	JLabel roomLabel = new JLabel("Room:");
	JLabel sourceTypeLabel = new JLabel("Source Type:");
	JLabel measurementTypeLabel = new JLabel("MeasurementType:");
	JLabel applianceGroupLabel = new JLabel("Appliance Group:");
	JLabel applianceTypeLabel = new JLabel("Appliance Type:");
	JLabel brandLabel = new JLabel("Brand:");
	JLabel modelLabel = new JLabel("Model:");
	JLabel serialLabel = new JLabel("Serial No.:");
	JLabel connectionLabel = new JLabel("Connection:");
	JLabel controlLabel = new JLabel("Control:");
	JLabel switchTypeLabel = new JLabel("Switch Type:");
	JLabel displayLabel = new JLabel("Display:");
	JLabel epsLabel = new JLabel("EPS:");
	JLabel delayStartLabel = new JLabel("Delay Start:");
	JLabel onWLabel = new JLabel("On W:");
	JLabel asWLabel = new JLabel("Active S W:");
	JLabel psWLabel = new JLabel("Passive S W:");
	JLabel offWLabel = new JLabel("Off W:");
	JLabel dsWLabel = new JLabel("Delay Start W:");
	JLabel yearOfPurchaseLabel = new JLabel("Year Of Purchase:");
	JLabel usageLabel = new JLabel("Usage:");
	JLabel usageUnitsLabel = new JLabel("Usage Units:");
	JLabel feature1Label = new JLabel("Feature1:");
	JLabel feature2Label = new JLabel("Feature2:");
	JLabel feature3Label = new JLabel("Feature3:");
	JLabel feature4Label = new JLabel("Feature4:");
	JLabel feature5Label = new JLabel("Feature5:");
	JLabel notesLabel = new JLabel("Comments:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField sourceIDInput = new JTextField(20); //source id
	JTextField sourceNameInput = new JTextField(20); //source name
	JComboBox circuitInput = new JComboBox(); //Circuit
	JComboBox roomInput = new JComboBox(); //room
	JComboBox sourceTypeInput = new JComboBox(Source.getSourceTypeList()); //source type
	JComboBox measurementTypeInput = new JComboBox(Source.getMeasurementList()); //measurement type
	JComboBox applianceGroupInput = new JComboBox(Appliance.getApplianceGroups()); //appliance group
	JComboBox applianceTypeInput = new JComboBox(Appliance.getApplianceTypes(applianceGroupInput.getSelectedIndex())); //appliance type
	JTextField brandInput = new JTextField(20); //brand
	JTextField modelInput = new JTextField(20); //model
	JTextField serialInput = new JTextField(20); //serial
	JComboBox connectionInput = new JComboBox(Appliance.getConnectionValues()); //connection
	JComboBox controlInput = new JComboBox(Appliance.getControlValues()); //control
	JComboBox switchTypeInput = new JComboBox(Appliance.getSwitchTypeValues()); //switch type
	JComboBox displayInput = new JComboBox(Appliance.getYNValues()); //display
	JComboBox epsInput = new JComboBox(Appliance.getYNValues()); //EPS
	JComboBox delayStartInput = new JComboBox(Appliance.getYNValues()); //delay start
	JTextField onWInput = new JTextField(20); //ON W
	JTextField asWInput = new JTextField(20); //AS W
	JTextField psWInput = new JTextField(20); //PS W
	JTextField offWInput = new JTextField(20); //OFF W
	JTextField dsWInput = new JTextField(20); //DS W
	JTextField yearOfPurchaseInput = new JTextField(20); //YOP
	JTextField usageInput = new JTextField(20); //usage
	JComboBox usageUnitsInput = new JComboBox(Appliance.getUsageUnits(applianceGroupInput.getSelectedItem().toString())); //usage units
	JTextField feature1Input = new JTextField(20); //feature 1
	JTextField feature2Input = new JTextField(20); //feature 2
	JTextField feature3Input = new JTextField(20); //feature 3
	JTextField feature4Input = new JTextField(20); //feature 4
	JTextField feature5Input = new JTextField(20); //feature 5
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public ApplianceEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new BorderLayout());
		
		JPanel splitPanel = new JPanel();
		splitPanel.setLayout(new GridLayout(1,3,20,0));
		
		JPanel panel1 = new JPanel(new GridLayout(16,2));
		JPanel panel2 = new JPanel(new GridLayout(16,2));

		splitPanel.add(panel1);
		splitPanel.add(panel2);
		
		panel1.add(siteIDLabel);
		panel1.add(siteIDInput);
		panel1.add(sourceIDLabel);
		panel1.add(sourceIDInput);
		panel1.add(sourceNameLabel);
		panel1.add(sourceNameInput);
		panel1.add(circuitLabel);
		panel1.add(circuitInput);
		panel1.add(roomLabel);
		panel1.add(roomInput);
		panel1.add(sourceTypeLabel);
		panel1.add(sourceTypeInput);
		panel1.add(measurementTypeLabel);
		panel1.add(measurementTypeInput);
		panel1.add(applianceGroupLabel);
		panel1.add(applianceGroupInput);
		panel1.add(applianceTypeLabel);
		panel1.add(applianceTypeInput);
		panel1.add(brandLabel);
		panel1.add(brandInput);
		panel1.add(modelLabel);
		panel1.add(modelInput);
		panel1.add(serialLabel);
		panel1.add(serialInput);
		panel1.add(connectionLabel);
		panel1.add(connectionInput);
		panel1.add(controlLabel);
		panel1.add(controlInput);
		panel1.add(switchTypeLabel);
		panel1.add(switchTypeInput);
		panel1.add(displayLabel);
		panel1.add(displayInput);
		
		panel2.add(epsLabel);
		panel2.add(epsInput);
		panel2.add(delayStartLabel);
		panel2.add(delayStartInput);
		panel2.add(onWLabel);
		panel2.add(onWInput);
		panel2.add(asWLabel);
		panel2.add(asWInput);
		panel2.add(psWLabel);
		panel2.add(psWInput);
		panel2.add(offWLabel);
		panel2.add(offWInput);
		panel2.add(dsWLabel);
		panel2.add(dsWInput);
		panel2.add(yearOfPurchaseLabel);
		panel2.add(yearOfPurchaseInput);
		panel2.add(usageLabel);
		panel2.add(usageInput);
		panel2.add(usageUnitsLabel);
		panel2.add(usageUnitsInput);
		panel2.add(feature1Label);
		panel2.add(feature1Input);
		panel2.add(feature2Label);
		panel2.add(feature2Input);
		panel2.add(feature3Label);
		panel2.add(feature3Input);
		panel2.add(feature4Label);
		panel2.add(feature4Input);
		panel2.add(feature5Label);
		panel2.add(feature5Input);
		panel2.add(notesLabel);
		panel2.add(notesInput);
		
		this.add(splitPanel,BorderLayout.CENTER);
		
		siteIDInput.setEnabled(false);
		sourceIDInput.setEnabled(false);
		
		roomInput.addActionListener(this);
		sourceTypeInput.addActionListener(this);
		applianceGroupInput.addActionListener(this);
		applianceTypeInput.addActionListener(this);
		
		sourceTypeInput.setSelectedItem("Appliance");
		sourceTypeInput.setEnabled(false);
	}

	private void resetApplianceEditForm(){
		siteIDInput.setText("");
		sourceIDInput.setText("");
		sourceNameInput.setText("");
		circuitInput.removeAllItems();
		roomInput.removeAllItems();
		sourceTypeInput.setSelectedItem("Appliance");
		measurementTypeInput.setSelectedIndex(0);
		applianceGroupInput.setSelectedIndex(0);
		applianceTypeInput.setSelectedIndex(0);
		brandInput.setText("");
		modelInput.setText("");
		serialInput.setText("");
		connectionInput.setSelectedIndex(0);
		controlInput.setSelectedIndex(0);
		switchTypeInput.setSelectedIndex(0);
		displayInput.setSelectedIndex(0);
		epsInput.setSelectedIndex(0);
		delayStartInput.setSelectedIndex(0);
		onWInput.setText("");
		asWInput.setText("");
		psWInput.setText("");
		offWInput.setText("");
		dsWInput.setText("");
		yearOfPurchaseInput.setText("");
		usageInput.setText("");
		usageUnitsInput.setSelectedIndex(0);
		feature1Input.setText("");
		feature2Input.setText("");
		feature3Input.setText("");
		feature4Input.setText("");
		feature5Input.setText("");
		notesInput.setText("");
	}
	
	private void addAppliance(Site site){		
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
				MySQL_Statement.executeUpdate("INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.siteID+",NULL,'Appliance','ActEnergy')");//creates new row in sources
				ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
				
				if (new_id_result.next()){ //if retrieved a new id
					newSourceID = new_id_result.getString(1);
				}
				sourceIDInput.setText(newSourceID);
	
				//create new appliance
				//MySQL_Statement.executeUpdate("INSERT INTO appliances (site_id,source_id) VALUES("+siteID+","+newSourceID+")");//creates new row in sources
				try{
					//get room types
					ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_number,room_type FROM rooms WHERE site_id ="+site.siteID);//gets relevant rooms
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
		
					//if (!roomInput.getItemAt(0).equals("No Rooms")){
					//	fillGPOs(siteID);
					//}else{
					//	circuitInput.addItem("No Rooms");
					//}
				} catch(SQLException sE){
					sE.printStackTrace();
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

			int response = JOptionPane.showOptionDialog(null,this,"Adding new appliance.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){
				Source newSource = new Source(site,newSourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
				if (newSource.isValid()){
					Appliance newAppliance = new Appliance(site,newSourceID,sourceNameInput.getText(),circuitIDList.get(circuitInput.getSelectedIndex()),roomIDList.get(roomInput.getSelectedIndex()),applianceGroupInput.getSelectedItem().toString(),applianceTypeInput.getSelectedItem().toString(),brandInput.getText(),modelInput.getText(),serialInput.getText(),connectionInput.getSelectedItem().toString(),controlInput.getSelectedItem().toString(),switchTypeInput.getSelectedItem().toString(),displayInput.getSelectedItem().toString(),epsInput.getSelectedItem().toString(),delayStartInput.getSelectedItem().toString(),onWInput.getText(),asWInput.getText(),psWInput.getText(),offWInput.getText(),dsWInput.getText(),yearOfPurchaseInput.getText(),usageInput.getText(),usageUnitsInput.getSelectedItem().toString(),feature1Input.getText(),feature2Input.getText(),feature3Input.getText(),feature4Input.getText(),feature5Input.getText(),notesInput.getText());
					if (newAppliance.isValid()){
						try{
							Statement MySQL_Statement = dbConn.createStatement();
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+site.siteID+" AND source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updApplianceSQL = "INSERT INTO appliances (site_id,source_id,circuit_id,room_id,appliance_group,appliance_type,brand,model,serial_no,connection_type,control,switch_type,display,eps,delay_start,on_w,as_w,ps_w,off_w,ds_w,year_of_purchase,usage_amount,usage_units,feature1,feature2,feature3,feature4,feature5,notes) VALUES("+site.siteID+","+newSourceID+","+circuitIDList.get(circuitInput.getSelectedIndex())+","+roomIDList.get(roomInput.getSelectedIndex())+",'"+applianceGroupInput.getSelectedItem().toString()+"','"+applianceTypeInput.getSelectedItem().toString()+"',"+(brandInput.getText().equals("")?"NULL":"'"+brandInput.getText()+"'")+","+(modelInput.getText().equals("")?"NULL":"'"+modelInput.getText()+"'")+","+(serialInput.getText().equals("")?"NULL":"'"+serialInput.getText()+"'")+",'"+connectionInput.getSelectedItem().toString()+"','"+controlInput.getSelectedItem().toString()+"','"+switchTypeInput.getSelectedItem().toString()+"','"+displayInput.getSelectedItem().toString()+"','"+epsInput.getSelectedItem().toString()+"','"+delayStartInput.getSelectedItem().toString()+"',"+(onWInput.getText().equals("")?"NULL":onWInput.getText())+","+(asWInput.getText().equals("")?"NULL":asWInput.getText())+","+(psWInput.getText().equals("")?"NULL":psWInput.getText())+","+(offWInput.getText().equals("")?"NULL":offWInput.getText())+","+(dsWInput.getText().equals("")?"NULL":dsWInput.getText())+","+(yearOfPurchaseInput.getText().equals("")?"NULL":yearOfPurchaseInput.getText())+","+(usageInput.getText().equals("")?"NULL":usageInput.getText())+",'"+usageUnitsInput.getSelectedItem().toString()+"',"+(feature1Input.getText().equals("")?"NULL":"'"+feature1Input.getText()+"'")+","+(feature2Input.getText().equals("")?"NULL":"'"+feature2Input.getText()+"'")+","+(feature3Input.getText().equals("")?"NULL":"'"+feature3Input.getText()+"'")+","+(feature4Input.getText().equals("")?"NULL":"'"+feature4Input.getText()+"'")+","+(feature5Input.getText().equals("")?"NULL":"'"+feature5Input.getText()+"'")+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
									MySQL_Statement.executeUpdate(updApplianceSQL);
	
									resetApplianceEditForm();
	
									//restart, because user clicked "Add another"
									if (response==JOptionPane.NO_OPTION){
										addAppliance(site);
									}
								}catch(SQLException sE){
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing appliance information.","Error",JOptionPane.ERROR_MESSAGE);
									addAppliance(site);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this source.site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								addAppliance(site);
							}
						} catch (SQLException sE){
							sE.printStackTrace();
						} 
					}
					else{
						addAppliance(site);
					}
				}
				else{
					addAppliance(site);
				}
			}
			else{
				removeAppliance(site,newSourceID); //incomplete source
			}
			resetApplianceEditForm();
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void editAppliance(Source source){	
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
				ResultSet applianceData = MySQL_Statement.executeQuery("SELECT sources.source_name,sources.source_type,sources.measurement_type,rooms.room_number,rooms.room_type,appliances.* FROM appliances LEFT JOIN sources ON sources.source_id = appliances.source_id LEFT JOIN rooms ON appliances.room_id = rooms.room_id WHERE appliances.site_id ="+source.site.siteID+" AND appliances.source_id = "+source.sourceID); //retrieves data pertaining to selected site
				if (applianceData.next()){ //if retrieved a new id
					sourceNameInput.setText(applianceData.getString("source_name"));
					circuitInput.setSelectedIndex(circuitIDList.lastIndexOf(applianceData.getString("circuit_id")));
					roomInput.setSelectedItem(applianceData.getString("room_number")+": "+applianceData.getString("room_type"));
					sourceTypeInput.setSelectedItem(applianceData.getString("source_type"));
					measurementTypeInput.setSelectedItem(applianceData.getString("measurement_type"));
					applianceGroupInput.setSelectedItem(applianceData.getString("appliance_group"));
					applianceTypeInput.setSelectedItem(applianceData.getString("appliance_type"));
					brandInput.setText(applianceData.getString("brand"));
					modelInput.setText(applianceData.getString("model"));
					serialInput.setText(applianceData.getString("serial_no"));
					connectionInput.setSelectedItem(applianceData.getString("connection_type"));
					controlInput.setSelectedItem(applianceData.getString("control"));
					switchTypeInput.setSelectedItem(applianceData.getString("switch_type"));
					displayInput.setSelectedItem(applianceData.getString("display"));
					epsInput.setSelectedItem(applianceData.getString("eps"));
					delayStartInput.setSelectedItem(applianceData.getString("delay_start"));
					onWInput.setText(applianceData.getString("on_w"));
					asWInput.setText(applianceData.getString("as_w"));
					psWInput.setText(applianceData.getString("ps_w"));
					offWInput.setText(applianceData.getString("off_w"));
					dsWInput.setText(applianceData.getString("ds_w"));
					yearOfPurchaseInput.setText(applianceData.getString("year_of_purchase"));
					usageInput.setText(applianceData.getString("usage_amount"));
					usageUnitsInput.setSelectedItem(applianceData.getString("usage_units"));
					feature1Input.setText(applianceData.getString("feature1"));
					feature2Input.setText(applianceData.getString("feature2"));
					feature3Input.setText(applianceData.getString("feature3"));
					feature4Input.setText(applianceData.getString("feature4"));
					feature5Input.setText(applianceData.getString("feature5"));
					notesInput.setText(applianceData.getString("notes"));
				}
			}
						
			if (!siteIDInput.getText().equals("") && !sourceIDInput.getText().equals("") && circuitInput.getItemCount()>0 && roomInput.getItemCount()>0){
	
				int response = JOptionPane.showConfirmDialog(null,this,"Editing appliance (Source ID: "+source.sourceID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Source newSource = new Source(source.site,source.sourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
					if (newSource.isValid()){
						Appliance newAppliance = new Appliance(source.site,source.sourceID,sourceNameInput.getText(),circuitIDList.get(circuitInput.getSelectedIndex()),roomIDList.get(roomInput.getSelectedIndex()),applianceGroupInput.getSelectedItem().toString(),applianceTypeInput.getSelectedItem().toString(),brandInput.getText(),modelInput.getText(),serialInput.getText(),connectionInput.getSelectedItem().toString(),controlInput.getSelectedItem().toString(),switchTypeInput.getSelectedItem().toString(),displayInput.getSelectedItem().toString(),epsInput.getSelectedItem().toString(),delayStartInput.getSelectedItem().toString(),onWInput.getText(),asWInput.getText(),psWInput.getText(),offWInput.getText(),dsWInput.getText(),yearOfPurchaseInput.getText(),usageInput.getText(),usageUnitsInput.getSelectedItem().toString(),feature1Input.getText(),feature2Input.getText(),feature3Input.getText(),feature4Input.getText(),feature5Input.getText(),notesInput.getText());
						if (newAppliance.isValid()){
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updApplianceSQL = "UPDATE appliances SET circuit_id="+circuitIDList.get(circuitInput.getSelectedIndex())+",room_id="+roomIDList.get(roomInput.getSelectedIndex())+",appliance_group='"+applianceGroupInput.getSelectedItem().toString()+"',appliance_type='"+applianceTypeInput.getSelectedItem().toString()+"',brand="+(brandInput.getText().equals("")?"NULL":"'"+brandInput.getText()+"'")+",model="+(modelInput.getText().equals("")?"NULL":"'"+modelInput.getText()+"'")+",serial_no="+(serialInput.getText().equals("")?"NULL":"'"+serialInput.getText()+"'")+",connection_type='"+connectionInput.getSelectedItem().toString()+"',control='"+controlInput.getSelectedItem().toString()+"',switch_type='"+switchTypeInput.getSelectedItem().toString()+"',display='"+displayInput.getSelectedItem().toString()+"',eps='"+epsInput.getSelectedItem().toString()+"',delay_start='"+delayStartInput.getSelectedItem().toString()+"',on_w="+(onWInput.getText().equals("")?"NULL":onWInput.getText())+",as_w="+(asWInput.getText().equals("")?"NULL":asWInput.getText())+",ps_w="+(psWInput.getText().equals("")?"NULL":psWInput.getText())+",off_w="+(offWInput.getText().equals("")?"NULL":offWInput.getText())+",ds_w="+(dsWInput.getText().equals("")?"NULL":dsWInput.getText())+",year_of_purchase="+(yearOfPurchaseInput.getText().equals("")?"NULL":yearOfPurchaseInput.getText())+",usage_amount="+(usageInput.getText().equals("")?"NULL":usageInput.getText())+",usage_units='"+usageUnitsInput.getSelectedItem().toString()+"',feature1="+(feature1Input.getText().equals("")?"NULL":"'"+feature1Input.getText()+"'")+",feature2="+(feature2Input.getText().equals("")?"NULL":"'"+feature2Input.getText()+"'")+",feature3="+(feature3Input.getText().equals("")?"NULL":"'"+feature3Input.getText()+"'")+",feature4="+(feature4Input.getText().equals("")?"NULL":"'"+feature4Input.getText()+"'")+",feature5="+(feature5Input.getText().equals("")?"NULL":"'"+feature5Input.getText()+"'")+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
									MySQL_Statement.executeUpdate(updApplianceSQL);
								}catch(SQLException sE){
									JOptionPane.showMessageDialog(null,"Error occured when writing appliance information.","Error",JOptionPane.ERROR_MESSAGE);
									editAppliance(source);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								editAppliance(source);
							}
						}
						else{
							editAppliance(source);
						}
					}
					else{
						editAppliance(source);
					}
				}
				resetApplianceEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for appliance (Source ID: "+source.sourceID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch(SQLException sE){
			sE.printStackTrace();
		}
	}
	
	private void questionRemoveAppliance(Site site,String sourceID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+sourceID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeAppliance(site,sourceID);
		}
	}
	
	private void removeAppliance(Site site,String sourceID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+site.siteID+" AND source_id = "+sourceID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified source (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}

	}
	
	/*void fillGPOs(String siteID){
		circuitInput.removeAllItems();
		try{
			//fill gpos
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet gpoIDsSQL = MySQL_Statement.executeQuery("SELECT gpo_id,gpo_name FROM gpos WHERE site_id ="+siteID+" AND room_id = "+roomInput.getSelectedItem().toString().split(": ")[0]);//gets relevant gpos
			if (gpoIDsSQL.next()){
				gpoIDsSQL.beforeFirst();
				while (gpoIDsSQL.next()){ //if got some rooms
					circuitInput.addItem(gpoIDsSQL.getString("gpo_id")+": "+gpoIDsSQL.getString("gpo_name"));
				}
			}
			else{
				circuitInput.addItem("No GPOs");
				circuitInput.addItem("Add New GPO");
			}
		}catch(SQLException sE){
			circuitInput.addItem("DB Error");
		}
	}*/

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(applianceGroupInput)){
			applianceTypeInput.removeActionListener(this);
			applianceTypeInput.removeAllItems();
			for(int i=0;i<Appliance.getApplianceTypes(applianceGroupInput.getSelectedIndex()).length;i++){
				applianceTypeInput.addItem(Appliance.getApplianceTypes(applianceGroupInput.getSelectedIndex())[i]);
			}
			applianceTypeInput.addActionListener(this);
		}
		else if (aE.getSource().equals(applianceTypeInput)){
			//TODO grey out fields that are not required
			//applianceInput21.setEnabled(true);
			//applianceInput22.setEnabled(true);
			//if(applianceInput5.getSelectedItem().toString().equals("Refrigerator")){
			//	applianceInput21.setEnabled(false);
			//	applianceInput22.setEnabled(false);
			//}
		}
		else if (aE.getSource().equals(sourceTypeInput)){
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
		//else if (aE.getSource().equals(roomInput) && roomInput.getItemCount()>0 && !roomInput.getItemAt(0).equals("No Rooms")){
		//	fillGPOs(siteIDInput.getText().split(": ")[0]);
		//}
	}
	
	public JPanel getApplianceButtonPanel(SiteTable siteTable,ApplianceTable applianceTable){
		return new ApplianceButtonPanel(siteTable,applianceTable);
	}
	
	//Button Panel
	public class ApplianceButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton applianceAddB = new JButton("Add");
		JButton applianceEditB = new JButton("Edit");
		JButton applianceRemB = new JButton("Remove");

		SiteTable siteTable;
		ApplianceTable applianceTable;
		
		ApplianceButtonPanel(SiteTable siteTable,ApplianceTable applianceTable){
			this.siteTable = siteTable;
			this.applianceTable = applianceTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(applianceAddB);
			this.add(applianceEditB);
			this.add(applianceRemB);
			
			applianceAddB.addActionListener(this);
			applianceEditB.addActionListener(this);
			applianceRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			applianceAddB.setEnabled(enabled);
			applianceEditB.setEnabled(enabled);
			applianceRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				//String siteID = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				//String siteName = siteTable.getValueAt(siteTable.getSelectedRow(),1).toString();
				//Site site = siteTable.siteList.get(siteTable.getSelectedRow());
				Site site = siteTable.siteList.get(siteTable.getSelectedRow());
				
				if (aE.getSource().equals(applianceAddB)){
					addAppliance(site);
				}
				else if (aE.getSource().equals(applianceEditB)){
					if (applianceTable.applianceListModel.isSelectionEmpty()==false){
						Source source = applianceTable.applianceList.get(applianceTable.getSelectedRow());
						editAppliance(source);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(applianceRemB)){
					if (applianceTable.applianceListModel.isSelectionEmpty()==false){
						Source source = applianceTable.applianceList.get(applianceTable.getSelectedRow());
						questionRemoveAppliance(site,source.sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				applianceTable.update(dbConn, site);
			}
		}
	}
}
