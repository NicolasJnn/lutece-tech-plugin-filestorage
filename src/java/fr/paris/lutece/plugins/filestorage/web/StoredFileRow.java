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

import java.io.Serializable;

import fr.paris.lutece.plugins.filestorage.business.StoredFile;

/**
 * Row of the management table: a file, its download link, and the actions the current administrator may perform on it.
 */
public class StoredFileRow implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final StoredFile _storedFile;
    private final String _strDownloadUrl;
    private final String _strPublicUrl;
    private final boolean _bModifiable;
    private final boolean _bRemovable;

    /**
     * Constructor.
     *
     * @param storedFile
     *            the file
     * @param strDownloadUrl
     *            the back office download URL
     * @param strPublicUrl
     *            the front office link, as inserted in contents
     * @param bModifiable
     *            true if the administrator may modify the file
     * @param bRemovable
     *            true if the administrator may remove the file
     */
    public StoredFileRow( StoredFile storedFile, String strDownloadUrl, String strPublicUrl, boolean bModifiable, boolean bRemovable )
    {
        _storedFile = storedFile;
        _strDownloadUrl = strDownloadUrl;
        _strPublicUrl = strPublicUrl;
        _bModifiable = bModifiable;
        _bRemovable = bRemovable;
    }

    /**
     * Returns the file.
     *
     * @return the file
     */
    public StoredFile getStoredFile( )
    {
        return _storedFile;
    }

    /**
     * Returns the back office download URL.
     *
     * @return the URL
     */
    public String getDownloadUrl( )
    {
        return _strDownloadUrl;
    }

    /**
     * Returns the front office link, as inserted in contents.
     *
     * @return the URL
     */
    public String getPublicUrl( )
    {
        return _strPublicUrl;
    }

    /**
     * Tells whether the administrator may modify the file.
     *
     * @return true if modifiable
     */
    public boolean isModifiable( )
    {
        return _bModifiable;
    }

    /**
     * Tells whether the administrator may remove the file.
     *
     * @return true if removable
     */
    public boolean isRemovable( )
    {
        return _bRemovable;
    }
}
