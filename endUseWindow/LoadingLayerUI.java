package endUseWindow;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

public class LoadingLayerUI extends LayerUI<JPanel>{
	private static final long serialVersionUID = 6835384657927783847L;
	
	private boolean shouldBeGrey;

	@Override
	public void paint (Graphics graphics, JComponent component) {
		super.paint (graphics, component);
		
		int height = component.getHeight();
		int width = component.getWidth();

		Graphics2D g2d = (Graphics2D)graphics.create();

		int on = 1*(shouldBeGrey==true?1:0);
		// Gray it out.
		Composite composite = g2d.getComposite();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .3f * on));
		g2d.fillRect(0, 0, width, height);
		
		
		
		//Add "Loading..." text
		String text = "Loading...";
		g2d.setPaint(Color.white);
		g2d.setFont(new Font(g2d.getFont().getName(), Font.BOLD, 18));
		graphics.setFont(new Font(g2d.getFont().getName(), Font.BOLD, 18));
		FontMetrics fm = g2d.getFontMetrics(graphics.getFont());
		Rectangle2D fontSize = fm.getStringBounds(text,graphics);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		g2d.drawString(text, Math.round((width-fontSize.getWidth())/2), Math.round((height/3)-(fontSize.getHeight()/2)));
		
		g2d.setComposite(composite);

		g2d.dispose();
	}
	
	public void start(){
		firePropertyChange("tick", 0, 1);
	}

	public void stop() {
		firePropertyChange("tick", 1, 0);
	}

	@Override
	public void applyPropertyChange(PropertyChangeEvent pce, JLayer l) {
		if ("tick".equals(pce.getPropertyName())) {
			if (pce.getOldValue().equals((Object)0) && pce.getNewValue().equals((Object)1)){
				shouldBeGrey = true;
				l.repaint();
			}
			else if (pce.getOldValue().equals((Object)1) && pce.getNewValue().equals((Object)0)){
				shouldBeGrey = false;
				l.repaint();
			}
		}
	}
}
