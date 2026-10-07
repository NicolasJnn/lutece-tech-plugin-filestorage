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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.portal.service.file.IFileDownloadUrlService;
import fr.paris.lutece.portal.service.file.implementation.DefaultFileDownloadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Named;

/**
 * Download service of the file storage: the encrypted links of the core, without expiry, served by the servlet of the plugin.
 */
@ApplicationScoped
@FileStorage
@Typed( IFileDownloadUrlService.class )
@Named( FileStorageFileDownloadService.BEAN_NAME )
public class FileStorageFileDownloadService extends DefaultFileDownloadService
{
    public static final String BEAN_NAME = "filestorage.fileDownloadService";
    public static final String SERVLET_PATH = "servlet/plugins/filestorage/file";

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValidityTime( )
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileDownloadUrlFO( String strFileKey, Map<String, String> additionnalData, String strFileStorageServiceProviderName )
    {
        return toServletUrl( super.getFileDownloadUrlFO( strFileKey, additionnalData, strFileStorageServiceProviderName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileDownloadUrlBO( String strFileKey, Map<String, String> additionnalData, String strFileStorageServiceProviderName )
    {
        return toServletUrl( super.getFileDownloadUrlBO( strFileKey, additionnalData, strFileStorageServiceProviderName ) );
    }

    /**
     * Sends a download link of the core to the servlet of the plugin.
     *
     * @param strUrl
     *            the link built by the core
     * @return the link to the servlet of the plugin, or null when the core returned none
     */
    static String toServletUrl( String strUrl )
    {
        if ( strUrl == null )
        {
            return null;
        }

        return StringUtils.replaceOnce( StringUtils.replaceOnce( strUrl, URL_FO, SERVLET_PATH ), URL_BO, SERVLET_PATH );
    }
}
