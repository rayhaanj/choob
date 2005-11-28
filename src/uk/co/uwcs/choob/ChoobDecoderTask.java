package uk.co.uwcs.choob;

import uk.co.uwcs.choob.plugins.*;
import uk.co.uwcs.choob.modules.*;
import uk.co.uwcs.choob.support.*;
import uk.co.uwcs.choob.support.events.*;
import java.util.*;
import java.util.regex.*;

final class LastEvents
{
	long lastmes[]={0,5000,10000};
	int stor=0;

	LastEvents()
	{
		save();
	}

	public long average()
	{
		return (lastmes[(1+stor)%3]-lastmes[(0+stor)%3]+lastmes[(2+stor)%3]-lastmes[(1+stor)%3]) / 2;
	}

	public void save()
	{
		stor%=3;
		lastmes[stor++]=(new java.util.Date()).getTime();
	}
}

final class ChoobDecoderTask extends ChoobTask
{
	private static DbConnectionBroker dbBroker;
	private static Modules modules;
	private static IRCInterface irc;
	private static Pattern triggerPattern;
	private static Pattern aliasPattern;
	private static Pattern commandPattern;
	private Event event;

	static Map<String,LastEvents>lastMessage = Collections.synchronizedMap(new HashMap<String,LastEvents>()); // Nick, Timestamp.
	static final long AVERAGE_MESSAGE_GAP=2000;

	static void initialise(DbConnectionBroker dbBroker, Modules modules, IRCInterface irc)
	{
		if (ChoobDecoderTask.dbBroker != null)
			return;
		ChoobDecoderTask.dbBroker = dbBroker;
		ChoobDecoderTask.modules = modules;
		ChoobDecoderTask.irc = irc;
		triggerPattern = Pattern.compile("^(?:" + irc.getTriggerRegex() + ")", Pattern.CASE_INSENSITIVE);
		commandPattern = Pattern.compile("^([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)$");
	}

	/** Creates a new instance of ChoobThread */
	ChoobDecoderTask(Event event)
	{
		super(null);
		this.event = event;
	}

	public synchronized void run()
	{
		List<ChoobTask> tasks = new LinkedList<ChoobTask>();

		if (event instanceof NickChange)
		{
			// FIXME: There is no way I can see to make this work here.
			// It needs to pick up when the BOT changes name, even through
			// external forces, and poke UtilModule about it.

			//NickChange nc = (NickChange)event;
			//if (nc.getNick().equals()) {
			//	// Make sure the trigger checking code is up-to-date with the current nickname.
			//	modules.util.updateTrigger();
			//}
		}

		// Process event calls first
		tasks.addAll(modules.plugin.getPlugMan().eventTasks(event));

		// Then filters
		if (event instanceof FilterEvent)
		{
			// FilterEvents are messages
			Message mes = (Message) event;
			tasks.addAll(modules.plugin.getPlugMan().filterTasks(mes));

			// For now, only FilterEvents will be logged...
			modules.history.addLog( (Message) event );
		}

		// Now if it's a message, deal with that too
		if (event instanceof CommandEvent)
		{
			// CommandEvents are messages
			Message mes = (Message) event;

			Matcher ma;

			// First, is does it have a trigger?
			String matchAgainst = mes.getMessage();
			ma = triggerPattern.matcher(matchAgainst);

			boolean mafind = ma.find();

			if ( mafind || mes instanceof PrivateMessage )
			{
				// OK, it's a command!

				// Decode into a string we can match as a command.
				int commandStart = (mafind ? ma.end() : 0);
				int commandEnd = matchAgainst.indexOf(' ', commandStart);
				if (commandEnd != -1)
					matchAgainst = matchAgainst.substring(commandStart, commandEnd);
				else
					matchAgainst = matchAgainst.substring(commandStart);

				if (matchAgainst.indexOf(' ') >= 0)
					matchAgainst = matchAgainst.substring(0, matchAgainst.indexOf(' '));

				ma = commandPattern.matcher(matchAgainst);
				if( ma.matches() )
				{
					LastEvents la=lastMessage.get(mes.getNick());
					if (la==null)
						lastMessage.put(mes.getNick(), new LastEvents());
					else
					{
						la.save();
						long laa=la.average();
						if (laa<AVERAGE_MESSAGE_GAP)
						{
							irc.sendMessage(mes.getNick(), "You're flooding, ignored. Please wait at least " + (AVERAGE_MESSAGE_GAP-laa) + "ms before your next message.");
							return;
						}
					}

					String pluginName  = ma.group(1);
					String commandName = ma.group(2);

					System.out.println("Plugin name: " + pluginName + ", Command name: " + commandName + ".");

					ChoobTask task = modules.plugin.getPlugMan().commandTask(pluginName, commandName, mes);
					if (task != null)
						tasks.add(task);
				}
			}
		}

		// We now have a neat list of tasks to perform. Queue them all.
		for(ChoobTask task: tasks)
		{
			ChoobThreadManager.queueTask(task);
		}

		// And done.
	}
}