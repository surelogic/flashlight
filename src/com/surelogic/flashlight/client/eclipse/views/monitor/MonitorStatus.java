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

public class MonitorStatus {

    private final String runName;
    private final String runTime;
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
    private final File completeFile;

    private ConnectionState state;
    private int timeout;

    public MonitorStatus(final String runName, final String runTime,
            final File fieldsFile, final File portFile, final File completeFile) {
        this.runName = runName;
        this.runTime = runTime;
        this.portFile = portFile;
        try {
            this.fields = new FieldDefs(fieldsFile);
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
        this.completeFile = completeFile;
    }

    public MonitorStatus(final MonitorStatus status) {
        if (status != null) {
            this.runName = status.runName;
            this.runTime = status.runTime;
            this.fields = status.fields;
            this.fieldMap = status.fieldMap;
            this.shared = new HashSet<String>(status.shared);
            this.unshared = new HashSet<String>(status.unshared);
            this.races = new HashSet<String>(status.races);
            this.activeProtected = new HashSet<String>(status.activeProtected);
            this.props = new HashMap<String, String>(status.props);
            this.deadlocks = new HashSet<String>(status.deadlocks);
            this.edges = new HashSet<List<String>>(status.edges);
            this.alerts = new HashSet<String>(status.alerts);
            this.listing = status.listing;
            this.portFile = status.portFile;
            this.completeFile = status.completeFile;
            this.state = status.state;
            this.timeout = status.timeout;
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

    public String getRunName() {
        return runName;
    }

    public String getRunTime() {
        return runTime;
    }

    public File getPortFile() {
        return portFile;
    }

    public File getCompleteFile() {
        return completeFile;
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
            this.edges = new HashSet<List<LockStatus>>();
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
