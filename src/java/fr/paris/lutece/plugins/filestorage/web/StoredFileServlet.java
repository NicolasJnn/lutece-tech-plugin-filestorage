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
package fr.paris.lutece.plugins.filestorage.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import fr.paris.lutece.plugins.filestorage.service.StoredFileDisposition;
import fr.paris.lutece.plugins.filestorage.service.FileStorageFileDownloadService;
import fr.paris.lutece.plugins.filestorage.service.FileStorageFileStoreServiceProviderProducer;
import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.securityheader.SecurityHeader;
import fr.paris.lutece.portal.business.securityheader.SecurityHeaderPageCategory;
import fr.paris.lutece.portal.business.securityheader.SecurityHeaderType;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.file.ExpiredLinkException;
import fr.paris.lutece.portal.service.file.FileService;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileDownloadUrlService;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.message.SiteMessageService;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.securityheader.SecurityHeaderService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.util.CdiHelper;
import fr.paris.lutece.portal.web.PortalJspBean;
import fr.paris.lutece.util.securityheader.SecurityHeaderUtil;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the files of the file storage in the browser, from the encrypted links of the core.
 */
public class StoredFileServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_UNKNOWN_PROVIDER = "portal.file.download.provider.unknown";
    private static final String MESSAGE_UNKNOWN_FILE = "portal.file.download.file.unknown";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String NOSNIFF = "nosniff";
    private static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String NO_REFERRER = "no-referrer";
    private static final String CACHE_PUBLIC = "public, max-age=";
    private static final String PROPERTY_CACHE_MAX_AGE = "filestorage.cacheMaxAge";
    private static final int DEFAULT_CACHE_MAX_AGE = 3600;
    private static final String PROPERTY_DOWNLOAD_SERVICE = "filestorage.fileStoreServiceProvider.downloadService";

    /**
     * Serves the file designated by the encrypted link, streaming its content.
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws IOException
     *             if the response cannot be written
     */
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        try
        {
            IFileStoreServiceProvider provider = getProvider( request );
            String strKey = getAuthorizedKey( request, provider );
            File file = null;
            InputStream inputStream = null;

            try
            {
                file = provider.getFileMetaData( strKey );
                inputStream = ( file == null ) ? null : provider.getInputStream( strKey );
            }
            catch( FileServiceException | RuntimeException e )
            {
                AppLogService.error( "Unable to read the file storage file {}", strKey, e );
            }

            if ( inputStream == null )
            {
                SiteMessageService.setMessage( request, MESSAGE_UNKNOWN_FILE );
                return;
            }

            try ( InputStream content = inputStream )
            {
                write( request, file, content, response );
            }
        }
        catch( UserNotSignedException e )
        {
            response.sendRedirect( response.encodeRedirectURL( PortalJspBean.redirectLogin( request ) ) );
        }
        catch( SiteMessageException e )
        {
            response.sendRedirect( AppPathService.getSiteMessageUrl( request ) );
        }
    }

    /**
     * Returns the provider of the file storage, whatever the provider named by the request.
     *
     * @param request
     *            the request
     * @return the provider
     * @throws SiteMessageException
     *             when the provider is not available
     */
    private static IFileStoreServiceProvider getProvider( HttpServletRequest request ) throws SiteMessageException
    {
        try
        {
            return CDI.current( ).select( FileService.class ).get( ).getFileStoreServiceProvider( FileStorageFileStoreServiceProviderProducer.PROVIDER_NAME );
        }
        catch( RuntimeException e )
        {
            AppLogService.error( "The file storage file provider is not available", e );
            SiteMessageService.setMessage( request, MESSAGE_UNKNOWN_PROVIDER );
            return null;
        }
    }

    /**
     * Decodes the link, then checks the access rights and the validity, as the core does before serving a file.
     *
     * @param request
     *            the request
     * @param provider
     *            the provider of the file storage
     * @return the key of the file
     * @throws SiteMessageException
     *             when the link is refused
     * @throws UserNotSignedException
     *             when the provider requires an authenticated user
     */
    private static String getAuthorizedKey( HttpServletRequest request, IFileStoreServiceProvider provider )
            throws SiteMessageException, UserNotSignedException
    {
        Map<String, String> fileData = null;

        try
        {
            IFileDownloadUrlService downloadService = CdiHelper.getReference( IFileDownloadUrlService.class,
                    AppPropertiesService.getProperty( PROPERTY_DOWNLOAD_SERVICE, FileStorageFileDownloadService.BEAN_NAME ) );
            fileData = downloadService.getRequestDataFO( request );

            if ( fileData != null )
            {
                provider.checkAccessRights( fileData, SecurityService.getInstance( ).getRegisteredUser( request ) );
                provider.checkLinkValidity( fileData );
            }
        }
        catch( AccessDeniedException | ExpiredLinkException e )
        {
            SiteMessageService.setMessage( request, e.getLocalizedMessage( ) );
        }
        catch( RuntimeException e )
        {
            AppLogService.error( "Unable to decode a file storage link", e );
            fileData = null;
        }

        if ( fileData == null || fileData.get( FileService.PARAMETER_FILE_ID ) == null )
        {
            SiteMessageService.setMessage( request, MESSAGE_UNKNOWN_FILE );
        }

        return fileData.get( FileService.PARAMETER_FILE_ID );
    }

    /**
     * Writes a file with the headers that present it safely in the browser, copying its content as a stream.
     *
     * @param request
     *            the request
     * @param file
     *            the metadata of the file
     * @param content
     *            the content
     * @param response
     *            the response
     * @throws IOException
     *             if the response cannot be written
     */
    private static void write( HttpServletRequest request, File file, InputStream content, HttpServletResponse response ) throws IOException
    {
        addSiteSecurityHeaders( request, response );
        response.setContentType( StoredFileDisposition.contentType( file.getMimeType( ) ) );

        if ( file.getSize( ) > 0 )
        {
            response.setContentLength( file.getSize( ) );
        }

        response.setHeader( HEADER_CONTENT_TYPE_OPTIONS, NOSNIFF );
        response.setHeader( HEADER_CONTENT_DISPOSITION, StoredFileDisposition.contentDisposition( file.getMimeType( ), file.getTitle( ) ) );
        response.setHeader( HEADER_CONTENT_SECURITY_POLICY, StoredFileDisposition.contentSecurityPolicy( file.getMimeType( ) ) );
        response.setHeader( HEADER_REFERRER_POLICY, NO_REFERRER );
        response.setHeader( HEADER_CACHE_CONTROL, CACHE_PUBLIC + AppPropertiesService.getPropertyInt( PROPERTY_CACHE_MAX_AGE, DEFAULT_CACHE_MAX_AGE ) );

        content.transferTo( response.getOutputStream( ) );
    }

    /**
     * Adds the security headers that the site applies to all its pages, as managed in the back office.
     *
     * @param request
     *            the request
     * @param response
     *            the response
     */
    private static void addSiteSecurityHeaders( HttpServletRequest request, HttpServletResponse response )
    {
        try
        {
            Collection<SecurityHeader> listHeaders = CDI.current( ).select( SecurityHeaderService.class ).get( )
                    .findActive( SecurityHeaderType.PAGE.getCode( ), SecurityHeaderPageCategory.ALL.getCode( ) );

            if ( listHeaders != null )
            {
                for ( SecurityHeader header : listHeaders )
                {
                    response.setHeader( header.getName( ), SecurityHeaderUtil.getSecurityHeaderValue( request, header ) );
                }
            }
        }
        catch( RuntimeException e )
        {
            AppLogService.error( "Unable to apply the security headers of the site to a file storage file", e );
        }
    }
}
