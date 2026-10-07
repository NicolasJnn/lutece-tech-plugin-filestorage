/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.filestorage.service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Checks the type and the size of an uploaded file.
 * <p>
 * A type is checked on three points: the extension must belong to the allowed types, the MIME type declared by the client must be consistent with the
 * extension, and the content must carry the binary signature of the type. A type unknown to the signature table is checked on its extension only.
 * </p>
 */
public class StoredFileValidator
{
    public static final String ERROR_FILE_EMPTY = "filestorage.message.error.fileEmpty";
    public static final String ERROR_FILE_TOO_LARGE = "filestorage.message.error.fileTooLarge";
    public static final String ERROR_TYPE_NOT_ALLOWED = "filestorage.message.error.typeNotAllowed";
    public static final String ERROR_MIME_TYPE_MISMATCH = "filestorage.message.error.mimeTypeMismatch";
    public static final String ERROR_CONTENT_MISMATCH = "filestorage.message.error.contentMismatch";
    public static final String DEFAULT_FILE_NAME = "file";
    public static final int MAX_FILE_NAME_LENGTH = 255;

    private static final String MIME_OCTET_STREAM = "application/octet-stream";
    private static final int TEXT_CHECK_LENGTH = 512;
    private static final int MAX_EXTENSION_LENGTH = 16;

    private static final FileType TYPE_PDF = new FileType( "pdf", List.of( "pdf" ), List.of( "application/pdf", "application/x-pdf" ),
            "%PDF-".getBytes( StandardCharsets.US_ASCII ), false );
    private static final FileType TYPE_PNG = new FileType( "png", List.of( "png" ), List.of( "image/png", "image/x-png" ),
            new byte [ ] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, false );
    private static final FileType TYPE_JPEG = new FileType( "jpeg", List.of( "jpg", "jpeg" ), List.of( "image/jpeg", "image/pjpeg" ),
            new byte [ ] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, false );
    private static final FileType TYPE_HTML = new FileType( "html", List.of( "html", "htm" ), List.of( "text/html" ), null, true );
    private static final List<FileType> KNOWN_TYPES = List.of( TYPE_PDF, TYPE_PNG, TYPE_JPEG, TYPE_HTML );

    private final Map<String, FileType> _mapTypesByExtension = new HashMap<>( );
    private final long _lMaxFileSize;

    /**
     * Constructor.
     *
     * @param listAllowedTypes
     *            the allowed types, by name or by extension
     * @param lMaxFileSize
     *            the maximum size in bytes, inclusive
     */
    public StoredFileValidator( Collection<String> listAllowedTypes, long lMaxFileSize )
    {
        _lMaxFileSize = lMaxFileSize;

        for ( String strType : listAllowedTypes )
        {
            String strNormalized = ( strType == null ) ? "" : strType.trim( ).toLowerCase( Locale.ROOT );

            if ( !strNormalized.isEmpty( ) )
            {
                FileType type = findKnownType( strNormalized );

                if ( type == null )
                {
                    type = new FileType( strNormalized, List.of( strNormalized ), List.of( ), null, false );
                }

                for ( String strExtension : type.getExtensions( ) )
                {
                    _mapTypesByExtension.put( strExtension, type );
                }
            }
        }
    }

    /**
     * Validates a file.
     *
     * @param strFileName
     *            the file name
     * @param strDeclaredMimeType
     *            the MIME type declared by the client
     * @param lSize
     *            the size in bytes
     * @param header
     *            the first bytes of the content
     * @return the outcome, carrying the verified MIME type when the file is accepted
     */
    public StoredFileValidation validate( String strFileName, String strDeclaredMimeType, long lSize, byte [ ] header )
    {
        if ( lSize <= 0 )
        {
            return StoredFileValidation.refused( ERROR_FILE_EMPTY );
        }

        if ( lSize > _lMaxFileSize )
        {
            return StoredFileValidation.refused( ERROR_FILE_TOO_LARGE );
        }

        FileType type = _mapTypesByExtension.get( getExtension( sanitizeFileName( strFileName ) ) );

        if ( type == null )
        {
            return StoredFileValidation.refused( ERROR_TYPE_NOT_ALLOWED );
        }

        if ( !type.isKnown( ) )
        {
            return StoredFileValidation.accepted( MIME_OCTET_STREAM );
        }

        if ( !type.acceptsDeclaredMimeType( strDeclaredMimeType ) )
        {
            return StoredFileValidation.refused( ERROR_MIME_TYPE_MISMATCH );
        }

        if ( !type.matchesContent( header ) )
        {
            return StoredFileValidation.refused( ERROR_CONTENT_MISMATCH );
        }

        return StoredFileValidation.accepted( type.getCanonicalMimeType( ) );
    }

    /**
     * Cleans a file name: keeps its last path segment, removes the characters that would break a response header, and shortens it while keeping its
     * extension.
     *
     * @param strFileName
     *            the file name
     * @return the cleaned name, never empty
     */
    public static String sanitizeFileName( String strFileName )
    {
        if ( strFileName == null )
        {
            return DEFAULT_FILE_NAME;
        }

        int nLastSeparator = Math.max( strFileName.lastIndexOf( '/' ), strFileName.lastIndexOf( '\\' ) );
        String strSegment = ( nLastSeparator >= 0 ) ? strFileName.substring( nLastSeparator + 1 ) : strFileName;

        StringBuilder sbCleaned = new StringBuilder( strSegment.length( ) );

        for ( char c : strSegment.toCharArray( ) )
        {
            if ( c != '"' && c != '\\' && !Character.isISOControl( c ) )
            {
                sbCleaned.append( c );
            }
        }

        String strCleaned = sbCleaned.toString( ).trim( );

        if ( strCleaned.isEmpty( ) )
        {
            return DEFAULT_FILE_NAME;
        }

        return shorten( strCleaned );
    }

    /**
     * Shortens a name to the maximum length while keeping its extension.
     *
     * @param strName
     *            the name
     * @return the shortened name
     */
    private static String shorten( String strName )
    {
        if ( strName.length( ) <= MAX_FILE_NAME_LENGTH )
        {
            return strName;
        }

        int nDot = strName.lastIndexOf( '.' );
        int nExtensionLength = strName.length( ) - nDot;

        if ( nDot > 0 && nExtensionLength <= MAX_EXTENSION_LENGTH )
        {
            return strName.substring( 0, MAX_FILE_NAME_LENGTH - nExtensionLength ) + strName.substring( nDot );
        }

        return strName.substring( 0, MAX_FILE_NAME_LENGTH );
    }

    /**
     * Returns the lower case extension of a file name.
     *
     * @param strFileName
     *            the file name
     * @return the extension, or an empty string when there is none
     */
    private static String getExtension( String strFileName )
    {
        int nDot = strFileName.lastIndexOf( '.' );

        if ( nDot <= 0 || nDot == strFileName.length( ) - 1 )
        {
            return "";
        }

        return strFileName.substring( nDot + 1 ).toLowerCase( Locale.ROOT );
    }

    /**
     * Finds a known type by its name or by one of its extensions.
     *
     * @param strNameOrExtension
     *            the normalized name or extension
     * @return the type, or null when it is unknown
     */
    private static FileType findKnownType( String strNameOrExtension )
    {
        for ( FileType type : KNOWN_TYPES )
        {
            if ( type.getName( ).equals( strNameOrExtension ) || type.getExtensions( ).contains( strNameOrExtension ) )
            {
                return type;
            }
        }

        return null;
    }

    /**
     * Definition of a file type.
     */
    private static final class FileType
    {
        private final String _strName;
        private final List<String> _listExtensions;
        private final List<String> _listMimeTypes;
        private final byte [ ] _signature;
        private final boolean _bText;

        /**
         * Constructor.
         *
         * @param strName
         *            the type name
         * @param listExtensions
         *            the extensions of the type
         * @param listMimeTypes
         *            the accepted MIME types, the first one being the canonical type; empty for a type unknown to the signature table
         * @param signature
         *            the binary signature that starts the content, or null
         * @param bText
         *            true if the content must be text
         */
        FileType( String strName, List<String> listExtensions, List<String> listMimeTypes, byte [ ] signature, boolean bText )
        {
            _strName = strName;
            _listExtensions = listExtensions;
            _listMimeTypes = listMimeTypes;
            _signature = signature;
            _bText = bText;
        }

        /**
         * Returns the type name.
         *
         * @return the name
         */
        String getName( )
        {
            return _strName;
        }

        /**
         * Returns the extensions of the type.
         *
         * @return the extensions
         */
        List<String> getExtensions( )
        {
            return _listExtensions;
        }

        /**
         * Tells whether the type belongs to the signature table.
         *
         * @return true if the type is known
         */
        boolean isKnown( )
        {
            return !_listMimeTypes.isEmpty( );
        }

        /**
         * Returns the canonical MIME type.
         *
         * @return the MIME type
         */
        String getCanonicalMimeType( )
        {
            return _listMimeTypes.get( 0 );
        }

        /**
         * Tells whether a declared MIME type is consistent with the type. An absent or generic binary type is tolerated.
         *
         * @param strDeclaredMimeType
         *            the declared MIME type
         * @return true if it is consistent
         */
        boolean acceptsDeclaredMimeType( String strDeclaredMimeType )
        {
            if ( strDeclaredMimeType == null )
            {
                return true;
            }

            int nParameters = strDeclaredMimeType.indexOf( ';' );
            String strBase = ( ( nParameters >= 0 ) ? strDeclaredMimeType.substring( 0, nParameters ) : strDeclaredMimeType ).trim( )
                    .toLowerCase( Locale.ROOT );

            return strBase.isEmpty( ) || MIME_OCTET_STREAM.equals( strBase ) || _listMimeTypes.contains( strBase );
        }

        /**
         * Tells whether a content matches the type.
         *
         * @param header
         *            the first bytes of the content
         * @return true if it matches
         */
        boolean matchesContent( byte [ ] header )
        {
            if ( header == null )
            {
                return false;
            }

            if ( _signature != null )
            {
                return startsWith( header, _signature );
            }

            return !_bText || isText( header );
        }

        /**
         * Tells whether a content is text: no null byte, and no binary signature of a known type.
         *
         * @param header
         *            the first bytes of the content
         * @return true if it is text
         */
        private static boolean isText( byte [ ] header )
        {
            int nLength = Math.min( header.length, TEXT_CHECK_LENGTH );

            for ( int i = 0; i < nLength; i++ )
            {
                if ( header [i] == 0 )
                {
                    return false;
                }
            }

            for ( FileType type : KNOWN_TYPES )
            {
                if ( type._signature != null && startsWith( header, type._signature ) )
                {
                    return false;
                }
            }

            return true;
        }

        /**
         * Tells whether a content starts with a signature.
         *
         * @param header
         *            the first bytes of the content
         * @param signature
         *            the signature
         * @return true if it starts with it
         */
        private static boolean startsWith( byte [ ] header, byte [ ] signature )
        {
            if ( header.length < signature.length )
            {
                return false;
            }

            for ( int i = 0; i < signature.length; i++ )
            {
                if ( header [i] != signature [i] )
                {
                    return false;
                }
            }

            return true;
        }
    }
}
