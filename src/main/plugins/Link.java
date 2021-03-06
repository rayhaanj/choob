import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.ChannelAction;
import uk.co.uwcs.choob.support.events.ChannelMessage;
import uk.co.uwcs.choob.support.events.Message;

/**
 * Choob link plugin
 *
 * @author Tim Retout <tim@retout.co.uk>
 *
 */

class OldLink
{
	public int id;
	public String URL;
	public String poster;
	public String channel;
	public long firstPostedTime;
	public long lastPostedTime;
}

public class Link
{
	public String[] info()
	{
		return new String[] {
			"Plugin which matches on links.",
			"Tim Retout /  Chris Hawley",
			"tim@retout.co.uk /  choob@blood-god.co.uk",
			"$Rev$$Date$"
		};
	}

	Modules mods;

	IRCInterface irc;

	/**
	 * Specifies the minimum time between when the bot last saw the link, and when it starts complaining about it being ooooold.
	 */
	private static final long FLOOD_INTERVAL = 15 * 60 * 1000; // 15 minutes

	public Link(final Modules mods, final IRCInterface irc)
	{
		this.irc = irc;
		this.mods = mods;

		// Purge
		/*
		 * List<OldLink> links = mods.odb.retrieve(OldLink.class, ""); for (OldLink link : links) { mods.odb.delete(link); }
		 */
	}

	/** Exceptions to what can be "olded". */
	final private static Pattern exceptionPattern = Pattern.compile(
		// Regexes are fun! Learn to love regexes.
		"^http://"
		+ "(?:"
		+ "(?:www\\.)?google\\.co(?:\\.uk|m)"				// google
		+ "|"
		+ "(?:[^/]*\\.)?(?:uwcs|warwickcompsoc)\\.co\\.uk"		// compsoc
		+ ")"
	);
	/** Regex to match links. */
	public static String filterLinkRegex = "http://\\S*";

	final private static Pattern linkPattern = Pattern.compile(filterLinkRegex);

	/**
	 * Called for every line that contains a link.
	 *
	 * @param mes The message containing the link.
	 * @param mods The bot modules.
	 * @param irc The irc interface.
	 */
	public void filterLink(final Message mes)
	{
		// Ignore stuff that isn't a channel message or action
		// Ignore synthetic messages
		// Ignore commands
		if (!(mes instanceof ChannelMessage||mes instanceof ChannelAction)
				|| mes.getSynthLevel() > 0
				|| mes.getFlags().containsKey("command") )
			return;

		final Matcher linkMatch = linkPattern.matcher(mes.getMessage());
		final ArrayList<OldLink> oldLinks = new ArrayList<OldLink>();

		// Iterate over links in line.
		while (linkMatch.find())
		{
			final String link = linkMatch.group(0);
			OldLink linkObj = getOldLink(link);
			if (linkObj != null)
			{
				// Flood protection
				if (System.currentTimeMillis() - linkObj.lastPostedTime < FLOOD_INTERVAL)
					continue;

				// Old link: Update the last-posted time.
				linkObj.lastPostedTime = mes.getMillis();
				mods.odb.update(linkObj);

				// Don't "old" reposts from the same user
				if (linkObj.poster.equalsIgnoreCase(mods.nick.getBestPrimaryNick(mes.getNick())))
					continue;

				oldLinks.add(linkObj);
			}
			else
			{
				// New link: Add to database.
				linkObj = new OldLink();
				linkObj.URL = link;
				linkObj.poster = mods.nick.getBestPrimaryNick(mes.getNick());
				linkObj.channel = mes.getContext();
				linkObj.firstPostedTime = mes.getMillis();
				linkObj.lastPostedTime = mes.getMillis();
				mods.odb.save(linkObj);
			}
		}
	}

	/**
	 * Command to manually check whether a link is old.
	 *
	 * @param mes The message delivering the command.
	 */
	public void commandIsOld(final Message mes)
	{
		// Ignore messages in channels.
		if (mes instanceof ChannelMessage)
			return;

		final Matcher linkMatch = linkPattern.matcher(mes.getMessage());

		if (linkMatch.find())
		{
			final String link = linkMatch.group(0);
			final OldLink linkObj = getOldLink(link);
			if (linkObj == null)
				irc.sendContextReply(mes, "Link is not old.");
			else if (System.currentTimeMillis() - linkObj.lastPostedTime < FLOOD_INTERVAL)
				irc.sendContextReply(mes, "Link is old, but posted recently.");
			else if (linkObj.poster.equalsIgnoreCase(mods.nick.getBestPrimaryNick(mes.getNick())))
				irc.sendContextReply(mes, "Link is old, but you posted it.");
			else
				irc.sendContextReply(mes, getOldResponse(linkObj));
		}
		else
			irc.sendContextReply(mes, "Usage: !isold <link>");
	}

	/**
	 * Check the database to see if a particular link is old.
	 *
	 * @param link The URI to check the database for.
	 * @return The link object if present in the database, else null.
	 */
	private OldLink getOldLink(final String link)
	{
		// Ensure that the link isn't in our exceptions list
		if (exceptionPattern.matcher(link).find())
			return null;

		// Check objectdb
		final String queryString = "WHERE URL = \"" + mods.odb.escapeString(link) + "\"";
		final List<OldLink> links = mods.odb.retrieve(OldLink.class, queryString);

		// Return the first result, if any.
		return links.size() > 0 ? links.get(0) : null;
	}

	/**
	 * Get the "ooold" response for an OldLink.
	 *
	 * @param linkObj The olde linke to fetche a response for.
	 * @return A suitably harsh response.
	 */
	private String getOldResponse(final OldLink linkObj)
	{
		final long timeSinceOriginal = System.currentTimeMillis() - linkObj.firstPostedTime;
		final int oldHours = (int) timeSinceOriginal / (60 * 60 * 1000);

		// Represent the number of hours since the original posting,
		// in base 2, using upper and lower case 'o's.
		return "O"
			+ Integer.toBinaryString(oldHours)
				.replaceAll("0", "o")
				.replaceAll("1", "O")
			+ "ld! (link originally posted in " + linkObj.channel
			+ ", " + mods.date.timeLongStamp(timeSinceOriginal)
			+ " ago by " + linkObj.poster + ")";
	}

	public void webListLinks(final PrintWriter out, final String params, final String[] user)
	{
		out.println("HTTP/1.0 200 OK");
		out.println("Content-Type: text/html");
		out.println();

		int timePeriod;

		try
		{
			timePeriod = Integer.parseInt(params);
		}
		catch( final NumberFormatException e )
		{
			out.println("<center><blink><h1>ERROR IN PARAMETER</h1></blink></center>");
			return;
		}

		final long cutOff = System.currentTimeMillis() - timePeriod * 60 * 60 * 1000;

		final String queryString = "SORT DESC INTEGER lastPostedTime WHERE firstPostedTime > " + cutOff;

		final List<OldLink> links = mods.odb.retrieve(OldLink.class, queryString);

		for( final OldLink link : links )
			out.println("<a href=\"" + mods.scrape.readyForHtml(link.URL) + "\">" + mods.scrape.readyForHtml(link.URL)
				+ "</a>&nbsp;-&nbsp;" + mods.scrape.readyForHtml(link.poster) + " in "
				+ mods.scrape.readyForHtml(link.channel) + "<br />");

		out.flush();
	}

	public void webLastLink(final PrintWriter out, final String params, final String[] user)
	{
		out.println("HTTP/1.0 200 OK");
		out.println("Content-Type: text/html");
		out.println();

		final String queryString = "SORT DESC INTEGER lastPostedTime LIMIT (1)";

		final List<OldLink> links = mods.odb.retrieve(OldLink.class, queryString);

		for( final OldLink link : links )
			out.println("<html><head><script>window.location = \"" + mods.scrape.readyForHtml(link.URL) + "\";</script></head><body></body></html>");

		out.println();
		out.flush();
	}
}
