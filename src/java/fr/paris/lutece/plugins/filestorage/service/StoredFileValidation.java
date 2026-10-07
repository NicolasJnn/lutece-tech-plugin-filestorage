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

/**
 * Outcome of the validation of an uploaded file.
 */
public final class StoredFileValidation
{
    private final String _strErrorKey;
    private final String _strMimeType;

    /**
     * Constructor.
     *
     * @param strErrorKey
     *            the i18n key of the error, or null when the file is valid
     * @param strMimeType
     *            the verified MIME type, or null when the file is refused
     */
    private StoredFileValidation( String strErrorKey, String strMimeType )
    {
        _strErrorKey = strErrorKey;
        _strMimeType = strMimeType;
    }

    /**
     * Builds the outcome of an accepted file.
     *
     * @param strMimeType
     *            the verified MIME type
     * @return the outcome
     */
    public static StoredFileValidation accepted( String strMimeType )
    {
        return new StoredFileValidation( null, strMimeType );
    }

    /**
     * Builds the outcome of a refused file.
     *
     * @param strErrorKey
     *            the i18n key of the error
     * @return the outcome
     */
    public static StoredFileValidation refused( String strErrorKey )
    {
        return new StoredFileValidation( strErrorKey, null );
    }

    /**
     * Tells whether the file is accepted.
     *
     * @return true if the file is accepted
     */
    public boolean isValid( )
    {
        return _strErrorKey == null;
    }

    /**
     * Returns the i18n key of the error.
     *
     * @return the error key, or null when the file is valid
     */
    public String getErrorKey( )
    {
        return _strErrorKey;
    }

    /**
     * Returns the verified MIME type.
     *
     * @return the MIME type, or null when the file is refused
     */
    public String getMimeType( )
    {
        return _strMimeType;
    }
}
