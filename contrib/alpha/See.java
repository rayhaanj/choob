/** @author Faux */

import uk.co.uwcs.choob.*;
import uk.co.uwcs.choob.modules.*;
import uk.co.uwcs.choob.support.*;
import uk.co.uwcs.choob.support.events.*;
import java.sql.*;
import java.text.*;
import java.io.*;
import java.text.DateFormatSymbols;

public class See
{

	final SimpleDateFormat sdfa = new SimpleDateFormat("Kaa ");
	final SimpleDateFormat sdfb = new SimpleDateFormat("EEEE");

	private Modules mods;
	private IRCInterface irc;
	public See(IRCInterface irc, Modules mods)
	{
		this.mods = mods;
		this.irc = irc;
	}

	String timeStamp(Timestamp d)
	{
		return mods.date.timeStamp((new java.util.Date()).getTime()-d.getTime(), false, 3, uk.co.uwcs.choob.modules.DateModule.longtokens.minute);
	}

	private final synchronized ResultSet getDataFor(final String nick, final Connection conn) throws SQLException
	{
		final Statement stat=conn.createStatement();

		stat.execute("DROP TEMPORARY TABLE IF EXISTS `tempt1`, `tempt2`; ");

		{
			final PreparedStatement s=conn.prepareStatement("CREATE TEMPORARY TABLE `tempt1` AS SELECT `Time` FROM `History` WHERE `Time` > " +  (System.currentTimeMillis()-(1000*60*60*24*5)) + " AND (CASE INSTR(`Nick`,'|') WHEN 0 THEN `Nick` ELSE LEFT(`Nick`, INSTR(`Nick`,'|')-1) END)=? AND `Channel`IS NOT NULL ORDER BY `Time`; ");
			s.setString(1, nick);
			s.executeUpdate();
		}

		stat.execute("ALTER TABLE `tempt1` ADD `index` INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST; ");

		stat.execute("CREATE TEMPORARY TABLE `tempt2` as SELECT * from `tempt1`; " );
		stat.execute("UPDATE `tempt2` SET `index`:=`index`+1; " );
		stat.execute("ALTER TABLE `tempt2` ADD PRIMARY KEY ( `index` ); ");

		return conn.prepareStatement("SELECT DATE_ADD(FROM_UNIXTIME( `tempt1`.`Time` /1000 ), INTERVAL ((`tempt2`.`Time` - `tempt1`.`Time` ) /1000) SECOND) as `start`, FROM_UNIXTIME( `tempt1`.`Time` /1000 ) AS `end`, ((`tempt1`.`Time` - `tempt2`.`Time` ) /1000 /3600) AS `diff` FROM `tempt2` INNER JOIN `tempt1` ON `tempt2`.`index` = `tempt1`.`index` HAVING `diff` > 6;").executeQuery();
	}

	public final synchronized void commandBodyClock( Message mes ) throws SQLException
	{
		String nick=mods.util.getParamString(mes).trim();

		if (nick.equals(""))
			nick=mes.getNick();

		nick=mods.nick.getBestPrimaryNick(nick);

		final Connection conn=mods.odb.getConnection();

		ResultSet rs = getDataFor(nick, conn);

		String ret="";

		if (!rs.last())
			irc.sendContextReply(mes, "I don't have enough information to work out the bodyclock for " + nick + ".");
		else
		{
			final Timestamp gotup=rs.getTimestamp("end");
			final long diff=rs.getTimestamp("end").getTime() - rs.getTimestamp("start").getTime();

			float bodyclock=8.0f+(((float)((new java.util.Date()).getTime()-gotup.getTime()))/(1000.0f*60.0f*60.0f));

			long minutes=(Math.round((bodyclock-Math.floor(bodyclock))*60.0f));

			if (minutes == 60)
			{
				minutes=0;
				bodyclock++;
			}

			ret+=nick + " probably got up " + timeStamp(gotup) + " ago after " +
				mods.date.timeStamp(diff, false, 2, uk.co.uwcs.choob.modules.DateModule.longtokens.hour) + " of sleep, making their body-clock time about " +
				((int)Math.floor(bodyclock) % 24) + ":" + (minutes < 10 ? "0" : "") + minutes;

			irc.sendContextReply(mes, ret + ".");
		}

		mods.odb.freeConnection(conn);
	}


	private final String datelet(Date d)
	{
		return sdfa.format(d).toLowerCase() + sdfb.format(d);
	}

	public final synchronized void commandPattern( Message mes ) throws SQLException
	{
		String nick=mods.util.getParamString(mes).trim();

		if (nick.equals(""))
			nick=mes.getNick();

		nick=mods.nick.getBestPrimaryNick(nick);

		final Connection conn=mods.odb.getConnection();

		ResultSet rs = getDataFor(nick, conn);

		if (!rs.first())
			irc.sendContextReply(mes, "I don't have enough information about " + nick + ".");
		else
		{
			rs.beforeFirst();


			String ret=nick + " was sleeping: ";
			while (rs.next())
			{
				final Timestamp gotup=rs.getTimestamp("end");
				final Date start = new Date(rs.getTimestamp("start").getTime());
				final Date end = new Date(rs.getTimestamp("end").getTime());
				ret += datelet(start) + " -> " + datelet(end) + ", ";
				System.out.println(start.getTime() + " " + end.getTime());
			}

			if (ret.length()>2)
				ret = ret.substring(0, ret.length() -2);
			irc.sendContextReply(mes, ret + ".");
		}


		mods.odb.freeConnection(conn);
	}


}
