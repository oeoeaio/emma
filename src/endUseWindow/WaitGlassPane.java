package endUseWindow;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;

public class WaitGlassPane extends JComponent{
	private static final long serialVersionUID = -3027913471608791788L;
    
    public WaitGlassPane() {
        setBackground(Color.WHITE);
    }

    protected void paintComponent(Graphics g) {
        // enables anti-aliasing
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        // gets the current clipping area
        Rectangle clip = g.getClipBounds();
        
        // sets a 65% translucent composite
        AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.35f);
        Composite composite = g2.getComposite();
        g2.setComposite(alpha);
        
        // fills the background
        g2.setColor(getBackground());
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);
        
        g2.setComposite(composite);
    }
}
