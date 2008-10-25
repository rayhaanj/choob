import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.Colors;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.ChoobException;
import uk.co.uwcs.choob.support.ChoobNoSuchCallException;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.ChannelJoin;
import uk.co.uwcs.choob.support.events.ChannelKick;
import uk.co.uwcs.choob.support.events.ChannelPart;
import uk.co.uwcs.choob.support.events.Message;
import uk.co.uwcs.choob.support.events.NickChange;
import uk.co.uwcs.choob.support.events.PrivateNotice;
import uk.co.uwcs.choob.support.events.QuitEvent;
import uk.co.uwcs.choob.support.events.ServerResponse;


/**
 * Choob nickserv checker
 *
 * @author bucko
 *
 * Anyone who needs further docs for this module has some serious Java issues.
 * :)
 */

// Holds the NickServ result
class ResultObj
{
	int result;
	long time;
}

public class NickServ
{
	public String[] info()
	{
		return new String[] {
			"NickServ checker plugin.",
			"The Choob Team",
			"choob@uwcs.co.uk",
			"$Rev$$Date$"
		};
	}

	private boolean ip_overrides=false; // If this is enabled, all nickserv checks will also be validates against a /USERIP. This can be poked on, but will be automatically disabled if a nickserv check succeeds or the file fails to read.

	private static int TIMEOUT = 10000; // Timeout on nick checks.
//	private static int CACHE_TIMEOUT = 3600000; // Timeout on nick check cache (1 hour).
	// ^^ Can only be used once we can verify a user is in a channel and thus trust their online-ness.

	private final Map<String,ResultObj> nickChecks;
	Modules mods;
	IRCInterface irc;

	/** Enable horrible hacks. If you know which network you're going to be using the bot on, finalize this, and all of the other code will get removed by the compiler. */
	boolean infooverride=false;

	final Pattern validInfoReply=Pattern.compile("^(?:\\s*Nickname: ([^\\s]+) ?(<< ONLINE >>)?)|(?:The nickname \\[([^\\s]+)\\] is not registered)$");
	final Pattern athemeNotRegistered=Pattern.compile("^([^\\s]+?) is not registered.$");
	final Pattern athemeCurrentlyOnline=Pattern.compile("^Last seen.*?: now$");
	final Pattern athemeCurrentlyOffline=Pattern.compile("^Last seen.*?:.*");
	final Pattern athemeCurrentNickInfo=Pattern.compile("^Information on ([^\\s]+) ?.*");

	private String nickCheck;
	private String nickNeedCheck;

	public NickServ(final Modules mods, final IRCInterface irc)
	{
		nickChecks = new HashMap<String,ResultObj>();
		this.irc = irc;
		this.mods = mods;
		if (infooverride==false)
			// Check ensure that our NickServ is sane, and, if not, enable workarounds.
			mods.interval.callBack(null, 100);
	}

	// This is triggered by the constructor.
	public synchronized void interval( final Object parameter )
	{
		final String nick = "ignore-me"; // It's completely irrelevant what this nick is.
		getNewNickCheck( nick );
	}

	public void destroy()
	{
		synchronized(nickChecks)
		{
			final Iterator<String> nicks = nickChecks.keySet().iterator();
			while(nicks.hasNext()) {
				final ResultObj result = getNickCheck(nicks.next());
				synchronized(result)
				{
					result.notifyAll();
				}
			}
		}
	}

	public String[] helpApi = {
		  "NickServ allows your plugin to poke NickServ and get auth status on"
		+ " nicknames. It gives you two API methods: Check and Status. Both"
		+ " take a single String parameter and Check returns a Boolean, Status"
		+ " an Integer. If the status is 3, the nick is considered authed,"
		+ " anything lower is not."
	};

	public String[] helpCommandCheck = {
		"Check if someone's authed with NickServ.",
		"[<NickName>]",
		"<NickName> is an optional nick to check"
	};
	public void commandCheck( final Message mes ) throws ChoobException
	{
		String nick = mods.util.getParamString( mes );
		if (nick.length() == 0)
			nick = mes.getNick();

		final int check1 = ((Integer)mods.plugin.callAPI("NickServ", "Status", nick)).intValue();
		if ( check1 == 3 )
		{
			irc.sendContextReply(mes, nick + " is authed (" + check1 + ")!");
		}
		else if ( check1 == 1 )
		{
			irc.sendContextReply(mes, nick + " is not identified (" + check1 + ")!");
		}
		else
		{
			irc.sendContextReply(mes, nick + " is not registered (" + check1 + ")!");
		}
	}

	public int apiStatus( final String nick )
	{
		ResultObj result = getCachedNickCheck( nick.toLowerCase() );
		if (result != null)
		{
			return result.result;
		}

		result = getNewNickCheck( nick.toLowerCase() );

		synchronized(result)
		{
			try
			{
				result.wait(30000); // Make sure if NickServ breaks we're not screwed
			}
			catch (final InterruptedException e)
			{
				// Ooops, timeout
				ip_overrides=true;
				return -1;
			}
		}
		final int status = result.result;
		return status;
	}

	public boolean apiCheck( final String nick )
	{
		return apiCheck( nick, false );
	}

	public boolean apiCheck( final String nick, final boolean assumption )
	{
		final int stat = apiStatus( nick );
		if (stat == -1)
			return assumption;
		return stat >= 3;
	}

	private ResultObj getNewNickCheck( final String nick )
	{
		ResultObj result;
		synchronized(nickChecks)
		{
			result = nickChecks.get( nick );
			if ( result == null )
			{
				// Not already waiting on this one
				result = new ResultObj();
				result.result = -1;
				this.nickNeedCheck = nick;
				AccessController.doPrivileged( new PrivilegedAction<Object>() {
					public Object run()
					{
						irc.sendMessage("NickServ", (infooverride ? "INFO " : "STATUS ") + nick);
						if (ip_overrides)
						{
							irc.sendRawLine("USERIP " + nick);
							irc.sendRawLine("USERHOST " + nick);
						}
						return null;
					}
				});
				nickChecks.put( nick, result );
			}
		}
		return result;
	}

	private ResultObj getNickCheck( final String nick )
	{
		synchronized(nickChecks)
		{
			return nickChecks.get( nick );
		}
	}

	private ResultObj getCachedNickCheck( final String nick )
	{
		ResultObj result;
		synchronized(nickChecks)
		{
			result = nickChecks.get( nick );
			if ( result == null )
				// !!! This should never really happen
				return null;

			if (result.result == -1)
				return null;

			if ( result.time + TIMEOUT < System.currentTimeMillis() )
			{
				// expired!
				// TODO - do this in an interval...
				nickChecks.remove( nick );
				return null;
			}
		}
		return result;
	}

	public String[] optionsGeneral = { "Password" };
	public boolean optionCheckGeneralPassword( final String value ) { return true; }
	public String[] helpOptionPassword = {
		"Set this to the bot's NickServ password to make it identify with NickServ."
	};

	public String[] helpCommandEnableOverride = {
		"Private use only, mmkay?"
	};
	public void commandEnableOverride(final Message mes)
	{
		ip_overrides=true;
		irc.sendContextReply(mes, "Kay.");
	}

	public void onServerResponse(final ServerResponse resp)
	{
		if (resp.getCode() == 401) // 401 Nick Not Found.
		{
			final Matcher ma = Pattern.compile("^[^ ]+ ([^ ]+) ").matcher(resp.getResponse().trim());
			if (ma.find() && ma.group(1).trim().toLowerCase().equals("nickserv"))
				ip_overrides=true;
		}

		if (ip_overrides && resp.getCode()==340 || resp.getCode()==302) // USERIP (and USERHOST) response, not avaliable through PircBOT, gogo magic numbers.
		{
			/*
			 * General response ([]s as quotes):
			 * [Botnick] :[Nick]=+[User]@[ip, or, more likely, hash]
			 *
			 * for (a terrible) example:
			 * Choobie| :Faux=+Faux@87029A85.60BE439B.C4C3F075.IP
			 */

			final Matcher ma=Pattern.compile("^[^ ]+ :([^=]+)=(.*)").matcher(resp.getResponse().trim());
			if (!ma.find())
			{
				System.err.println("Unexpected non-match.");
				return;
			}
			final ResultObj result = getNickCheck( ma.group(1).trim().toLowerCase() );
			if ( result == null )
			{
				System.out.println("Something else handled it (" + ma.group(1).trim().toLowerCase() + "), we shouldn't be here.");
				ip_overrides=false;
				return;
			}

			System.out.println("Checking the file for \"" + ma.group(2) + "\".");

			synchronized(result)
			{
				String line;

				result.result = 0;

				try
				{
					final BufferedReader allowed = new BufferedReader(new FileReader("userip.list"));

					while((line=allowed.readLine())!=null)
						if (ma.group(2).equals(line))
						{
							result.result=3;
							break;
						}
				}
				catch (final IOException e)
				{
					// e.printStackTrace();
					ip_overrides=false;
					System.err.println("Error reading userip.list, disabling overrides.");
				}

				result.time = System.currentTimeMillis();

				result.notifyAll();
			}
		}
	}


	public void onPrivateNotice( final Message mes )
	{
		if ( ! (mes instanceof PrivateNotice) )
			return; // Only interested in private notices

		if ( ! mes.getNick().toLowerCase().equals( "nickserv" ) )
			return; // Not from NickServ --> also don't care

		if (!infooverride && (mes.getMessage().trim().toLowerCase().equals("unknown command [status]") || mes.getMessage().trim().equals("You are not logged in.")))
		{
			// Ohes nose, horribly broken network! Let's pretend that it didn't just slap us in the face with a glove.
			System.err.println("Reverting to badly broken NickServ handling.");

			infooverride = true;

			synchronized (nickChecks)
			{
				nickChecks.clear(); // <-- Ooh, lets break things.
			}

			// Any pending nick checks will fail, but.. well, bah.
			return;
		}

		final List<String> params = mods.util.getParams( mes );

		String nick;
		int status = 0;
		if (infooverride)
		{
			// Atheme:
			final Matcher anr = athemeNotRegistered.matcher(mes.getMessage());
			final Matcher aon = athemeCurrentlyOnline.matcher(mes.getMessage());
			final Matcher aoff = athemeCurrentlyOffline.matcher(mes.getMessage());
			final Matcher acni = athemeCurrentNickInfo.matcher(mes.getMessage());

			if(acni.matches())
			{
				nick = acni.group(1);
				if(Colors.removeFormattingAndColors(nick).equalsIgnoreCase(this.nickNeedCheck))
				{
					this.nickCheck = Colors.removeFormattingAndColors(nick);
					this.nickNeedCheck = null;
				}
				return;
			}

			if (anr.matches())
			{
				nick = Colors.removeFormattingAndColors(anr.group(1));
				if(Colors.removeFormattingAndColors(nick).equalsIgnoreCase(this.nickCheck))
				{
					status = 0;
					this.nickCheck = null;
				}
			}
			else if (aon.matches())
			{
				nick = this.nickCheck;
				this.nickCheck = null;
				status = 3;
			}
			else if (aoff.matches())
			{
				nick = this.nickCheck;
				this.nickCheck = null;
				status = 1;
			}
			else
			{
				if (mes.getMessage().indexOf("Nickname: ") == -1 && mes.getMessage().indexOf("The nickname [") == -1)
					return; // Wrong type of message!

				final Matcher ma = validInfoReply.matcher(Colors.removeFormattingAndColors(mes.getMessage()));

				if (!ma.matches())
					return;

				nick = ma.group(1);

				if (nick == null)
				{
					// Unregistered
					nick = ma.group(3);
					status = 0;
				}
				else
					// Registered
					status = ma.group(2) == null || ma.group(2).equals("") ? 1 : 3;
			}
		}
		else
		{
			if ( mes.getMessage().matches(".*(?i:/msg NickServ IDENTIFY).*") )
			{
				// must identify
				String pass = null;
				try
				{
					pass = (String)mods.plugin.callAPI("Options", "GetGeneralOption", "password");
				}
				catch (final ChoobNoSuchCallException e)
				{
					System.err.println("Options plugin not loaded; can't get NickServ password.");
					return;
				}

				if (pass != null)
				{
					System.err.println("Sending NickServ password!");
					irc.sendContextMessage(mes, "IDENTIFY " + pass);
				}
				else
					System.err.println("Password option for plugin NickServ not set...");
				return;
			}
			else if ( params.size() > 2 && params.get(1).equalsIgnoreCase("is") )
			{
				// Online! But not registered (I'd hope)
				nick = params.get(0);
				status = 1;
			}
			else if ( params.size() == 4 && params.get(0).equalsIgnoreCase("nickname") && params.get(2).equalsIgnoreCase("isn't") && params.get(3).equalsIgnoreCase("registered.") )
			{
				// Registered but offline.
				nick = Colors.removeFormattingAndColors(params.get(1));
				status = 0;
			}
			else if ( params.get(0).equalsIgnoreCase("status") )
			{
				nick = params.get(1);
				status = Integer.valueOf(params.get(2)).intValue();

				if (status == 0)
				{
					// We'd like 0 = not registered, 1 = offline/not identified.
					// As such we need to check existence too. now.
					irc.sendContextMessage(mes, "INFO " + nick);
					return;
				}
			}
			else
				return; // Wrong type of message!
		}

		final ResultObj result = getNickCheck( nick.toLowerCase() );
		if ( result == null )
			return; // XXX

		synchronized(result)
		{
			result.result = status;
			result.time = System.currentTimeMillis();

			result.notifyAll();
		}

		ip_overrides=false;

	}

	// Expire old checks when appropriate...

	public void onNickChange( final NickChange nc )
	{
		synchronized(nickChecks)
		{
			nickChecks.remove(nc.getNick());
			nickChecks.remove(nc.getNewNick());
		}
	}

	public void onJoin( final ChannelJoin cj )
	{
		synchronized(nickChecks)
		{
			nickChecks.remove(cj.getNick());
		}
	}

	public void onKick( final ChannelKick ck )
	{
		synchronized(nickChecks)
		{
			nickChecks.remove(ck.getTarget());
		}
	}

	public void onPart( final ChannelPart cp )
	{
		synchronized(nickChecks)
		{
			nickChecks.remove(cp.getNick());
		}
	}

	public void onQuit( final QuitEvent qe )
	{
		synchronized(nickChecks)
		{
			nickChecks.remove(qe.getNick());
		}
	}
}
