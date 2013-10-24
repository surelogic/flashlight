package com.surelogic.flashlight.common.prep;

class LockNode {
    final long id;
    final LockType type;

    LockNode(long id, LockType type) {
        super();
        this.id = id;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public LockType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ id >>> 32);
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LockNode other = (LockNode) obj;
        if (id != other.id) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

}