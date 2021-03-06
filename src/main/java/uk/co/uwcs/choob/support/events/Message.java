/**
 *
 * @author Horrible Perl Script. Ewwww.
 */

package uk.co.uwcs.choob.support.events;

public class Message extends IRCEvent implements MessageEvent, ContextEvent, UserEvent, AimedEvent
{
	/**
	 * message
	 */
	private final String message;

	/**
	 * Get the value of message
	 * @return The value of message
	 */
	@Override public String getMessage() {
		 return message;
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
	 * target
	 */
	private final String target;

	/**
	 * Get the value of target
	 * @return The value of target
	 */
	@Override public String getTarget() {
		 return target;
	}

	/**
	 * Get the reply context in which this event resides
	 * @return The context
	 */
	@Override public String getContext() {
		return getNick();
	}


	/**
	 * Construct a new Message.
	 */
	public Message(final String methodName, final long millis, final int random, final String message, final String nick, final String login, final String hostname, final String target)
	{
		super(methodName, millis, random);
		this.message = message;
		this.nick = nick;
		this.login = login;
		this.hostname = hostname;
		this.target = target;
	}

	/**
	 * Synthesize a new Message from an old one.
	 */
	public Message(final Message old, final String message)
	{
		super(old);
		this.message = message;
		this.nick = old.nick;
		this.login = old.login;
		this.hostname = old.hostname;
		this.target = old.target;
	}

	/**
	 * Synthesize a new Message from this one.
	 * @return The new Message object.
	 */
	public Event cloneEvent(final String message)
	{
		return new Message(this, message);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (obj == null || !(obj instanceof Message))
			return false;
		if ( !super.equals(obj) )
			return false;
		final Message thing = (Message)obj;
		if ( true && message.equals(thing.message) && nick.equals(thing.nick) && login.equals(thing.login) && hostname.equals(thing.hostname) && target.equals(thing.target) )
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		final StringBuffer out = new StringBuffer("Message(");
		out.append(super.toString());
		out.append(", message = " + message);
		out.append(", nick = " + nick);
		out.append(", login = " + login);
		out.append(", hostname = " + hostname);
		out.append(", target = " + target);
		out.append(")");
		return out.toString();
	}

}
