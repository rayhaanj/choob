package uk.co.uwcs.choob.support;

import java.security.Permission;
import java.security.ProtectionDomain;

import uk.co.uwcs.choob.modules.SecurityModule;

/**
 * Choob protection domain implementation.
 * Just shells out to modules.SecurityModule really.
 * @author bucko
 */
public final class ChoobProtectionDomain extends ProtectionDomain
{
	private SecurityModule mod;
	private String pluginName;

	public ChoobProtectionDomain( SecurityModule mod, String pluginName )
	{
		super( null, null );
		this.mod = mod;
		this.pluginName = pluginName;
	}

	public boolean implies( Permission perm )
	{
		// XXX HAX ATTACK XXX
		if ( perm instanceof ChoobSpecialStackPermission )
		{
			((ChoobSpecialStackPermission)perm).add(pluginName);
			return true;
		}
		return mod.hasPluginPerm( perm, pluginName );
	}
}
