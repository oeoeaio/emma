package endUseWindow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class LogWindow extends JFrame implements ActionListener {
	static final long serialVersionUID = 474802667938989652L;
	
	//Main Panel
	private final JPanel mainPanel = new JPanel(new BorderLayout());
	//Log Text Area
	private final JTextArea log = new JTextArea();
	private final JScrollPane logScroll = new JScrollPane(log);
	//Bottom Panel
	private final JPanel bottomPanel = new JPanel(new FlowLayout());
	//Save Button
	private final JButton saveButton = new JButton("Save Log");
	
	private final JFileChooser fChooser = new JFileChooser();
	private boolean showGUI = true;
	private boolean logFileOK = false;
	private BufferedWriter outputStream;
	
	
	
	public LogWindow(final String title){ //this version displays a log window
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI(title);
			}
		});
		
	}
	
	public LogWindow(final File file){ //this version write directly to a log file and does not display a log
		this.showGUI = false;
		try{
			outputStream = new BufferedWriter(new FileWriter(file));
			logFileOK = true;
		}catch(IOException e){
			System.out.println("Error creating logFile. Log will be printed here...");
		}
	}
	
	void buildGUI(String title){
		this.setVisible(false);
		this.setSize(600,300);
		this.setLocation(300,200);
		this.setTitle(title);
		
		mainPanel.add(logScroll,BorderLayout.CENTER);
		mainPanel.add(bottomPanel,BorderLayout.SOUTH);
		bottomPanel.add(saveButton);
		saveButton.addActionListener(this);


		getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.validate();
		this.setVisible(true);
		this.toFront();
		this.requestFocus();
	}
			
	public void println (final String printText){
		if (showGUI){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					log.append(printText+"\r\n");
					log.setCaretPosition(log.getDocument().getLength());
				}
			});
		}
		else{
			if (logFileOK){
				try {
					outputStream.write(printText+"\r\n");
					outputStream.flush();
				} catch (IOException e) {
					System.out.println(printText);
				}
			}
			else{
				System.out.println(printText);
			}
		}
	}
	
	public void printString (final String printText){
		if (showGUI){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					log.append(printText);
					log.setCaretPosition(log.getDocument().getLength());
				}
			});
		}
		else{
			if (logFileOK){
				try {
					outputStream.write(printText);
					outputStream.flush();
				} catch (IOException e) {
					System.out.print(printText);
				}
			}
			else{
				System.out.print(printText);
			}
		}
	}


	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource()==saveButton){
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			String now = dateFormatter.format(new Date().getTime());
           
            fChooser.setSelectedFile(new File("./"+this.getTitle().replaceAll(" ", "")+" - "+now+".txt"));
            int fChooserOption = fChooser.showSaveDialog(fChooser);
            if (fChooserOption==JFileChooser.APPROVE_OPTION){
                File logFile = fChooser.getSelectedFile();
                try {
                    BufferedWriter outputStream = new BufferedWriter(new FileWriter(logFile));
                    outputStream.write(log.getText());
                    outputStream.close();
                }
                catch (IOException e) {
                	JOptionPane.showMessageDialog(this,"Could not write to selected file.\r\nPlease ensure you have permission to write to this location.","Write Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
		
	}

}