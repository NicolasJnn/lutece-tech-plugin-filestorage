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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import fr.paris.lutece.portal.service.upload.MultipartItem;

/**
 * Uploaded file whose name and MIME type are replaced by verified values.
 */
public class SanitizedMultipartItem implements MultipartItem
{
    private final MultipartItem _item;
    private final String _strName;
    private final String _strContentType;

    /**
     * Constructor.
     *
     * @param item
     *            the uploaded file
     * @param strName
     *            the cleaned file name
     * @param strContentType
     *            the verified MIME type
     */
    public SanitizedMultipartItem( MultipartItem item, String strName, String strContentType )
    {
        _item = item;
        _strName = strName;
        _strContentType = strContentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFieldName( )
    {
        return _item.getFieldName( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( )
    {
        return _strName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType( )
    {
        return _strContentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize( )
    {
        return _item.getSize( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream( ) throws IOException
    {
        return _item.getInputStream( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte [ ] get( ) throws UncheckedIOException
    {
        return _item.get( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( ) throws IOException
    {
        _item.delete( );
    }
}
