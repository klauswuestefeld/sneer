package dfcsantos.wusic.gui;

import java.awt.Dimension;

import sneer.bricks.software.bricks.snappstarter.Snapp;
import sneer.foundation.brickness.Brick;

@Snapp
@Brick
public interface WusicGui {

	Dimension PREFERRED_SIZE = new Dimension(460, 130);

	String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";

}
