package fileManagement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import endUseWindow.Source;


public class SourceClashWindow extends JFrame implements ActionListener {
	static final long serialVersionUID = 474802667938989652L;
	
	//Main Panel
	JPanel mainPanel = new JPanel(new BorderLayout());
	//Top Panel
	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel topLabel = new JLabel("");
	//Middle Panel
	JPanel middlePanel = new JPanel(new GridLayout(1,3));
	//Option 1 Panel
	JPanel o1Panel = new JPanel(new BorderLayout());
	JPanel o1InfoPanel = new JPanel(new GridLayout(0,1));
	JLabel o1Label = new JLabel("");
	JRadioButton o1Button = new JRadioButton("<html>Existing information<br>(below) for this source is<br>correct.</html>");
	//Option 2 Panel
	JPanel o2Panel = new JPanel(new BorderLayout());
	JPanel o2InfoPanel = new JPanel(new GridLayout(0,1));
	JLabel o2Label = new JLabel("");
	JRadioButton o2Button = new JRadioButton("<html>The conflicting information<br>(below) for this source is<br>correct.</html>");
	//Option 3 Panel
	JPanel o3Panel = new JPanel(new BorderLayout());
	JRadioButton o3Button = new JRadioButton("<html>Do nothing and ignore this<br>record for the time being.</html>");
	ButtonGroup options = new ButtonGroup();
	int selectedOption = 3;
	
	//Bottom Panel
	JPanel bottomPanel = new JPanel(new FlowLayout());
	JButton submitButton = new JButton("Submit");
	
	void buildGUI(String sourceID, Source existing, Source conflicting){
		this.setVisible(false);
		this.setSize(600,300);
		this.setLocation(350,250);
		this.setTitle("Clash of source data detected.");
		
		mainPanel.add(topPanel,BorderLayout.NORTH);
		mainPanel.add(middlePanel,BorderLayout.CENTER);
		mainPanel.add(bottomPanel,BorderLayout.SOUTH);
		
		topPanel.add(topLabel);
		topLabel.setText("A mismatch in information has been detected for source "+sourceID+". Please select an option to resolve this conflict.");
		
		middlePanel.add(o1Panel);
		middlePanel.add(o2Panel);
		middlePanel.add(o3Panel);
		
		o1Panel.setBorder(BorderFactory.createMatteBorder(1,1,1,0,Color.black));
		o2Panel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
		o3Panel.setBorder(BorderFactory.createMatteBorder(1,0,1,1,Color.black));
		
		options.add(o1Button);
		options.add(o2Button);
		options.add(o3Button);
		o1Button.addActionListener(this);
		o2Button.addActionListener(this);
		o3Button.addActionListener(this);
		o3Button.setSelected(true);
		
		o1Panel.add(o1Button,BorderLayout.NORTH);
		o1Panel.add(o1InfoPanel,BorderLayout.CENTER);
		
		o1InfoPanel.setBorder(new EmptyBorder(0,5,0,0));
		o1InfoPanel.add(new JLabel("<html><b>Existing Information:</b></html>"));
		o1InfoPanel.add(new JLabel("Source Name: "+existing.getSourceName()));
		o1InfoPanel.add(new JLabel("Source Type: "+existing.getSourceType()));
		o1InfoPanel.add(new JLabel("Measurement Type: "+existing.getMeasurementType()));
				
		o2Panel.add(o2Button,BorderLayout.NORTH);
		o2Panel.add(o2InfoPanel,BorderLayout.CENTER);
		
		o2InfoPanel.setBorder(new EmptyBorder(0,5,0,0));
		o2InfoPanel.add(new JLabel("<html><b>Conflicting Information:</b></html>"));
		o2InfoPanel.add(new JLabel("Source Name: "+conflicting.getSourceName()));
		o2InfoPanel.add(new JLabel("Source Type: "+conflicting.getSourceType()));
		o2InfoPanel.add(new JLabel("Measurement Type: "+conflicting.getMeasurementType()));
		
		o3Panel.add(o3Button, BorderLayout.NORTH);
		
		bottomPanel.add(submitButton);
		submitButton.addActionListener(this);

		getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.validate();
		this.setVisible(true);
		this.toFront();
		this.requestFocus();
	}
	
	
	int getSelectedOption(final String sourceID,final Source existing,final Source conflicting){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI(sourceID,existing,conflicting);
			}
		});
		
		try {
			synchronized (submitButton) {
				submitButton.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.setVisible(false);
		this.dispose();	
		
		return selectedOption;
	}


	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource()==submitButton){
			synchronized (submitButton) {
				submitButton.notify();
			}
        }
		else if(aE.getSource()==o1Button){
			selectedOption = 1;
		}
		else if(aE.getSource()==o2Button){
			selectedOption = 2;
		}
		else if(aE.getSource()==o3Button){
			selectedOption = 3;
		}
		
	}
}