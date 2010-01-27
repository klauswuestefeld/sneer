package dfcsantos.wusic.gui.impl;

import static sneer.foundation.environments.Environments.my;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.foundation.lang.Consumer;
import dfcsantos.wusic.Wusic;
import dfcsantos.wusic.Wusic.OperatingMode;

abstract class ControlPanel extends JPanel {

	private static final Wusic _controller	= my(Wusic.class);

	private static final String RESUME_ICON	= "\u25BA";
	private static final String PAUSE_ICON	= "\u2161";
//	private static final String BACK_ICON	= "<<";
	private static final String SKIP_ICON	= ">>";
	private static final String STOP_ICON	= "\u25A0";

	private final JButton _pauseResume		= new JButton();
//	private final JButton _back				= new JButton();
	private final JButton _skip				= new JButton();
	private final JButton _stop				= new JButton();

	@SuppressWarnings("unused") private WeakContract toAvoidGC;

	ControlPanel() {
		super(new FlowLayout(FlowLayout.LEFT, 9, 3));

	    toAvoidGC = _controller.isPlaying().addReceiver(new Consumer<Boolean>() { @Override public void consume(Boolean isPlaying) {
	    	if (isMyOperatingMode())
	    		_pauseResume.setText(isPlaying ? PAUSE_ICON : RESUME_ICON);
	    	else
	    		_pauseResume.setText(RESUME_ICON);
		}});

	    _pauseResume.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent evt) {
	    	pauseResumeActionPerformed();
        }});
	    add(_pauseResume);

//	    _back.setText(BACK_ICON);
//	    _back.addActionListener(new ActionListener() {
//	        public void actionPerformed(ActionEvent evt) {
//	            backActionPerformed();
//	        }
//	    });
//	    add(_back);

	    _skip.setText(SKIP_ICON);
	    _skip.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent evt) {
	    	skipActionPerformed();
	    }});
	    add(_skip);

	    _stop.setText(STOP_ICON);
//	    _stop.setForeground(Color.RED);
	    _stop.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent evt) {
	    	stopActionPerformed();
	    }});
	    add(_stop);
	}

	void update(OperatingMode operatingMode) {
		if (isMyOperatingMode(operatingMode))
			enableButtons();
		else
			disableButtons();
	}

	void enableButtons() {
		_skip.setEnabled(true);
		_stop.setEnabled(true);
	}

	void disableButtons() {
		_skip.setEnabled(false);
		_stop.setEnabled(false);
	}

	private void pauseResumeActionPerformed() {
		switchOperatingModeIfNecessary();
		_controller.pauseResume();
	}

	private void switchOperatingModeIfNecessary() {
		if (isMyOperatingMode()) return;

		_controller.switchOperatingMode();
	}

//	private void backActionPerformed() {
//	    Wusic.back();
//	}

	private void skipActionPerformed() {
	    _controller.skip();
	}

	private void stopActionPerformed() {
	    _controller.stop();
	}

	private boolean isMyOperatingMode() {
		return isMyOperatingMode(_controller.operatingMode().currentValue());
	}

	abstract boolean isMyOperatingMode(OperatingMode operatingMode);

}
