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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import fr.paris.lutece.plugins.filestorage.business.StoredFile;
import fr.paris.lutece.plugins.filestorage.business.StoredFileHome;
import fr.paris.lutece.plugins.filestorage.service.StoredFileResourceIdService;
import fr.paris.lutece.plugins.filestorage.service.StoredFileService;
import fr.paris.lutece.plugins.filestorage.service.StoredFileValidation;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.util.IPager;
import fr.paris.lutece.portal.web.util.Pager;
import fr.paris.lutece.util.url.UrlItem;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Back office management of the file storage files.
 */
@SessionScoped
@Named
@Controller( controllerJsp = "ManageStoredFiles.jsp", controllerPath = "jsp/admin/plugins/filestorage/", right = StoredFileJspBean.RIGHT_MANAGE_FILESTORAGE )
public class StoredFileJspBean extends MVCAdminJspBean
{
    public static final String RIGHT_MANAGE_FILESTORAGE = "FILESTORAGE_MANAGEMENT";

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_MANAGE = "admin/plugins/filestorage/manage_storedfiles.html";
    private static final String TEMPLATE_CREATE = "admin/plugins/filestorage/create_storedfile.html";
    private static final String TEMPLATE_MODIFY = "admin/plugins/filestorage/modify_storedfile.html";

    private static final String VIEW_MANAGE = "manageStoredFiles";
    private static final String VIEW_CREATE = "createStoredFile";
    private static final String VIEW_MODIFY = "modifyStoredFile";
    private static final String VIEW_CONFIRM_REMOVE = "confirmRemoveStoredFile";

    private static final String ACTION_CREATE = "createStoredFile";
    private static final String ACTION_MODIFY = "modifyStoredFile";
    private static final String ACTION_REMOVE = "removeStoredFile";

    private static final String PARAMETER_ID = "id_file";
    private static final String PARAMETER_FILE = "stored_file";
    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_DESCRIPTION = "description";
    private static final String PARAMETER_SEARCH = "search";

    private static final String MARK_FILE_LIST = "storedfile_list";
    private static final String MARK_STORED_FILE = "storedfile";
    private static final String MARK_DOWNLOAD_URL = "download_url";
    private static final String MARK_SEARCH = "search";
    private static final String MARK_CAN_CREATE = "can_create";
    private static final String MARK_MAX_FILE_SIZE = "max_file_size_mb";
    private static final String MARK_ALLOWED_TYPES = "allowed_types";
    private static final String MARK_MAX_TITLE_LENGTH = "max_title_length";
    private static final String MARK_MAX_DESCRIPTION_LENGTH = "max_description_length";

    private static final String PROPERTY_PAGE_TITLE_MANAGE = "filestorage.manage_storedfiles.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_CREATE = "filestorage.create_storedfile.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY = "filestorage.modify_storedfile.pageTitle";

    private static final String MESSAGE_CONFIRM_REMOVE = "filestorage.message.confirmRemoveStoredFile";
    private static final String MESSAGE_ERROR_TITLE_MANDATORY = "filestorage.message.error.titleMandatory";
    private static final String MESSAGE_ERROR_TITLE_TOO_LONG = "filestorage.message.error.titleTooLong";
    private static final String MESSAGE_ERROR_DESCRIPTION_TOO_LONG = "filestorage.message.error.descriptionTooLong";
    private static final String MESSAGE_ERROR_STORAGE = "filestorage.message.error.storage";
    private static final String MESSAGE_ERROR_INVALID_TOKEN = "filestorage.message.error.invalidToken";
    private static final String MESSAGE_ERROR_UNAUTHORIZED = "filestorage.message.error.unauthorized";
    private static final String INFO_CREATED = "filestorage.info.storedFileCreated";
    private static final String INFO_MODIFIED = "filestorage.info.storedFileModified";
    private static final String INFO_REMOVED = "filestorage.info.storedFileRemoved";

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final String METHOD_POST = "POST";
    private static final String ERROR_METHOD_NOT_ALLOWED = "Method not allowed";

    @Inject
    private Models _models;

    @Inject
    private StoredFileService _storedFileService;

    @Inject
    @Pager( name = "filestorageFileList", listBookmark = MARK_FILE_LIST, defaultItemsPerPage = "filestorage.storedFile.itemsPerPage", baseUrl = "jsp/admin/plugins/filestorage/ManageStoredFiles.jsp" )
    private IPager<Integer, StoredFileRow> _pager;

    private String _strSearch;

    /**
     * Displays the list of the files.
     *
     * @param request
     *            the request
     * @return the page
     */
    @View( value = VIEW_MANAGE, defaultView = true )
    public String getManageStoredFiles( HttpServletRequest request )
    {
        String strSearch = request.getParameter( PARAMETER_SEARCH );

        if ( strSearch != null )
        {
            _strSearch = strSearch.trim( );
        }

        List<StoredFile> listFiles = StringUtils.isBlank( _strSearch ) ? StoredFileHome.findAll( ) : StoredFileHome.findByTitle( _strSearch );
        Map<Integer, StoredFile> mapFiles = new LinkedHashMap<>( );

        for ( StoredFile storedFile : listFiles )
        {
            mapFiles.put( storedFile.getIdFile( ), storedFile );
        }

        AdminUser user = getUser( );
        StoredFileService storedFileService = _storedFileService;

        _pager.withIdList( new ArrayList<>( mapFiles.keySet( ) ) ).populateModels( request, _models,
                listIds -> toRows( listIds, mapFiles, user, storedFileService ), getLocale( ) );

        _models.put( MARK_SEARCH, StringUtils.defaultString( _strSearch ) );
        _models.put( MARK_CAN_CREATE, isAuthorized( RBAC.WILDCARD_RESOURCES_ID, StoredFileResourceIdService.PERMISSION_CREATE, user ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE, TEMPLATE_MANAGE );
    }

    /**
     * Displays the creation form.
     *
     * @param request
     *            the request
     * @return the page
     * @throws AccessDeniedException
     *             if the administrator may not create files
     */
    @View( VIEW_CREATE )
    public String getCreateStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( RBAC.WILDCARD_RESOURCES_ID, StoredFileResourceIdService.PERMISSION_CREATE );
        putUploadLimits( );
        _models.put( SecurityTokenService.MARK_TOKEN, getSecurityTokenService( ).getToken( request, ACTION_CREATE ) );

        return getPage( PROPERTY_PAGE_TITLE_CREATE, TEMPLATE_CREATE );
    }

    /**
     * Creates a file.
     *
     * @param request
     *            the multipart request
     * @return the redirection
     * @throws AccessDeniedException
     *             if the token is invalid or the administrator may not create files
     */
    @Action( value = ACTION_CREATE, securityTokenDisabled = true )
    public String doCreateStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        checkToken( request, ACTION_CREATE );
        checkPermission( RBAC.WILDCARD_RESOURCES_ID, StoredFileResourceIdService.PERMISSION_CREATE );

        String strTitle = StringUtils.trimToEmpty( request.getParameter( PARAMETER_TITLE ) );
        String strDescription = StringUtils.trimToEmpty( request.getParameter( PARAMETER_DESCRIPTION ) );

        if ( !checkTextFields( strTitle, strDescription, false ) )
        {
            return redirectView( request, VIEW_CREATE );
        }

        MultipartItem item = getUploadedFile( request );
        StoredFileValidation validation = _storedFileService.validate( item );

        if ( !validation.isValid( ) )
        {
            addValidationError( validation );

            return redirectView( request, VIEW_CREATE );
        }

        try
        {
            _storedFileService.create( item, validation, strTitle, strDescription, getUser( ).getAccessCode( ) );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "Unable to store a file storage file", e );
            addError( MESSAGE_ERROR_STORAGE, getLocale( ) );

            return redirectView( request, VIEW_CREATE );
        }

        addInfo( INFO_CREATED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE );
    }

    /**
     * Displays the modification form.
     *
     * @param request
     *            the request
     * @return the page
     * @throws AccessDeniedException
     *             if the administrator may not modify the file
     */
    @View( VIEW_MODIFY )
    public String getModifyStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        StoredFile storedFile = findRequestedFile( request );

        if ( storedFile == null )
        {
            return redirectView( request, VIEW_MANAGE );
        }

        checkPermission( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_MODIFY );

        _models.put( MARK_STORED_FILE, storedFile );
        _models.put( MARK_DOWNLOAD_URL, _storedFileService.getBackOfficeDownloadUrl( storedFile ) );
        putUploadLimits( );
        _models.put( SecurityTokenService.MARK_TOKEN, getSecurityTokenService( ).getToken( request, ACTION_MODIFY ) );

        return getPage( PROPERTY_PAGE_TITLE_MODIFY, TEMPLATE_MODIFY );
    }

    /**
     * Modifies a file: its title, its description and, when a new file is uploaded, its content.
     *
     * @param request
     *            the multipart request
     * @return the redirection
     * @throws AccessDeniedException
     *             if the token is invalid or the administrator may not modify the file
     */
    @Action( value = ACTION_MODIFY, securityTokenDisabled = true )
    public String doModifyStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        checkToken( request, ACTION_MODIFY );

        StoredFile storedFile = findRequestedFile( request );

        if ( storedFile == null )
        {
            return redirectView( request, VIEW_MANAGE );
        }

        checkPermission( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_MODIFY );

        String strTitle = StringUtils.trimToEmpty( request.getParameter( PARAMETER_TITLE ) );
        String strDescription = StringUtils.trimToEmpty( request.getParameter( PARAMETER_DESCRIPTION ) );

        if ( !checkTextFields( strTitle, strDescription, true ) )
        {
            return redirect( request, VIEW_MODIFY, PARAMETER_ID, storedFile.getIdFile( ) );
        }

        storedFile.setTitle( strTitle );
        storedFile.setDescription( strDescription );

        MultipartItem item = getUploadedFile( request );

        if ( item == null || item.getSize( ) == 0 )
        {
            _storedFileService.update( storedFile );
        }
        else
        {
            StoredFileValidation validation = _storedFileService.validate( item );

            if ( !validation.isValid( ) )
            {
                addValidationError( validation );

                return redirect( request, VIEW_MODIFY, PARAMETER_ID, storedFile.getIdFile( ) );
            }

            try
            {
                _storedFileService.replace( storedFile, item, validation );
            }
            catch( FileServiceException e )
            {
                AppLogService.error( "Unable to replace a file storage file", e );
                addError( MESSAGE_ERROR_STORAGE, getLocale( ) );

                return redirect( request, VIEW_MODIFY, PARAMETER_ID, storedFile.getIdFile( ) );
            }
        }

        addInfo( INFO_MODIFIED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE );
    }

    /**
     * Asks the confirmation of the removal of a file.
     *
     * @param request
     *            the request
     * @return the confirmation message
     * @throws AccessDeniedException
     *             if the administrator may not remove the file
     */
    @View( value = VIEW_CONFIRM_REMOVE, securityTokenAction = ACTION_REMOVE )
    public String getConfirmRemoveStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        StoredFile storedFile = findRequestedFile( request );

        if ( storedFile == null )
        {
            return redirectView( request, VIEW_MANAGE );
        }

        checkPermission( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_DELETE );

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE ) );
        url.addParameter( PARAMETER_ID, storedFile.getIdFile( ) );

        String strMessageUrl = AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE,
                new Object [ ] { StringEscapeUtils.escapeHtml4( storedFile.getTitle( ) ) }, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION );

        return redirect( request, strMessageUrl );
    }

    /**
     * Removes a file.
     *
     * @param request
     *            the request
     * @return the redirection
     * @throws AccessDeniedException
     *             if the administrator may not remove the file
     */
    @Action( ACTION_REMOVE )
    public String doRemoveStoredFile( HttpServletRequest request ) throws AccessDeniedException
    {
        if ( !METHOD_POST.equalsIgnoreCase( request.getMethod( ) ) )
        {
            throw new AccessDeniedException( ERROR_METHOD_NOT_ALLOWED );
        }

        StoredFile storedFile = findRequestedFile( request );

        if ( storedFile != null )
        {
            checkPermission( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_DELETE );
            _storedFileService.remove( storedFile );
            addInfo( INFO_REMOVED, getLocale( ) );
        }

        return redirectView( request, VIEW_MANAGE );
    }

    /**
     * Builds the rows of a page of the table.
     *
     * @param listIds
     *            the identifiers of the page
     * @param mapFiles
     *            the listed files, by identifier
     * @param user
     *            the current administrator
     * @param storedFileService
     *            the file service
     * @return the rows
     */
    private static List<StoredFileRow> toRows( List<Integer> listIds, Map<Integer, StoredFile> mapFiles, AdminUser user, StoredFileService storedFileService )
    {
        List<StoredFileRow> listRows = new ArrayList<>( listIds.size( ) );

        for ( Integer nId : listIds )
        {
            StoredFile storedFile = mapFiles.get( nId );

            if ( storedFile != null )
            {
                listRows.add( new StoredFileRow( storedFile, storedFileService.getBackOfficeDownloadUrl( storedFile ),
                        storedFileService.getDownloadUrl( storedFile ),
                        isAuthorized( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_MODIFY, user ),
                        isAuthorized( storedFile.getResourceId( ), StoredFileResourceIdService.PERMISSION_DELETE, user ) ) );
            }
        }

        return listRows;
    }

    /**
     * Tells whether an administrator holds a permission on a file.
     *
     * @param strIdFile
     *            the identifier of the file, or the wildcard
     * @param strPermission
     *            the permission
     * @param user
     *            the administrator
     * @return true if authorized
     */
    private static boolean isAuthorized( String strIdFile, String strPermission, AdminUser user )
    {
        return RBACService.isAuthorized( StoredFile.RESOURCE_TYPE, strIdFile, strPermission, user );
    }

    /**
     * Checks that the current administrator holds a permission on a file.
     *
     * @param strIdFile
     *            the identifier of the file, or the wildcard
     * @param strPermission
     *            the permission
     * @throws AccessDeniedException
     *             if the administrator does not hold it
     */
    private void checkPermission( String strIdFile, String strPermission ) throws AccessDeniedException
    {
        if ( !isAuthorized( strIdFile, strPermission, getUser( ) ) )
        {
            throw new AccessDeniedException( I18nService.getLocalizedString( MESSAGE_ERROR_UNAUTHORIZED, getLocale( ) ) );
        }
    }

    /**
     * Checks the security token of a form.
     *
     * @param request
     *            the request
     * @param strAction
     *            the action the token was issued for
     * @throws AccessDeniedException
     *             if the token is invalid
     */
    private void checkToken( HttpServletRequest request, String strAction ) throws AccessDeniedException
    {
        if ( !getSecurityTokenService( ).validate( request, strAction ) )
        {
            throw new AccessDeniedException( I18nService.getLocalizedString( MESSAGE_ERROR_INVALID_TOKEN, getLocale( ) ) );
        }
    }

    /**
     * Loads the file whose identifier is in the request.
     *
     * @param request
     *            the request
     * @return the file, or null when the identifier is missing, invalid or unknown
     */
    private static StoredFile findRequestedFile( HttpServletRequest request )
    {
        try
        {
            return StoredFileHome.findByPrimaryKey( Integer.parseInt( request.getParameter( PARAMETER_ID ) ) );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }

    /**
     * Returns the uploaded file of a multipart request.
     *
     * @param request
     *            the request
     * @return the uploaded file, or null when the request is not multipart or carries no file
     */
    private static MultipartItem getUploadedFile( HttpServletRequest request )
    {
        if ( request instanceof MultipartHttpServletRequest multipartRequest )
        {
            return multipartRequest.getFile( PARAMETER_FILE );
        }

        return null;
    }

    /**
     * Checks the title and the description, adding an error for each problem.
     *
     * @param strTitle
     *            the title
     * @param strDescription
     *            the description
     * @param bTitleMandatory
     *            true if the title is mandatory
     * @return true if both are valid
     */
    private boolean checkTextFields( String strTitle, String strDescription, boolean bTitleMandatory )
    {
        boolean bValid = true;

        if ( bTitleMandatory && strTitle.isEmpty( ) )
        {
            addError( MESSAGE_ERROR_TITLE_MANDATORY, getLocale( ) );
            bValid = false;
        }

        if ( strTitle.length( ) > MAX_TITLE_LENGTH )
        {
            addError( I18nService.getLocalizedString( MESSAGE_ERROR_TITLE_TOO_LONG, new Object [ ] { MAX_TITLE_LENGTH }, getLocale( ) ) );
            bValid = false;
        }

        if ( strDescription.length( ) > MAX_DESCRIPTION_LENGTH )
        {
            addError( I18nService.getLocalizedString( MESSAGE_ERROR_DESCRIPTION_TOO_LONG, new Object [ ] { MAX_DESCRIPTION_LENGTH }, getLocale( ) ) );
            bValid = false;
        }

        return bValid;
    }

    /**
     * Adds the error of a refused file, with the limits it refers to.
     *
     * @param validation
     *            the refused outcome
     */
    private void addValidationError( StoredFileValidation validation )
    {
        Object [ ] args = new Object [ ] { getMaxFileSizeInMegabytes( ), _storedFileService.getAllowedTypes( ) };
        addError( I18nService.getLocalizedString( validation.getErrorKey( ), args, getLocale( ) ) );
    }

    /**
     * Puts the upload limits in the model.
     */
    private void putUploadLimits( )
    {
        _models.put( MARK_MAX_FILE_SIZE, getMaxFileSizeInMegabytes( ) );
        _models.put( MARK_ALLOWED_TYPES, _storedFileService.getAllowedTypes( ) );
        _models.put( MARK_MAX_TITLE_LENGTH, MAX_TITLE_LENGTH );
        _models.put( MARK_MAX_DESCRIPTION_LENGTH, MAX_DESCRIPTION_LENGTH );
    }

    /**
     * Returns the maximum size of a file, in megabytes.
     *
     * @return the maximum size
     */
    private String getMaxFileSizeInMegabytes( )
    {
        NumberFormat format = NumberFormat.getNumberInstance( getLocale( ) );
        format.setMaximumFractionDigits( 1 );
        return format.format( (double) _storedFileService.getMaxFileSize( ) / BYTES_PER_MEGABYTE );
    }
}
