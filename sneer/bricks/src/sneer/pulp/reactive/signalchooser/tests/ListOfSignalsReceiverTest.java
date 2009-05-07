package sneer.pulp.reactive.signalchooser.tests;

import static sneer.commons.environments.Environments.my;

import org.junit.Assert;
import org.junit.Test;

import sneer.brickness.testsupport.BrickTest;
import sneer.pulp.reactive.Register;
import sneer.pulp.reactive.Signal;
import sneer.pulp.reactive.Signals;
import sneer.pulp.reactive.collections.ListRegister;
import sneer.pulp.reactive.collections.ListSignal;
import sneer.pulp.reactive.collections.CollectionSignals;
import sneer.pulp.reactive.collections.impl.VisitingListReceiver;
import sneer.pulp.reactive.signalchooser.ListOfSignalsReceiver;
import sneer.pulp.reactive.signalchooser.SignalChooser;
import sneer.pulp.reactive.signalchooser.SignalChoosers;

public class ListOfSignalsReceiverTest extends BrickTest {

	private final SignalChoosers _factory = my(SignalChoosers.class); 
	
	@SuppressWarnings("unused")
	private Object _refToAvoidGc;
	private ListRegister<Register<String>> _listRegister = my(CollectionSignals.class).newListRegister();
	private EventRecorder _recorder;

	@Test
	public void test() {
		_recorder = new EventRecorder();
		
		addElement("0");
		assertEvents("");
		_refToAvoidGc = _factory.newManager(_listRegister.output(), newListOfSignalsReceiver(_listRegister.output()));
		
		Register<String> r1 = addElement("1");
		Register<String> r2 = addElement("2");
		addElement("3");
		addElement("4");
		addElement(r1);
		assertEvents("Added(1)=1, Added(2)=2, Added(3)=3, Added(4)=4, Added(5)=1, "); //[ 0, 1, 2, 3, 4, 1 ]
		
		_listRegister.move(1, 4);
		assertEvents("Removed(1)=1, Added(4)=1, ");	//[ 0, 4, 2, 3, 1, 1 ]
		
		r1.setter().consume("1b");
		r2.setter().consume("2b");
		assertEvents("Changed(1)=1b, Changed(5)=1b, Changed(2)=2b, "); //[ 0, 2b, 3, 4, 1b, 1b ]

		_listRegister.move(5, 1);
		assertEvents("Removed(5)=1b, Added(1)=1b, "); 	//[ 0, 1b, 2b, 3, 4, 1b ]
		
		_listRegister.removeAt(0);
		_listRegister.removeAt(3);
		_listRegister.removeAt(2);
		assertEvents("Removed(0)=0, Removed(3)=4, Removed(2)=3, ");	//[ 1b, 2b, 1b ]
		
		r1.setter().consume("1c");
		r2.setter().consume("2c");
		assertEvents("Changed(0)=1c, Changed(2)=1c, Changed(1)=2c, "); //[ 1c, 2c, 1c ]
		
		_listRegister.addAt(1, r2);
		assertEvents("Added(1)=2c, "); //[ 1c, 2c, 2c, 1c ]
		
		_listRegister.move(1, 0);
		_listRegister.move(2, 3);
		assertEvents("Removed(1)=2c, Added(0)=2c, Removed(2)=2c, Added(3)=2c, "); //[ 2c, 1c, 1c, 2c ]
		
		_listRegister.remove(r1);
		_listRegister.remove(r1);
		assertEvents("Removed(1)=1c, Removed(1)=1c, "); //[ 2c, 2c ]
		
	}

	private Register<String> addElement(String value) {
		return addElement(my(Signals.class).newRegister(value));
	}
	
	private Register<String> addElement(Register<String> register) {
		_listRegister.add(register);
		return register;
	}

	private void assertEvents(String expected) {
		Assert.assertEquals(expected, _recorder._events.toString());
		_recorder.clear();
	}
	
	private ListOfSignalsReceiver<Register<String>>newListOfSignalsReceiver(ListSignal<Register<String>> input) {
		return new MyListOfSignalsReceiver(input);
	}
	
	class MyListOfSignalsReceiver extends VisitingListReceiver<Register<String>> implements ListOfSignalsReceiver<Register<String>>{
		
		public MyListOfSignalsReceiver(ListSignal<Register<String>> input) {
			super(input);
		}

		@Override 
		public SignalChooser<Register<String>> signalChooser() {
			return new SignalChooser<Register<String>>(){ @Override public Signal<?>[] signalsToReceiveFrom( Register<String> element) {
				return new Signal<?>[]{element.output()};
			}};
		}

		@Override public void elementSignalChanged(int index, Register<String> element) {  
			_recorder.record("Changed", index, value(element));}
		
		@Override public void elementAdded(int index, Register<String> element) { 
			_recorder.record("Added", index, value(element)); }
		
		@Override public void elementRemoved(int index, Register<String> element) { 
			_recorder.record("Removed", index, value(element)); }
		
		@Override public void elementReplaced(int index, Register<String> oldElement,	Register<String> newElement) { 
			_recorder.record("Replaced", index, value(oldElement), value(newElement)); }		

		private String value(Register<String> element) {
			return element.output().currentValue();
		}
	}
	
	private class EventRecorder {
		
		private StringBuilder _events = new StringBuilder();
		
		private void record(String name, int index, String oldValue, String newValue) {
			record(name, ""+index, oldValue + "->" + newValue);
		}
		
		private void record(String name, int index, String value) {
			record(name, ""+index, value);
		}

		private void record(String name, String index, String value) {
			_events.append(name).append('(').append(index).append(")=").append(value).append(", ");
		}

		private void clear() {
			_events = new StringBuilder();
		}
	}
}