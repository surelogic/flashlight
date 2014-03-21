package com.surelogic.flashlight.prep.events;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.IAttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.prep.AbstractDataScan;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class SaxElemHandler extends AbstractDataScan {

    private final EventHandler handler;
    private final EventBuilder builder;

    SaxElemHandler(EventHandler handler, EventBuilder builder) {
        super(null);
        this.handler = handler;
        this.builder = builder;

    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        PrepEvent event = PrepEvent.getEvent(qName);
        Event elem = builder.getEvent(event, preprocessAttributes(attributes));
        if (elem != null) {
            handler.handle(elem);
        }
    }

    protected PreppedAttributes preprocessAttributes(final Attributes a) {
        // System.err.println("Got "+e.getLabel());
        final PreppedAttributes attrs;
        if (a != null) {
            final int size = a.getLength();
            attrs = new PreppedAttributes();

            for (int i = 0; i < size; i++) {
                final String name = a.getQName(i);
                final String value = a.getValue(i);
                final IAttributeType key = PreppedAttributes.mapAttr(name);
                attrs.put(key, value);
            }
            return attrs;
        }
        return null;
    }
}
