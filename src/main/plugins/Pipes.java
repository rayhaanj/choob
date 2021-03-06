import java.util.List;
import java.util.regex.Pattern;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.ChoobNoSuchCallException;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.Message;


class Pipes
{
	final Modules mods;
	private final IRCInterface irc;

	public Pipes(final Modules mods, final IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
	}

	static String firstToken(String s)
	{
		final int ind = s.indexOf(' ');
		if (ind == -1)
			return s;
		return s.substring(0, ind);
	}

	public void commandEval(final Message mes) throws Exception
	{
		final String eval;
		try
		{
			eval = eval(mods.util.getParamString(mes), new LoopbackExeculator(mes));
		}
		catch (PipesIllegalArgumentException e)
		{
			irc.sendContextReply(mes, e.getMessage());
			return;
		}

		List<String> messes = irc.cutString(eval, mes.getNick().length() + 2 + 25);
		irc.sendContextReply(mes, messes.get(0) + (messes.size() > 1 ?
			" (" + (messes.size() - 1) + " more message" + (messes.size() == 2 ? "" : "s") + ")" :
			""));
	}

	private final class LoopbackExeculator implements Execulator
	{
		// XXX mes can be NULL. D:
		private final Message mes;
		private final String nick;
		private final String target;

		LoopbackExeculator(final Message mes)
		{
			this(mes, mes.getNick(), mes.getTarget());
		}

		LoopbackExeculator(final String nick, final String target)
		{
			this(null, nick, target);
		}

		public LoopbackExeculator(final Message mes, final String nick, final String target)
		{
			this.mes = mes;
			this.nick = nick;
			this.target = target;
		}

		@Override
		public String exec(String s, String stdin) throws Exception
		{
			final String[] qq = s.trim().split(" ", 2);
			String cmd = qq[0].trim();
			String arg = qq.length > 1 ? qq[1] : "";

			// Restrict size of input to all commands.
			arg = arg.substring(0, Math.min(arg.length(), 1024));

			try
			{

				if ("sed".equals(cmd))
					return (String)mods.plugin.callAPI("MiscUtils", "Sed", arg, stdin);

				if ("java".equals(cmd))
					return (String)mods.plugin.callAPI("Executor", "ran", stdin);

				if ("tr".equals(cmd))
					return (String)mods.plugin.callAPI("MiscUtils", "Trans", arg, stdin);

				if ("pick".equals(cmd))
				{
					// If something's been provided on stdin, we'll use it instead of the history.
					// This allows piping into commands that are rooted in !pick.
					if (!"".equals(stdin))
						return stdin;

					// Let's just pray this won't be reached with a null mes! \o/
					final List<Message> history = mods.history.getLastMessages(mes, 20);
					final Pattern picker = (Pattern)mods.plugin.callAPI("MiscUtils", "LinePicker", arg);
					for (Message m : history)
						if (picker.matcher(m.getMessage()).find())
							return m.getMessage();
					throw new IllegalArgumentException("Couldn't pick anything with " + arg);
				}

				if ("nick".equals(cmd))
					return nick;

				if ("export".equals(cmd))
					// mes
					return (String)mods.plugin.callAPI("Alias", "CreateAlias", mes, nick, nullToEmpty(target), "fakelias "+ arg);

			}
			catch (ChoobNoSuchCallException e)
			{
				throw new PipesIllegalArgumentException("Pipe built-in '" + cmd + "' is missing required components. " + e.toString());
			}

			if ("xargs".equals(cmd))
			{
				final String[] rr = arg.split(" ", 2);
				cmd = rr[0];
				arg = (rr.length > 1 ? rr[1] : "") + stdin;
			}

			try
			{
				final String alcmd = cmd + " " + arg;

				Object res = mods.plugin.callAPI("alias", "get", cmd);
				if (null != res)
				{
					cmd = (String) mods.plugin.callAPI("alias", "applyalias", res, alcmd.split(" "), arg,
							nullToEmpty(nick), nullToEmpty(target));
					arg = "";
				}
			}
			catch (ChoobNoSuchCallException e)
			{
				e.printStackTrace();
				// Whatever, no alias support.
			}

			String[] alis = cmd.split(" ", 2);
			String[] cmds = alis[0].split("\\.", 2);

			if (cmds.length != 2)
				throw new PipesIllegalArgumentException("Command '" + cmd + "' is not a pipes built-in or an alias.");

			// Fiddle talk.say -> talk.reply, which is pipable and does the same thing (well, close enough)
			if ("talk".equalsIgnoreCase(cmds[0]) && "say".equalsIgnoreCase(cmds[1]))
				cmds[1] = "reply";

			if (alis.length > 1 && alis[1].length() > 0)
				arg = alis[1] + arg;

			if ("pipes".equalsIgnoreCase(cmds[0]) && "eval".equalsIgnoreCase(cmds[1]))
			{
				if ("".equals(arg))
				{
					arg = stdin;
					stdin = "";
				}
				return eval(arg, this, stdin);
			}

			try
			{
				return (String) mods.plugin.callGeneric(cmds[0], "command", cmds[1], arg);
			}
			catch (ChoobNoSuchCallException e)
			{
				cmd = cmds[0] + "." + cmds[1];
				if (mods.plugin.validCommand(cmd))
					throw new PipesIllegalArgumentException("Command '" + cmd + "' cannot be called in a pipeline.");
				throw new PipesIllegalArgumentException("Command '" + cmd + "' does not exist.");
			}
		}
	}

	static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	static class PipesIllegalArgumentException extends IllegalArgumentException
	{
		public PipesIllegalArgumentException(String message)
		{
			super(message);
		}
	}

	static class ParseException extends Exception
	{
		private final StringIterator pos;

		/**
		 * @param pos Location the error occoured.
		 */
		public ParseException(String string, StringIterator pos)
		{
			super(string);
			this.pos = pos;
		}

		@Override
		public String toString() {
			return super.toString() + " before " + pos + "$$";
		}
	}

	/** Keep track of the position of ourselves in a String. */
	static class StringIterator
	{
		private int i;
		final private char[] c;

		StringIterator (String s)
		{
			c = s.toCharArray();
		}

		char get() throws ParseException
		{
			if (i < c.length)
				return c[i++];
			throw new ParseException("Unexpected end", this);
		}

		char peek() throws ParseException
		{
			if (i < c.length)
				return c[i];
			throw new ParseException("Peek past end", this);
		}

		boolean hasMore()
		{
			return i < c.length;
		}

		@Override
		public String toString()
		{
			return new String(c, i, c.length - i);
		}

		public int length()
		{
			return c.length - i;
		}
	}

	/** @param si Positioned just after the ( in a valid $(; will terminate just after the ).
	 * @throws Exception iff comes from Execulator.  */
	private static String eval(final StringIterator si, Execulator e, String stdin) throws Exception
	{
		StringBuilder sb = new StringBuilder(si.length());
		boolean dquote = false, squote = false, bslash = false;

		while (si.hasMore())
		{
			final char c = si.get();
			if (!squote && !dquote && !bslash && '$' == c && '(' == si.peek())
			{
				si.get();
				sb.append(eval(si, e, ""));
			}
			else if (!squote && !dquote && !bslash && ')' == c)
			{
				if (bslash || dquote || squote)
					throw new ParseException("Unexpected end of expression", si);
				return e.exec(sb.toString(), stdin);
			}
			else if ('"' == c)
				if (squote || bslash)
				{
					sb.append(c);
					bslash = false;
				}
				else
					dquote = !dquote;
			else if ('\'' == c)
				if (bslash)
				{
					if (dquote)
						sb.append("\\");
					sb.append(c);
					bslash = false;
				}
				else if (dquote)
					sb.append(c);
				else
					squote = !squote;
			else if ('\\' == c)
				if (bslash)
				{
					sb.append(c);
					bslash = false;
				}
				else if (squote)
					sb.append(c);
				else
					bslash = true;
			else if ('|' == c)
			{
				if (!squote && !dquote && !bslash)
				{
					stdin = e.exec(sb.toString(), stdin);
					sb.setLength(0);
				}
				else
					sb.append(c);
			}
			else
			{
				if (bslash)
					if (dquote)
						bslash = false;
					else
						throw new ParseException("illegal escape sequence: " + c, si);
				sb.append(c);
			}
		}
		throw new ParseException("Expected )", si);
	}

	static String eval(String s, Execulator e) throws Exception
	{
		return eval(s, e, "");
	}

	static String eval(String s, Execulator e, String stdin) throws Exception
	{
		final StringIterator si = new StringIterator(s + ")");
		final String res = eval(si, e, stdin);
		// XXX #testEnd
		if (si.hasMore() && (si.get() != ')' || si.hasMore()))
			throw new ParseException("Trailing characters", si);
		return res;
	}

	static String eval(String s) throws Exception
	{
		return eval(s, SysoExeculator);
	}

	public String apiEval(String s, String nick, String context, String stdin) throws Exception
	{
		return eval(s, new LoopbackExeculator(nick, context), stdin);
	}

	static interface Execulator
	{
		String exec(String s, String stdin) throws Exception;
	}

	private final static Execulator SysoExeculator = new Execulator()
	{

		@Override
		public String exec(String s, String stdin)
		{
			System.out.println(s + " with stdin of: " + stdin);
			return s;
		}
	};
}
