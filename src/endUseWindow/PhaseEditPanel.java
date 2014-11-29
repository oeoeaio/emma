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

public class PhaseEditPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Appliance Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel sourceIDLabel = new JLabel("Source ID:");
	JLabel sourceNameLabel = new JLabel("Source Name:");
	JLabel sourceTypeLabel = new JLabel("Source Type:");
	JLabel measurementTypeLabel = new JLabel("MeasurementType:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField sourceIDInput = new JTextField(20); //source id
	JTextField sourceNameInput = new JTextField(20); //source name
	JComboBox<String> sourceTypeInput = new JComboBox<String>(Source.getSourceTypeList()); //source type
	JComboBox<String> measurementTypeInput = new JComboBox<String>(Source.getMeasurementList()); //measurement type
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public PhaseEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new GridLayout(16,2));
		
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
		
		sourceTypeInput.setSelectedItem("Phase");
		sourceTypeInput.setEnabled(false);
	}

	private void resetPhaseEditForm(){
		siteIDInput.setText("");
		sourceIDInput.setText("");
		sourceNameInput.setText("");
		sourceTypeInput.setSelectedItem("Phase");
		measurementTypeInput.setSelectedIndex(0);
		notesInput.setText("");
	}
	
	private void addPhase(Site site){

		//PREPARE NEW SOURCE
		String newSourceID = null;
		if (siteIDInput.getText().equals("") && sourceIDInput.getText().equals("")){
			//set sites
			siteIDInput.setText(site.siteID+": "+site.siteName);
			
			try{
				//get new source
				Statement MySQL_Statement = dbConn.createStatement();
				MySQL_Statement.executeUpdate("INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.siteID+",NULL,'Phase','Volts')");//creates new row in sources
				ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
	
				if (new_id_result.next()){ //if retrieved a new id
					newSourceID = new_id_result.getString(1);
				}
				sourceIDInput.setText(newSourceID);
	
			} catch (SQLException sE){
				sE.printStackTrace();
				JOptionPane.showMessageDialog(null,"Error occured when creating new source.","Error",JOptionPane.ERROR_MESSAGE);
			} 
		}
		else{ //this happens when an error has occurred with the previous source add attempt
			newSourceID = sourceIDInput.getText();
		}

		//GET USER RESPONSE
		if (newSourceID != null){
			sourceNameInput.requestFocus();

			int response = JOptionPane.showOptionDialog(null,this,"Adding new phase.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){
				Source newSource = new Source(site,newSourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
				if (newSource.isValid()){
					Phase newPhase = new Phase(site,newSourceID,sourceNameInput.getText(),notesInput.getText());
					if (newPhase.isValid()){
						try{
							Statement MySQL_Statement = dbConn.createStatement();
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+site.siteID+" AND source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updPhaseSQL = "INSERT INTO phases (site_id,source_id,notes) VALUES("+site.siteID+","+newSourceID+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
									MySQL_Statement.executeUpdate(updPhaseSQL);
	
									resetPhaseEditForm();
	
									//restart, because user clicked "Add another"
									if (response==JOptionPane.NO_OPTION){
										addPhase(site);
									}
								}catch(SQLException sE){
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing phase information.","Error",JOptionPane.ERROR_MESSAGE);
									addPhase(site);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								addPhase(site);
							}
						} catch (SQLException sE){
							sE.printStackTrace();
						} 
					}
					else{
						addPhase(site);
					}
				}
				else{
					addPhase(site);
				}
			}
			else{
				removePhase(site,newSourceID); //incomplete source
			}
			resetPhaseEditForm();
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void editPhase(Source source){	
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			siteIDInput.setText(source.site.siteID+": "+source.site.siteName);
			sourceIDInput.setText(source.sourceID);
			ResultSet phaseData = MySQL_Statement.executeQuery("SELECT sources.source_name,sources.source_type,sources.measurement_type,phases.* FROM phases LEFT JOIN sources ON sources.source_id = phases.source_id WHERE phases.site_id ="+source.site.siteID+" AND phases.source_id = "+source.sourceID); //retrieves data pertaining to selected site
						
			if (phaseData.next()){ //if found relevant record
				sourceNameInput.setText(phaseData.getString("source_name"));
				sourceTypeInput.setSelectedItem(phaseData.getString("source_type"));
				measurementTypeInput.setSelectedItem(phaseData.getString("measurement_type"));
				notesInput.setText(phaseData.getString("notes"));
	
				int response = JOptionPane.showConfirmDialog(null,this,"Editing phase (Source ID: "+source.sourceID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Source newSource = new Source(source.site,source.sourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
					if (newSource.isValid()){
						Phase newPhase = new Phase(source.site,source.sourceID,sourceNameInput.getText(),notesInput.getText());
						if (newPhase.isValid()){
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updPhaseSQL = "UPDATE phases SET notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
									MySQL_Statement.executeUpdate(updPhaseSQL);
								}catch(SQLException sE){
									JOptionPane.showMessageDialog(null,"Error occured when writing phase information.","Error",JOptionPane.ERROR_MESSAGE);
									editPhase(source);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								editPhase(source);
							}
						}
						else{
							editPhase(source);
						}
					}
					else{
						editPhase(source);
					}
				}
				resetPhaseEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for phase (Source ID: "+source.sourceID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch(SQLException sE){
			sE.printStackTrace();
		}
	}
	
	private void questionRemovePhase(Site site,String sourceID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+sourceID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removePhase(site,sourceID);
		}
	}
	
	private void removePhase(Site site,String sourceID){
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
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Phase")){
				measurementTypeInput.addItem("Volts");
			}else if(sourceTypeInput.getSelectedItem().toString().equals("Temperature")){
				measurementTypeInput.addItem("Temp");
				measurementTypeInput.addItem("AvgTemp");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Water")){
				measurementTypeInput.addItem("Pulse");
			}
		}
	}
	
	public JPanel getPhaseButtonPanel(SiteTable siteTable,PhaseTable phaseTable){
		return new PhaseButtonPanel(siteTable,phaseTable);
	}
	
	//Button Panel
	public class PhaseButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton phaseAddB = new JButton("Add");
		JButton phaseEditB = new JButton("Edit");
		JButton phaseRemB = new JButton("Remove");

		SiteTable siteTable;
		PhaseTable phaseTable;
		
		PhaseButtonPanel(SiteTable siteTable,PhaseTable phaseTable){
			this.siteTable = siteTable;
			this.phaseTable = phaseTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(phaseAddB);
			this.add(phaseEditB);
			this.add(phaseRemB);
			
			phaseAddB.addActionListener(this);
			phaseEditB.addActionListener(this);
			phaseRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			phaseAddB.setEnabled(enabled);
			phaseEditB.setEnabled(enabled);
			phaseRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				Site site = siteTable.siteList.get(siteTable.getSelectedRow());
				
				if (aE.getSource().equals(phaseAddB)){
					addPhase(site);
				}
				else if (aE.getSource().equals(phaseEditB)){
					if (phaseTable.phaseListModel.isSelectionEmpty()==false){
						Source source = phaseTable.phaseList.get(phaseTable.getSelectedRow());
						editPhase(source);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(phaseRemB)){
					if (phaseTable.phaseListModel.isSelectionEmpty()==false){
						Source source = phaseTable.phaseList.get(phaseTable.getSelectedRow());
						questionRemovePhase(site,source.sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				phaseTable.update(dbConn, site);
			}
		}
	}
}
