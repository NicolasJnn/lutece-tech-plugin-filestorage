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

import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.portal.service.file.IFileDownloadUrlService;
import fr.paris.lutece.portal.service.file.IFileRBACService;
import fr.paris.lutece.portal.service.file.IFileStoreService;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.file.implementation.FileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.CdiHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Produces the file store service provider of the file storage from the three implementations named in the configuration.
 */
@ApplicationScoped
public class FileStorageFileStoreServiceProviderProducer
{
    public static final String BEAN_NAME = "filestorage.fileStoreServiceProvider";
    public static final String PROVIDER_NAME = "fileStorageFileStoreProvider";

    /**
     * Produces the provider.
     *
     * @param strFileStoreImplName
     *            the bean name of the file store service
     * @param strRbacImplName
     *            the bean name of the RBAC service applied to downloads
     * @param strDownloadImplName
     *            the bean name of the download URL service
     * @return the provider
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_NAME )
    public IFileStoreServiceProvider produceFileStoreServiceProvider(
            @ConfigProperty( name = "filestorage.fileStoreServiceProvider.fileStoreService" ) String strFileStoreImplName,
            @ConfigProperty( name = "filestorage.fileStoreServiceProvider.rbacService" ) String strRbacImplName,
            @ConfigProperty( name = "filestorage.fileStoreServiceProvider.downloadService" ) String strDownloadImplName )
    {
        return new FileStoreServiceProvider( PROVIDER_NAME, CdiHelper.getReference( IFileStoreService.class, strFileStoreImplName ),
                CdiHelper.getReference( IFileDownloadUrlService.class, strDownloadImplName ),
                CdiHelper.getReference( IFileRBACService.class, strRbacImplName ), false );
    }
}
