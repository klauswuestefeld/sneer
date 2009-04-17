package spikes.demos;

import static sneer.commons.environments.Environments.my;

import java.awt.FlowLayout;

import javax.swing.JFrame;

import sneer.commons.environments.Environments;
import sneer.hardware.gui.guithread.GuiThread;
import sneer.hardware.gui.timebox.TimeboxedEventQueue;
import sneer.kernel.container.ContainersOld;
import sneer.pulp.reactive.Register;
import sneer.pulp.reactive.Signals;
import sneer.skin.widgets.reactive.NotificationPolicy;
import sneer.skin.widgets.reactive.ReactiveWidgetFactory;
import sneer.skin.widgets.reactive.TextWidget;
import sneer.skin.widgets.reactive.Widget;
import wheel.io.Logger;

public class ReactiveWidgetsDemo {

	private ReactiveWidgetsDemo(){
		
		my(TimeboxedEventQueue.class).startQueueing(500000);
		
		my(GuiThread.class).strictInvokeAndWait(new Runnable(){ @Override public void run() {

			final ReactiveWidgetFactory rfactory = my(ReactiveWidgetFactory.class);
			final Register<String> register = my(Signals.class).newRegister("Jose das Coves");
			
			TextWidget<?> textWidget;
			
			textWidget = rfactory.newTextField(register.output(), register.setter());
			createTestFrame(textWidget, 10, 10, 300, 100, "OnTyping");

			textWidget = rfactory.newEditableLabel(register.output(), register.setter());
			createTestFrame(textWidget, 10, 120, 300, 100, "OnTyping");

			textWidget = rfactory.newLabel(register.output());
			createTestFrame(textWidget, 10, 230, 300, 100, "OnTyping");

			textWidget = rfactory.newTextPane(register.output(), register.setter());
			createTestFrame(textWidget, 10, 340, 300, 100, "OnTyping");
			
			Widget<JFrame> frame = rfactory.newFrame(register.output());
			frame.getMainWidget().setBounds(10, 450, 300, 100);
			frame.getMainWidget().setVisible(true);
			
			textWidget = rfactory.newTextField(register.output(), register.setter(), NotificationPolicy.OnEnterPressed);
			createTestFrame(textWidget, 350, 10, 300, 100, "OnEnterPressed");

			textWidget = rfactory.newEditableLabel(register.output(), register.setter(), NotificationPolicy.OnEnterPressed);
			createTestFrame(textWidget, 350, 120, 300, 100, "OnEnterPressed");

			textWidget = rfactory.newTextPane(register.output(), register.setter(), NotificationPolicy.OnEnterPressed);
			createTestFrame(textWidget, 350, 230, 300, 100, "OnEnterPressed");
			
			textWidget = rfactory.newTextPane(register.output(), register.setter(), NotificationPolicy.OnEnterPressedOrLostFocus);
			createTestFrame(textWidget, 350, 340, 300, 100, "OnLostFocus");
		}});
	}

	private void createTestFrame(final TextWidget<?> textWidget, final int x, final int y, final int width, final int height, final String title) {
		final JFrame frm = new JFrame();
		frm.setTitle(textWidget.getClass().getSimpleName() + " - " + title);
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.getContentPane().setLayout(new FlowLayout());
		frm.getContentPane().add(textWidget.getComponent());
		frm.setVisible(true);
		frm.setBounds(x, y, width, height);
	}
	
	public static void main(String[] args) throws Exception {
		Logger.redirectTo(System.out);
		Environments.runWith(ContainersOld.newContainer(), new Runnable(){ @Override public void run() {
			try {
				new ReactiveWidgetsDemo();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}});
	}
}