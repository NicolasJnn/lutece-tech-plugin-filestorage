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

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * How a file of the file storage is presented by the browser.
 */
public final class StoredFileDisposition
{
    private static final String MIME_PDF = "application/pdf";
    private static final Set<String> DISPLAYED_TYPES = Set.of( MIME_PDF, "image/png", "image/jpeg", "text/html" );
    private static final String INLINE = "inline";
    private static final String ATTACHMENT = "attachment";
    private static final String PARAMETER_SEPARATOR = ";";
    private static final String MIME_BINARY = "application/octet-stream";
    private static final String SANDBOX = "sandbox";
    private static final String FRAME_ANCESTORS = "frame-ancestors 'self'";

    /**
     * Private constructor.
     */
    private StoredFileDisposition( )
    {
    }

    /**
     * Builds the Content-Disposition header of a file.
     *
     * @param strMimeType
     *            the MIME type of the file
     * @param strFileName
     *            the name of the file
     * @return the header value
     */
    public static String contentDisposition( String strMimeType, String strFileName )
    {
        String strType = DISPLAYED_TYPES.contains( normalize( strMimeType ) ) ? INLINE : ATTACHMENT;

        return strType + "; filename=\"" + StoredFileValidator.sanitizeFileName( strFileName ) + "\"";
    }

    /**
     * Builds the Content-Security-Policy header of a file: no file may be framed by another site, and everything but the PDF
     * is rendered in a sandbox, where no script runs and the page has no access to the origin of the site.
     *
     * @param strMimeType
     *            the MIME type of the file
     * @return the header value
     */
    public static String contentSecurityPolicy( String strMimeType )
    {
        return MIME_PDF.equals( normalize( strMimeType ) ) ? FRAME_ANCESTORS : SANDBOX + "; " + FRAME_ANCESTORS;
    }

    /**
     * Returns the type sent to the browser: the canonical type when it is one of the displayed types, a binary type otherwise.
     *
     * @param strMimeType
     *            the MIME type of the file
     * @return the content type
     */
    public static String contentType( String strMimeType )
    {
        String strType = normalize( strMimeType );

        return DISPLAYED_TYPES.contains( strType ) ? strType : MIME_BINARY;
    }

    /**
     * Reduces a MIME type to its lower case type and subtype.
     *
     * @param strMimeType
     *            the MIME type
     * @return the normalized MIME type, empty when null
     */
    private static String normalize( String strMimeType )
    {
        return StringUtils.substringBefore( StringUtils.defaultString( strMimeType ), PARAMETER_SEPARATOR ).trim( ).toLowerCase( Locale.ROOT );
    }
}
