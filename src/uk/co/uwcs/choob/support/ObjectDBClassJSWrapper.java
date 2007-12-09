package uk.co.uwcs.choob.support;

import org.mozilla.javascript.*;

public final class ObjectDBClassJSWrapper implements ObjectDBClass {
	private Function cls;
	
	public ObjectDBClassJSWrapper(Object obj) {
		if (!(obj instanceof Function)) {
			throw new RuntimeException("Trying to wrap a non-function type as a class!");
		}
		this.cls = (Function)obj;
	}
	
	public String getName() {
		try {
			try {
				String ctorName = (String)JSUtils.getProperty((Scriptable)cls, "name");
				Scriptable scope = ((Scriptable)cls).getParentScope();
				
				// Get plugin name from scope (HACK)!
				String plugName = "<error>";
				while (scope != null) {
					try {
						plugName = (String)JSUtils.getProperty(scope, "__jsplugman_pluginName");
						scope = null;
					} catch (NoSuchFieldException e) {
						scope = scope.getParentScope();
					}
				}
				
				return "plugins." + plugName + "." + ctorName;
			} catch (NoSuchFieldException e) {
				// Do nothing.
			}
		} finally {
			Context.exit();
		}
		return "";
	}
	
	public Object newInstance() {
		Context cx = Context.enter();
		try {
			Scriptable scope = cls.getParentScope();
			return cls.construct(cx, scope, new Object[0]);
		} finally {
			Context.exit();
		}
	}
}