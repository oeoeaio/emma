package outputs;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.LogWindow;
import endUseWindow.SiteTable;
import endUseWindow.Source;
import endUseWindow.SourceTable;



public class RefrigAnalysisPanel extends JPanel implements ActionListener,ListSelectionListener,ItemListener{
	private static final long serialVersionUID = 1L;
	
	//Main Panel
	JPanel mainPanel = new JPanel();
	
	//Source Select Panel
	JPanel siteSelectPanel = new JPanel(new GridLayout(2,1));

	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Select Site");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname"});
	JScrollPane siteScroll = new JScrollPane(siteTable);

	//Source Select Panel
	JPanel sourceSelectPanel = new JPanel(new GridLayout(1,2));

	//Refigerator Source Panel Components
	JPanel refrigSourcePanel = new JPanel(new BorderLayout());
	JPanel refrigSourceTitleP = new JPanel(new FlowLayout());
	JLabel refrigSourceLabel = new JLabel("Select Refrigerator Source");
	SourceTable refrigSourceTable = new SourceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name"});
	JScrollPane refrigSourceScroll = new JScrollPane(refrigSourceTable);
	
	//Temperature Source Panel Components
	JPanel tempSourcePanel = new JPanel(new BorderLayout());
	JPanel tempSourceTitleP = new JPanel(new FlowLayout());
	JLabel tempSourceLabel = new JLabel("Select Temperature Source");
	SourceTable tempSourceTable = new SourceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name"});
	JScrollPane tempSourceScroll = new JScrollPane(tempSourceTable);
	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox<String> startDateS = new JComboBox<String>();
	JLabel endDateL = new JLabel("End Date");
	JComboBox<String> endDateS = new JComboBox<String>();
	JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Threshold Panel
	JPanel thresholdPanel = new JPanel();
	JLabel threshLabel1 = new JLabel("Theshold 1");
	JTextField threshInput1 = new JTextField(3);
	JLabel threshLabel2 = new JLabel("Theshold 2");
	JTextField threshInput2 = new JTextField(3);
	JLabel basePowerL = new JLabel("Base Power Offset");
	JComboBox<Integer> basePowerS = new JComboBox<Integer>(new Integer[] {1,2,3,4,5,6,7,8,9,10});

	
	//Power Correction
	JPanel pwrCorrPanel = new JPanel();
	JLabel pwrCorrL = new JLabel("Power Correction: ");
	JTextField pwrCorrT = new JTextField(3);
	JLabel pwrCorrL2 = new JLabel("(value to subtract)");
	
	//Blackout Panel
	JPanel blackoutPanel = new JPanel();
	JLabel blackoutOffCountLabel = new JLabel("Blackout (OFF mins/period): ");
	JTextField blackoutOffCount = new JTextField(3);
	
	//Process Button
	JPanel analysePanel = new JPanel();
	JButton analyseButton = new JButton("Analyse...");
	
	//Variables
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String inDateFormat = new String();
	int inFirstRow = -1; //the first row that valid data appears in input file
	String tempDateFormat = new String();
	int tempFirstRow = -1; //the first row that valid data appears in temp file
	Connection dbConn;
	ArrayList<Long> dateRange = new ArrayList<Long>();

	public RefrigAnalysisPanel(Connection dbConn){
		//version 0.0.10
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);
		siteTable.siteListModel.addListSelectionListener(this);
		
		//refrig sources
		refrigSourcePanel.add(refrigSourceTitleP,BorderLayout.NORTH);
		refrigSourceTitleP.add(refrigSourceLabel);
		refrigSourcePanel.add(refrigSourceScroll,BorderLayout.CENTER);
		refrigSourceTable.sourceListModel.addListSelectionListener(this);
		
		//temp sources
		tempSourcePanel.add(tempSourceTitleP,BorderLayout.NORTH);
		tempSourceTitleP.add(tempSourceLabel);
		tempSourcePanel.add(tempSourceScroll,BorderLayout.CENTER);
		tempSourceTable.sourceListModel.addListSelectionListener(this);
		
		siteSelectPanel.add(sitePanel);
		siteSelectPanel.add(sourceSelectPanel);
		sourceSelectPanel.add(refrigSourcePanel);
		sourceSelectPanel.add(tempSourcePanel);
		
		//Date Panel
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startDateS.addItemListener(this);
		
		//Output File
		//outFilePanel.add(outFileField);
		//outFilePanel.add(outFileButton);
		//outFileField.setEditable(false);
		//outFileField.setEnabled(false);
		//outFileButton.addActionListener(this);
		
		thresholdPanel.setLayout(new FlowLayout());
		thresholdPanel.add(threshLabel1);
		thresholdPanel.add(threshInput1);
		thresholdPanel.add(threshLabel2);
		thresholdPanel.add(threshInput2);
		thresholdPanel.add(basePowerL);
		thresholdPanel.add(basePowerS);
		threshInput1.setText("50");
		threshInput2.setText("150");
		basePowerS.setSelectedItem(5);
		
		pwrCorrPanel.add(pwrCorrL);
		pwrCorrPanel.add(pwrCorrT);
		pwrCorrPanel.add(pwrCorrL2);
		pwrCorrT.setText("0.0");
		
		blackoutPanel.add(blackoutOffCountLabel);
		blackoutPanel.add(blackoutOffCount);
		blackoutOffCount.setText("45");

		
		analysePanel.setLayout(new FlowLayout());
		analysePanel.add(analyseButton);
		analyseButton.addActionListener(this);
		analyseButton.setEnabled(false);
		
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
		mainPanel.add(siteSelectPanel);
		mainPanel.add(datePanel);
		//mainPanel.add(outFilePanel);
		mainPanel.add(thresholdPanel);
		mainPanel.add(pwrCorrPanel);
		mainPanel.add(blackoutPanel);
		mainPanel.add(analysePanel);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		
		siteTable.updateList(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);
	}


	public void actionPerformed(ActionEvent aE) {
		/*if (aE.getSource()==outFileButton){
			outFileChooser.setCurrentDirectory(new File("./"));
			outFileChooser.setSelectedFile(new File("./output.csv"));
			int returnVal = outFileChooser.showSaveDialog(outFileChooser);
			if (returnVal==JFileChooser.APPROVE_OPTION){
				outFile = outFileChooser.getSelectedFile();
				if (outFile.exists()){
					if (JOptionPane.showConfirmDialog(null, "The File "+outFile.getName().toString()+" already exists.\nDo you wish to overwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION)==JOptionPane.YES_OPTION){
						outFileField.setText(outFile.getPath());
						if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false && refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0).toString()!=null){
							outFileField.setText(outFile.getPath());
							analyseButton.setEnabled(true);
						}
						else{
							outFileField.setText("");
							outFile = null;
							analyseButton.setEnabled(true);
						}
					}
					else{
						outFileField.setText("");
						outFile = null;
						analyseButton.setEnabled(true);
					}
				}
				else{
					if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false && refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0).toString()!=null){
						outFileField.setText(outFile.getPath());
						analyseButton.setEnabled(true);
					}
					else{
						outFileField.setText("");
						outFile = null;
						analyseButton.setEnabled(true);
					}
				}
			}
		}
		else */if (aE.getSource()==analyseButton){
			if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false && refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0) != null){
				Source refrigSource = refrigSourceTable.sourceList.get(refrigSourceTable.getSelectedRow());
				long startDate = dateRange.get(startDateS.getSelectedIndex());
				long endDate = dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex());
				if (threshInput1.getText().matches("\\d{1,5}") && threshInput2.getText().matches("\\d{1,5}")){
					if (Double.parseDouble(threshInput1.getText())<=Double.parseDouble(threshInput2.getText()) || (Double.parseDouble(threshInput1.getText())>0 && Double.parseDouble(threshInput2.getText())==0)){
						if (pwrCorrT.getText().matches("\\d{1,2}[.]\\d{1,2}")){
							long[] lowFreqFileDates = null;
							try{
								String findLowFreqFiles = "SELECT UNIX_TIMESTAMP(start_date) AS start_date,UNIX_TIMESTAMP(end_date) AS end_date FROM files WHERE site_id = "+refrigSource.getSite().getSiteID()+" AND source_id = "+refrigSource.getSourceID()+" AND end_date >= '"+sqlDateFormatter.format(startDate)+"' AND start_date <= '"+sqlDateFormatter.format(endDate)+"' AND frequency >= 600";
								ResultSet lowFrewFilesRS = dbConn.createStatement().executeQuery(findLowFreqFiles);
								if (lowFrewFilesRS.next()){
									lowFreqFileDates = new long[] {lowFrewFilesRS.getLong("start_date"), lowFrewFilesRS.getLong("end_date")};
								}

								lowFrewFilesRS.close();
							} catch(SQLException sE){

							}
							boolean processOk = true;
							if (lowFreqFileDates!=null){ // If found some files with low frequency
								int response = JOptionPane.showConfirmDialog(this, "Refrigerator data with low sample frequency exists between "+dateFormatter.format(lowFreqFileDates[0]*1000)+" and "+dateFormatter.format(lowFreqFileDates[1]*1000)+"\r\nIt is suggested that these data are too infrequent to be useful.\r\nWould you like to continue anyway?","Warning: Infrequent data detected",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
								processOk = (response==JOptionPane.YES_OPTION);
							}
							if (processOk){
								Source tempSource = null;
								if (tempSourceTable.sourceListModel.isSelectionEmpty()==false){
									tempSource = tempSourceTable.sourceList.get(tempSourceTable.getSelectedRow());
								}
								Thread analysisThread = new Thread(new RefrigAnalysis(dbConn,new LogWindow("Refrigerator Analysis Log"),refrigSourceTable.sourceList.get(refrigSourceTable.getSelectedRow()),tempSource,dateRange.get(startDateS.getSelectedIndex()),dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex()),Double.parseDouble(threshInput1.getText()),Double.parseDouble(threshInput2.getText()),Integer.parseInt(basePowerS.getSelectedItem().toString()),Double.parseDouble(pwrCorrT.getText()),Double.parseDouble(blackoutOffCount.getText())));
								analysisThread.start();
							}

						}
						else{
							JOptionPane.showMessageDialog(null, "Error Code 006\r\nPlease enter a positive decimal for power correction eg. '0.4'.\r\nYou may specify up to two (2) decimal places.\r\nIf you do not wish to use a power correction, please use '0.0'.", "Error", JOptionPane.ERROR_MESSAGE);
						}
					}	
					else{
						JOptionPane.showMessageDialog(null, "Error Code 005\r\nTheshold 2 must be greater than Threshold 1 unless Theshold 2 is not to be used,\r\nin which case it should be set to the same value as Theshold 1 or to a value of 0 (zero).", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null, "Error Code 004\r\nPlease enter a positive integer value for both thresholds. (eg. 50 and 150)\r\nTheshold 2 may also be zero if it is not to be used.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			else{
	        	JOptionPane.showMessageDialog(null, "Error Code 002\r\nPlease select a file to Analyse.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	int getYearFormat(Set<String> dateSet){
		int yearFormat = 0;
		int counter = 0;
		int totalCount = 0;
		Iterator<String> it = dateSet.iterator();
		while (it.hasNext()){
			totalCount+=it.next().length();
			counter++;
		}
		yearFormat = totalCount/counter;
		return yearFormat;
	}
	
	/*void updateSites() throws SQLException{
		siteTable.siteListModel.removeListSelectionListener(this);
		siteTable.siteTableModel.setNumRows(0);
		Statement MySQL_Statement = dbConn.createStatement();
		String getSitesSQL = "SELECT * FROM sites ORDER BY site_id";
		ResultSet siteResults = MySQL_Statement.executeQuery(getSitesSQL);
		
		while (siteResults.next()){
			siteTable.siteTableModel.addRow(new String[]{siteResults.getString("site_id"),siteResults.getString("given_name"),siteResults.getString("surname"),siteResults.getString("suburb"),siteResults.getString("state")});
		}
		siteTable.siteListModel.addListSelectionListener(this);
	}*/
	
	/*void updateSources() throws SQLException{
		//Refrig sources
		refrigSourceTable.sourceListModel.removeListSelectionListener(this);
		refrigSourceTable.sourceTableModel.setNumRows(0);
		if (siteTable.siteListModel.isSelectionEmpty()==false && siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()!=null){
			Statement MySQL_Statement = dbConn.createStatement();
			String getSourcesSQL = "SELECT * FROM sources WHERE site_id = '"+siteTable.getValueAt(siteTable.getSelectedRow(),0)+"' AND source_type = 'RF' ORDER BY source_id";
			ResultSet sourceResults = MySQL_Statement.executeQuery(getSourcesSQL);
			
			while (sourceResults.next()){
				refrigSourceTable.sourceTableModel.addRow(new String[] {sourceResults.getString("source_id"),sourceResults.getString("brand"),sourceResults.getString("model"),sourceResults.getString("serial"),sourceResults.getString("location")});
			}
		}
		refrigSourceTable.sourceListModel.addListSelectionListener(this);
		
		//Temperature sources
		tempSourceTable.sourceListModel.removeListSelectionListener(this);
		tempSourceTable.sourceTableModel.setNumRows(0);
		if (siteTable.siteListModel.isSelectionEmpty()==false && siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()!=null){
			Statement MySQL_Statement = dbConn.createStatement();
			String getSourcesSQL = "SELECT * FROM sources WHERE site_id = '"+siteTable.getValueAt(siteTable.getSelectedRow(),0)+"' AND source_type = 'TP' ORDER BY source_id";
			ResultSet sourceResults = MySQL_Statement.executeQuery(getSourcesSQL);
			
			while (sourceResults.next()){
				tempSourceTable.sourceTableModel.addRow(new String[] {sourceResults.getString("source_id"),sourceResults.getString("location")});
			}
		}
		tempSourceTable.sourceListModel.addListSelectionListener(this);
	}*/
	
	void populateStartDate() throws SQLException,Exception{
		//Refrig sources
		startDateS.removeActionListener(this);
		startDateS.removeAllItems();
		dateRange.clear();
		if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false && refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0).toString()!=null){
			Statement MySQL_Statement = dbConn.createStatement();
			Date before = new Date();
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE(MIN(date_time))) AS min_date,UNIX_TIMESTAMP(DATE_ADD(DATE(MAX(date_time)), INTERVAL 1 DAY)) AS max_date FROM data_sa WHERE site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND source_id = "+refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			System.out.println(new Date().getTime()-before.getTime());
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				Calendar maxCal = Calendar.getInstance();
				maxCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				maxCal.setTimeInMillis(minMaxResults.getLong("max_date")*1000);
			
				while( minCal.getTimeInMillis() <= maxCal.getTimeInMillis() ){
					dateRange.add(minCal.getTimeInMillis());
					startDateS.addItem(dateFormatter.format(minCal.getTimeInMillis()));
					minCal.add(Calendar.DATE, 1);
				}
				startDateS.setEnabled(true);
				populateEndDate();
			}
			else{
				throw new Exception("ERR:NoData");
			}
			System.out.println(new Date().getTime()-before.getTime());
		}
		else{ //This should never be bale to happen
			throw new Exception("NoSource");
		}
		if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false && refrigSourceTable.getValueAt(refrigSourceTable.getSelectedRow(),0).toString()!=null){
			analyseButton.setEnabled(true);
		}
		startDateS.addActionListener(this);
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.size();i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(i)));
		}
		endDateS.setEnabled(true);
	}

	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
			refrigSourceTable.update(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()),"appliances","Refrigerator");
			tempSourceTable.update(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()),"temperatures");
			if (refrigSourceTable.sourceListModel.isSelectionEmpty()){
				startDateS.removeAllItems();
				endDateS.removeAllItems();
				analyseButton.setEnabled(false);
			}
		}
		else if (lSE.getSource().equals(refrigSourceTable.sourceListModel) && lSE.getValueIsAdjusting()==false){
			
			if (refrigSourceTable.sourceListModel.isSelectionEmpty()==false){
				try {
					populateStartDate();
				} catch (SQLException e) {
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					analyseButton.setEnabled(false);
					startDateS.addItem("ERR:NoData");
					endDateS.addItem("ERR:NoData");
					e.printStackTrace();
				}catch (Exception e){
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					analyseButton.setEnabled(false);
					startDateS.addItem(e.getMessage());
					endDateS.addItem(e.getMessage());
				}
			}
			else{
				startDateS.removeAllItems();
				endDateS.removeAllItems();
				analyseButton.setEnabled(false);
			}
		}
		
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
	}	
}
