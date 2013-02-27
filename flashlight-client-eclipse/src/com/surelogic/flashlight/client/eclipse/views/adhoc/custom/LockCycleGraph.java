package com.surelogic.flashlight.client.eclipse.views.adhoc.custom;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.ui.adhoc.AbstractQueryResultCustomDisplay;

public final class LockCycleGraph extends AbstractQueryResultCustomDisplay {

  @Override
  public void init() {
    System.out.println("init() called");
  }

  @Override
  public void dispose() {
    System.out.println("dispose() called");
  }

  @Override
  protected void displayResult(Composite panel) {
    Button b = new Button(panel, SWT.PUSH);
    b.setText("Run a query");
    b.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        System.out.println("button pushed");
        AdHocQuery sub = getResult().getManager().getTopLevelQueries().iterator().next();
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("test1", "a value");
        extra.put("test3", "a 3rd value");

        scheduleQuery(sub, extra);
      }
    });
  }
}
