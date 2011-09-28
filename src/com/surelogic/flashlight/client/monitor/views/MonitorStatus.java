package com.surelogic.flashlight.client.monitor.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private String listing;
    private final int port;

    private ConnectionState state;

    public MonitorStatus(final String runName, final String runTime,
            final File fieldsFile, final int port) {
        this.runName = runName;
        this.runTime = runTime;
        this.port = port;
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
    }

    public MonitorStatus(final MonitorStatus status) {
        if (status != null) {
            this.runName = status.runName;
            this.runTime = status.runTime;
            this.fields = status.fields;
            this.port = status.port;
            this.shared = new HashSet<String>(status.shared);
            this.unshared = new HashSet<String>(status.unshared);
            this.fieldMap = status.fieldMap;
            this.listing = status.listing;
        } else {
            throw new IllegalArgumentException("status may not be null");
        }
        // TODO Make sure that we have everything that we need here.
    }

    public ConnectionState getState() {
        return state;
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

    public int getPort() {
        return port;
    }

    public Set<String> getShared() {
        return shared;
    }

    public Set<String> getUnshared() {
        return unshared;
    }

    public List<FieldStatus> getFields() {
        List<FieldStatus> list = new ArrayList<FieldStatus>();
        for (FieldDef def : fieldMap.values()) {
            String name = def.getQualifiedFieldName();
            list.add(new FieldStatus(def, shared.contains(name), unshared
                    .contains(name)));
        }
        return list;
    }

    public String getListing() {
        return listing;
    }

    public void setListing(final String listing) {
        this.listing = listing;
    }

    enum ConnectionState {
        SEARCHING, CONNECTED, TERMINATED, NOTFOUND
    }

    static class FieldStatus {
        private final FieldDef field;
        private final boolean shared;
        private final boolean unshared;

        private FieldStatus(final FieldDef field, final boolean shared,
                final boolean unshared) {
            this.field = field;
            this.shared = shared;
            this.unshared = unshared;
        }

        public boolean isShared() {
            return shared;
        }

        public boolean isUnshared() {
            return unshared;
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

    }
}
