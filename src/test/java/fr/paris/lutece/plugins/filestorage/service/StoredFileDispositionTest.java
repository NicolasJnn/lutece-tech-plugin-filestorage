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
 * Unit tests of {@link StoredFileDisposition}.
 */
public class StoredFileDispositionTest
{
    /**
     * The four types of the specification are displayed in the browser.
     */
    @Test
    public void testSpecifiedTypesAreDisplayed( )
    {
        for ( String strMimeType : new String [ ] { "application/pdf", "image/png", "image/jpeg", "text/html" } )
        {
            assertEquals( "inline; filename=\"a.bin\"", StoredFileDisposition.contentDisposition( strMimeType, "a.bin" ), strMimeType );
        }
    }

    /**
     * A type added by configuration without a known signature is still downloaded.
     */
    @Test
    public void testOtherTypesAreDownloaded( )
    {
        assertEquals( "attachment; filename=\"a.docx\"", StoredFileDisposition.contentDisposition( "application/octet-stream", "a.docx" ) );
        assertEquals( "attachment; filename=\"a\"", StoredFileDisposition.contentDisposition( null, "a" ) );
    }

    /**
     * Everything but the PDF is rendered in a sandbox; no file may be framed by another site.
     */
    @Test
    public void testContentSecurityPolicy( )
    {
        String strSandboxed = "sandbox; frame-ancestors 'self'";
        assertEquals( strSandboxed, StoredFileDisposition.contentSecurityPolicy( "text/html" ) );
        assertEquals( strSandboxed, StoredFileDisposition.contentSecurityPolicy( "image/png" ) );
        assertEquals( strSandboxed, StoredFileDisposition.contentSecurityPolicy( "image/jpeg" ) );
        assertEquals( strSandboxed, StoredFileDisposition.contentSecurityPolicy( "application/octet-stream" ) );
        assertEquals( strSandboxed, StoredFileDisposition.contentSecurityPolicy( null ) );
        assertEquals( "frame-ancestors 'self'", StoredFileDisposition.contentSecurityPolicy( "application/pdf" ) );
    }

    /**
     * The MIME type is compared without its parameters nor its case.
     */
    @Test
    public void testMimeTypeParametersAndCase( )
    {
        assertEquals( "inline; filename=\"a.html\"", StoredFileDisposition.contentDisposition( "Text/HTML; charset=UTF-8", "a.html" ) );
        assertEquals( "frame-ancestors 'self'", StoredFileDisposition.contentSecurityPolicy( "APPLICATION/PDF" ) );
    }

    /**
     * The type sent to the browser is the canonical one, never the stored value as is.
     */
    @Test
    public void testContentType( )
    {
        assertEquals( "text/html", StoredFileDisposition.contentType( "Text/HTML; charset=UTF-8" ) );
        assertEquals( "image/png", StoredFileDisposition.contentType( "image/png" ) );
        assertEquals( "application/octet-stream", StoredFileDisposition.contentType( "application/pdf, text/html" ) );
        assertEquals( "application/octet-stream", StoredFileDisposition.contentType( "image/svg+xml" ) );
        assertEquals( "application/octet-stream", StoredFileDisposition.contentType( null ) );
    }

    /**
     * A list of types is never taken for its first element.
     */
    @Test
    public void testTypeListIsDownloaded( )
    {
        assertEquals( "attachment; filename=\"a.pdf\"", StoredFileDisposition.contentDisposition( "application/pdf, text/html", "a.pdf" ) );
        assertEquals( "sandbox; frame-ancestors 'self'", StoredFileDisposition.contentSecurityPolicy( "application/pdf, text/html" ) );
    }

    /**
     * A file name that would break the header is sanitized.
     */
    @Test
    public void testFileNameIsSanitized( )
    {
        assertEquals( "inline; filename=\"ab.pdf\"", StoredFileDisposition.contentDisposition( "application/pdf", "a\"\r\nb.pdf" ) );
    }
}
