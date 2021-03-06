/**
 *
 * @author Horrible Perl Script. Ewwww.
 */

package uk.co.uwcs.choob.support.events;

public class ChannelJoin extends IRCEvent implements ChannelEvent, ContextEvent, UserEvent
{
	/**
	 * channel
	 */
	private final String channel;

	/**
	 * Get the value of channel
	 * @return The value of channel
	 */
	@Override public String getChannel() {
		 return channel;
	}

	/**
	 * nick
	 */
	private final String nick;

	/**
	 * Get the value of nick
	 * @return The value of nick
	 */
	@Override public String getNick() {
		 return nick;
	}

	/**
	 * login
	 */
	private final String login;

	/**
	 * Get the value of login
	 * @return The value of login
	 */
	@Override public String getLogin() {
		 return login;
	}

	/**
	 * hostname
	 */
	private final String hostname;

	/**
	 * Get the value of hostname
	 * @return The value of hostname
	 */
	@Override public String getHostname() {
		 return hostname;
	}

	/**
	 * Get the reply context in which this event resides
	 * @return The context
	 */
	@Override public String getContext() {
		return getChannel();
	}


	/**
	 * Construct a new ChannelJoin.
	 */
	public ChannelJoin(final String methodName, final long millis, final int random, final String channel, final String nick, final String login, final String hostname)
	{
		super(methodName, millis, random);
		this.channel = channel;
		this.nick = nick;
		this.login = login;
		this.hostname = hostname;
	}

	/**
	 * Synthesize a new ChannelJoin from an old one.
	 */
	public ChannelJoin(final ChannelJoin old)
	{
		super(old);
		this.channel = old.channel;
		this.nick = old.nick;
		this.login = old.login;
		this.hostname = old.hostname;
	}

	/**
	 * Synthesize a new ChannelJoin from this one.
	 * @return The new ChannelJoin object.
	 */
	@Override
	public Event cloneEvent()
	{
		return new ChannelJoin(this);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (obj == null || !(obj instanceof ChannelJoin))
			return false;
		if ( !super.equals(obj) )
			return false;
		final ChannelJoin thing = (ChannelJoin)obj;
		if ( true && channel.equals(thing.channel) && nick.equals(thing.nick) && login.equals(thing.login) && hostname.equals(thing.hostname) )
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		final StringBuffer out = new StringBuffer("ChannelJoin(");
		out.append(super.toString());
		out.append(", channel = " + channel);
		out.append(", nick = " + nick);
		out.append(", login = " + login);
		out.append(", hostname = " + hostname);
		out.append(")");
		return out.toString();
	}

}
