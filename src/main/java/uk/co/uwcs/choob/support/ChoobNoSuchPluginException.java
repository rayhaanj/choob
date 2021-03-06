/**
 * Exception for Choob plugin not found errors.
 * @author bucko
 */

package uk.co.uwcs.choob.support;

public final class ChoobNoSuchPluginException extends ChoobNoSuchCallException
{
	private static final long serialVersionUID = -6553157796822467031L;

	public ChoobNoSuchPluginException(final String plugin)
	{
		super(plugin);
	}

	public ChoobNoSuchPluginException(final String plugin, final String call)
	{
		super(plugin, call);
	}
}
