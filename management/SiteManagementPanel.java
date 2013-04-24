package management;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.Site;
import endUseWindow.SiteTable;
import endUseWindow.Source;
import endUseWindow.SourceTable;

public class SiteManagementPanel extends JPanel implements ActionListener,ListSelectionListener{
	private static final long serialVersionUID = 5421907525894521938L;

	//Main Panel
	//GridLayout mainPanelLayout = new GridLayout(2,1);
	//JPanel mainPanel = new JPanel(mainPanelLayout);
	
	//Management Panel
	JPanel managementPanel = new JPanel(new GridLayout(2,1,0,50));
	
	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Sites");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname","Suburb","State"});
	JScrollPane siteScroll = new JScrollPane(siteTable);
	JPanel siteButtonPanel = new JPanel(new FlowLayout());
	JButton siteAddB = new JButton("Add");
	JButton siteEditB = new JButton("Edit");
	JButton siteRemB = new JButton("Remove");
	
	//Source Select Panel
	//Refigerator Source List Panel Components
	JPanel sourcePanel = new JPanel(new BorderLayout());
	JPanel sourceTitleP = new JPanel(new FlowLayout());
	JLabel sourceLabel = new JLabel("Sources");
	SourceTable sourceTable = new SourceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Source Type","Measurement Type"});
	JScrollPane sourceScroll = new JScrollPane(sourceTable);
	JPanel sourceButtonPanel = new JPanel(new FlowLayout());
	JButton sourceAddB = new JButton("Add");
	JButton sourceEditB = new JButton("Edit");
	JButton sourceRemB = new JButton("Remove");
	
	//Source Edit Panel
	JPanel sourceEditPanel = new JPanel(new GridLayout(6,2));
	JLabel sourceLabel1 = new JLabel("Site ID:");
	JLabel sourceLabel2 = new JLabel("Source ID:");
	JLabel sourceLabel3 = new JLabel("Source Name:");
	JLabel sourceLabel4 = new JLabel("Source Type:");
	JLabel sourceLabel5 = new JLabel("Measurement Type:");
	JLabel sourceLabel6 = new JLabel("Location:");
	JTextField sourceInput1 = new JTextField(20);
	JTextField sourceInput2 = new JTextField(20);
	JTextField sourceInput3 = new JTextField(20);
	JComboBox<String> sourceInput4 = new JComboBox<String>(Source.getSourceTypeList());
	JComboBox<String> sourceInput5 = new JComboBox<String>(Source.getMeasurementList());
	JTextField sourceInput6 = new JTextField(20);
	
	//site Edit Panel
	JPanel siteEditPanel = new JPanel(new GridLayout(10,2));
	JLabel siteIDL = new JLabel("Site ID:");
	JLabel siteNameL = new JLabel("Site Name:");
	JLabel concentratorL = new JLabel("Concentrator No.:");
	JLabel startDateL = new JLabel("Start Date (yyyy-mm-dd):");
	JLabel endDateL = new JLabel("End Date (yyyy-mm-dd):");
	JLabel givenNameL = new JLabel("Given Name:");
	JLabel surnameL = new JLabel("Surname:");
	JLabel suburbL = new JLabel("Town/Suburb:");
	JLabel stateL = new JLabel("State:");
	JTextField siteID = new JTextField(20);
	JTextField siteName = new JTextField(20);
	JTextField concentrator = new JTextField(20);
	JTextField startDate = new JTextField(20);
	JTextField endDate = new JTextField(20);
	JTextField givenName = new JTextField(20);
	JTextField surname = new JTextField(20);
	JTextField suburb = new JTextField(20);
	JComboBox<String> state = new JComboBox<String>(new String[] {"","NSW","VIC","QLD","ACT","WA","SA","TAS","NT"});
	
	Connection dbConn;
	
	public SiteManagementPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		managementPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);
		sitePanel.add(siteButtonPanel,BorderLayout.SOUTH);
		siteButtonPanel.add(siteAddB);
		siteButtonPanel.add(siteEditB);
		siteButtonPanel.add(siteRemB);
		
		//sources
		sourcePanel.add(sourceTitleP,BorderLayout.NORTH);
		sourceTitleP.add(sourceLabel);
		sourcePanel.add(sourceScroll,BorderLayout.CENTER);
		//sourcePanel.add(sourceButtonPanel,BorderLayout.SOUTH);
		//sourceButtonPanel.add(sourceAddB);
		//sourceButtonPanel.add(sourceEditB);
		//sourceButtonPanel.add(sourceRemB);
		
		sourceEditPanel.add(sourceLabel1);
		sourceEditPanel.add(sourceInput1);
		sourceEditPanel.add(sourceLabel2);
		sourceEditPanel.add(sourceInput2);
		sourceEditPanel.add(sourceLabel3);
		sourceEditPanel.add(sourceInput3);
		sourceEditPanel.add(sourceLabel4);
		sourceEditPanel.add(sourceInput4);
		sourceEditPanel.add(sourceLabel5);
		sourceEditPanel.add(sourceInput5);
		sourceEditPanel.add(sourceLabel6);
		sourceEditPanel.add(sourceInput6);
		sourceInput1.setEnabled(false);
		sourceInput2.setEnabled(false);
		
		siteEditPanel.add(siteIDL);
		siteEditPanel.add(siteID);
		siteEditPanel.add(siteNameL);
		siteEditPanel.add(siteName);
		siteEditPanel.add(concentratorL);
		siteEditPanel.add(concentrator);
		siteEditPanel.add(startDateL);
		siteEditPanel.add(startDate);
		siteEditPanel.add(endDateL);
		siteEditPanel.add(endDate);
		siteEditPanel.add(givenNameL);
		siteEditPanel.add(givenName);
		siteEditPanel.add(surnameL);
		siteEditPanel.add(surname);
		siteEditPanel.add(suburbL);
		siteEditPanel.add(suburb);
		siteEditPanel.add(stateL);
		siteEditPanel.add(state);
		siteID.setEnabled(false);
		
		siteTable.siteListModel.addListSelectionListener(this);
		sourceTable.sourceListModel.addListSelectionListener(this);
		siteAddB.addActionListener(this);
		siteEditB.addActionListener(this);
		siteRemB.addActionListener(this);
		//sourceAddB.addActionListener(this);
		//sourceEditB.addActionListener(this);
		//sourceRemB.addActionListener(this);
		
		managementPanel.add(sitePanel);
		managementPanel.add(sourcePanel);
		
		siteTable.updateList(dbConn);
		this.add(managementPanel,BorderLayout.CENTER);
				
		//updateSites();
		//updateSources();
	}
	
	/*void updateSites(){
		try{
			final Statement MySQL_Statement = dbConn.createStatement();
			final String getSitesSQL = "SELECT * FROM sites ORDER BY site_id";
			final ResultSet siteResults = MySQL_Statement.executeQuery(getSitesSQL);
	
			siteTable.siteListModel.removeListSelectionListener(this);
			siteTable.siteTableModel.setNumRows(0);
		
			while (siteResults.next()){
				siteTable.siteTableModel.addRow(new String[]{siteResults.getString("site_id"),siteResults.getString("site_name"),siteResults.getString("given_name"),siteResults.getString("surname"),siteResults.getString("suburb"),siteResults.getString("state")});
			}
			siteTable.siteListModel.addListSelectionListener(this);
		} catch(SQLException sE){
			this.removeAll();
			this.add(new JLabel("An error occured while connecting to the database."));
			sE.printStackTrace();
		}

	}
	
	void updateSources(){
		if (siteTable.siteListModel.isSelectionEmpty()==false && siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()!=null){
			try{
				final Statement MySQL_Statement = dbConn.createStatement();
				final String getSourcesSQL = "SELECT * FROM sources WHERE site_id = '"+siteTable.getValueAt(siteTable.getSelectedRow(),0)+"' ORDER BY source_id";
				final ResultSet sourceResults = MySQL_Statement.executeQuery(getSourcesSQL);
	
				sourceTable.sourceListModel.removeListSelectionListener(this);
				sourceTable.sourceTableModel.setNumRows(0);
				
				while (sourceResults.next()){
					sourceTable.sourceTableModel.addRow(new String[] {sourceResults.getString("source_id"),sourceResults.getString("source_name"),sourceResults.getString("source_type"),sourceResults.getString("brand"),sourceResults.getString("model"),sourceResults.getString("serial"),sourceResults.getString("location")});
				}
				sourceTable.sourceListModel.addListSelectionListener(this);

			} catch(SQLException sE){
				this.removeAll();
				this.add(new JLabel("An error occured while connecting to the database."));
				sE.printStackTrace();
			}
		}
	}*/

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(siteAddB)){
			try{
				String newSiteID = null;
				if (siteID.getText().equals("")){
					Statement MySQL_Statement = dbConn.createStatement();
					MySQL_Statement.executeUpdate("INSERT INTO sites () VALUES()");//creates new row in sites
					ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
					
					if (new_id_result.next()){ //if retrieved a new id
						newSiteID = new_id_result.getString(1);
					}
				}
				else{
					newSiteID = siteID.getText();
				}
				
				if (newSiteID != null){
					Statement MySQL_Statement = dbConn.createStatement();
					siteID.setText(newSiteID);
					
					int response = JOptionPane.showOptionDialog(this,siteEditPanel,"Adding new site (id: "+newSiteID+")",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},"Add Another");
					if (response == JOptionPane.YES_OPTION){
						Site newSite = new Site(newSiteID,siteName.getText(),concentrator.getText(),startDate.getText(),endDate.getText(),givenName.getText(),surname.getText(),suburb.getText(),state.getSelectedItem().toString());
						if (newSite.isValid()){
							String newSiteSQL = "UPDATE sites SET site_name="+(siteName.getText().equals("")?"NULL":"'"+siteName.getText()+"'")+",concentrator="+(concentrator.getText().equals("")?"NULL":"'"+concentrator.getText()+"'")+",start_date="+(startDate.getText().equals("")?"NULL":"'"+startDate.getText()+"'")+",end_date="+(endDate.getText().equals("")?"NULL":"'"+endDate.getText()+"'")+",given_name="+(givenName.getText().equals("")?"NULL":"'"+givenName.getText()+"'")+",surname="+(surname.getText().equals("")?"NULL":"'"+surname.getText()+"'")+",suburb="+(suburb.getText().equals("")?"NULL":"'"+suburb.getText()+"'")+",state='"+state.getSelectedItem()+"' WHERE site_id = "+newSiteID; //adds specified information into the database
							MySQL_Statement.executeUpdate(newSiteSQL);
						}
						else{
							//Fire new event
							siteAddB.doClick();
						}
					}
					else if(response == JOptionPane.NO_OPTION){
						Site newSite = new Site(newSiteID,siteName.getText(),concentrator.getText(),startDate.getText(),endDate.getText(),givenName.getText(),surname.getText(),suburb.getText(),state.getSelectedItem().toString());
						if (newSite.isValid()){
							String newSiteSQL = "UPDATE sites SET site_name="+(siteName.getText().equals("")?"NULL":"'"+siteName.getText()+"'")+",concentrator="+(concentrator.getText().equals("")?"NULL":"'"+concentrator.getText()+"'")+",start_date="+(startDate.getText().equals("")?"NULL":"'"+startDate.getText()+"'")+",end_date="+(endDate.getText().equals("")?"NULL":"'"+endDate.getText()+"'")+",given_name="+(givenName.getText().equals("")?"NULL":"'"+givenName.getText()+"'")+",surname="+(surname.getText().equals("")?"NULL":"'"+surname.getText()+"'")+",suburb="+(suburb.getText().equals("")?"NULL":"'"+suburb.getText()+"'")+",state='"+state.getSelectedItem()+"' WHERE site_id = "+newSiteID; //adds specified information into the database
							MySQL_Statement.executeUpdate(newSiteSQL);
						
							siteTable.updateList(dbConn);
							//updateSites()
							//updateSources();
							siteID.setText("");
							siteName.setText("");
							concentrator.setText("");
							startDate.setText("");
							endDate.setText("");
							givenName.setText("");
							surname.setText("");
							suburb.setText("");
							state.setSelectedItem("");
							
							//Fire new event
							siteAddB.doClick();
						}
						else{
							//Fire new event
							siteAddB.doClick();
						}
					}
					else{
						MySQL_Statement.executeUpdate("DELETE FROM sites WHERE site_id = "+newSiteID); //retrieves data pertaining to selected site
					}
					siteTable.updateList(dbConn);
					//updateSites();
					//updateSources();
					siteID.setText("");
					siteName.setText("");
					concentrator.setText("");
					startDate.setText("");
					endDate.setText("");
					givenName.setText("");
					surname.setText("");
					suburb.setText("");
					state.setSelectedItem("");
				}
				else{
					JOptionPane.showMessageDialog(this,"An error occured when creating new site ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
				}
			} catch (SQLException sE){
				sE.printStackTrace();
			} 
		}
		else if (aE.getSource().equals(siteEditB)){
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				try{
					String selectedSite = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
					Statement MySQL_Statement = dbConn.createStatement();
					ResultSet siteData = MySQL_Statement.executeQuery("SELECT * FROM sites WHERE site_id = "+selectedSite); //retrieves data pertaining to selected site
					
					if (siteData.next()){
						siteID.setText(selectedSite);
						siteName.setText(siteData.getString("site_name"));
						concentrator.setText(siteData.getString("concentrator"));
						startDate.setText(siteData.getString("start_date"));
						endDate.setText(siteData.getString("end_date"));
						givenName.setText(siteData.getString("given_name"));
						surname.setText(siteData.getString("surname"));
						suburb.setText(siteData.getString("suburb"));
						state.setSelectedItem(siteData.getString("state"));
						int response = JOptionPane.showConfirmDialog(this,siteEditPanel,"Editing site: ",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
						if (response == JOptionPane.OK_OPTION){
							String editSiteSQL = "UPDATE sites SET site_name="+(siteName.getText().equals("")?"NULL":"'"+siteName.getText()+"'")+",concentrator="+(concentrator.getText().equals("")?"NULL":"'"+concentrator.getText()+"'")+",start_date="+(startDate.getText().equals("")?"NULL":"'"+startDate.getText()+"'")+",end_date="+(endDate.getText().equals("")?"NULL":"'"+endDate.getText()+"'")+",given_name="+(givenName.getText().equals("")?"NULL":"'"+givenName.getText()+"'")+",surname="+(surname.getText().equals("")?"NULL":"'"+surname.getText()+"'")+",suburb="+(suburb.getText().equals("")?"NULL":"'"+suburb.getText()+"'")+",state='"+state.getSelectedItem()+"' WHERE site_id = "+selectedSite; //adds specified information into the database						
							MySQL_Statement.executeUpdate(editSiteSQL);
							siteTable.updateList(dbConn);
							//updateSites();
							//updateSources();
						}
						siteID.setText("");
						siteName.setText("");
						concentrator.setText("");
						startDate.setText("");
						endDate.setText("");
						givenName.setText("");
						surname.setText("");
						suburb.setText("");
						state.setSelectedItem("");
					}
					else{
						JOptionPane.showMessageDialog(this,"An error occured when retrieving data for the specified site ("+selectedSite+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
					}
				} catch (SQLException sE){
					sE.printStackTrace();
				}
			}
		}
		else if (aE.getSource().equals(siteRemB)){
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				String selectedSite = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected site ("+selectedSite+") and all associated sources,files and data?","Removing Site...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (response == JOptionPane.YES_OPTION){
					try{
						Statement MySQL_Statement = dbConn.createStatement();
						MySQL_Statement.executeUpdate("DELETE FROM sites WHERE site_id = "+selectedSite); //retrieves data pertaining to selected site
						siteTable.updateList(dbConn);
						//updateSites();
						//updateSources();
					} catch (SQLException sE){
						JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified site ("+selectedSite+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
						sE.printStackTrace();
					}
				}
			}
		}
		else if (aE.getSource().equals(sourceAddB)){
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				try{
					String newSourceID = null;
					if (sourceInput1.getText().equals("") && sourceInput2.getText().equals("")){
						Statement MySQL_Statement = dbConn.createStatement();
						MySQL_Statement.executeUpdate("INSERT INTO sources (site_id) VALUES("+siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()+")");//creates new row in sources
						ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
						
						if (new_id_result.next()){ //if retrieved a new id
							newSourceID = new_id_result.getString(1);
						}
					}
					else{ //this happens when an error has occurred with the previous source add attempt
						newSourceID = sourceInput2.getText();
					}
					
					if (newSourceID != null){
						Statement MySQL_Statement = dbConn.createStatement();
						Site site = siteTable.siteList.get(siteTable.getSelectedRow());
						sourceInput1.setText(site.getSiteID());
						sourceInput2.setText(newSourceID);
						sourceInput3.requestFocus();
						
						int response = JOptionPane.showOptionDialog(this,sourceEditPanel,"Adding new source (id: "+newSourceID+")",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
						if (response == JOptionPane.YES_OPTION){
							Source newSource = new Source(site,newSourceID,sourceInput3.getText(),sourceInput4.getSelectedItem().toString(),sourceInput5.getSelectedItem().toString());
							if (newSource.isValid()){
								String newSourceSQL = "UPDATE sources SET site_id="+(sourceInput1.getText().equals("")?"NULL":"'"+sourceInput1.getText()+"'")+",source_name="+(sourceInput3.getText().equals("")?"NULL":"'"+sourceInput3.getText()+"'")+",source_type="+(sourceInput4.getSelectedIndex()+1)+",measurement_type="+(sourceInput5.getSelectedIndex()+1)+",location="+(sourceInput6.getText().equals("")?"NULL":"'"+sourceInput6.getText()+"'")+" WHERE source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(newSourceSQL);
							}
							else{
								sourceAddB.doClick();
							}
						}
						else if (response==JOptionPane.NO_OPTION){
							Source newSource = new Source(site,newSourceID,sourceInput3.getText(),sourceInput4.getSelectedItem().toString(),sourceInput5.getSelectedItem().toString());
							if (newSource.isValid()){
								String newSourceSQL = "UPDATE sources SET site_id="+(sourceInput1.getText().equals("")?"NULL":"'"+sourceInput1.getText()+"'")+",source_name="+(sourceInput3.getText().equals("")?"NULL":"'"+sourceInput3.getText()+"'")+",source_type="+(sourceInput4.getSelectedIndex()+1)+",measurement_type="+(sourceInput5.getSelectedIndex()+1)+",location="+(sourceInput6.getText().equals("")?"NULL":"'"+sourceInput6.getText()+"'")+" WHERE source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(newSourceSQL);
								
								sourceTable.updateList(dbConn, site);
								sourceTable.scrollRectToVisible(sourceTable.getCellRect(sourceTable.getRowCount(),0,true));
								sourceInput1.setText("");
								sourceInput2.setText("");
								sourceInput3.setText("");
								sourceInput4.setSelectedIndex(0);
								sourceInput5.setSelectedIndex(0);
								sourceInput6.setText("");
								
								sourceAddB.doClick();
							}
							else{
								sourceAddB.doClick();
							}
						}
						else{
							MySQL_Statement.executeUpdate("DELETE FROM sources WHERE source_id = "+newSourceID); //removes data pertaining to selected site
						}
						sourceTable.updateList(dbConn, site);
						sourceInput1.setText("");
						sourceInput2.setText("");
						sourceInput3.setText("");
						sourceInput4.setSelectedIndex(0);
						sourceInput5.setSelectedIndex(0);
						sourceInput6.setText("");
					}
					else{
						JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
					}
				} catch (SQLException sE){
					sE.printStackTrace();
				} 
			}
		}
		else if (aE.getSource().equals(sourceEditB)){
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				try{				
					String selectedSource = sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString();
					Statement MySQL_Statement = dbConn.createStatement();
					ResultSet sourceData = MySQL_Statement.executeQuery("SELECT site_id,source_id,source_name,source_type+0,measurement_type+0,location FROM sources WHERE source_id = "+selectedSource); //retrieves data pertaining to selected site
					
					if (sourceData.next()){ //if retrieved a new id
						sourceInput1.setText(siteTable.getValueAt(siteTable.getSelectedRow(),0).toString());
						sourceInput2.setText(selectedSource);
						sourceInput3.setText(sourceData.getString(3));
						sourceInput4.setSelectedIndex(sourceData.getInt(4)-1);
						sourceInput5.setSelectedIndex(sourceData.getInt(5)-1);
						sourceInput6.setText(sourceData.getString(6));
						
						int response = JOptionPane.showConfirmDialog(this,sourceEditPanel,"Editing source (id: "+selectedSource+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
						if (response == JOptionPane.OK_OPTION){
							String updSourceSQL = "UPDATE sources SET site_id="+(sourceInput1.getText().equals("")?"NULL":"'"+sourceInput1.getText()+"'")+",source_name="+(sourceInput3.getText().equals("")?"NULL":"'"+sourceInput3.getText()+"'")+",source_type="+(sourceInput4.getSelectedIndex()+1)+",measurement_type="+(sourceInput5.getSelectedIndex()+1)+",location="+(sourceInput6.getText().equals("")?"NULL":"'"+sourceInput6.getText()+"'")+" WHERE source_id = "+selectedSource; //adds specified information into the database
							MySQL_Statement.executeUpdate(updSourceSQL);
							sourceTable.updateList(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
						}
						sourceInput1.setText("");
						sourceInput2.setText("");
						sourceInput3.setText("");
						sourceInput4.setSelectedIndex(0);
						sourceInput5.setSelectedIndex(0);
						sourceInput6.setText("");
					}
					else{
						JOptionPane.showMessageDialog(this,"An error occured when writing data source (id: "+selectedSource+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
					}
				} catch (SQLException sE){
					sE.printStackTrace();
				} 
			}
		}
		else if (aE.getSource().equals(sourceRemB)){
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				String selectedSource = sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString();
				int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+selectedSource+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (response == JOptionPane.YES_OPTION){
					try{
						Statement MySQL_Statement = dbConn.createStatement();
						MySQL_Statement.executeUpdate("DELETE FROM sources WHERE source_id = "+selectedSource); //removes data pertaining to selected source
						sourceTable.updateList(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
					} catch (SQLException sE){
						JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified site ("+selectedSource+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
						sE.printStackTrace();
					}
				}
			}
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false && siteTable.getSelectedRow()!=-1){
			sourceTable.updateList(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
		}
	}
}
	

