package sneer.bricks.hardware.cpu.utils.consumers.parsers.integer.impl;

import sneer.bricks.hardware.cpu.exceptions.IllegalParameter;
import sneer.bricks.hardware.cpu.lang.PickyConsumer;

class IntegerParserImpl implements PickyConsumer<String> {

	private PickyConsumer<Integer> _endConsumer;

	IntegerParserImpl(PickyConsumer<Integer> endConsumer) {
		_endConsumer = endConsumer;
	}

	@Override
	public void consume(String string) throws IllegalParameter {
		Integer result;

		try {
			result = Integer.valueOf(string);

		} catch (NumberFormatException e) {
			throw new IllegalParameter(string + " is not a valid number.");
		}

		_endConsumer.consume(result);
	}
}