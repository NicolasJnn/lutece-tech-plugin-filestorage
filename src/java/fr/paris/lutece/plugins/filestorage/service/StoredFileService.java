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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.plugins.filestorage.business.StoredFile;
import fr.paris.lutece.plugins.filestorage.business.StoredFileHome;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Business operations on the file storage files: validation, storage of the content and catalogue.
 */
@ApplicationScoped
public class StoredFileService
{
    public static final String ERROR_FILE_UNREADABLE = "filestorage.message.error.fileUnreadable";

    private static final int HEADER_LENGTH = 512;
    private static final String TYPE_SEPARATOR = ",";

    @Inject
    @Named( FileStorageFileStoreServiceProviderProducer.BEAN_NAME )
    private IFileStoreServiceProvider _provider;

    @Inject
    @ConfigProperty( name = "filestorage.allowedTypes", defaultValue = "pdf,html,jpeg,png" )
    private String _strAllowedTypes;

    @Inject
    @ConfigProperty( name = "filestorage.maxFileSize", defaultValue = "15728640" )
    private long _lMaxFileSize;

    private StoredFileValidator _validator;

    /**
     * Builds the validator from the configuration.
     */
    @PostConstruct
    void init( )
    {
        _validator = new StoredFileValidator( Arrays.asList( _strAllowedTypes.split( TYPE_SEPARATOR ) ), _lMaxFileSize );
    }

    /**
     * Returns the maximum size of a file, in bytes.
     *
     * @return the maximum size
     */
    public long getMaxFileSize( )
    {
        return _lMaxFileSize;
    }

    /**
     * Returns the allowed types, as configured.
     *
     * @return the allowed types
     */
    public String getAllowedTypes( )
    {
        return Arrays.stream( _strAllowedTypes.split( TYPE_SEPARATOR ) ).map( String::trim ).filter( StringUtils::isNotEmpty ).collect( Collectors.joining( ", " ) );
    }

    /**
     * Validates an uploaded file.
     *
     * @param item
     *            the uploaded file, or null
     * @return the outcome, carrying the verified MIME type when the file is accepted
     */
    public StoredFileValidation validate( MultipartItem item )
    {
        if ( item == null )
        {
            return StoredFileValidation.refused( StoredFileValidator.ERROR_FILE_EMPTY );
        }

        byte [ ] header;

        try ( InputStream inputStream = item.getInputStream( ) )
        {
            header = inputStream.readNBytes( HEADER_LENGTH );
        }
        catch( IOException e )
        {
            AppLogService.error( "Unable to read an uploaded file", e );

            return StoredFileValidation.refused( ERROR_FILE_UNREADABLE );
        }

        return _validator.validate( item.getName( ), item.getContentType( ), item.getSize( ), header );
    }

    /**
     * Stores a validated file and adds it to the catalogue.
     *
     * @param item
     *            the uploaded file
     * @param validation
     *            the successful outcome of its validation
     * @param strTitle
     *            the title, or blank to use the file name
     * @param strDescription
     *            the description
     * @param strCreator
     *            the access code of the administrator
     * @return the created file
     * @throws FileServiceException
     *             if the content cannot be stored
     */
    public StoredFile create( MultipartItem item, StoredFileValidation validation, String strTitle, String strDescription, String strCreator )
            throws FileServiceException
    {
        String strFileName = StoredFileValidator.sanitizeFileName( item.getName( ) );
        String strKey = storeContent( item, strFileName, validation );

        StoredFile storedFile = new StoredFile( );
        storedFile.setFileKey( strKey );
        storedFile.setTitle( StringUtils.isBlank( strTitle ) ? strFileName : strTitle.trim( ) );
        storedFile.setDescription( StringUtils.defaultString( strDescription ).trim( ) );
        storedFile.setMimeType( validation.getMimeType( ) );
        storedFile.setFileSize( item.getSize( ) );
        storedFile.setDateCreation( new Timestamp( System.currentTimeMillis( ) ) );
        storedFile.setCreator( StringUtils.defaultString( strCreator ) );

        try
        {
            return StoredFileHome.create( storedFile );
        }
        catch( RuntimeException e )
        {
            deleteContent( strKey );
            throw e;
        }
    }

    /**
     * Replaces the content of a file by a validated file.
     *
     * @param storedFile
     *            the file
     * @param item
     *            the new uploaded file
     * @param validation
     *            the successful outcome of its validation
     * @throws FileServiceException
     *             if the content cannot be stored
     */
    public void replace( StoredFile storedFile, MultipartItem item, StoredFileValidation validation ) throws FileServiceException
    {
        String strOldKey = storedFile.getFileKey( );
        String strFileName = StoredFileValidator.sanitizeFileName( item.getName( ) );
        String strNewKey = storeContent( item, strFileName, validation );

        storedFile.setFileKey( strNewKey );
        storedFile.setMimeType( validation.getMimeType( ) );
        storedFile.setFileSize( item.getSize( ) );

        try
        {
            StoredFileHome.update( storedFile );
        }
        catch( RuntimeException e )
        {
            storedFile.setFileKey( strOldKey );
            deleteContent( strNewKey );
            throw e;
        }

        deleteContent( strOldKey );
    }

    /**
     * Updates the title and the description of a file.
     *
     * @param storedFile
     *            the file
     */
    public void update( StoredFile storedFile )
    {
        StoredFileHome.update( storedFile );
    }

    /**
     * Removes a file from the catalogue, then its content.
     *
     * @param storedFile
     *            the file
     */
    public void remove( StoredFile storedFile )
    {
        StoredFileHome.remove( storedFile.getIdFile( ) );
        deleteContent( storedFile.getFileKey( ) );
    }

    /**
     * Returns the front office download URL of a file.
     *
     * @param storedFile
     *            the file
     * @return the encrypted URL, relative to the webapp
     */
    public String getDownloadUrl( StoredFile storedFile )
    {
        return _provider.getFileDownloadUrlFO( storedFile.getFileKey( ) );
    }

    /**
     * Returns the back office download URL of a file.
     *
     * @param storedFile
     *            the file
     * @return the encrypted URL, relative to the webapp
     */
    public String getBackOfficeDownloadUrl( StoredFile storedFile )
    {
        return _provider.getFileDownloadUrlBO( storedFile.getFileKey( ) );
    }

    /**
     * Stores a validated content.
     *
     * @param item
     *            the uploaded file
     * @param strFileName
     *            the sanitized file name
     * @param validation
     *            the successful outcome of its validation
     * @return the key of the stored content
     * @throws FileServiceException
     *             if the content cannot be stored
     */
    private String storeContent( MultipartItem item, String strFileName, StoredFileValidation validation ) throws FileServiceException
    {
        try
        {
            return _provider.storeFileItem( new SanitizedMultipartItem( item, strFileName, validation.getMimeType( ) ) );
        }
        catch( RuntimeException e )
        {
            throw new FileServiceException( e.getMessage( ), e );
        }
    }

    /**
     * Deletes a content, logging the failure instead of propagating it.
     *
     * @param strKey
     *            the key of the content
     */
    private void deleteContent( String strKey )
    {
        try
        {
            _provider.delete( strKey );
        }
        catch( FileServiceException | RuntimeException e )
        {
            AppLogService.error( "Unable to delete the file storage content {}", strKey, e );
        }
    }
}
