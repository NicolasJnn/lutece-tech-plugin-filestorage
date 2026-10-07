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

import java.time.Duration;
import java.util.Set;

import fr.paris.lutece.plugins.filestorage.business.StoredFileHome;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Daemon removing from the disk store the files left behind by interrupted writes and deletions, and the files no longer in the
 * catalogue.
 */
public class FileStorageCleanupDaemon extends Daemon
{
    private static final String PROPERTY_FILE_STORE = "filestorage.fileStoreServiceProvider.fileStoreService";
    private static final String PROPERTY_MIN_AGE = "filestorage.cleanup.minAge";
    private static final long DEFAULT_MIN_AGE = 3600L;

    /**
     * Cleans the disk store, unless the files are kept elsewhere or the catalogue cannot be read.
     */
    @Override
    public void run( )
    {
        if ( !FileStorageFileSystemFileService.BEAN_NAME.equals( AppPropertiesService.getProperty( PROPERTY_FILE_STORE ) ) )
        {
            setLastRunLogs( "The file storage does not keep its files on disk: nothing to clean" );
            return;
        }

        Set<String> catalogueKeys;

        try
        {
            catalogueKeys = StoredFileHome.findFileKeys( );
        }
        catch( RuntimeException e )
        {
            AppLogService.error( "The file storage catalogue cannot be read: cleaning cancelled", e );
            setLastRunLogs( "The catalogue cannot be read: nothing removed" );
            return;
        }

        Duration minAge = Duration.ofSeconds( AppPropertiesService.getPropertyLong( PROPERTY_MIN_AGE, DEFAULT_MIN_AGE ) );
        FileStorageFileSystemFileService.CleanupReport report = CDI.current( ).select( FileStorageFileSystemFileService.class, FileStorage.Literal.INSTANCE ).get( )
                .cleanOrphans( catalogueKeys, minAge );

        setLastRunLogs( "Removed " + report.getTotal( ) + " entries (" + report + ")" );
    }
}
