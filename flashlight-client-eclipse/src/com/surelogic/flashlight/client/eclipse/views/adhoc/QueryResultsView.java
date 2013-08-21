package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.adhoc.AdHocQueryMeta;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.adhoc.views.menu.QueryMenuMediator;
import com.surelogic.common.ui.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.flashlight.common.model.EmptyQueriesCache;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class QueryResultsView extends AbstractQueryResultsView {

  @Override
  public AdHocManager getManager() {
    return FlashlightDataSource.getManager();
  }

  @Override
  protected void setupNoResultsPane(Composite parent) {
    final RunDirectory run = FlashlightDataSource.getInstance().getSelectedRun();
    if (run == null || !run.isPrepared()) {
      // no run selected to show an overview of
      super.setupNoResultsPane(parent);
    } else {
      // show overview
      setupOverview(parent, run);
    }
  }

  private final int f_dimension = 64;
  private final Point f_size = new Point(f_dimension, f_dimension);
  private final Image f_javaRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_JAVA_APP, false), f_size);
  private final Image f_androidRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_ANDROID_APP, false), f_size);

  private Image getImage(@NonNull final RunDirectory run) {
    return run.isAndroid() ? f_androidRunning : f_javaRunning;
  }

  private void setupOverview(@NonNull final Composite parent, @NonNull final RunDirectory run) {
    final Composite panel = new Composite(parent, SWT.NONE);
    panel.setLayout(new GridLayout());
    {
      final Composite runId = new Composite(panel, SWT.NONE);
      runId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      runId.setLayout(new GridLayout(2, false));
      // runId.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN));
      Label image = new Label(runId, SWT.NONE);
      image.setImage(getImage(run));
      image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 2));
      Label nameLabel = new Label(runId, SWT.NONE);
      nameLabel.setText(run.getDescription().getName());
      nameLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
      nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
      Label dirLabel = new Label(runId, SWT.NONE);
      dirLabel.setText(run.getDirectory().getAbsolutePath());
      dirLabel.setForeground(EclipseColorUtility.getSubtleTextColor());
      dirLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
    }

    final Composite newsPanel = new Composite(panel, SWT.NONE);
    newsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    final GridLayout newsPanelLayout = new GridLayout(2, true);
    newsPanelLayout.horizontalSpacing = newsPanelLayout.verticalSpacing = 2;
    newsPanelLayout.marginWidth = newsPanelLayout.marginHeight = 2;
    newsPanel.setLayout(newsPanelLayout);
    newsPanel.setBackground(EclipseColorUtility.getSubtleTextColor());

    final Composite goodNews = new Composite(newsPanel, SWT.NONE);
    goodNews.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    goodNews.setLayout(new GridLayout());
    goodNews.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    final Label goodNewsLabel = new Label(goodNews, SWT.CENTER);
    goodNewsLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    goodNewsLabel.setText("Good News");
    goodNewsLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
    goodNewsLabel.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
    goodNewsLabel.setForeground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    final Composite badNews = new Composite(newsPanel, SWT.NONE);
    badNews.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    badNews.setLayout(new GridLayout());
    badNews.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    final Label badNewsLabel = new Label(badNews, SWT.CENTER);
    badNewsLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    badNewsLabel.setText("Bad News");
    badNewsLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
    badNewsLabel.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
    badNewsLabel.setForeground(panel.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    setupNewsItems(run, goodNews, badNews);
  }

  private void setupNewsItems(@NonNull final RunDirectory run, @NonNull final Composite goodNews, @NonNull final Composite badNews) {
    List<AdHocQuery> topLevelQueries = getManager().getRootQueryList();
    final EmptyQueriesCache cache = EmptyQueriesCache.getInstance();
    for (final AdHocQuery query : topLevelQueries) {
      final boolean queryIsEmpty = cache.queryResultWillBeEmpty(run, query);

      if (queryIsEmpty) {
        /*
         * No results
         */
        addNewsItem(AdHocQuery.META_FL_EMPTY_IS_GOOD_NEWS_NAME, goodNews, query, false);
        addNewsItem(AdHocQuery.META_FL_EMPTY_IS_BAD_NEWS_NAME, badNews, query, false);
      } else {
        /*
         * Has results
         */
        addNewsItem(AdHocQuery.META_FL_RESULT_IS_GOOD_NEWS_NAME, goodNews, query, true);
        addNewsItem(AdHocQuery.META_FL_RESULT_IS_BAD_NEWS_NAME, badNews, query, true);
      }
    }
  }

  private void addNewsItem(@NonNull String metaName, @NonNull final Composite to, @NonNull final AdHocQuery query,
      boolean useLinkToRunQuery) {
    @Nullable
    AdHocQueryMeta meta = query.getMetaWithName(metaName);
    if (meta != null) {
      final String text = meta.getText().trim();
      if (useLinkToRunQuery) {
        final Link item = new Link(to, SWT.WRAP);
        item.setText("<a href=\"run\">" + text + "</a>");
        item.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if ("run".equals(e.text)) {
              QueryMenuMediator.runQueryInContext(getManager(), query);
            }
          }
        });
      } else {
        final Label item = new Label(to, SWT.WRAP);
        item.setText(text);
      }
    }
  }
}
