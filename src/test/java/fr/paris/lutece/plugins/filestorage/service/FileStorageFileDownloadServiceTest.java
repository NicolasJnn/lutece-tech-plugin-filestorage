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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the URL rewriting of {@link FileStorageFileDownloadService}.
 */
public class FileStorageFileDownloadServiceTest
{
    private static final String BASE = "http://localhost:8080/site/";
    private static final String QUERY = "?provider=fileStorageFileStoreProvider&#38;data=AbC-_==";

    /**
     * A front office link of the core is sent to the servlet of the plugin, its parameters untouched.
     */
    @Test
    public void testFrontOfficeUrlIsRewritten( )
    {
        assertEquals( BASE + "servlet/plugins/filestorage/file" + QUERY,
                FileStorageFileDownloadService.toServletUrl( BASE + "jsp/site/file/download" + QUERY ) );
    }

    /**
     * A back office link of the core is sent to the same servlet.
     */
    @Test
    public void testBackOfficeUrlIsRewritten( )
    {
        assertEquals( BASE + "servlet/plugins/filestorage/file" + QUERY,
                FileStorageFileDownloadService.toServletUrl( BASE + "jsp/admin/file/download" + QUERY ) );
    }

    /**
     * A null link, returned by the core when the encryption fails, stays null.
     */
    @Test
    public void testNullUrlStaysNull( )
    {
        assertEquals( null, FileStorageFileDownloadService.toServletUrl( null ) );
    }
}
