package com.surelogic._flashlight.rewriter.engine;

public abstract class AbstractIndentingMessager implements EngineMessenger {
  private final String indent;
  
  protected AbstractIndentingMessager(final String prefix) {
    indent = prefix;
  }
  
  protected AbstractIndentingMessager() {
    this("  ");
  }
  
  protected final String indentMessage(final int nesting, final String message) {
    if (nesting == 0) {
      return message;
    } else {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < nesting; i++) sb.append(indent);
      sb.append(message);
      return sb.toString();
    }
  }
}
