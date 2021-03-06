/*
 * PluginLoader.java
 *
 * Created on June 13, 2005, 1:25 PM
 */
package uk.co.uwcs.choob;

import java.net.URL;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.ChoobException;
import uk.co.uwcs.choob.support.ChoobInvocationError;
import uk.co.uwcs.choob.support.ChoobNoSuchCallException;
import uk.co.uwcs.choob.support.ChoobNoSuchPluginException;
import uk.co.uwcs.choob.support.ChoobPermission;
import uk.co.uwcs.choob.support.HelpNotSpecifiedException;
import uk.co.uwcs.choob.support.NoSuchCommandException;
import uk.co.uwcs.choob.support.NoSuchPluginException;
import uk.co.uwcs.choob.support.events.Event;
import uk.co.uwcs.choob.support.events.Message;


/**
 * Root class of a plugin manager
 * @author bucko
 */
public abstract class ChoobPluginManager
{
	final Modules mods;
	final ChoobPluginManagerState state;

	// Ensure derivative classes have permissions...
	public ChoobPluginManager(Modules mods, ChoobPluginManagerState state)
	{
		AccessController.checkPermission(new ChoobPermission("root"));
		this.state = state;
		this.mods = mods;
	}

	protected abstract Object createPlugin(String pluginName, URL fromLocation) throws ChoobException;
	protected abstract void destroyPlugin(String pluginName);


	public String[] getHelp(String pluginName, String commandName) throws NoSuchCommandException
	{
		throw new NoSuchCommandException(commandName);
	}

	public String[] getInfo(String pluginName) throws NoSuchPluginException
	{
		throw new NoSuchPluginException(pluginName);
	}

	/**
	 * (Re)loads a plugin from an URL and a plugin name. Note that in the case
	 * of reloading, the old plugin will be disposed of AFTER the new one is
	 * loaded.
	 * @param pluginName Class name of plugin.
	 * @param fromLocation URL from which to get the plugin's contents.
	 * @return true if the plugin was reloaded
	 * @throws Exception Thrown if there's a syntactical error in the plugin's source.
	 */
	public final boolean loadPlugin(final String pluginName, final URL fromLocation) throws ChoobException
	{
		AccessController.checkPermission(new ChoobPermission("plugin.load." + pluginName.toLowerCase()));

		// Make sure we're ready to add commands.
		if (state.commands.get(pluginName.toLowerCase()) == null)
			state.commands.put(pluginName.toLowerCase(), new ArrayList<String>());

		createPlugin(pluginName, fromLocation);

		// Now plugin is loaded with no problems. Install it.

		// XXX Possible problem with double loading here. Shouldn't matter,
		// though.

		ChoobPluginManager man;
		synchronized(state.pluginMap)
		{
			man = state.pluginMap.remove(pluginName.toLowerCase());
			state.pluginMap.put(pluginName.toLowerCase(), this);
		}
		synchronized(state.pluginManagers)
		{
			if (!state.pluginManagers.contains(this))
				state.pluginManagers.add(this);
		}
		if (man != null && man != this)
			man.destroyPlugin(pluginName);

		// If man existed, so did the plugin.
		return man != null;
	}

	public final void unloadPlugin(final String pluginName) throws ChoobNoSuchPluginException
	{
		AccessController.checkPermission(new ChoobPermission("plugin.unload." + pluginName.toLowerCase()));

		ChoobPluginManager man;
		synchronized(state.pluginMap)
		{
			man = state.pluginMap.remove(pluginName.toLowerCase());
		}
		if (man != null)
			man.destroyPlugin(pluginName);
		else
			throw new ChoobNoSuchPluginException(pluginName, "UNLOAD");

		synchronized (state.commands) {
			state.commands.remove(pluginName.toLowerCase());
		}
	}

	/**
	 * Get a list of plugins.
	 */
	public final String[] plugins()
	{
		synchronized(state.pluginMap)
		{
			final Set<String> keys = state.pluginMap.keySet();
			final String[] ret = new String[keys.size()];
			return keys.toArray(ret);
		}
	}

	/**
	 * Get a list of commands in a plugin.
	 */
	public final String[] commands(final String pluginName)
	{
		synchronized(state.commands)
		{
			final List<String> coms = state.commands.get(pluginName.toLowerCase());
			if (coms == null)
				return null;
			final String[] ret = new String[coms.size()];
			return coms.toArray(ret);
		}
	}

	/**
	 * Adds a command to the internal database.
	 */
	public final void addCommand(final String pluginName, final String commandName)
	{
		synchronized(state.phoneticCommands)
		{
			if (pluginName != null)
				state.phoneticCommands.addWord((pluginName + "." + commandName).toLowerCase());
			else
				state.phoneticCommands.removeWord(commandName.toLowerCase());
		}
		synchronized(state.commands)
		{
			if (pluginName != null)
				state.commands.get(pluginName.toLowerCase()).add(commandName);
			else
				state.commands.get("").add(commandName);
		}
	}

	/**
	 * Removes a command from the internal database.
	 */
	public final void removeCommand(final String pluginName, final String commandName)
	{
		synchronized(state.phoneticCommands)
		{
			if (pluginName != null)
				state.phoneticCommands.removeWord((pluginName + "." + commandName).toLowerCase());
			else
				state.phoneticCommands.removeWord(commandName.toLowerCase());
		}
		synchronized(state.commands)
		{
			if (pluginName != null)
				state.commands.get(pluginName.toLowerCase()).remove(commandName);
			else
				state.commands.get("").remove(commandName);
		}
	}

	/**
	 * Remove a command from the internal database. Use the two parameter
	 * version in preference to this!
	 */
	public final void removeCommand(final String commandName)
	{
		final Matcher ma = Pattern.compile("(\\w+)\\.(\\w+)").matcher(commandName);
		if (ma.matches())
			removeCommand(ma.group(1), ma.group(2));
		else
			removeCommand(null, commandName);
	}

	/**
	 * Add a command to the internal database. Use the two parameter
	 * version in preference to this!
	 */
	public final void addCommand(final String commandName)
	{
		final Matcher ma = Pattern.compile("(\\w+)\\.(\\w+)").matcher(commandName);
		if (ma.matches())
			addCommand(ma.group(1), ma.group(2));
		else
			addCommand(null, commandName);
	}

	public final ProtectionDomain getProtectionDomain( final String pluginName )
	{
		return mods.security.getProtectionDomain( pluginName );
	}

	// TODO make these return ChoobTask[], and implement a spawnCommand
	// etc. method to queue the tasks.

	/**
	 * Attempts to call a method in the plugin, triggered by a line from IRC.
	 * @param command Command to call.
	 * @param ev Message object from IRC.
	 */
	abstract public ChoobTask commandTask(String plugin, String command, Message ev);

	/**
	 * Run an interval on the given plugin
	 */
	abstract public ChoobTask intervalTask(String pluginName, Object param);

	/**
	 * Perform any event handling on the given Event.
	 * @param ev Event to pass along.
	 */
	abstract public List<ChoobTask> eventTasks(Event ev);

	/**
	 * Run any filters on the given Message.
	 * @param ev Message to pass along.
	 */
	abstract public List<ChoobTask> filterTasks(Message ev);

	/**
	 * Attempt to perform an API call on a contained plugin.
	 * @param APIName The name of the API call.
	 * @param params Params to pass through.
	 * @throws ChoobNoSuchCallException when the call didn't exist.
	 * @throws ChoobInvocationError when the call threw an exception.
	 */
	abstract public Object doAPI(String pluginName, String APIName, Object... params) throws ChoobNoSuchCallException;

	/**
	 * Attempt to perform an API call on a contained plugin.
	 * @param prefix The prefix (ie. type) of call.
	 * @param genericName The name of the call.
	 * @param params Params to pass through.
	 * @throws ChoobNoSuchCallException when the call didn't exist.
	 * @throws ChoobInvocationError when the call threw an exception.
	 */
	abstract public Object doGeneric(String pluginName, String prefix, String genericName, Object... params) throws ChoobNoSuchCallException;

	/**
	 * Get the description of a command specified in an annotation
	 * @param pluginName	The plugin name the command is in.
	 * @param commandName	The command name.
	 * @return	The description.
	 * @throws uk.co.uwcs.choob.support.HelpNotSpecifiedException	If the command was not annotated with help.
	 */
	public String getCommandDescription(String pluginName, String commandName) throws HelpNotSpecifiedException
	{
		throw new HelpNotSpecifiedException();
	}


	/**
	 * Get the parameters of a command specified in an annotation
	 * @param pluginName	The plugin name the command is in.
	 * @param commandName	The command name.
	 * @return	The parameters.
	 * @throws uk.co.uwcs.choob.support.HelpNotSpecifiedException	If the command was not annotated with help.
	 */
	public String getCommandParameters(String pluginName, String commandName) throws HelpNotSpecifiedException
	{
		throw new HelpNotSpecifiedException();
	}

	/**
	 * Get the parameter descriptions of a command specified in an annotation
	 * @param pluginName	The plugin name the command is in.
	 * @param commandName	The command name.
	 * @return	The usage.
	 * @throws uk.co.uwcs.choob.support.HelpNotSpecifiedException	If the command was not annotated with help.
	 */
	public String[] getCommandParameterDescriptions(String pluginName, String commandName) throws HelpNotSpecifiedException
	{
		throw new HelpNotSpecifiedException();
	}

}

