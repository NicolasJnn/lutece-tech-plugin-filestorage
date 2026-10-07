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
package fr.paris.lutece.plugins.filestorage.business;

import java.io.Serializable;
import java.sql.Timestamp;

import fr.paris.lutece.portal.service.rbac.RBACResource;

/**
 * File of the file storage: the catalogue entry referencing the content held by the file store.
 */
public class StoredFile implements RBACResource, Serializable
{
    public static final String RESOURCE_TYPE = "FILESTORAGE_FILE";

    private static final long serialVersionUID = 1L;

    private int _nIdFile;
    private String _strFileKey;
    private String _strTitle;
    private String _strDescription;
    private String _strMimeType;
    private long _lFileSize;
    private Timestamp _dateCreation;
    private String _strCreator;

    /**
     * Returns the identifier.
     *
     * @return the identifier
     */
    public int getIdFile( )
    {
        return _nIdFile;
    }

    /**
     * Sets the identifier.
     *
     * @param nIdFile
     *            the identifier
     */
    public void setIdFile( int nIdFile )
    {
        _nIdFile = nIdFile;
    }

    /**
     * Returns the key of the content in the file store.
     *
     * @return the key
     */
    public String getFileKey( )
    {
        return _strFileKey;
    }

    /**
     * Sets the key of the content in the file store.
     *
     * @param strFileKey
     *            the key
     */
    public void setFileKey( String strFileKey )
    {
        _strFileKey = strFileKey;
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String getTitle( )
    {
        return _strTitle;
    }

    /**
     * Sets the title.
     *
     * @param strTitle
     *            the title
     */
    public void setTitle( String strTitle )
    {
        _strTitle = strTitle;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the description.
     *
     * @param strDescription
     *            the description
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the verified MIME type.
     *
     * @return the MIME type
     */
    public String getMimeType( )
    {
        return _strMimeType;
    }

    /**
     * Sets the verified MIME type.
     *
     * @param strMimeType
     *            the MIME type
     */
    public void setMimeType( String strMimeType )
    {
        _strMimeType = strMimeType;
    }

    /**
     * Returns the size in bytes.
     *
     * @return the size
     */
    public long getFileSize( )
    {
        return _lFileSize;
    }

    /**
     * Sets the size in bytes.
     *
     * @param lFileSize
     *            the size
     */
    public void setFileSize( long lFileSize )
    {
        _lFileSize = lFileSize;
    }

    /**
     * Returns the creation date.
     *
     * @return the creation date
     */
    public Timestamp getDateCreation( )
    {
        return _dateCreation;
    }

    /**
     * Sets the creation date.
     *
     * @param dateCreation
     *            the creation date
     */
    public void setDateCreation( Timestamp dateCreation )
    {
        _dateCreation = dateCreation;
    }

    /**
     * Returns the access code of the administrator who created the file.
     *
     * @return the creator
     */
    public String getCreator( )
    {
        return _strCreator;
    }

    /**
     * Sets the access code of the administrator who created the file.
     *
     * @param strCreator
     *            the creator
     */
    public void setCreator( String strCreator )
    {
        _strCreator = strCreator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceId( )
    {
        return String.valueOf( _nIdFile );
    }
}
