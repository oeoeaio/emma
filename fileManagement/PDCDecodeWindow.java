package fileManagement;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import endUseWindow.LogWindow;

public class PDCDecodeWindow extends JFrame implements ActionListener{
	private static final long serialVersionUID = 6307555201299049958L;
	
	//Main Panel
	private final JPanel mainPanel = new JPanel(new BorderLayout());
	//File Panel Components
	private final JPanel filePanel = new JPanel(new BorderLayout());
	private final DefaultListModel<String> fileListModel = new DefaultListModel<String>();
	private final JList<String> fileSelectField = new JList<String>(); 
	private final JScrollPane fileScroll = new JScrollPane(fileSelectField);
	//File Select Button Panel Components
	private final JPanel fileButtonPanel = new JPanel();
	private final JButton fileSelectButton = new JButton("Select/Add files...");
	//Button Panel Components
	private final JPanel nextButtonPanel = new JPanel();
	private final JButton removeButton = new JButton("Remove Selected Files");
	private final JButton nextButton = new JButton("Next...");
	
	//File Selection Window
	private final JFileChooser fChooser = new JFileChooser("./");
	private final ArrayList<File> fileList = new ArrayList<File>();
	
	public static void main(String[] args) {
		// Get the native look and feel class name
		String nativeLF = UIManager.getSystemLookAndFeelClassName();

		// Install the look and feel
		try {
		    UIManager.setLookAndFeel(nativeLF);
		} catch (InstantiationException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnsupportedLookAndFeelException e) {
		} catch (IllegalAccessException e) {
		}
		
		new PDCDecodeWindow();	
	}
	
	public PDCDecodeWindow(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setTitle(".pdc file decoder");
		this.setSize(400,400);
		this.setLocation(300,250);
		filePanel.add(fileScroll,BorderLayout.CENTER);
		filePanel.add(fileButtonPanel,BorderLayout.LINE_END);
		fileButtonPanel.setLayout(new BoxLayout(fileButtonPanel,BoxLayout.Y_AXIS));
		fileButtonPanel.add(fileSelectButton);
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
		mainPanel.add(filePanel,BorderLayout.CENTER);
		mainPanel.add(nextButtonPanel,BorderLayout.SOUTH);
		getContentPane().add(mainPanel);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
			nextButton.setEnabled(true);

		}
		else if ((fileListModel.getSize() == fileList.size()) && fileList.size()==0){
			nextButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
		
		
		if (AcnEvt.getSource() == nextButton){
			Thread processThread = new Thread(new PDCValidator(fileList,null,new LogWindow("PDC File Processing Log"),true,false,true,false));
			processThread.start();
		}
	}
}
