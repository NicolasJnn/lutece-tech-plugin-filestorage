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
package fr.paris.lutece.plugins.filestorage.business;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Data access of the file storage files.
 */
@ApplicationScoped
public class StoredFileDAO implements IStoredFileDAO
{
    private static final String SQL_QUERY_SELECTALL = "SELECT id_file, file_key, title, description, mime_type, file_size, date_creation, creator FROM filestorage_file";
    private static final String SQL_ORDER_BY_DATE = " ORDER BY date_creation DESC, id_file DESC";
    private static final String SQL_QUERY_SELECT = SQL_QUERY_SELECTALL + " WHERE id_file = ?";
    private static final String SQL_QUERY_SELECT_BY_TITLE = SQL_QUERY_SELECTALL + " WHERE LOWER( title ) LIKE ? ESCAPE '!'" + SQL_ORDER_BY_DATE;
    private static final String SQL_QUERY_SELECT_KEYS = "SELECT file_key FROM filestorage_file";
    private static final String SQL_QUERY_SELECT_REFERENCE = "SELECT id_file, title FROM filestorage_file ORDER BY title";
    private static final String SQL_QUERY_INSERT = "INSERT INTO filestorage_file ( file_key, title, description, mime_type, file_size, date_creation, creator ) VALUES ( ?, ?, ?, ?, ?, ?, ? )";
    private static final String SQL_QUERY_UPDATE = "UPDATE filestorage_file SET file_key = ?, title = ?, description = ?, mime_type = ?, file_size = ? WHERE id_file = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM filestorage_file WHERE id_file = ?";
    private static final char LIKE_ESCAPE = '!';

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( StoredFile storedFile, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, storedFile.getFileKey( ) );
            daoUtil.setString( nIndex++, storedFile.getTitle( ) );
            daoUtil.setString( nIndex++, storedFile.getDescription( ) );
            daoUtil.setString( nIndex++, storedFile.getMimeType( ) );
            daoUtil.setLong( nIndex++, storedFile.getFileSize( ) );
            daoUtil.setTimestamp( nIndex++, storedFile.getDateCreation( ) );
            daoUtil.setString( nIndex, storedFile.getCreator( ) );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                storedFile.setIdFile( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( StoredFile storedFile, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, storedFile.getFileKey( ) );
            daoUtil.setString( nIndex++, storedFile.getTitle( ) );
            daoUtil.setString( nIndex++, storedFile.getDescription( ) );
            daoUtil.setString( nIndex++, storedFile.getMimeType( ) );
            daoUtil.setLong( nIndex++, storedFile.getFileSize( ) );
            daoUtil.setInt( nIndex, storedFile.getIdFile( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nIdFile, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nIdFile );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredFile load( int nIdFile, Plugin plugin )
    {
        StoredFile storedFile = null;

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nIdFile );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                storedFile = dataToObject( daoUtil );
            }
        }

        return storedFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StoredFile> selectAll( Plugin plugin )
    {
        return selectList( SQL_QUERY_SELECTALL + SQL_ORDER_BY_DATE, null, plugin );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StoredFile> selectByTitle( String strTerm, Plugin plugin )
    {
        String strPattern = "%" + escapeLike( ( strTerm == null ) ? "" : strTerm.toLowerCase( Locale.ROOT ) ) + "%";

        return selectList( SQL_QUERY_SELECT_BY_TITLE, strPattern, plugin );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList selectReferenceList( Plugin plugin )
    {
        ReferenceList referenceList = new ReferenceList( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_REFERENCE, plugin ) )
        {
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                referenceList.addItem( daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
        }

        return referenceList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> selectFileKeys( Plugin plugin )
    {
        Set<String> setKeys = new HashSet<>( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_KEYS, plugin ) )
        {
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                setKeys.add( daoUtil.getString( 1 ) );
            }
        }

        return setKeys;
    }

    /**
     * Loads a list of files.
     *
     * @param strQuery
     *            the query
     * @param strParameter
     *            the only parameter of the query, or null
     * @param plugin
     *            the plugin
     * @return the files
     */
    private List<StoredFile> selectList( String strQuery, String strParameter, Plugin plugin )
    {
        List<StoredFile> listFiles = new ArrayList<>( );

        try ( DAOUtil daoUtil = new DAOUtil( strQuery, plugin ) )
        {
            if ( strParameter != null )
            {
                daoUtil.setString( 1, strParameter );
            }

            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                listFiles.add( dataToObject( daoUtil ) );
            }
        }

        return listFiles;
    }

    /**
     * Escapes the wildcards of a LIKE pattern.
     *
     * @param strTerm
     *            the searched term
     * @return the escaped term
     */
    private static String escapeLike( String strTerm )
    {
        StringBuilder sbEscaped = new StringBuilder( strTerm.length( ) );

        for ( char c : strTerm.toCharArray( ) )
        {
            if ( c == LIKE_ESCAPE || c == '%' || c == '_' )
            {
                sbEscaped.append( LIKE_ESCAPE );
            }

            sbEscaped.append( c );
        }

        return sbEscaped.toString( );
    }

    /**
     * Builds a file from the current row.
     *
     * @param daoUtil
     *            the data access utility positioned on a row
     * @return the file
     */
    private static StoredFile dataToObject( DAOUtil daoUtil )
    {
        int nIndex = 1;
        StoredFile storedFile = new StoredFile( );
        storedFile.setIdFile( daoUtil.getInt( nIndex++ ) );
        storedFile.setFileKey( daoUtil.getString( nIndex++ ) );
        storedFile.setTitle( daoUtil.getString( nIndex++ ) );
        storedFile.setDescription( daoUtil.getString( nIndex++ ) );
        storedFile.setMimeType( daoUtil.getString( nIndex++ ) );
        storedFile.setFileSize( daoUtil.getLong( nIndex++ ) );
        storedFile.setDateCreation( daoUtil.getTimestamp( nIndex++ ) );
        storedFile.setCreator( daoUtil.getString( nIndex ) );

        return storedFile;
    }
}
