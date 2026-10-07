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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link StoredFileValidator}.
 */
public class StoredFileValidatorTest
{
    private static final long MAX_SIZE = 15L * 1024 * 1024;
    private static final byte [ ] PDF = "%PDF-1.7\n".getBytes( StandardCharsets.US_ASCII );
    private static final byte [ ] PNG = new byte [ ] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00 };
    private static final byte [ ] JPEG = new byte [ ] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00 };
    private static final byte [ ] HTML = "<!DOCTYPE html><html><body>ok</body></html>".getBytes( StandardCharsets.UTF_8 );
    private static final byte [ ] EXE = new byte [ ] { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00 };

    private final StoredFileValidator _validator = new StoredFileValidator( List.of( "pdf", "html", "jpeg", "png" ), MAX_SIZE );

    /**
     * Each type of the specification is accepted with its canonical MIME type.
     */
    @Test
    public void testAcceptsTheFourSpecifiedTypes( )
    {
        assertAccepted( "rapport.pdf", "application/pdf", PDF, "application/pdf" );
        assertAccepted( "logo.png", "image/png", PNG, "image/png" );
        assertAccepted( "photo.jpg", "image/jpeg", JPEG, "image/jpeg" );
        assertAccepted( "photo.jpeg", "image/jpeg", JPEG, "image/jpeg" );
        assertAccepted( "page.html", "text/html", HTML, "text/html" );
        assertAccepted( "page.htm", "text/html", HTML, "text/html" );
    }

    /**
     * The extension check ignores case.
     */
    @Test
    public void testExtensionIsCaseInsensitive( )
    {
        assertAccepted( "RAPPORT.PDF", "application/pdf", PDF, "application/pdf" );
    }

    /**
     * Browsers that send no type or the generic binary type are not refused for it.
     */
    @Test
    public void testTolerantToMissingOrGenericDeclaredType( )
    {
        assertAccepted( "rapport.pdf", null, PDF, "application/pdf" );
        assertAccepted( "rapport.pdf", "", PDF, "application/pdf" );
        assertAccepted( "rapport.pdf", "application/octet-stream", PDF, "application/pdf" );
    }

    /**
     * Known aliases and type parameters are accepted.
     */
    @Test
    public void testAcceptsMimeAliasesAndParameters( )
    {
        assertAccepted( "rapport.pdf", "application/x-pdf", PDF, "application/pdf" );
        assertAccepted( "photo.jpg", "image/pjpeg", JPEG, "image/jpeg" );
        assertAccepted( "page.html", "text/html; charset=UTF-8", HTML, "text/html" );
    }

    /**
     * An extension outside the configured list is refused.
     */
    @Test
    public void testRefusesExtensionNotAllowed( )
    {
        assertRefused( "programme.exe", "application/octet-stream", EXE, StoredFileValidator.ERROR_TYPE_NOT_ALLOWED );
        assertRefused( "script.js", "text/javascript", HTML, StoredFileValidator.ERROR_TYPE_NOT_ALLOWED );
    }

    /**
     * A file without extension is refused.
     */
    @Test
    public void testRefusesFileWithoutExtension( )
    {
        assertRefused( "rapport", "application/pdf", PDF, StoredFileValidator.ERROR_TYPE_NOT_ALLOWED );
    }

    /**
     * A declared type inconsistent with the extension is refused.
     */
    @Test
    public void testRefusesDeclaredTypeInconsistentWithExtension( )
    {
        assertRefused( "rapport.pdf", "image/png", PDF, StoredFileValidator.ERROR_MIME_TYPE_MISMATCH );
    }

    /**
     * A binary renamed with an allowed extension is refused on its content.
     */
    @Test
    public void testRefusesContentNotMatchingTheExtension( )
    {
        assertRefused( "rapport.pdf", "application/pdf", EXE, StoredFileValidator.ERROR_CONTENT_MISMATCH );
        assertRefused( "logo.png", "image/png", JPEG, StoredFileValidator.ERROR_CONTENT_MISMATCH );
    }

    /**
     * A binary file presented as HTML is refused.
     */
    @Test
    public void testRefusesBinaryPresentedAsHtml( )
    {
        assertRefused( "page.html", "text/html", EXE, StoredFileValidator.ERROR_CONTENT_MISMATCH );
        assertRefused( "page.html", "text/html", PDF, StoredFileValidator.ERROR_CONTENT_MISMATCH );
    }

    /**
     * The maximum size is inclusive.
     */
    @Test
    public void testAcceptsExactlyTheMaximumSize( )
    {
        StoredFileValidation result = _validator.validate( "rapport.pdf", "application/pdf", MAX_SIZE, PDF );
        assertTrue( result.isValid( ), "a file of exactly the maximum size must be accepted" );
    }

    /**
     * One byte over the maximum size is refused.
     */
    @Test
    public void testRefusesOneByteOverTheMaximumSize( )
    {
        StoredFileValidation result = _validator.validate( "rapport.pdf", "application/pdf", MAX_SIZE + 1, PDF );
        assertFalse( result.isValid( ) );
        assertEquals( StoredFileValidator.ERROR_FILE_TOO_LARGE, result.getErrorKey( ) );
    }

    /**
     * An empty file is refused.
     */
    @Test
    public void testRefusesEmptyFile( )
    {
        StoredFileValidation result = _validator.validate( "rapport.pdf", "application/pdf", 0, new byte [ 0 ] );
        assertFalse( result.isValid( ) );
        assertEquals( StoredFileValidator.ERROR_FILE_EMPTY, result.getErrorKey( ) );
    }

    /**
     * Removing a type from the configuration refuses it.
     */
    @Test
    public void testConfigurationRestrictsTheTypes( )
    {
        StoredFileValidator pdfOnly = new StoredFileValidator( List.of( "pdf" ), MAX_SIZE );
        assertTrue( pdfOnly.validate( "rapport.pdf", "application/pdf", PDF.length, PDF ).isValid( ) );
        StoredFileValidation result = pdfOnly.validate( "logo.png", "image/png", PNG.length, PNG );
        assertFalse( result.isValid( ) );
        assertEquals( StoredFileValidator.ERROR_TYPE_NOT_ALLOWED, result.getErrorKey( ) );
    }

    /**
     * Configured types are normalised.
     */
    @Test
    public void testConfigurationIsNormalised( )
    {
        StoredFileValidator validator = new StoredFileValidator( List.of( " PDF ", "", "jpg" ), MAX_SIZE );
        assertTrue( validator.validate( "rapport.pdf", "application/pdf", PDF.length, PDF ).isValid( ) );
        assertTrue( validator.validate( "photo.jpeg", "image/jpeg", JPEG.length, JPEG ).isValid( ) );
    }

    /**
     * A type unknown to the signature table is checked on its extension only and stored as generic binary.
     */
    @Test
    public void testUnknownConfiguredTypeIsCheckedOnExtensionOnly( )
    {
        StoredFileValidator validator = new StoredFileValidator( List.of( "csv" ), MAX_SIZE );
        byte [ ] csv = "a;b\n1;2\n".getBytes( StandardCharsets.UTF_8 );
        StoredFileValidation result = validator.validate( "data.csv", "text/csv", csv.length, csv );
        assertTrue( result.isValid( ) );
        assertEquals( "application/octet-stream", result.getMimeType( ) );
    }

    /**
     * File names are cleaned of characters that would break a response header.
     */
    @Test
    public void testSanitizeFileNameRemovesHeaderBreakingCharacters( )
    {
        assertEquals( "rapport.pdf", StoredFileValidator.sanitizeFileName( "rap\"port.pdf" ) );
        assertEquals( "rapport.pdf", StoredFileValidator.sanitizeFileName( "rap\r\nport.pdf" ) );
        assertEquals( "rapport.pdf", StoredFileValidator.sanitizeFileName( "rap\tport\u0000.pdf" ) );
    }

    /**
     * File names are reduced to their last path segment.
     */
    @Test
    public void testSanitizeFileNameKeepsTheLastPathSegment( )
    {
        assertEquals( "rapport.pdf", StoredFileValidator.sanitizeFileName( "C:\\Users\\x\\rapport.pdf" ) );
        assertEquals( "rapport.pdf", StoredFileValidator.sanitizeFileName( "../../etc/rapport.pdf" ) );
    }

    /**
     * An empty or absent name gets a default value.
     */
    @Test
    public void testSanitizeFileNameDefault( )
    {
        assertEquals( StoredFileValidator.DEFAULT_FILE_NAME, StoredFileValidator.sanitizeFileName( null ) );
        assertEquals( StoredFileValidator.DEFAULT_FILE_NAME, StoredFileValidator.sanitizeFileName( " \" \r " ) );
    }

    /**
     * A too long name is shortened while keeping its extension.
     */
    @Test
    public void testSanitizeFileNameShortensAndKeepsTheExtension( )
    {
        String strName = StoredFileValidator.sanitizeFileName( "a".repeat( 400 ) + ".pdf" );
        assertEquals( StoredFileValidator.MAX_FILE_NAME_LENGTH, strName.length( ) );
        assertTrue( strName.endsWith( ".pdf" ) );
    }

    /**
     * Checks that a file is accepted with the expected MIME type.
     *
     * @param strName
     *            the file name
     * @param strDeclaredType
     *            the declared MIME type
     * @param content
     *            the file content
     * @param strExpectedType
     *            the MIME type expected in the result
     */
    private void assertAccepted( String strName, String strDeclaredType, byte [ ] content, String strExpectedType )
    {
        StoredFileValidation result = _validator.validate( strName, strDeclaredType, content.length, content );
        assertTrue( result.isValid( ), strName + " must be accepted, got " + result.getErrorKey( ) );
        assertEquals( strExpectedType, result.getMimeType( ), "MIME type of " + strName );
    }

    /**
     * Checks that a file is refused with the expected error.
     *
     * @param strName
     *            the file name
     * @param strDeclaredType
     *            the declared MIME type
     * @param content
     *            the file content
     * @param strExpectedError
     *            the error key expected in the result
     */
    private void assertRefused( String strName, String strDeclaredType, byte [ ] content, String strExpectedError )
    {
        StoredFileValidation result = _validator.validate( strName, strDeclaredType, content.length, content );
        assertFalse( result.isValid( ), strName + " must be refused" );
        assertEquals( strExpectedError, result.getErrorKey( ), "error of " + strName );
    }
}
