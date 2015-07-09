package com.surelogic._flashlight;

import junit.framework.TestCase;

public class TestEntities extends TestCase {

  public void test() {
    Entities e = new Entities();
    e.defineStandardXML();
    assertEquals("&amp;", e.escape("&"));
    assertEquals("&amp;&amp;", e.escape("&&"));
    assertEquals("&apos;", e.escape("'"));
    assertEquals("&apos;&apos;", e.escape("''"));
    assertEquals("&gt;", e.escape(">"));
    assertEquals("&gt;&gt;", e.escape(">>"));
    assertEquals("&lt;", e.escape("<"));
    assertEquals("&lt;&lt;", e.escape("<<"));
    assertEquals("&quot;", e.escape("\""));
    assertEquals("&quot;I can&apos;t do &lt;it&gt;&quot;", e.escape("\"I can't do <it>\""));
  }

}
