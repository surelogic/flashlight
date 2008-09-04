package com.surelogic._flashlight.rewriter;

public abstract class AbstractIndentingMessager implements EngineMessenger {
  private final String indent;
  private int level = 0;
  
  protected AbstractIndentingMessager(final String prefix) {
    indent = prefix;
  }
  
  protected AbstractIndentingMessager() {
    this("  ");
  }

  public final void increaseNesting() {
    level += 1;
  }
  
  public final void decreaseNesting() {
    level -= 1;
  }
  
  protected final String indentMessage(final String message) {
    if (level == 0) {
      return message;
    } else {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < level; i++) sb.append(indent);
      sb.append(message);
      return sb.toString();
    }
  }
}
