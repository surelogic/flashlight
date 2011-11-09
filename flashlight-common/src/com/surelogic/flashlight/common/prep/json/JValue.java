package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

/**
 * Represents a JSON value of some sort. All implementors of this interface are
 * expected to implement {@link #toString()} as well as
 * {@link #append(StringBuilder, int)}.
 * 
 * @author nathan
 * 
 */
public interface JValue {
	/**
	 * Append the value to the given {@link StringBuilder}. Depth is a hint as
	 * to how far the value should be tabbed over. Implementors of append do not
	 * need to add tabs to the first line of their value, but should take the
	 * hint into account for any additional lines needed to display the value.
	 * 
	 * @param builder
	 * @param depth
	 */
	void append(Appendable builder, int depth) throws IOException;

}
