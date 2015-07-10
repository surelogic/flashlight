package com.surelogic._flashlight;

/**
 * Represents a command for use by the Flashlight console. All implementors of
 * this class are expected to be thread-safe.
 */
public interface ConsoleCommand {
  /**
   * The name of this command. This is for descriptive purposes only. Invalid
   * names:
   * <ul>
   * <li><code>stop</code>,
   * <li><code>exit</code>.
   * 
   * @return
   */
  String getDescription();

  /**
   * 
   * @param nextLine
   * @return <code>null</code> if the command was not handled, an informative
   *         message otherwise
   */
  String handle(String command);

}
