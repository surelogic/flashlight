package com.surelogic.flashlight.prep.events;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.prep.PrepEvent;

public interface EventBuilder {

    Event getEvent(PrepEvent type, PreppedAttributes pa);
}
