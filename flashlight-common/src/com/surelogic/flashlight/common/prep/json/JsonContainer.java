package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

abstract class JsonContainer<T> {
  abstract void addVal(String def, JValue val);

  abstract T builder();

  public T bool(final String def, final boolean val) {
    addVal(def, new JBool(val));
    return builder();
  }

  public JObject object(final String name) {
    JObject o = new JObject();
    addVal(name, o);
    return o;
  }

  public T object(final String name, final Object... params) {
    JObject o = new JObject();
    addVal(name, o);
    if (params.length % 2 == 1) {
      throw new IllegalArgumentException(String.format("Must specify an even number of parameters: %s", params.toString()));
    }
    for (int i = 0; i < params.length; i += 2) {
      o.addVal((String) params[i], coerce(params[i + 1]));
    }
    return builder();
  }

  public JArray array(final String name) {
    JArray a = new JArray();
    addVal(name, a);
    return a;
  }

  public JArray array(final String name, final Object... objects) {
    JArray a = new JArray();
    addVal(name, a);
    for (Object o : objects) {
      a.val(o);
    }
    return a;
  }

  public T string(final String name, final String string) {
    JString s = new JString(string);
    addVal(name, s);
    return builder();
  }

  public T literal(final String name, final String string) {
    JLiteral l = new JLiteral(string);
    addVal(name, l);
    return builder();
  }

  public T val(final String name, final Object val) {
    addVal(name, coerce(val));
    return builder();
  }

  static JValue coerce(final Object object) {
    if (object == null) {
      return new JNull();
    } else if (object instanceof String) {
      return new JString((String) object);
    } else if (object instanceof Boolean) {
      return new JBool((Boolean) object);
    } else if (object.getClass().isArray()) {
      JArray arr = new JArray();
      for (Object obj : (Object[]) object) {
        arr.val(obj);
      }
      return arr;
    } else if (object instanceof Collection) {
      JArray arr = new JArray();
      for (Object obj : (Collection<?>) object) {
        arr.val(obj);
      }
      return arr;
    } else if (object instanceof Map) {
      JObject obj = new JObject();
      for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet()) {
        obj.addVal((String) e.getKey(), coerce(e.getValue()));
      }
    } else if (object instanceof Number) {
      return new JNumber((Number) object);
    }
    throw new IllegalArgumentException(String.format("This object is not directly convertible to JSON: %s", object));
  }

  static <T extends Appendable> T tabs(final T builder, final int n) throws IOException {
    for (int i = 0; i < n; i++) {
      builder.append('\t');
    }
    return builder;
  }
}
