package fileManagement;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;
import endUseWindow.SiteTable;
import endUseWindow.SourceTable;


public class StandAloneImportPanel extends JPanel implements ActionListener,ListSelectionListener{
	private static final long serialVersionUID = 5421907525894521938L;

	//Main Panel
	GridLayout mainPanelLayout = new GridLayout(2,1);
	JPanel mainPanel = new JPanel(mainPanelLayout);
	
	//Management Panel
	GridLayout managementPanelLayout = new GridLayout(1,3);
	JPanel managementPanel = new JPanel(managementPanelLayout);
	
	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Sites");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname"});
	JScrollPane siteScroll = new JScrollPane(siteTable);
	//JPanel siteButtonPanel = new JPanel(new FlowLayout());
	//JButton siteAddB = new JButton("Add");
	//JButton siteEditB = new JButton("Edit");
	//JButton siteRemB = new JButton("Remove");
	
	//Source Select Panel
	//Refrigerator Source List Panel Components
	JPanel sourcePanel = new JPanel(new BorderLayout());
	JPanel sourceTitleP = new JPanel(new FlowLayout());
	JLabel sourceLabel = new JLabel("Sources");
	SourceTable sourceTable = new SourceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Source Type","Measurement Type"});
	JScrollPane sourceScroll = new JScrollPane(sourceTable);
	//JPanel sourceButtonPanel = new JPanel(new FlowLayout());
	//JButton sourceAddB = new JButton("Add");
	//JButton sourceEditB = new JButton("Edit");
	//JButton sourceRemB = new JButton("Remove");
	
	
	//File Management Panel
	BorderLayout fileManagementPanelLayout = new BorderLayout();
	JPanel fileManagementPanel = new JPanel(fileManagementPanelLayout);
	
	//File List Panel Components
	JPanel filePanel = new JPanel(new BorderLayout());
	JPanel fileTitleP = new JPanel(new FlowLayout());
	JLabel fileLabel = new JLabel("File IDs");
	JTable fileList = new JTable() {
		private static final long serialVersionUID = -9183019632497684024L;
		public boolean isCellEditable(int rowIndex, int vColIndex) {
			return false;
	    }
	}; 
	ListSelectionModel fileListModel = fileList.getSelectionModel();
	DefaultTableModel fileTableModel = (DefaultTableModel)fileList.getModel();
	JScrollPane fileScroll = new JScrollPane(fileList);
	JPanel fileButtonPanel = new JPanel(new FlowLayout());
	JButton fileRemB = new JButton("Remove");
	
	//File Input Panel Components
	JPanel inputFilePanel = new JPanel(new BorderLayout());
	DefaultListModel<String> inputFileListModel = new DefaultListModel<String>();
	JList<String> fileSelectField = new JList<String>(); 
	JScrollPane inputFileScroll = new JScrollPane(fileSelectField);
	//File Select Button Panel Components
	//JPanel inputFileButtonPanel = new JPanel();
	JButton fileSelectButton = new JButton("Click to select files to add the selected source...");
	//Button Panel Components
	JPanel nextButtonPanel = new JPanel();
	JButton inputFileRemB = new JButton("Remove Selected Files");
	JButton nextB = new JButton("Start Processing...");
	
	//File Selection Window
	ArrayList<File> inputFileList = new ArrayList<File>();
	
	MySQLConnection mySQLConnection;
	Connection dbConn;
	
	public StandAloneImportPanel(MySQLConnection mysqlConnection){
		this.mySQLConnection = mysqlConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(20,50,50,50));
		
		mainPanelLayout.setVgap(15);
		managementPanelLayout.setHgap(20);
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);
		
		//sources
		sourcePanel.add(sourceTitleP,BorderLayout.NORTH);
		sourceTitleP.add(sourceLabel);
		sourcePanel.add(sourceScroll,BorderLayout.CENTER);
		
		//files
		filePanel.add(fileTitleP,BorderLayout.NORTH);
		fileTitleP.add(fileLabel);
		filePanel.add(fileScroll,BorderLayout.CENTER);
		filePanel.add(fileButtonPanel,BorderLayout.SOUTH);
		fileButtonPanel.add(fileRemB);
		fileList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		fileList.setColumnSelectionAllowed(false);
		fileList.setRowSelectionAllowed(true);
		fileTableModel.setColumnIdentifiers(new String[] {"File ID","File Name"});
		
		//Input Panel
		inputFilePanel.add(fileSelectButton,BorderLayout.NORTH);
		inputFilePanel.add(inputFileScroll,BorderLayout.CENTER);
		//inputFilePanel.add(inputFileButtonPanel,BorderLayout.LINE_END);
		//inputFileButtonPanel.setLayout(new BoxLayout(inputFileButtonPanel,BoxLayout.Y_AXIS));
		//inputFileButtonPanel.add(fileSelectButton);
		fileSelectButton.setEnabled(false);
		fileSelectField.setEnabled(false);
		fileSelectField.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		fileSelectField.setModel(inputFileListModel);
		nextButtonPanel.add(inputFileRemB);
		inputFileRemB.setEnabled(false);
		nextButtonPanel.add(nextB);
		nextB.setEnabled(false);
		
		siteTable.siteListModel.addListSelectionListener(this);
		sourceTable.sourceListModel.addListSelectionListener(this);
		fileListModel.addListSelectionListener(this);
		fileRemB.addActionListener(this);
		fileSelectButton.addActionListener(this);
		inputFileRemB.addActionListener(this);
		nextB.addActionListener(this);
		
		managementPanel.add(sitePanel);
		managementPanel.add(sourcePanel);
		managementPanel.add(filePanel);
		fileManagementPanel.add(inputFilePanel,BorderLayout.CENTER);
		mainPanel.add(managementPanel);
		mainPanel.add(fileManagementPanel);
		this.add(mainPanel,BorderLayout.CENTER);
		this.add(nextButtonPanel,BorderLayout.SOUTH);
		
		siteTable.updateList(dbConn);
	}
	

	void updateFiles() throws SQLException{
		if (siteTable.siteListModel.isSelectionEmpty()==false && sourceTable.sourceListModel.isSelectionEmpty()==false){ //&& siteList.getValueAt(siteList.getSelectedRow(),0).toString()!=null ???
			final Statement MySQL_Statement = dbConn.createStatement();
			final String getFilesSQL = "SELECT * FROM files WHERE site_id = '"+siteTable.getValueAt(siteTable.getSelectedRow(),0)+"' AND source_id = '"+sourceTable.getValueAt(sourceTable.getSelectedRow(),0)+"' ORDER BY file_id";
			final ResultSet fileResults = MySQL_Statement.executeQuery(getFilesSQL);
			
			fileListModel.removeListSelectionListener(this);
			fileTableModel.setNumRows(0);
			while (fileResults.next()){
				fileTableModel.addRow(new String[] {fileResults.getString("file_id"),fileResults.getString("file_name")});
			}
			fileListModel.addListSelectionListener(this);
		}
		
	}


	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(fileRemB)){
			if (fileListModel.isSelectionEmpty()==false){
				String selectedFile = fileList.getValueAt(fileList.getSelectedRow(),0).toString();
				int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected file ("+selectedFile+") and all associated data?","Removing File...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (response == JOptionPane.YES_OPTION){
					try{
						Statement MySQL_Statement = dbConn.createStatement();
						MySQL_Statement.executeUpdate("DELETE FROM files WHERE file_id = "+selectedFile); //removes data pertianing to selected file
						updateFiles();
					} catch (SQLException sE){
						JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified site ("+selectedFile+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
						sE.printStackTrace();
					}
				}
			}
		}
		else if (aE.getSource() == fileSelectButton){
			JFileChooser fileChooser = new JFileChooser("./");
			Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
			File lastDir = new File(fileSettings.get("LastStandAloneImport", fileChooser.getCurrentDirectory().getAbsolutePath()));
			fileChooser.setCurrentDirectory(lastDir);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(true);
			int fChooserOption = fileChooser.showOpenDialog(fileChooser);
			if (fChooserOption==JFileChooser.APPROVE_OPTION){
				if (fileChooser.getSelectedFiles().length>0){fileSettings.put("LastStandAloneImport", fileChooser.getSelectedFiles()[0].getParent());}
				File[] selectedFiles = fileChooser.getSelectedFiles();
				for (int i=0;i<fileChooser.getSelectedFiles().length;i++){
					inputFileListModel.addElement(selectedFiles[i].toString());
					inputFileList.add(selectedFiles[i]);
				}
			}
		}
		else if (aE.getSource() == inputFileRemB){ //remove button clicked
			if (fileSelectField.isSelectionEmpty()==false){
				if (JOptionPane.showConfirmDialog(null,"Are you sure you wish to delete the selected files from the list?","WARNING",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION){
					int[] selectedFileIndices = fileSelectField.getSelectedIndices(); //set Array containing indices so that reassignment doesn't alter indices positions
					for (int i=selectedFileIndices.length-1;i>=0;i--){ //must go backwards to ensure indices remain the same for the duration of the loop
						inputFileListModel.remove(selectedFileIndices[i]);
						inputFileList.remove(selectedFileIndices[i]);
					}
				}
			}
		}
		else if (aE.getSource() == nextB){
			//TODO this really need to run in another thread
			LogWindow logWindow = new LogWindow("Data Import Log");
			StandAloneValidator standAloneValidator = new StandAloneValidator(mySQLConnection,logWindow);
			Thread validatorThread = new Thread(standAloneValidator);
			validatorThread.start();
			for (int i=0;i<inputFileList.size();i++){
				DataFile dataFile = new DataFile();
				dataFile.siteID = siteTable.getValueAt(siteTable.getSelectedRow(),0).toString();
				dataFile.sourceID = sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString();
				dataFile.fileName = inputFileList.get(i).getName().toString();
				dataFile.file = inputFileList.get(i);
				standAloneValidator.addFile(dataFile);
			}
			for (int i=inputFileListModel.getSize()-1;i>=0;i--){ //must go backwards to ensure indices remain the same for the duration of the loop
				inputFileListModel.remove(i);
				inputFileList.remove(i);
			}
			synchronized(standAloneValidator.fileList){
				standAloneValidator.moreFilesComing = false;
				standAloneValidator.fileList.notify();
			}			
			logWindow.println("\r\nFinished sending all files to writer.");
		}
		
		if ((inputFileListModel.getSize() == inputFileList.size()) && inputFileList.size()!=0){ //if there is a file in the list
			inputFileRemB.setEnabled(true);
			nextB.setEnabled(true);
			siteTable.setEnabled(false);
			sourceTable.setEnabled(false);
		}
		else if ((inputFileListModel.getSize() == inputFileList.size()) && inputFileList.size()==0){ //no file selected
			inputFileRemB.setEnabled(false);
			nextB.setEnabled(false);
			siteTable.setEnabled(true);
			sourceTable.setEnabled(true);
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
			try {
				sourceTable.updateList(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()));
				updateFiles();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				fileSelectButton.setEnabled(true);
				fileSelectField.setEnabled(true);
			}
			else{
				fileSelectButton.setEnabled(false);
				fileSelectField.setEnabled(false);
			}
		}
		if (lSE.getSource().equals(sourceTable.sourceListModel) && lSE.getValueIsAdjusting()==false){
			try {
				updateFiles();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				fileSelectButton.setEnabled(true);
				fileSelectField.setEnabled(true);
			}
			else{
				fileSelectButton.setEnabled(false);
				fileSelectField.setEnabled(false);
			}
		}
	}
}
	

