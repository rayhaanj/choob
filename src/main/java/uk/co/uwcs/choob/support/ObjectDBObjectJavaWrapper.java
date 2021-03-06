package uk.co.uwcs.choob.support;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.*;
import java.util.LinkedList;
import java.util.List;

public final class ObjectDBObjectJavaWrapper implements ObjectDBObject {
	private Object obj;

	public ObjectDBObjectJavaWrapper(Object obj) {
		this.obj = obj;
		getId();
	}

	@Override public String getClassName() {
		return obj.getClass().getName();
	}

	@Override public ObjectDBClass getODBClass() {
		return new ObjectDBClassJavaWrapper(getClass());
	}

	@Override public int getId() {
		try {
			final Object obj2 = obj;
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Integer>() {
					@Override public Integer run() throws NoSuchFieldException, IllegalAccessException {
						Field f = obj2.getClass().getField("id");
						return f.getInt(obj2);
					}
				});
		} catch (PrivilegedActionException e) {
			// Must be a NoSuchFieldException...
			throw new ObjectDBError("Class " + obj.getClass() + " does not have a unique 'id' property. Please add one.");
		}
	}

	@Override public void setId(int id) {
		try {
			final Object obj2 = obj;
			final int val2 = id;
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override public Object run() throws NoSuchFieldException, IllegalAccessException {
						Field f = obj2.getClass().getField("id");
						f.setInt(obj2, val2);
						return null;
					}
				});
		} catch(PrivilegedActionException e) {
			// Must be a NoSuchFieldException...
			throw new ObjectDBError("Class " + obj.getClass() + " does not have a unique 'id' property. Please add one.");
		}
	}

	@Override public String[] getFields() {
		List<String> fields = new LinkedList<String>();
		Field[] fieldObjs = obj.getClass().getFields();

		for (int i = 0; i < fieldObjs.length; i++) {
			fields.add(fieldObjs[i].getName());
		}

		String[] sFields = new String[0];
		return fields.toArray(sFields);
	}

	@Override public Type getFieldType(String name) throws NoSuchFieldException {
		return obj.getClass().getField(name).getType();
	}

	@Override public Object getFieldValue(String name) throws NoSuchFieldException, IllegalAccessException {
		return obj.getClass().getField(name).get(obj);
	}

	@Override public void setFieldValue(String name, Object value) throws NoSuchFieldException, IllegalAccessException {
		obj.getClass().getField(name).set(obj, value);
	}
}
