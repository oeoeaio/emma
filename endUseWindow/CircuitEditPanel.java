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

public class CircuitEditPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Circuit Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel sourceIDLabel = new JLabel("Source ID:");
	JLabel sourceNameLabel = new JLabel("Circuit Name:");
	JLabel sourceTypeLabel = new JLabel("Source Type:");
	JLabel measurementTypeLabel = new JLabel("Measurement Type:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField sourceIDInput = new JTextField(20); //source id
	JTextField sourceNameInput = new JTextField(20); //circuit name
	JComboBox sourceTypeInput = new JComboBox(Source.getSourceTypeList()); //source type
	JComboBox measurementTypeInput = new JComboBox(Source.getMeasurementList()); //measurement type
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public CircuitEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new GridLayout(7,2));
		
		this.add(siteIDLabel);
		this.add(siteIDInput);
		this.add(sourceIDLabel);
		this.add(sourceIDInput);
		this.add(sourceNameLabel);
		this.add(sourceNameInput);
		this.add(sourceTypeLabel);
		this.add(sourceTypeInput);
		this.add(measurementTypeLabel);
		this.add(measurementTypeInput);
		this.add(notesLabel);
		this.add(notesInput);
		
		siteIDInput.setEnabled(false);
		sourceIDInput.setEnabled(false);

		sourceTypeInput.addActionListener(this);
		sourceTypeInput.setSelectedItem("Circuit");
		sourceTypeInput.setEnabled(false);
	}

	private void resetCircuitEditForm(){
		siteIDInput.setText("");
		sourceIDInput.setText("");
		sourceNameInput.setText("");
		sourceTypeInput.setSelectedItem("Circuit");
		measurementTypeInput.setSelectedIndex(0);
		notesInput.setText("");
	}
	
	private void addCircuit(Site site){
		//PREPARE NEW SOURCE
		String newSourceID = null;
		if (siteIDInput.getText().equals("")){
			//set sites
			siteIDInput.setText(site.siteID+": "+site.siteName);
			try{
				//get new source
				Statement MySQL_Statement = dbConn.createStatement();
				MySQL_Statement.executeUpdate("INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.siteID+",NULL,'Circuit','ActPower')");//creates new row in sources
				ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
	
				if (new_id_result.next()){ //if retrieved a new id
					newSourceID = new_id_result.getString(1);
				}
				sourceIDInput.setText(newSourceID);
			} catch (SQLException sE){
				sE.printStackTrace();
			} 
		}
		else{ //this happens when an error has occurred with the previous source add attempt
			newSourceID = sourceIDInput.getText();
		}

		//GET USER RESPONSE
		if (newSourceID != null){
			sourceNameInput.requestFocus();

			int response = JOptionPane.showOptionDialog(null,this,"Adding new circuit.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){ //"Done" was selected
				Source newSource = new Source(site,newSourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
				if (newSource.isValid()){
					Circuit newCircuit = new Circuit(site,null,sourceNameInput.getText(),notesInput.getText());
					if (newCircuit.isValid()){
						try{
							Statement MySQL_Statement = dbConn.createStatement();
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+site.siteID+" AND source_id = "+newSourceID; //updates specified information into the database
								System.out.println(updSourceSQL);
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updCircuitSQL = "INSERT INTO circuits (site_id,source_id,notes) VALUES("+site.siteID+","+newSourceID+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
									MySQL_Statement.executeUpdate(updCircuitSQL);
	
									resetCircuitEditForm();
	
									//restart, because user clicked "Add another"
									if(response==JOptionPane.NO_OPTION){
										addCircuit(site);
									}
								}catch(SQLException sE){
									if (sE.getErrorCode()==1062){
										JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing circuit for this site.","Error",JOptionPane.ERROR_MESSAGE);
									}
									else{
										sE.printStackTrace();
										JOptionPane.showMessageDialog(null,"Error occured when writing circuit information.","Error",JOptionPane.ERROR_MESSAGE);
									}
									addCircuit(site);
								}
							}catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									JOptionPane.showMessageDialog(null,"Error occured when writing circuit information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								addCircuit(site);
							}

						}catch(SQLException sE){
							sE.printStackTrace();
						}
					}
					else{
						addCircuit(site);
					}
				}
				else{
					addCircuit(site);
				}
			}
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void editCircuit(Source source){
		try{
			
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet circuitData = MySQL_Statement.executeQuery("SELECT sources.*,circuits.* FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id ="+source.site.siteID+" AND circuits.source_id = "+source.sourceID); //retrieves data pertaining to selected site

			if (circuitData.next()){ //if retrieved a record
				siteIDInput.setText(source.site.siteID+": "+source.site.siteName);
				sourceIDInput.setText(source.sourceID);
				sourceNameInput.setText(circuitData.getString("source_name"));
				sourceTypeInput.setSelectedItem(circuitData.getString("source_type"));
				measurementTypeInput.setSelectedItem(circuitData.getString("measurement_type"));
				notesInput.setText(circuitData.getString("notes"));

				int response = JOptionPane.showConfirmDialog(null,this,"Editing circuit (Source ID: "+source.sourceID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Source newSource = new Source(source.site,source.sourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
					if (newSource.isValid()){
						Circuit newCircuit = new Circuit(source.site,null,sourceNameInput.getText(),notesInput.getText());
						if (newCircuit.isValid()){
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //updates specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updCircuitSQL = "UPDATE circuits SET notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //updates specified information into the database
									MySQL_Statement.executeUpdate(updCircuitSQL);
								}catch(SQLException sE){
									if (sE.getErrorCode()==1062){
										JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing circuit for this site.","Error",JOptionPane.ERROR_MESSAGE);
									}
									else{
										JOptionPane.showMessageDialog(null,"Error occured when writing circuit information.","Error",JOptionPane.ERROR_MESSAGE);
									}
									editCircuit(source);
								}
							}catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									JOptionPane.showMessageDialog(null,"Error occured when writing circuit information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								editCircuit(source);
							}
	
						}
						else{
							editCircuit(source);
						}
					}
					else{
						editCircuit(source);
					}
				}
				resetCircuitEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for circuit (Source ID: "+source.sourceID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch (SQLException sE){
			sE.printStackTrace();
		} 
	}
	
	private void questionRemoveCircuit(Site site,String sourceID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+sourceID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeCircuit(site,sourceID);
		}
	}
	
	private void removeCircuit(Site site,String sourceID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+site.siteID+" AND source_id = "+sourceID); //removes data pertaining to selected circuit
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified circuit (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent aE) {
		// TODO Auto-generated method stub
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
	}
	
	
	public JPanel getCircuitButtonPanel(SiteTable siteTable,CircuitTable circuitTable){
		return new CircuitButtonPanel(siteTable,circuitTable);
	}
	
	//Button Panel
	private class CircuitButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton circuitAddB = new JButton("Add");
		JButton circuitEditB = new JButton("Edit");
		JButton circuitRemB = new JButton("Remove");

		SiteTable siteTable;
		CircuitTable circuitTable;
		
		CircuitButtonPanel(SiteTable siteTable,CircuitTable circuitTable){
			this.siteTable = siteTable;
			this.circuitTable = circuitTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(circuitAddB);
			this.add(circuitEditB);
			this.add(circuitRemB);
			
			circuitAddB.addActionListener(this);
			circuitEditB.addActionListener(this);
			circuitRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			circuitAddB.setEnabled(enabled);
			circuitEditB.setEnabled(enabled);
			circuitRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				//String siteID = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				//String siteName = siteTable.getValueAt(siteTable.getSelectedRow(),1).toString();
				Site site = siteTable.siteList.get(siteTable.getSelectedRow());
				
				
				if (aE.getSource().equals(circuitAddB)){
					addCircuit(site);
				}
				else if (aE.getSource().equals(circuitEditB)){
					if (circuitTable.circuitListModel.isSelectionEmpty()==false){
						Source source = circuitTable.circuitList.get(circuitTable.getSelectedRow());
						editCircuit(source);
					}
					else{
						JOptionPane.showMessageDialog(this,"No circuit selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(circuitRemB)){
					if (circuitTable.circuitListModel.isSelectionEmpty()==false){
						Source source = circuitTable.circuitList.get(circuitTable.getSelectedRow());
						questionRemoveCircuit(site,source.sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No circuit selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				circuitTable.update(dbConn, site);
			}
		}
	}

}
