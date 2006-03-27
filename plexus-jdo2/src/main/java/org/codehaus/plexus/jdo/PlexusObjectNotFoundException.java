package org.codehaus.plexus.jdo;

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class PlexusObjectNotFoundException
    extends ContinuumStoreException
{
    public ContinuumObjectNotFoundException( String type, String id )
    {
        super( "Could not find object. Type '" + type + "'. Id: '" + id + "'." );
    }
}
