package outputs;

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
import javax.swing.WindowConstants;

import endUseWindow.LogWindow;


public class WLExtractor extends JFrame implements Runnable,ItemListener,ActionListener {
	private static final long serialVersionUID = -3989983616744319157L;

	//Main Panel
	JPanel mainPanel = new JPanel();

	//Installation Panel
	JPanel instPanel = new JPanel(new FlowLayout());
	JLabel installation_id_selectL = new JLabel("Installation ID");
	JComboBox installation_id_select = new JComboBox();
	
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
	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox startDateS = new JComboBox();
	JLabel endDateL = new JLabel("End Date");
	JComboBox endDateS = new JComboBox();
	JLabel exclusiveL = new JLabel("(exclusive)");
	
	
	//Start Panel
	JPanel startPanel = new JPanel(new FlowLayout());
	JButton startButton = new JButton("Begin Extraction");
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	Connection dbConn;
	
	LogWindow logWindow;
	
	ArrayList<long[]> dateRange = new ArrayList<long[]>();
	
	WLExtractor(Connection passedDB,LogWindow passedLogWindow){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		dbConn = passedDB;
		logWindow = passedLogWindow;
		this.setSize(600,300);
		this.setLocation(300,200);
		this.setTitle("Extract CT Channels from Database");
		this.setVisible(true);
		
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
		chPanel.setLayout(new BoxLayout(chPanel,BoxLayout.PAGE_AXIS));
		
		mainPanel.add(instPanel);
		mainPanel.add(radPanel);
		mainPanel.add(chPanel);	
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
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(exclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(allChannels);
		buttonGroup.add(noChannels);
		allChannels.addActionListener(this);
		noChannels.addActionListener(this);

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
			endDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[startDateS.getSelectedIndex()+endDateS.getSelectedIndex()]);
			
			for (int i=0;i<chPanelChs.length;i++){ //count selected channels
				chStates[i] = chPanelChs[i].isSelected();
				totalChCount = totalChCount + (chPanelChs[i].isSelected()?1:0);
			}
			
			if (totalChCount > 0){
				this.setVisible(false);
				Thread wlExtractorProcess = new Thread(new WLExtractorProcess(logWindow,dbConn,chStates,totalChCount,Integer.parseInt(installation_id_select.getSelectedItem().toString()),installation_id_select.getSelectedIndex(),startDate,endDate));
				wlExtractorProcess.start();
				this.dispose();
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
	}
}
