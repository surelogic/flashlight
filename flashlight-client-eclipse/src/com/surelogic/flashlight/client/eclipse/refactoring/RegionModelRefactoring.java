package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.refactor.PromisesAnnotationRewriter;
import com.surelogic.flashlight.common.model.RunDirectory;
import com.surelogic.flashlight.recommend.RecommendRegions;
import com.surelogic.flashlight.recommend.RecommendedRegion;

public class RegionModelRefactoring extends Refactoring {
    private static final Logger LOG = SLLogger
            .getLoggerFor(RegionModelRefactoring.class);
    private final RegionRefactoringInfo info;
    private Map<String, Map<String, RecommendedRegion>> targetRegions;
    private Map<String, Map<String, RecommendedRegion>> targetClassRegions;
    private Map<String, Map<String, List<RecommendedRegion>>> targetFieldRegions;

    public RegionModelRefactoring(final RegionRefactoringInfo info) {
        this.info = info;
    }

    @Override
    public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        targetRegions = new HashMap<>();
        targetClassRegions = new HashMap<>();
        targetFieldRegions = new HashMap<>();
        for (final RunDirectory desc : info.getSelectedRuns()) {
            final DBConnection conn = desc.getDB();
            try {
                conn.bootAndCheckSchema();
                mergeRegions(targetRegions,
                        conn.withReadOnly(RecommendRegions.lockIsThisRegions()));
                mergeRegions(
                        targetClassRegions,
                        conn.withReadOnly(RecommendRegions.lockIsClassRegions()));
                for (final RecommendedRegion r : conn
                        .withReadOnly(RecommendRegions
                                .recommendedFieldLockRegions())) {
                    Map<String, List<RecommendedRegion>> map = targetFieldRegions
                            .get(r.getPackage());
                    if (map == null) {
                        map = new HashMap<>();
                        targetFieldRegions.put(r.getPackage(), map);
                    }
                    List<RecommendedRegion> list = map.get(r.getClazz());
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(r.getClazz(), list);
                    }
                    list.add(r);
                }
            } catch (final Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
                return RefactoringStatus.createErrorStatus(e.getMessage());
            }
        }
        return RefactoringStatus.create(Status.OK_STATUS);
    }

    /**
     * Utility method used to merge the results of two different runs into a
     * unified set of recommended regions
     * 
     * @param regions
     * @param two
     * @return
     */
    private static void mergeRegions(
            final Map<String, Map<String, RecommendedRegion>> regions,
            final List<RecommendedRegion> list) {
        for (final RecommendedRegion r : list) {
            Map<String, RecommendedRegion> packageRegion = regions.get(r
                    .getPackage());
            if (packageRegion == null) {
                packageRegion = new HashMap<>();
                regions.put(r.getPackage(), packageRegion);
            }
            final RecommendedRegion clazzRegion = packageRegion.get(r
                    .getClazz());
            if (clazzRegion == null) {
                packageRegion.put(r.getClazz(), r);
            } else {
                clazzRegion.addFields(r.getFields());
            }
        }
    }

    @Override
    public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        // TODO Quick checks
        return RefactoringStatus.create(Status.OK_STATUS);
    }

    @Override
    public Change createChange(final IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        final CompositeChange root = new CompositeChange(String.format(
                "Changes to %s", info.getSelectedProject().getProject()
                        .getName()));
        final TextEditGroup g = new TextEditGroup(
                "Adding @RegionLock annotations");
        for (final IPackageFragment fragment : info.getSelectedProject()
                .getPackageFragments()) {
            final String packageName = fragment.getElementName();
            final Map<String, RecommendedRegion> targetMap = targetRegions
                    .get(packageName);
            final Map<String, RecommendedRegion> targetClassMap = targetClassRegions
                    .get(packageName);
            final Map<String, List<RecommendedRegion>> targetFieldMap = targetFieldRegions
                    .get(packageName);
            final PromisesAnnotationRewriter rewrite = new PromisesAnnotationRewriter();

            for (final ICompilationUnit cu : fragment.getCompilationUnits()) {
                final RegionModel model = new RegionModel();
                for (final IType type : cu.getTypes()) {
                    final String typeName = type.getElementName();
                    model.mergeRegion(targetMap == null ? null : targetMap
                            .get(typeName));
                    model.mergeRegion(targetClassMap == null ? null
                            : targetClassMap.get(typeName));
                    if (targetFieldMap != null) {
                        final List<RecommendedRegion> fieldRegions = targetFieldMap
                                .get(typeName);
                        if (fieldRegions != null) {
                            for (final RecommendedRegion r : fieldRegions) {
                                model.mergeRegion(r);
                            }
                        }
                    }
                }
                if (!model.isEmpty()) {
                    final Map<String, TypeNode> typeMap = TypeReconciler
                            .createTypeMap(cu);
                    final NamingScheme ns = NamingScheme.DEFAULT;
                    rewrite.setCompilationUnit(cu);
                    rewrite.writeAnnotations(
                            model.generateAnnotations(typeMap, ns), g);
                    final TextEdit edit = rewrite.getTextEdit();
                    final IFile file = (IFile) cu.getResource();
                    final TextFileChange change = new TextFileChange(
                            file.getName(), file);
                    change.setEdit(edit);
                    root.add(change);
                }
            }
        }

        return root;
    }

    @Override
    public String getName() {
        return I18N.msg("flashlight.recommend.refactor.regionIsThis");
    }

}
