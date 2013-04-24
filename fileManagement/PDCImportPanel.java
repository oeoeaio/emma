package fileManagement;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;

public class PDCImportPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 6307555201299049958L;
	
	//File Panel Components
	JPanel filePanel = new JPanel(new BorderLayout());
	DefaultListModel<String> fileListModel = new DefaultListModel<String>();
	JList<String> fileSelectField = new JList<String>(); 
	JScrollPane fileScroll = new JScrollPane(fileSelectField);
	//File Select Button Panel Components
	JPanel fileButtonPanel = new JPanel();
	JButton fileSelectButton = new JButton("Select/Add files...");
	JCheckBox toFileCheckBox = new JCheckBox("Output To File");
	JCheckBox toDatabaseCheckBox = new JCheckBox("Output To Database");
	JCheckBox validateWChNames = new JCheckBox("Valid W Ch Names");
	
	//Button Panel Components
	JPanel nextButtonPanel = new JPanel();
	JButton removeButton = new JButton("Remove Selected Files");
	JButton nextButton = new JButton("Next...");
	
	//File Selection Window
	JFileChooser fChooser = new JFileChooser("./");
	ArrayList<File> fileList = new ArrayList<File>();
	
	//Database Connection
	Connection dbConn;
	MySQLConnection mySQLConnection;
	
	public PDCImportPanel(MySQLConnection mySQLConnection){
		this.mySQLConnection = mySQLConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		filePanel.add(fileScroll,BorderLayout.CENTER);
		filePanel.add(fileButtonPanel,BorderLayout.LINE_END);
		fileButtonPanel.setLayout(new BoxLayout(fileButtonPanel,BoxLayout.Y_AXIS));
		fileButtonPanel.add(fileSelectButton);
		fileButtonPanel.add(toFileCheckBox);
		fileButtonPanel.add(toDatabaseCheckBox);
		fileButtonPanel.add(validateWChNames);
		fileSelectButton.addActionListener(this);
		fileSelectField.setEnabled(true);
		fileSelectField.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		fileSelectField.setModel(fileListModel);
		nextButtonPanel.add(removeButton);
		removeButton.setEnabled(false);
		removeButton.addActionListener(this);
		nextButtonPanel.add(nextButton);
		nextButton.setEnabled(false);
		nextButton.addActionListener(this);
		this.add(filePanel,BorderLayout.CENTER);
		this.add(nextButtonPanel,BorderLayout.SOUTH);
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent AcnEvt) { //Button Click Events
		if (AcnEvt.getSource() == fileSelectButton){
			fChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fChooser.setMultiSelectionEnabled(true);
			int fChooserOption = fChooser.showOpenDialog(fChooser);
			if (fChooserOption==JFileChooser.APPROVE_OPTION){
				File[] selectedFiles = fChooser.getSelectedFiles();
				for (int i=0;i<fChooser.getSelectedFiles().length;i++){
					fileListModel.addElement(selectedFiles[i].toString());
					fileList.add(selectedFiles[i]);
				}
				
			}
		}
		if (AcnEvt.getSource() == removeButton){ //remove button clicked
			if (JOptionPane.showConfirmDialog(null,"Are you sure you wish to delete the selected files from the list?","WARNING",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION){
				int[] selectedFileIndices = fileSelectField.getSelectedIndices(); //set Array containing indices so that reassignment doesn't alter indices positions
				for (int i=selectedFileIndices.length-1;i>=0;i--){ //must go backwards to ensure indices remain the same for the duration of the loop
					fileListModel.remove(selectedFileIndices[i]);
					fileList.remove(selectedFileIndices[i]);
				}
			}
		}
		if ((fileListModel.getSize() == fileList.size()) && fileList.size()!=0){ //if there is a file in the list
			removeButton.setEnabled(true);
			if ((toFileCheckBox.isSelected() || toDatabaseCheckBox.isSelected())){ //if checkboxes are selected
				nextButton.setEnabled(true);
			}
			else if (toFileCheckBox.isSelected()==false && toDatabaseCheckBox.isSelected()==false){
				nextButton.setEnabled(false);
			}
		}
		else if ((fileListModel.getSize() == fileList.size()) && fileList.size()==0){
			nextButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
		
		
		if (AcnEvt.getSource() == nextButton){
			try {
				dbConn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread processThread = new Thread(new PDCValidator(fileList,mySQLConnection,new LogWindow("PDC File Processing Log"),toFileCheckBox.isSelected(),toDatabaseCheckBox.isSelected(),true,false,validateWChNames.isSelected()));
			processThread.start();
		}
	}
}
