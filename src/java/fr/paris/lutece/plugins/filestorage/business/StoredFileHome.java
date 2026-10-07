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

import java.util.List;
import java.util.Set;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.util.ReferenceList;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Static facade of the file storage files.
 */
public final class StoredFileHome
{
    public static final String PLUGIN_NAME = "filestorage";

    private static IStoredFileDAO _dao = CDI.current( ).select( IStoredFileDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( PLUGIN_NAME );

    /**
     * Private constructor.
     */
    private StoredFileHome( )
    {
    }

    /**
     * Creates a file.
     *
     * @param storedFile
     *            the file
     * @return the file, with its generated identifier
     */
    public static StoredFile create( StoredFile storedFile )
    {
        _dao.insert( storedFile, _plugin );

        return storedFile;
    }

    /**
     * Updates a file.
     *
     * @param storedFile
     *            the file
     */
    public static void update( StoredFile storedFile )
    {
        _dao.store( storedFile, _plugin );
    }

    /**
     * Removes a file.
     *
     * @param nIdFile
     *            the identifier
     */
    public static void remove( int nIdFile )
    {
        _dao.delete( nIdFile, _plugin );
    }

    /**
     * Finds a file.
     *
     * @param nIdFile
     *            the identifier
     * @return the file, or null when it does not exist
     */
    public static StoredFile findByPrimaryKey( int nIdFile )
    {
        return _dao.load( nIdFile, _plugin );
    }

    /**
     * Finds all the files, the most recent first.
     *
     * @return the files
     */
    public static List<StoredFile> findAll( )
    {
        return _dao.selectAll( _plugin );
    }

    /**
     * Finds the files whose title contains a term.
     *
     * @param strTerm
     *            the searched term
     * @return the files
     */
    public static List<StoredFile> findByTitle( String strTerm )
    {
        return _dao.selectByTitle( strTerm, _plugin );
    }

    /**
     * Returns the keys of all the stored contents.
     *
     * @return the keys
     */
    public static Set<String> findFileKeys( )
    {
        return _dao.selectFileKeys( _plugin );
    }

    /**
     * Returns the identifiers and titles of all the files.
     *
     * @return the reference list
     */
    public static ReferenceList getReferenceList( )
    {
        return _dao.selectReferenceList( _plugin );
    }
}
