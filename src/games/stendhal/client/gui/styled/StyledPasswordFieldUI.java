package games.stendhal.client.gui.styled;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;

public class StyledPasswordFieldUI extends BasicPasswordFieldUI {
	/** Pixels before the first letter and after the last */
	private static final int PADDING = 2;
	
	private final Style style;
	
	// Required by UIManager
	public static ComponentUI createUI(JComponent field) {
		// BasicTextFieldUI can not be shared
		return new StyledPasswordFieldUI(StyleUtil.getStyle());
	}
	
	/**
	 * Create a new StyledPasswordFieldUI.
	 * 
	 * @param style pixmap style
	 */
	public StyledPasswordFieldUI(Style style) {
		this.style = style;
	}
	
	@Override
	public void installUI(JComponent field) {
		super.installUI(field);
		field.setBorder(BorderFactory.createCompoundBorder(style.getBorderDown(),
				BorderFactory.createEmptyBorder(0, PADDING, 0, PADDING)));
	}
}
