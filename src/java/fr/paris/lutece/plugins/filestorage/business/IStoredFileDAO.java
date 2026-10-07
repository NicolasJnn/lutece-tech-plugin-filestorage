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
import fr.paris.lutece.util.ReferenceList;

/**
 * Data access of the file storage files.
 */
public interface IStoredFileDAO
{
    /**
     * Inserts a file and sets its generated identifier.
     *
     * @param storedFile
     *            the file
     * @param plugin
     *            the plugin
     */
    void insert( StoredFile storedFile, Plugin plugin );

    /**
     * Updates a file.
     *
     * @param storedFile
     *            the file
     * @param plugin
     *            the plugin
     */
    void store( StoredFile storedFile, Plugin plugin );

    /**
     * Deletes a file.
     *
     * @param nIdFile
     *            the identifier
     * @param plugin
     *            the plugin
     */
    void delete( int nIdFile, Plugin plugin );

    /**
     * Loads a file.
     *
     * @param nIdFile
     *            the identifier
     * @param plugin
     *            the plugin
     * @return the file, or null when it does not exist
     */
    StoredFile load( int nIdFile, Plugin plugin );

    /**
     * Loads all the files, the most recent first.
     *
     * @param plugin
     *            the plugin
     * @return the files
     */
    List<StoredFile> selectAll( Plugin plugin );

    /**
     * Loads the files whose title contains a term, the most recent first.
     *
     * @param strTerm
     *            the searched term
     * @param plugin
     *            the plugin
     * @return the files
     */
    List<StoredFile> selectByTitle( String strTerm, Plugin plugin );

    /**
     * Loads the keys of all the stored contents.
     *
     * @param plugin
     *            the plugin
     * @return the keys
     */
    Set<String> selectFileKeys( Plugin plugin );

    /**
     * Loads the identifiers and titles of all the files.
     *
     * @param plugin
     *            the plugin
     * @return the reference list
     */
    ReferenceList selectReferenceList( Plugin plugin );
}
