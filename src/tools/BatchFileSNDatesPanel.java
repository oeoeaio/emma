package tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


import endUseWindow.LogWindow;


public class BatchFileSNDatesPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2156704145987000265L;
	
	JPanel mainPanel = new JPanel(new FlowLayout());
	JLabel fileSelectL = new JLabel("Please select a files to process (.csv): ");
	JTextField fileSelectF = new JTextField(20);
	JButton fileSelectB = new JButton("Select Files...");
	JButton processB = new JButton("Process");
	
	File[] selectedFiles = null;
	ArrayList<String[]> dataList = new ArrayList<String[]>();
	LogWindow logWindow;

	public BatchFileSNDatesPanel(){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
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
			Thread getSNDates = new Thread(new getSNDates(new LogWindow("Fetch Dates And SNs Log"),selectedFiles));
			getSNDates.start();
		}
		if (aE.getSource() == fileSelectB){
			JFileChooser fChooser = new JFileChooser("./");
			fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fChooser.setMultiSelectionEnabled(true);
			int fChooserOption = fChooser.showOpenDialog(fChooser);
			if (fChooserOption==JFileChooser.APPROVE_OPTION){
				selectedFiles = fChooser.getSelectedFiles();
			}
			else{
				selectedFiles = null;
				fileSelectF.setText("");
			}
		}
		
	}
}
