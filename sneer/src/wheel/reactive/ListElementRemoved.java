package wheel.reactive;

import wheel.reactive.ListSignal.ListValueChange;
import wheel.reactive.ListSignal.ListValueChangeVisitor;

public class ListElementRemoved<VO> implements ListValueChange<VO> {

	private final int _index;
	private final VO _element;

	public ListElementRemoved(int index, VO element) {
		_index = index;
		_element = element;
	}

	public void accept(ListValueChangeVisitor<VO> visitor) {
		visitor.elementRemoved(_index, _element);
	}

	@Override
	public String toString() {
		return "List element removed at " + _index;
	}
	
}
