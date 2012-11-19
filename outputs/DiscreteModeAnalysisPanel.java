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
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.BoxLayout;
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

import endUseWindow.LogWindow;
import endUseWindow.SiteTable;
import endUseWindow.SourceTable;


public class DiscreteModeAnalysisPanel extends JPanel implements ItemListener,ActionListener,ListSelectionListener {
	private static final long serialVersionUID = -3989983616744319157L;

	//Main Panel
	JPanel mainPanel = new JPanel();

	//Source Select Panel
	JPanel sourceSelectPanel = new JPanel(new GridLayout(1,2));

	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Select Site");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname"});
	JScrollPane siteScroll = new JScrollPane(siteTable);

	//Source Select Panel
	//Refigerator Source List Panel Components
	JPanel sourcePanel = new JPanel(new BorderLayout());
	JPanel sourceTitleP = new JPanel(new FlowLayout());
	JLabel sourceLabel = new JLabel("Select Source");
	SourceTable sourceTable = new SourceTable(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION,new String[] {"Source ID","Source Name","Source Type","Measurement Type"});
	JScrollPane sourceScroll = new JScrollPane(sourceTable);
	
	//Button Panel
	JPanel buttonPanel = new JPanel(new FlowLayout());
	//Buttons	
	JButton allSources = new JButton("Select All Sources");
	JButton noSources = new JButton("Select No Sources");
	
	//SamplePeriodPanel
	JPanel samplePeriodPanel = new JPanel(new FlowLayout());
	JLabel samplePeriodL = new JLabel("Sample Period");
	JComboBox samplePeriodS = new JComboBox();
	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox startDateS = new JComboBox();
	JLabel endDateL = new JLabel("End Date");
	JComboBox endDateS = new JComboBox();
	JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Start Panel
	JPanel startPanel = new JPanel(new FlowLayout());
	JButton startButton = new JButton("Set Mode Thresholds...");
	JLabel startL = new JLabel("(extraction will begin once all mode thresholds have been set)");
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	ThresholdPanel thresholdPanel = new ThresholdPanel();
	
	boolean allOK = true;
	
	Connection dbConn;
	
	ArrayList<Long> dateRange = new ArrayList<Long>();
	
	public DiscreteModeAnalysisPanel(Connection dbConn){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){	
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
		
		mainPanel.add(sourceSelectPanel);
		mainPanel.add(buttonPanel);
		mainPanel.add(samplePeriodPanel);
		mainPanel.add(datePanel);
		mainPanel.add(startPanel);
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);

		// sources
		sourcePanel.add(sourceTitleP,BorderLayout.NORTH);
		sourceTitleP.add(sourceLabel);
		sourcePanel.add(sourceScroll,BorderLayout.CENTER);
		
		sourceSelectPanel.add(sitePanel);
		sourceSelectPanel.add(sourcePanel);
		
		//buttons
		buttonPanel.add(allSources);
		buttonPanel.add(noSources);
		allSources.addActionListener(this);
		noSources.addActionListener(this);
		
		samplePeriodPanel.add(samplePeriodL);
		samplePeriodPanel.add(samplePeriodS);
		
		samplePeriodS.addItem("10");
		samplePeriodS.addItem("60");
		samplePeriodS.addItem("1440");
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		startDateS.addItemListener(this);
		
		siteTable.siteListModel.addListSelectionListener(this);
		sourceTable.sourceListModel.addListSelectionListener(this);
		
		siteTable.updateList(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);

		this.validate();
	}
	
	/*public void run(){
		try {
			if (dbConn!=null){
				Statement getInstallationsStatement = dbConn.createStatement();
				String getInstallationsSQL = "SELECT DISTINCT installation_id FROM data_all ORDER BY installation_id";
				ResultSet getInstallationsRS = getInstallationsStatement.executeQuery(getInstallationsSQL);
				while (getInstallationsRS.next()){
					installation_id_select.addItem(getInstallationsRS.getInt("installation_id"));
					
					Statement getMaxStatement = dbConn.createStatement();
					String getMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(date_time)),INTERVAL 1 DAY)) AS minDate,UNIX_TIMESTAMP(DATE(MAX(date_time))) AS maxDate FROM data_all WHERE installation_id = "+getInstallationsRS.getInt("installation_id");
					ResultSet getMaxRS = getMaxStatement.executeQuery(getMaxSQL);
					if (getMaxRS.next()){
						Calendar minCal = Calendar.getInstance();
						minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						minCal.setTimeInMillis(getMaxRS.getLong("minDate")*1000);
						Calendar maxCal = Calendar.getInstance();
						maxCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						maxCal.setTimeInMillis(getMaxRS.getLong("maxDate")*1000);
						
						int rowsRequired = (int)((maxCal.getTimeInMillis()-minCal.getTimeInMillis())/(60000*60*24) + 1);
	
						long[] dateRange_TEMP = new long[rowsRequired];
						for (int i=0;minCal.getTimeInMillis() <= maxCal.getTimeInMillis();i++){
							dateRange_TEMP[i] = minCal.getTimeInMillis();
							minCal.add(Calendar.DATE, 1);
						}
						dateRange.add(dateRange_TEMP);
					}
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		installation_id_select.setSelectedIndex(0);
		installation_id_select.addItemListener(this);
		startDateS.addItemListener(this);

		
	}*/
	
	void populateStartDate() throws SQLException,Exception{
		startDateS.removeActionListener(this);
		startDateS.removeAllItems();
		dateRange.clear();
		if (sourceTable.sourceListModel.isSelectionEmpty()==false && sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString()!=null){
			
			String sourceList = "(";
			for (int i=0;i<sourceTable.getSelectedRows().length;i++){
				sourceList = sourceList + sourceTable.getValueAt(sourceTable.getSelectedRows()[i],0) + (i<sourceTable.getSelectedRows().length-1?",":"");
			}
			sourceList = sourceList + ")";
			
			Statement MySQL_Statement = dbConn.createStatement();
			//Date before = new Date();
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(start_date)),INTERVAL 1 DAY)) AS min_date,UNIX_TIMESTAMP(DATE(MAX(end_date))) AS max_date FROM files WHERE site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND source_id IN "+sourceList;
			System.out.println(getMinMaxSQL);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			//System.out.println(new Date().getTime()-before.getTime());
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				long maxCal = minMaxResults.getLong("max_date")*1000;
			
				for (int i=0;minCal.getTimeInMillis() <= maxCal;i++){
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
			//System.out.println(new Date().getTime()-before.getTime());
		}
		else{ //This should never be able to happen
			throw new Exception("NoSource");
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
	
	double[][] getThresholds(String[] sourceNames){
		double[][] thresholdArray = new double[sourceNames.length][4];
		String[] thArray = new String[4];
		for (int i=0;i<sourceNames.length;i++){ //cycle through channels to be extracted
			Arrays.fill(thresholdArray[i],-1.0);
			boolean inputsOk = false;
			while (inputsOk == false){ //check all inputs are ok for each channel before moving on to the next
				int response = JOptionPane.showConfirmDialog(this,thresholdPanel,"Threshold settings for source: "+sourceNames[i],JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					//System.out.println(th1T.getText().toString()+" 1");
					thArray = new String[] {thresholdPanel.th1T.getText().toString(),thresholdPanel.th2T.getText().toString(),thresholdPanel.th3T.getText().toString(),thresholdPanel.th4T.getText().toString()};
					//System.out.println(th1T.getText().toString()+" 5 "+thArray[0]+" "+thArray[1]+" "+thArray[2]+" "+thArray[3]); 
					for (int j=0;j<Integer.parseInt(thresholdPanel.modeCountS.getSelectedItem().toString())-1;j++){
						//System.out.println(th1T.getText().toString()+" 2 "+(j+1)+" "+thArray[0]+" "+thArray[1]+" "+thArray[2]+" "+thArray[3]);
						if (thArray[j].matches("\\d{1,3}([.]\\d){0,1}")){
							if (j==0){ //first Threshold
								if (Double.parseDouble(thArray[j])>=0){ //first Threshold must be greater than or equal to 0
									inputsOk = true;
								}
								else{
									inputsOk = false;
									JOptionPane.showMessageDialog(null, "Error:\r\nThreshold 1 must be greater than or equal to 0.", "ERROR", JOptionPane.ERROR_MESSAGE);
									break;
								}
							}
							else{ //all thresholds besides the first
								if (Double.parseDouble(thArray[j])>Double.parseDouble(thArray[j-1])){ //must be greater than the previous threshold
									inputsOk = true;
								}
								else{
									inputsOk = false;
									JOptionPane.showMessageDialog(null, "Error:\r\nThreshold "+(j+1)+" must be greater than Threshold "+(j)+".\r\nPlease correct this error nefore proceeding.", "ERROR", JOptionPane.ERROR_MESSAGE);
									break;
								}
							}
						}
						else{
							//System.out.println(thArray[j]+" "+modeCountS.getSelectedItem().toString()+" "+th1T.getText().toString());
							inputsOk = false;
							JOptionPane.showMessageDialog(null, "Error:\r\nAll Thresholds must be a positive number of up to one (1) decimal place.\r\nThreshold "+(j+1)+" does not currently meet this requirement.\r\nIf you do not wish to use this threshold, please select a lower value for 'No. of Modes'.", "ERROR", JOptionPane.ERROR_MESSAGE);
							break;
						}
					}
					if (Integer.parseInt(thresholdPanel.modeCountS.getSelectedItem().toString())==1){ //case for when no thresholds are required
						inputsOk = true;
					}
				}
				else{
					int cancel_yn = JOptionPane.showConfirmDialog(null, "Warning:\r\nAre you sure you want to exit the Threshold setting process?\r\nDoing so will cancel the extraction process and allow you to reselect channels.", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
					if (cancel_yn == JOptionPane.YES_OPTION){
						inputsOk = false;
						break; //break this loop - will lead to cancellation of threshold setting process
					}
					else{
						inputsOk = false; //return for another go at setting thresholds for this channel
					}
				}
			}

			if (inputsOk){
				allOK = true;
				for (int j=0;j<Integer.parseInt(thresholdPanel.modeCountS.getSelectedItem().toString())-1;j++){
					thresholdArray[i][j] = Double.parseDouble(thArray[j]);
				}
				thresholdPanel.th1T.setText("");
				thresholdPanel.th2T.setText("");
				thresholdPanel.th3T.setText("");
				thresholdPanel.th4T.setText("");
			}
			else{ //means cancel button was hit
				allOK = false;
				break;
			}
		}
		
		return thresholdArray;
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(startButton)){
			
			int[] selectedSources = new int[sourceTable.getSelectedRows().length];
			String[] sourceNames = new String[sourceTable.getSelectedRows().length];
			
			for (int i=0;i<selectedSources.length;i++){
				selectedSources[i] = Integer.parseInt(sourceTable.getValueAt(sourceTable.getSelectedRows()[i], 0).toString());
				sourceNames[i] = sourceTable.getValueAt(sourceTable.getSelectedRows()[i], 1).toString();
			}
			
			long startDate = dateRange.get(startDateS.getSelectedIndex());
			long endDate = dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex());
			int samplePeriod = Integer.parseInt(samplePeriodS.getSelectedItem().toString());
			
			if (selectedSources.length > 0){
				double[][] thresholdValues = getThresholds(sourceNames);
				
				if (allOK){
					Thread avgExtractorProcess = new Thread(new DiscreteModeAnalysis(new LogWindow("Discrete Mode Average Analysis Log"),dbConn,selectedSources,sourceNames,Integer.parseInt(siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()),siteTable.getValueAt(siteTable.getSelectedRow(),1).toString(),startDate,endDate,samplePeriod,thresholdValues));
					avgExtractorProcess.start();
				}
			}
			else{
				JOptionPane.showMessageDialog(this, "No channels selected for processing.\r\nPlease ensure at least one channel is selected.", "No Channels Selected", JOptionPane.WARNING_MESSAGE);
			}
		}
		else if (aE.getSource().equals(allSources)){
			sourceTable.selectAll();
		}
		else if (aE.getSource().equals(noSources)){
			sourceTable.clearSelection();
		}
	}
	
	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
			sourceTable.updateList(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()));
			
			if (sourceTable.sourceListModel.isSelectionEmpty()){
				startButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
		else if (lSE.getSource().equals(sourceTable.sourceListModel) && lSE.getValueIsAdjusting()==false){
			
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				try {
					populateStartDate();
					if (startDateS.getItemCount()>0 && endDateS.getItemCount()>0){
						startButton.setEnabled(true);
					}
				} catch (SQLException e) {
					startButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem("ERR:NoData");
					endDateS.addItem("ERR:NoData");
					e.printStackTrace();
				}catch (Exception e){
					startButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem(e.getMessage());
					endDateS.addItem(e.getMessage());
				}
			}
			else{
				startButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
	}
	
	class ThresholdPanel extends JPanel implements ItemListener{
		private static final long serialVersionUID = -533969869442150716L;
		
		//Threshold Selector
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel countPanel = new JPanel(new FlowLayout());
		JLabel modeCountL = new JLabel("No. of Modes");
		JComboBox modeCountS = new JComboBox(new Integer[] {1,2,3,4,5});
		JPanel thPanel = new JPanel(new FlowLayout());
		JLabel m1L = new JLabel("|Mode 1|");
		JLabel th1L = new JLabel(" <= ");
		JTextField th1T = new JTextField(4);
		JLabel m2L = new JLabel(" < |Mode 2|");
		JLabel th2L = new JLabel(" <= ");
		JTextField th2T = new JTextField(4);
		JLabel m3L = new JLabel(" < |Mode 3|");
		JLabel th3L = new JLabel(" <= ");
		JTextField th3T = new JTextField(4);
		JLabel m4L = new JLabel(" < |Mode 4|");
		JLabel th4L = new JLabel(" <= ");
		JTextField th4T = new JTextField(4);
		JLabel m5L = new JLabel(" < |Mode 5|");
		
		ThresholdPanel(){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		void buildGUI(){
			this.add(countPanel,BorderLayout.NORTH);
			this.add(thPanel,BorderLayout.SOUTH);
			countPanel.add(modeCountL);
			countPanel.add(modeCountS);
			
			thPanel.add(m1L);
			thPanel.add(th1L);
			thPanel.add(th1T);
			thPanel.add(m2L);
			thPanel.add(th2L);
			thPanel.add(th2T);
			thPanel.add(m3L);
			thPanel.add(th3L);
			thPanel.add(th3T);
			thPanel.add(m4L);
			thPanel.add(th4L);
			thPanel.add(th4T);
			thPanel.add(m5L);
			modeCountS.addItemListener(this);
			modeCountS.setSelectedItem(3);
		}
		
		void showHideModeInputs(){
			int modeCount = Integer.parseInt(modeCountS.getSelectedItem().toString());

			m1L.setEnabled(true);
			th1L.setEnabled(false);
			th1T.setEnabled(false);
			m2L.setEnabled(false);
			th2L.setEnabled(false);
			th2T.setEnabled(false);
			m3L.setEnabled(false);
			th3L.setEnabled(false);
			th3T.setEnabled(false);
			m4L.setEnabled(false);
			th4L.setEnabled(false);
			th4T.setEnabled(false);
			m5L.setEnabled(false);
			if (modeCount>=2){
				th1L.setEnabled(true);
				th1T.setEnabled(true);
				m2L.setEnabled(true);
			}
			if (modeCount>=3){
				th2L.setEnabled(true);
				th2T.setEnabled(true);
				m3L.setEnabled(true);
			}
			if (modeCount>=4){
				th3L.setEnabled(true);
				th3T.setEnabled(true);
				m4L.setEnabled(true);
			}
			if (modeCount>=5){
				th4L.setEnabled(true);
				th4T.setEnabled(true);
				m5L.setEnabled(true);
			}		
		}

		@Override
		public void itemStateChanged(ItemEvent iE) {
			if (iE.getSource().equals(thresholdPanel.modeCountS) && iE.getStateChange()==ItemEvent.SELECTED){
				thresholdPanel.showHideModeInputs();
			}
		}
	}
}
