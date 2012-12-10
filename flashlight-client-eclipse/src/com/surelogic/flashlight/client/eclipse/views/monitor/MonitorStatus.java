package com.surelogic.flashlight.client.eclipse.views.monitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.surelogic._flashlight.common.FieldDef;
import com.surelogic._flashlight.common.FieldDefs;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.flashlight.client.eclipse.model.RunManager;

public class MonitorStatus {
    private final String runId;
    private final File runDirectory;
    private final FieldDefs fields;
    private final Map<String, FieldDef> fieldMap;
    private final Set<String> shared;
    private final Set<String> unshared;
    private final Set<String> races;
    private final Set<String> activeProtected;
    private final Map<String, String> props;
    private final Set<String> deadlocks;

    private final Set<List<String>> edges;

    private final Set<String> alerts;

    private String listing;

    private final File portFile;

    private ConnectionState state;
    private int timeout;

    public MonitorStatus(final String runId) {
        this.runId = runId;
        runDirectory = RunManager.getInstance().getDirectoryFrom(runId);
        portFile = new File(runDirectory,
                InstrumentationConstants.FL_PORT_FILE_LOC);
        File fieldsFile = new File(runDirectory,
                InstrumentationConstants.FL_FIELDS_FILE_LOC);
        try {
            fields = new FieldDefs(fieldsFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        fieldMap = new TreeMap<String, FieldDef>();
        for (FieldDef def : fields.values()) {
            fieldMap.put(def.getQualifiedFieldName(), def);
        }
        shared = new HashSet<String>();
        unshared = new HashSet<String>();
        races = new HashSet<String>();
        activeProtected = new HashSet<String>();
        deadlocks = new HashSet<String>();
        edges = new HashSet<List<String>>();
        alerts = new HashSet<String>();
        props = new HashMap<String, String>();
        state = ConnectionState.SEARCHING;
    }

    public MonitorStatus(final MonitorStatus status) {
        if (status != null) {
            runId = status.runId;
            runDirectory = status.runDirectory;
            fields = status.fields;
            fieldMap = status.fieldMap;
            shared = new HashSet<String>(status.shared);
            unshared = new HashSet<String>(status.unshared);
            races = new HashSet<String>(status.races);
            activeProtected = new HashSet<String>(status.activeProtected);
            props = new HashMap<String, String>(status.props);
            deadlocks = new HashSet<String>(status.deadlocks);
            edges = new HashSet<List<String>>(status.edges);
            alerts = new HashSet<String>(status.alerts);
            listing = status.listing;
            portFile = status.portFile;
            state = status.state;
            timeout = status.timeout;
        } else {
            throw new IllegalArgumentException("status may not be null");
        }
    }

    public ConnectionState getState() {
        return state;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void setState(final ConnectionState state) {
        this.state = state;
    }

    public File getRunDirectory() {
        return runDirectory;
    }

    public String getRunId() {
        return runId;
    }

    public File getPortFile() {
        return portFile;
    }

    public Set<String> getShared() {
        return shared;
    }

    public Set<String> getUnshared() {
        return unshared;
    }

    public Set<String> getRaces() {
        return races;
    }

    public Set<String> getActivelyProtected() {
        return activeProtected;
    }

    public List<FieldStatus> getFields() {
        List<FieldStatus> list = new ArrayList<FieldStatus>();
        for (FieldDef def : fieldMap.values()) {
            String name = def.getQualifiedFieldName();
            list.add(new FieldStatus(def, shared.contains(name), unshared
                    .contains(name), races.contains(name), activeProtected
                    .contains(name), alerts.contains(name)));
        }
        return list;
    }

    public Set<List<String>> getEdges() {
        return edges;
    }

    public Set<String> getDeadlocks() {
        return deadlocks;
    }

    public List<LockStatus> getLocks() {
        final Map<String, LockStatus> locks = new HashMap<String, MonitorStatus.LockStatus>();
        for (List<String> edge : getEdges()) {
            List<LockStatus> newEdge = new ArrayList<MonitorStatus.LockStatus>(
                    edge.size());
            for (String lock : edge) {
                LockStatus status = locks.get(lock);
                if (status == null) {
                    status = new LockStatus(deadlocks.contains(lock), lock);
                    locks.put(lock, status);
                }
                newEdge.add(status);
                status.getEdges().add(newEdge);
            }
        }
        return new ArrayList<LockStatus>(locks.values());
    }

    public Set<String> getAlerts() {
        return alerts;
    }

    public String getListing() {
        return listing;
    }

    public void setListing(final String listing) {
        this.listing = listing;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public String getProperty(final String property) {
        return props.get(property);
    }

    public String setProperty(final String property, final String value) {
        return props.put(property, value);
    }

    public enum ConnectionState {
        SEARCHING, CONNECTED, TERMINATED, NOTFOUND
    }

    static class FieldStatus {
        private final FieldDef field;
        private final boolean shared;
        private final boolean unshared;
        private final boolean dataRace;
        private final boolean activelyProtected;
        private final boolean edtAlert;

        private FieldStatus(final FieldDef field, final boolean shared,
                final boolean unshared, final boolean dataRace,
                final boolean activelyProtected, final boolean edtAlert) {
            this.field = field;
            this.shared = shared;
            this.unshared = unshared;
            this.dataRace = dataRace;
            this.activelyProtected = activelyProtected;
            this.edtAlert = edtAlert;
        }

        public boolean isShared() {
            return shared;
        }

        public boolean isUnshared() {
            return unshared;
        }

        public boolean hasDataRace() {
            return dataRace;
        }

        public boolean isActivelyProtected() {
            return activelyProtected;
        }

        public long getId() {
            return field.getId();
        }

        public String getClazz() {
            return field.getClazz();
        }

        public String getField() {
            return field.getField();
        }

        public String getQualifiedFieldName() {
            return field.getQualifiedFieldName();
        }

        public boolean isStatic() {
            return field.isStatic();
        }

        public boolean isFinal() {
            return field.isFinal();
        }

        public boolean isVolatile() {
            return field.isVolatile();
        }

        public boolean isEDTAlert() {
            return edtAlert;
        }

    }

    static class LockStatus {
        private final boolean deadlock;
        private final String name;
        private final Set<List<LockStatus>> edges;

        public LockStatus(final boolean deadlock, final String name) {
            this.deadlock = deadlock;
            this.name = name;
            edges = new HashSet<List<LockStatus>>();
        }

        public String getName() {
            return name;
        }

        public Set<List<LockStatus>> getEdges() {
            return edges;
        }

        public boolean isDeadlocked() {
            return deadlock;
        }
    }
}
