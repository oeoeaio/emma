package outputs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import endUseWindow.LogWindow;


public class DISCRETETODExtractor extends JFrame implements Runnable,ItemListener,ActionListener {
	private static final long serialVersionUID = -3989983616744319157L;

	//Main Panel
	JPanel mainPanel = new JPanel();

	//Installation Panel
	JPanel instPanel = new JPanel(new FlowLayout());
	JLabel installation_id_selectL = new JLabel("Installation ID");
	JComboBox<Integer> installation_id_select = new JComboBox<Integer>();
	
	//Radio Button Panel
	JPanel radPanel = new JPanel(new FlowLayout());
	//Radio Label
	JLabel radioLabel = new JLabel("Channels");
	//Radio Buttons	
	JButton allChannels = new JButton("Select All");
	JButton noChannels = new JButton("Select None");
	
	//Checkbox Panel
	JPanel chPanel = new JPanel();
	JPanel[] chPanels = new JPanel[8];
	JCheckBox[] chPanelChs = new JCheckBox[48];
	
	//Analysis Period Panel
	JPanel analysisPeriodPanel = new JPanel(new FlowLayout());
	JLabel analysisPeriodL = new JLabel("Analysis Period:  ");
	JRadioButton monthlyRadio = new JRadioButton("Monthly");
	JRadioButton selectionRadio = new JRadioButton("Selection");
	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox<String> startDateS = new JComboBox<String>();
	JLabel endDateL = new JLabel("End Date");
	JComboBox<String> endDateS = new JComboBox<String>();
	JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Threshold Selector
	JPanel topPanel = new JPanel(new BorderLayout());
	JPanel countPanel = new JPanel(new FlowLayout());
	JLabel modeCountL = new JLabel("No. of Modes");
	JComboBox<Integer> modeCountS = new JComboBox<Integer>(new Integer[] {1,2,3,4,5});
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
	
	//Start Panel
	JPanel startPanel = new JPanel(new FlowLayout());
	JButton startButton = new JButton("Set Mode Thresholds...");
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	Connection dbConn;
	
	LogWindow logWindow;
	
	boolean allOK = true;
	
	ArrayList<long[]> dateRange = new ArrayList<long[]>();
	
	DISCRETETODExtractor(Connection passedDB,LogWindow passedLogWindow){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		dbConn = passedDB;
		logWindow = passedLogWindow;
		this.setSize(600,500);
		this.setLocation(300,200);
		this.setTitle("Extract CT Channels from Database");
		this.setVisible(true);
		
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
		chPanel.setLayout(new BoxLayout(chPanel,BoxLayout.PAGE_AXIS));
		
		mainPanel.add(instPanel);
		mainPanel.add(radPanel);
		mainPanel.add(chPanel);
		mainPanel.add(analysisPeriodPanel);
		mainPanel.add(datePanel);
		mainPanel.add(startPanel);
		
		instPanel.add(installation_id_selectL);
		instPanel.add(installation_id_select);
		radPanel.add(radioLabel);
		radPanel.add(allChannels);
		radPanel.add(noChannels);
		
		for (int i=0;i<8;i++){ //build Channel Panels
			chPanels[i] = new JPanel(new FlowLayout());
			chPanel.add(chPanels[i]);
			for (int j=0;j<6;j++){
				chPanelChs[(i*6)+j] = new JCheckBox((((i*6)+j)+1<10?"0":"")+Integer.toString(((i*6)+j)+1));
				chPanels[i].add(chPanelChs[(i*6)+j]);
				chPanelChs[(i*6)+j].addItemListener(this);
			}
		}
		
		analysisPeriodPanel.add(analysisPeriodL);
		analysisPeriodPanel.add(monthlyRadio);
		analysisPeriodPanel.add(selectionRadio);
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		ButtonGroup chButtonGroup = new ButtonGroup();
		chButtonGroup.add(allChannels);
		chButtonGroup.add(noChannels);
		allChannels.addActionListener(this);
		noChannels.addActionListener(this);
		
		ButtonGroup periodButtonGroup = new ButtonGroup();
		periodButtonGroup.add(monthlyRadio);
		periodButtonGroup.add(selectionRadio);
		monthlyRadio.addActionListener(this);
		selectionRadio.addActionListener(this);
		
		getContentPane().add(mainPanel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.validate();
	}
	
	public void run(){
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

		populateStartDate();
		populateEndDate();
		installation_id_select.setSelectedIndex(0);
		installation_id_select.addItemListener(this);
		startDateS.addItemListener(this);
		
		monthlyRadio.setSelected(true);
		startDateL.setEnabled(false);
		startDateS.setEnabled(false);
		endDateL.setEnabled(false);
		endDateS.setEnabled(false);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	void populateStartDate(){
		startDateS.removeAllItems();
		for (int i=0;i<dateRange.get(installation_id_select.getSelectedIndex()).length;i++){
			startDateS.addItem(dateFormatter.format(dateRange.get(installation_id_select.getSelectedIndex())[i]));
		}
		startDateS.setEnabled(true);
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.get(installation_id_select.getSelectedIndex()).length;i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(installation_id_select.getSelectedIndex())[i]));
		}
		endDateS.setEnabled(true);
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(installation_id_select) && iE.getStateChange()==ItemEvent.SELECTED){
			populateStartDate();
			populateEndDate();
		}
		else if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
		else if (iE.getSource().equals(modeCountS) && iE.getStateChange()==ItemEvent.SELECTED){
			showHideModeInputs();
		}
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
	
	double[][] getThresholds(boolean[] chStates){
		double[][] thresholdArray = new double[48][4];
		
		topPanel.add(countPanel,BorderLayout.NORTH);
		topPanel.add(thPanel,BorderLayout.SOUTH);
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
		
		String[] thArray = new String[4];
		for (int i=0;i<chStates.length;i++){ //cycle through channels to be extracted
			if (chStates[i]){
				//final JComponent[] panels = new JComponent[] {topPanel};
				
				boolean inputsOk = false;
				while (inputsOk == false){ //check all inputs are ok for each channel before moving on to the next
					int response = JOptionPane.showConfirmDialog(this,topPanel,"Threshold settings for channel: "+(i+1),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
					if (response == JOptionPane.OK_OPTION){
						//System.out.println(th1T.getText().toString()+" 1");
						thArray = new String[] {th1T.getText().toString(),th2T.getText().toString(),th3T.getText().toString(),th4T.getText().toString()};
						//System.out.println(th1T.getText().toString()+" 5 "+thArray[0]+" "+thArray[1]+" "+thArray[2]+" "+thArray[3]); 
						for (int j=0;j<Integer.parseInt(modeCountS.getSelectedItem().toString())-1;j++){
							//System.out.println(th1T.getText().toString()+" 2 "+(j+1)+" "+thArray[0]+" "+thArray[1]+" "+thArray[2]+" "+thArray[3]);
							if (thArray[j].matches("\\d{1,3}([.]\\d){0,1}")){
								if (j==0){ //first Threshold
									if (Double.parseDouble(thArray[j])>0){ //first Threshold must be greater than 0
										inputsOk = true;
									}
									else{
										inputsOk = false;
										JOptionPane.showMessageDialog(null, "Error:\r\nThreshold 1 must be greater than 0.\r\nPlease enter a number greater than 0.", "ERROR", JOptionPane.ERROR_MESSAGE);
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
						if (Integer.parseInt(modeCountS.getSelectedItem().toString())==1){ //case for when no thresholds are required
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
					for (int j=0;j<Integer.parseInt(modeCountS.getSelectedItem().toString())-1;j++){
						thresholdArray[i][j] = Double.parseDouble(thArray[j]);
					}
					th1T.setText("");
					th2T.setText("");
					th3T.setText("");
					th4T.setText("");
				}
				else{ //means cancel button was hit
					allOK = false;
					break;
				}
			}
		}
		
		return thresholdArray;
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(startButton)){
			
			boolean[] chStates = new boolean[chPanelChs.length];
			int totalChCount = 0;	
			
			Calendar startDate = new GregorianCalendar();
			startDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			startDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[startDateS.getSelectedIndex()]);
			Calendar endDate = new GregorianCalendar();
			endDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			endDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[startDateS.getSelectedIndex()+endDateS.getSelectedIndex()]+(1000*60*60*24));
			
			for (int i=0;i<chPanelChs.length;i++){ //count selected channels
				chStates[i] = chPanelChs[i].isSelected();
				totalChCount = totalChCount + (chPanelChs[i].isSelected()?1:0);
			}
			
			if (totalChCount > 0){
				String analysisPeriod = (monthlyRadio.isSelected()? "monthly" : (selectionRadio.isSelected()? "selection": null));
				if (analysisPeriod.equals("selection") || analysisPeriod.equals("monthly")){
					double[][] thresholdValues = getThresholds(chStates);
					if (analysisPeriod.equals("monthly")){
						startDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[0]);
						endDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[dateRange.get(installation_id_select.getSelectedIndex()).length-1]);
					}
					
					if (allOK){
						this.setVisible(false);
						Thread todExtractorProcess = new Thread(new DISCRETETODExtractorProcess(logWindow,dbConn,chStates,totalChCount,Integer.parseInt(installation_id_select.getSelectedItem().toString()),installation_id_select.getSelectedIndex(),startDate,endDate,analysisPeriod,thresholdValues));
						todExtractorProcess.start();
						this.dispose();
					}
				}
				else{
					JOptionPane.showMessageDialog(this, "Please select either 'Monthly' or 'Selection' for Analysis Type.", "No Analysis Type Selected", JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(this, "No channels selected for processing.\r\nPlease ensure at least one channel is selected.", "No Channels Selected", JOptionPane.WARNING_MESSAGE);
			}
		}
		else if (aE.getSource().equals(allChannels)){
			for (int i=0;i<chPanelChs.length;i++){
				chPanelChs[i].setSelected(true);
			}
		}
		else if (aE.getSource().equals(noChannels)){
			for (int i=0;i<chPanelChs.length;i++){
				chPanelChs[i].setSelected(false);
			}
		}
		else if (aE.getSource().equals(monthlyRadio)){
			startDateL.setEnabled(false);
			startDateS.setEnabled(false);
			endDateL.setEnabled(false);
			endDateS.setEnabled(false);
		}
		else if (aE.getSource().equals(selectionRadio)){
			startDateL.setEnabled(true);
			startDateS.setEnabled(true);
			endDateL.setEnabled(true);
			endDateS.setEnabled(true);
		}
	}
}
