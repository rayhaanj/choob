/*
 * NickModule.java
 *
 * Created on August 1, 2005, 01:26 PM
 */

package uk.co.uwcs.choob.modules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper functions for dealing with users' nicks.
 */

public final class NickModule
{
	static final Pattern jbNickPattern = Pattern.compile("^([a-zA-Z0-9_-]+?)(?:\\||\\`).*$");
	static final Pattern irssiNickPattern = Pattern.compile("^_*([a-zA-Z0-9_-]+?)_*$");

	/**
	 * Guesses the user's primary nick.
	 * @param nick String representing the Nick.
	 * @return User's primary nick, a guess at their primary nick, or the supplied nick.
	 */
	public String getBestPrimaryNick(final String nick)
	{
		Matcher ma = jbNickPattern.matcher(nick);

		if (ma.matches())
			return ma.group(1);

		ma=irssiNickPattern.matcher(nick);

		if (ma.matches())
			return ma.group(1);

		return nick;
	}
}
