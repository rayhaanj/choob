/**
 *
 * @author Horrible Perl Script. Ewwww.
 */

package uk.co.uwcs.choob.support.events;

import java.util.HashMap;
import java.util.Map;

public class IRCEvent extends Event implements IRCRootEvent
{
	/**
	 * millis
	 */
	private final long millis;

	/**
	 * Get the value of millis
	 * @return The value of millis
	 */
	@Override public long getMillis() {
		 return millis;
	}

	/**
	 * random
	 */
	private final int random;

	/**
	 * Get the value of random
	 * @return The value of random
	 */
	@Override public int getRandom() {
		 return random;
	}

	/**
	 * synthLevel
	 */
	private final int synthLevel;

	/**
	 * Get the value of synthLevel
	 * @return The value of synthLevel
	 */
	public int getSynthLevel() {
		 return synthLevel;
	}

	/**
	 * flags
	 */
	private final Map<String,String> flags;

	/**
	 * Get the value of flags
	 * @return The value of flags
	 */
	public Map<String,String> getFlags() {
		 return flags;
	}


	/**
	 * Construct a new IRCEvent.
	 */
	public IRCEvent(final String methodName, final long millis, final int random)
	{
		super(methodName);
		this.millis = millis;
		this.random = random;
		java.security.AccessController.checkPermission(new uk.co.uwcs.choob.support.ChoobPermission("event.create"));
		this.synthLevel = 0;
		this.flags = new HashMap<String,String>();
	}

	/**
	 * Synthesize a new IRCEvent from an old one.
	 */
	public IRCEvent(final IRCEvent old)
	{
		super(old);
		this.millis = old.millis;
		this.random = old.random;
		java.security.AccessController.checkPermission(new uk.co.uwcs.choob.support.ChoobPermission("event.create"));
		this.synthLevel = old.synthLevel + 1;
		this.flags = new HashMap<String,String>();
		// Properties starting "_" should not be cloned implicitly.
		for (final String prop : old.flags.keySet()) {
			if (!prop.startsWith("_"))
				this.flags.put(prop, new String(old.flags.get(prop)));
		}
	}

	/**
	 * Synthesize a new IRCEvent from this one.
	 * @return The new IRCEvent object.
	 */
	@Override
	public Event cloneEvent()
	{
		return new IRCEvent(this);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (obj == null || !(obj instanceof IRCEvent))
			return false;
		if ( !super.equals(obj) )
			return false;
		final IRCEvent thing = (IRCEvent)obj;
		if ( true && millis == thing.millis && random == thing.random && synthLevel == thing.synthLevel && flags == thing.flags )
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		final StringBuffer out = new StringBuffer("IRCEvent(");
		out.append(super.toString());
		out.append(", millis = " + millis);
		out.append(", random = " + random);
		out.append(", synthLevel = " + synthLevel);
		out.append(", flags = " + flags);
		out.append(")");
		return out.toString();
	}

}
