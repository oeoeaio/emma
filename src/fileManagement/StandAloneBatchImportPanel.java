package fileManagement;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;


public class StandAloneBatchImportPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2156704145987000265L;
	
	JPanel mainPanel = new JPanel(new BorderLayout());
	JLabel fileSelectL = new JLabel("Please select a batch file log (.csv): ");
	JTextField fileSelectF = new JTextField(20);
	JButton fileSelectB = new JButton("Select File...");
	JButton processB = new JButton("Process");
	
	File selectedFile = null;
	LogWindow logWindow;
	MySQLConnection mySQLConnection;

	public StandAloneBatchImportPanel(MySQLConnection mySQLConnection){
		this.mySQLConnection = mySQLConnection;	
		
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		mainPanel.add(fileSelectL);
		mainPanel.add(fileSelectF);
		mainPanel.add(fileSelectB);
		mainPanel.add(processB);
		fileSelectB.addActionListener(this);
		processB.addActionListener(this);
		
		this.add(mainPanel,BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(processB)){
			Thread processBatch = new Thread(new StandAloneBatchImporter(mySQLConnection,new LogWindow("Data Import Log"),selectedFile));
			processBatch.start();
		}
		if (aE.getSource() == fileSelectB){
			JFileChooser fChooser = new JFileChooser("./");
			fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fChooser.setMultiSelectionEnabled(false);
			int fChooserOption = fChooser.showOpenDialog(fChooser);
			if (fChooserOption==JFileChooser.APPROVE_OPTION){
				selectedFile = fChooser.getSelectedFile();
				fileSelectF.setText(selectedFile.getName());
			}
			else{
				selectedFile = null;
				fileSelectF.setText("");
			}
		}
		
	}
}
