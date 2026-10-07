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

import fr.paris.lutece.plugins.filestorage.business.StoredFile;
import fr.paris.lutece.plugins.filestorage.business.StoredFileHome;
import fr.paris.lutece.portal.service.rbac.Permission;
import fr.paris.lutece.portal.service.rbac.ResourceIdService;
import fr.paris.lutece.portal.service.rbac.ResourceType;
import fr.paris.lutece.portal.service.rbac.ResourceTypeManager;
import fr.paris.lutece.util.ReferenceList;

/**
 * RBAC resource type of the file storage files.
 */
public class StoredFileResourceIdService extends ResourceIdService
{
    public static final String PERMISSION_CREATE = "CREATE";
    public static final String PERMISSION_MODIFY = "MODIFY";
    public static final String PERMISSION_DELETE = "DELETE";

    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "filestorage.permission.resourceType.storedFile.label";
    private static final String PROPERTY_LABEL_CREATE = "filestorage.permission.resourceType.storedFile.create";
    private static final String PROPERTY_LABEL_MODIFY = "filestorage.permission.resourceType.storedFile.modify";
    private static final String PROPERTY_LABEL_DELETE = "filestorage.permission.resourceType.storedFile.delete";

    /**
     * Constructor.
     */
    public StoredFileResourceIdService( )
    {
        setPluginName( StoredFileHome.PLUGIN_NAME );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register( )
    {
        ResourceType resourceType = new ResourceType( );
        resourceType.setResourceIdServiceClass( StoredFileResourceIdService.class.getName( ) );
        resourceType.setPluginName( StoredFileHome.PLUGIN_NAME );
        resourceType.setResourceTypeKey( StoredFile.RESOURCE_TYPE );
        resourceType.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

        resourceType.registerPermission( newPermission( PERMISSION_CREATE, PROPERTY_LABEL_CREATE ) );
        resourceType.registerPermission( newPermission( PERMISSION_MODIFY, PROPERTY_LABEL_MODIFY ) );
        resourceType.registerPermission( newPermission( PERMISSION_DELETE, PROPERTY_LABEL_DELETE ) );

        ResourceTypeManager.registerResourceType( resourceType );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList getResourceIdList( Locale locale )
    {
        return StoredFileHome.getReferenceList( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle( String strId, Locale locale )
    {
        try
        {
            StoredFile storedFile = StoredFileHome.findByPrimaryKey( Integer.parseInt( strId ) );

            return ( storedFile != null ) ? storedFile.getTitle( ) : "";
        }
        catch( NumberFormatException e )
        {
            return "";
        }
    }

    /**
     * Builds a permission.
     *
     * @param strKey
     *            the permission key
     * @param strTitleKey
     *            the i18n key of its title
     * @return the permission
     */
    private static Permission newPermission( String strKey, String strTitleKey )
    {
        Permission permission = new Permission( );
        permission.setPermissionKey( strKey );
        permission.setPermissionTitleKey( strTitleKey );

        return permission;
    }
}
