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

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.util.ReferenceList;
import jakarta.inject.Inject;

/**
 * Round trip of {@link StoredFileDAO} against its public API.
 */
public class StoredFileDAOTest extends LuteceTestCase
{
    private static final String TITLE = "PWTEST-rapport-annuel.pdf";
    private static final String TITLE_MODIFIED = "PWTEST-rapport-annuel-v2.pdf";

    @Inject
    private IStoredFileDAO _dao;

    /**
     * Inserts, reads, updates, searches and deletes a file.
     */
    @Test
    public void testRoundTrip( )
    {
        Plugin plugin = null;
        StoredFile storedFile = newStoredFile( );

        try
        {
            _dao.insert( storedFile, plugin );
            assertTrue( storedFile.getIdFile( ) > 0, "the insertion must set a generated identifier" );

            StoredFile loaded = _dao.load( storedFile.getIdFile( ), plugin );
            assertNotNull( loaded, "the inserted file must be found" );
            assertEquals( "key-42", loaded.getFileKey( ) );
            assertEquals( TITLE, loaded.getTitle( ) );
            assertEquals( "Annual report", loaded.getDescription( ) );
            assertEquals( "application/pdf", loaded.getMimeType( ) );
            assertEquals( 15728640L, loaded.getFileSize( ), "a size of 15 MB must survive the round trip" );
            assertEquals( "admin", loaded.getCreator( ) );
            assertNotNull( loaded.getDateCreation( ) );

            loaded.setTitle( TITLE_MODIFIED );
            loaded.setDescription( "Annual report, second version" );
            _dao.store( loaded, plugin );
            StoredFile updated = _dao.load( storedFile.getIdFile( ), plugin );
            assertEquals( TITLE_MODIFIED, updated.getTitle( ) );
            assertEquals( "Annual report, second version", updated.getDescription( ) );
            assertEquals( "key-42", updated.getFileKey( ), "the update must not change the key" );

            assertTrue( containsId( _dao.selectAll( plugin ), storedFile.getIdFile( ) ), "the file must be listed" );
            assertTrue( _dao.selectFileKeys( plugin ).contains( "key-42" ), "the key must be listed among the stored keys" );
            assertTrue( containsId( _dao.selectByTitle( "annuel-v2", plugin ), storedFile.getIdFile( ) ), "the search must find the file" );
            assertFalse( containsId( _dao.selectByTitle( "no-such-title", plugin ), storedFile.getIdFile( ) ), "the search must filter" );

            ReferenceList referenceList = _dao.selectReferenceList( plugin );
            assertTrue( referenceList.stream( ).anyMatch( item -> String.valueOf( storedFile.getIdFile( ) ).equals( item.getCode( ) )
                    && TITLE_MODIFIED.equals( item.getName( ) ) ), "the reference list must carry the identifier and the title" );
        }
        finally
        {
            _dao.delete( storedFile.getIdFile( ), plugin );
        }

        assertNull( _dao.load( storedFile.getIdFile( ), plugin ), "the deleted file must not be found" );
    }

    /**
     * Builds a file to insert.
     *
     * @return the file
     */
    private static StoredFile newStoredFile( )
    {
        StoredFile storedFile = new StoredFile( );
        storedFile.setFileKey( "key-42" );
        storedFile.setTitle( TITLE );
        storedFile.setDescription( "Annual report" );
        storedFile.setMimeType( "application/pdf" );
        storedFile.setFileSize( 15728640L );
        storedFile.setDateCreation( new Timestamp( System.currentTimeMillis( ) ) );
        storedFile.setCreator( "admin" );
        return storedFile;
    }

    /**
     * Tells whether a list contains a file.
     *
     * @param listFiles
     *            the files
     * @param nIdFile
     *            the identifier
     * @return true if it is in the list
     */
    private static boolean containsId( List<StoredFile> listFiles, int nIdFile )
    {
        return listFiles.stream( ).anyMatch( file -> file.getIdFile( ) == nIdFile );
    }
}
