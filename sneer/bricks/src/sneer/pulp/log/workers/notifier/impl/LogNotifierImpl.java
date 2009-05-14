package sneer.pulp.log.workers.notifier.impl;

import static sneer.commons.environments.Environments.my;
import sneer.pulp.events.EventNotifier;
import sneer.pulp.events.EventNotifiers;
import sneer.pulp.events.EventSource;
import sneer.pulp.log.LogWorker;
import sneer.pulp.log.Logger;
import sneer.pulp.log.filter.LogFilter;
import sneer.pulp.log.formatter.LogFormatter;
import sneer.pulp.log.workers.notifier.LogNotifier;

public class LogNotifierImpl implements LogNotifier {

	private final LogFilter _filter = my(LogFilter.class);
	private final LogFormatter _formatter = my(LogFormatter.class);
	private final EventNotifier<String> _loggedMessages = my(EventNotifiers.class).create();
	
	{
		my(Logger.class).setDelegate(new LogWorker(){
			@Override public void log(String message, Object... messageInsets) {  notifyEntry(_formatter.format(message, messageInsets)); }
			@Override public void log(Throwable throwable, String message, Object... messageInsets) { notifyEntry(_formatter.format(throwable, message, messageInsets));}
			@Override public void log(Throwable throwable) { notifyEntry(_formatter.format(throwable)); }
			@Override public void logShort(Throwable throwable, String message, Object... messageInsets) { notifyEntry(_formatter.formatShort(throwable, message, messageInsets));}
		});
	}
	
	private void notifyEntry(String msg){
		if(_filter.acceptLogEntry(msg))
			_loggedMessages.notifyReceivers(msg);
	}

	@Override
	public EventSource<String> loggedMessages() {
		return _loggedMessages.output();
	}
}
