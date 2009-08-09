package sneer.tests.adapters;

import java.util.ArrayList;
import java.util.List;

import sneer.bricks.hardware.io.log.LogWorker;
import sneer.bricks.hardware.io.log.Logger;

class LoggerForTests implements Logger {

	private static List<String> _allPrefixes = new ArrayList<String>();
	
	private final String _prefix;

	
	LoggerForTests(String prefix) {
		_allPrefixes.add(prefix);
		_prefix = prefix + " " + count(prefix);
	}


	@Override
	public void log(String message, Object... messageInsets) {
		String formatted = format(message, messageInsets);
		if (formatted.contains("Tuple")) return;
		//System.out.println(formatted); //////////////////////// This is the line you uncomment to turn logging on for functional tests.
	}

	@Override
	public void setDelegate(LogWorker worker) {
		throw new UnsupportedOperationException();
	}

	String format(String message, Object... messageInsets) {
		StringBuilder result = new StringBuilder();
		
		result.append(_prefix);
		result.append(": ");
		
		formatInsets(result, message, messageInsets);
		
		return result.toString();
	}


	private void formatInsets(StringBuilder builder, String message, Object... messageInsets) {
		String[] parts = message.split("\\{\\}");
		int i = 0;
		while (true) {
			if (i == parts.length) break;
			builder.append(parts[i]);
		
			if (i == messageInsets.length) break;
			builder.append(messageInsets[i]);
			i++;
		}
	}

	
	private int count(String prefix) {
		int result = 0;
		for (String existing : _allPrefixes)
			if (existing.equals(prefix)) result++;
		return result;
	}


	
}
