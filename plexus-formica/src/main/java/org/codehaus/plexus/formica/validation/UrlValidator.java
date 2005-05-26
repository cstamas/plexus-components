package org.codehaus.plexus.formica.validation;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.apache.oro.text.perl.Perl5Util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Validates URLs.</p>
 * Behavour of validation is modified by passing in options:
 * <li>ALLOW_2_SLASHES - [FALSE]  Allows double '/' characters in the path
 * component.</li>
 * <li>NO_FRAGMENT- [FALSE]  By default fragments are allowed, if this option is
 * included then fragments are flagged as illegal.</li>
 * <li>ALLOW_ALL_SCHEMES - [FALSE] By default only http, https, and ftp are
 * considered valid schemes.  Enabling this option will let any scheme pass validation.</li>
 * <p/>
 * <p>Originally based in on php script by Debbie Dyer, validation.php v1.2b, Date: 03/07/02,
 * http://javascript.internet.com. However, this validation now bears little resemblance
 * to the php original.</p>
 * <pre>
 *   Example of usage:
 *   Construct a UrlValidator with valid schemes of "http", and "https".
 * <p/>
 *    String[] schemes = {"http","https"}.
 *    Urlvalidator urlValidator = new Urlvalidator(schemes);
 *    if (urlValidator.isValid("ftp")) {
 *       System.out.println("url is valid");
 *    } else {
 *       System.out.println("url is invalid");
 *    }
 * <p/>
 *    prints "url is invalid"
 *   If instead the default constructor is used.
 * <p/>
 *    Urlvalidator urlValidator = new Urlvalidator();
 *    if (urlValidator.isValid("ftp")) {
 *       System.out.println("url is valid");
 *    } else {
 *       System.out.println("url is invalid");
 *    }
 * <p/>
 *   prints out "url is valid"
 *  </pre>
 *
 * @author Robert Leland
 * @author David Graham
 * @version $Revision$ $Date$
 * @see <a href='http://www.ietf.org/rfc/rfc2396.txt' >
 *      Uniform Resource Identifiers (URI): Generic Syntax
 *      </a>
 */
public class UrlValidator
    extends AbstractValidator
{

    /**
     * Allows all validly formatted schemes to pass validation instead of supplying a
     * set of valid schemes.
     */
    public static int ALLOW_ALL_SCHEMES = 1;


    private static String ALPHA_CHARS = "a-zA-Z";

    private static String ALPHA_NUMERIC_CHARS = ALPHA_CHARS + "\\d";

    private static String SPECIAL_CHARS = ";/@&=,.?:+$";

    private static String VALID_CHARS = "[^\\s" + SPECIAL_CHARS + "]";

    private static String SCHEME_CHARS = ALPHA_CHARS;

    // Drop numeric, and  "+-." for now
    private static String AUTHORITY_CHARS = ALPHA_NUMERIC_CHARS + "\\-\\.";

    private static String ATOM = VALID_CHARS + '+';

    /**
     * This expression derived/taken from the BNF for URI (RFC2396).
     */
    private static String URL_PATTERN =
        "/^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?/";
    //                                                                      12            3  4          5       6   7        8 9

    /**
     * Schema/Protocol (ie. http:, ftp:, file:, etc).
     */
    private static int PARSE_URL_SCHEME = 2;

    /**
     * Includes hostname/ip and port number.
     */
    private static int PARSE_URL_AUTHORITY = 4;

    private static int PARSE_URL_PATH = 5;

    private static int PARSE_URL_QUERY = 7;

    private static int PARSE_URL_FRAGMENT = 9;

    /**
     * Protocol (ie. http:, ftp:,https:).
     */
    private static String SCHEME_PATTERN = "/^[" + SCHEME_CHARS + "]/";

    private static String AUTHORITY_PATTERN =
        "/^([" + AUTHORITY_CHARS + "]*)(:\\d*)?(.*)?/";
    //                                                                            1                          2  3       4

    private static int PARSE_AUTHORITY_HOST_IP = 1;

    private static int PARSE_AUTHORITY_PORT = 2;

    /**
     * Should always be empty.
     */
    private static int PARSE_AUTHORITY_EXTRA = 3;

    private static String PATH_PATTERN =
        "/^(/[-a-zA-Z0-9_:@&?=+,.!/~*'%$]*)$/";

    private static String QUERY_PATTERN = "/^(.*)$/";

    private static String LEGAL_ASCII_PATTERN = "/^[\\000-\\177]+$/";

    private static String IP_V4_DOMAIN_PATTERN =
        "/^(\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})$/";

    private static String DOMAIN_PATTERN =
        "/^" + ATOM + "(\\." + ATOM + ")*$/";

    private static String PORT_PATTERN = "/^:(\\d{1,5})$/";

    private static String ATOM_PATTERN = "/(" + ATOM + ")/";

    private static String ALPHA_PATTERN = "/^[" + ALPHA_CHARS + "]/";

    /**
     * The set of schemes that are allowed to be in a URL.
     */
    private Set allowedSchemes = new HashSet();

    /**
     * If no schemes are provided, default to this set.
     */
    protected String[] defaultSchemes = {"http", "https", "ftp" };

    /**
     * Create a UrlValidator with default properties.
     */
    public UrlValidator()
    {
        this( null );
    }

    /**
     * Behavior of validation is modified by passing in several strings options:
     *
     * @param schemes Pass in one or more url schemes to consider valid, passing in
     *                a null will default to "http,https,ftp" being valid.
     *                If a non-null schemes is specified then all valid schemes must
     *                be specified. Setting the ALLOW_ALL_SCHEMES option will
     *                ignore the contents of schemes.
     */
    public UrlValidator( String[] schemes )
    {
        this( schemes, 0 );
    }

    /**
     * Initialize a UrlValidator with the given validation options.
     *
     * @param options The options should be set using the public constants declared in
     *                this class.  To set multiple options you simply add them together.  For example,
     *                ALLOW_2_SLASHES + NO_FRAGMENTS enables both of those options.
     */
    public UrlValidator( int options )
    {
        this( null, options );
    }

    /**
     * Behavour of validation is modified by passing in options:
     *
     * @param schemes The set of valid schemes.
     * @param options The options should be set using the public constants declared in
     *                this class.  To set multiple options you simply add them together.  For example,
     */
    public UrlValidator( String[] schemes, int options )
    {
        if ( schemes == null )
        {
            schemes = this.defaultSchemes;
        }

        this.allowedSchemes.addAll( Arrays.asList( schemes ) );
    }

    /**
     * <p>Checks if a field has a valid url address.</p>
     *
     * @param value The value validation is being performed on.  A <code>null</code>
     *              value is considered invalid.
     * @return true if the url is valid.
     */
    public boolean validate( String value )
    {
        if ( value == null )
        {
            return false;
        }

        Perl5Util matchUrlPat = new Perl5Util();

        Perl5Util matchAsciiPat = new Perl5Util();

        if ( !matchAsciiPat.match( LEGAL_ASCII_PATTERN, value ) )
        {
            return false;
        }

        // Check the whole url address structure
        if ( !matchUrlPat.match( URL_PATTERN, value ) )
        {
            return false;
        }

        if ( !isValidScheme( matchUrlPat.group( PARSE_URL_SCHEME ) ) )
        {
            return false;
        }

        if ( !isValidAuthority( matchUrlPat.group( PARSE_URL_AUTHORITY ) ) )
        {
            return false;
        }

        if ( !isValidPath( matchUrlPat.group( PARSE_URL_PATH ) ) )
        {
            return false;
        }

        if ( !isValidQuery( matchUrlPat.group( PARSE_URL_QUERY ) ) )
        {
            return false;
        }

        if ( !isValidFragment( matchUrlPat.group( PARSE_URL_FRAGMENT ) ) )
        {
            return false;
        }

        return true;
    }

    /**
     * Validate scheme. If schemes[] was initialized to a non null,
     * then only those scheme's are allowed.  Note this is slightly different
     * than for the constructor.
     *
     * @param scheme The scheme to validate.  A <code>null</code> value is considered
     *               invalid.
     * @return true if valid.
     */
    protected boolean isValidScheme( String scheme )
    {
        if ( scheme == null )
        {
            return false;
        }

        Perl5Util schemeMatcher = new Perl5Util();
        if ( !schemeMatcher.match( SCHEME_PATTERN, scheme ) )
        {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the authority is properly formatted.  An authority is the combination
     * of hostname and port.  A <code>null</code> authority value is considered invalid.
     */
    protected boolean isValidAuthority( String authority )
    {
        if ( authority == null )
        {
            return false;
        }

        Perl5Util authorityMatcher = new Perl5Util();
        Perl5Util matchIPV4Pat = new Perl5Util();

        if ( !authorityMatcher.match( AUTHORITY_PATTERN, authority ) )
        {
            return false;
        }

        boolean ipV4Address;
        boolean hostname = false;

        // check if authority is IP address or hostname
        String hostIP = authorityMatcher.group( PARSE_AUTHORITY_HOST_IP );
        ipV4Address = matchIPV4Pat.match( IP_V4_DOMAIN_PATTERN, hostIP );

        if ( ipV4Address )
        {
            // this is an IP address so check components
            for ( int i = 1; i <= 4; i++ )
            {
                String ipSegment = matchIPV4Pat.group( i );
                if ( ipSegment == null || ipSegment.length() <= 0 )
                {
                    return false;
                }

                try
                {
                    if ( Integer.parseInt( ipSegment ) > 255 )
                    {
                        return false;
                    }
                }
                catch ( NumberFormatException e )
                {
                    return false;
                }

            }
        }
        else
        {
            // Domain is hostname name
            Perl5Util domainMatcher = new Perl5Util();
            hostname = domainMatcher.match( DOMAIN_PATTERN, hostIP );
        }

        //rightmost hostname will never start with a digit.
        if ( hostname )
        {
            String[] domainSegment = new String[10];
            boolean match = true;
            int segmentCount = 0;
            int segmentLength;
            Perl5Util atomMatcher = new Perl5Util();

            while ( match )
            {
                match = atomMatcher.match( ATOM_PATTERN, hostIP );
                if ( match )
                {
                    domainSegment[segmentCount] = atomMatcher.group( 1 );
                    segmentLength = domainSegment[segmentCount].length() + 1;
                    hostIP =
                        ( segmentLength >= hostIP.length() )
                        ? ""
                        : hostIP.substring( segmentLength );

                    segmentCount++;
                }
            }
            String topLevel = domainSegment[segmentCount - 1];
            if ( topLevel.length() < 2 || topLevel.length() > 4 )
            {
                return false;
            }

            // First letter of top level must be a alpha
            Perl5Util alphaMatcher = new Perl5Util();
            if ( !alphaMatcher.match( ALPHA_PATTERN, topLevel.substring( 0, 1 ) ) )
            {
                return false;
            }

            // Make sure there's a host name preceding the authority.
            if ( segmentCount < 2 )
            {
                return false;
            }
        }

        if ( !hostname && !ipV4Address )
        {
            return false;
        }

        String port = authorityMatcher.group( PARSE_AUTHORITY_PORT );
        if ( port != null )
        {
            Perl5Util portMatcher = new Perl5Util();
            if ( !portMatcher.match( PORT_PATTERN, port ) )
            {
                return false;
            }
        }

        String extra = authorityMatcher.group( PARSE_AUTHORITY_EXTRA );

        if ( !isBlankOrNull( extra ) )
        {
            return false;
        }

        return true;
    }

    protected boolean isBlankOrNull( String s )
    {
        return ( ( s == null ) || ( s.trim().length() ) == 0 );
    }

    /**
     * Returns true if the path is valid.  A <code>null</code> value is considered invalid.
     */
    protected boolean isValidPath( String path )
    {
        if ( path == null )
        {
            return false;
        }

        Perl5Util pathMatcher = new Perl5Util();

        if ( !pathMatcher.match( PATH_PATTERN, path ) )
        {
            return false;
        }

        if ( path.endsWith( "/" ) )
        {
            return false;
        }

        int slash2Count = countToken( "//", path );
        int slashCount = countToken( "/", path );
        int dot2Count = countToken( "..", path );

        if ( dot2Count > 0 )
        {
            if ( ( slashCount - slash2Count - 1 ) <= dot2Count )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the query is null or it's a properly formatted query string.
     */
    protected boolean isValidQuery( String query )
    {
        if ( query == null )
        {
            return true;
        }

        Perl5Util queryMatcher = new Perl5Util();
        return queryMatcher.match( QUERY_PATTERN, query );
    }

    /**
     * Returns true if the given fragment is null or fragments are allowed.
     *
     * @todo this alwyas return true
     */
    protected boolean isValidFragment( String fragment )
    {
        if ( fragment == null )
        {
            return true;
        }

        //return this.options.isOff( NO_FRAGMENTS );
        return true;
    }

    /**
     * Returns the number of times the token appears in the target.
     */
    protected int countToken( String token, String target )
    {
        int tokenIndex = 0;
        int count = 0;
        while ( tokenIndex != -1 )
        {
            tokenIndex = target.indexOf( token, tokenIndex );
            if ( tokenIndex > -1 )
            {
                tokenIndex++;
                count++;
            }
        }
        return count;
    }
}
