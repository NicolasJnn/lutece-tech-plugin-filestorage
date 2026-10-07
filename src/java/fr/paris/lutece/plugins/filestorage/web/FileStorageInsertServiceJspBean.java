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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import fr.paris.lutece.plugins.filestorage.business.StoredFile;
import fr.paris.lutece.plugins.filestorage.business.StoredFileHome;
import fr.paris.lutece.plugins.filestorage.service.StoredFileService;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.web.insert.InsertServiceJspBean;
import fr.paris.lutece.portal.web.insert.InsertServiceSelectionBean;
import fr.paris.lutece.util.html.HtmlTemplate;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Insert service of the file storage: lets an editor insert in a content a link to a stored file.
 */
@RequestScoped
@Named
public class FileStorageInsertServiceJspBean extends InsertServiceJspBean implements InsertServiceSelectionBean
{
    public static final String RIGHT_INSERT_STORED_FILE = "FILESTORAGE_INSERT";

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_SELECTOR = "admin/plugins/filestorage/insert_storedfile_selector.html";

    private static final String PARAMETER_INPUT = "input";
    private static final String TARGET_SELF = "_self";
    private static final String TARGET_BLANK = "_blank";
    private static final String PARAMETER_SELECTED_TEXT = "selected_text";
    private static final String PARAMETER_SEARCH = "search";
    private static final String PARAMETER_ID = "id_file";
    private static final String PARAMETER_TEXT = "text";
    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_TARGET = "target";

    private static final String MARK_FILE_LIST = "storedfile_list";
    private static final String MARK_INPUT = "input_name";
    private static final String MARK_SELECTED_TEXT = "selected_text";
    private static final String MARK_SEARCH = "search";
    private static final String MARK_AUTHORIZED = "authorized";

    private static final String MESSAGE_UNAUTHORIZED = "filestorage.message.error.insertUnauthorized";
    private static final String MESSAGE_NO_FILE_SELECTED = "filestorage.message.error.noFileSelected";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInsertServiceSelectorUI( HttpServletRequest request )
    {
        boolean bAuthorized = isAuthorized( request );
        String strSearch = StringUtils.trimToEmpty( request.getParameter( PARAMETER_SEARCH ) );

        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_AUTHORIZED, bAuthorized );
        model.put( MARK_INPUT, StringUtils.defaultString( request.getParameter( PARAMETER_INPUT ) ) );
        model.put( MARK_SELECTED_TEXT, StringUtils.defaultString( request.getParameter( PARAMETER_SELECTED_TEXT ) ) );
        model.put( MARK_SEARCH, strSearch );

        if ( bAuthorized )
        {
            List<StoredFile> listFiles = strSearch.isEmpty( ) ? StoredFileHome.findAll( ) : StoredFileHome.findByTitle( strSearch );
            model.put( MARK_FILE_LIST, listFiles );
        }

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_SELECTOR, AdminUserService.getLocale( request ), model );

        return template.getHtml( );
    }

    /**
     * Inserts in the content the link to the selected file.
     *
     * @param request
     *            the request
     * @return the URL that performs the insertion, or the URL of an error message
     */
    public String doInsertLink( HttpServletRequest request )
    {
        if ( !isAuthorized( request ) )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_UNAUTHORIZED, AdminMessage.TYPE_STOP );
        }

        StoredFile storedFile = findRequestedFile( request );

        if ( storedFile == null )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_NO_FILE_SELECTED, AdminMessage.TYPE_STOP );
        }

        String strText = StringUtils.defaultIfBlank( StringEscapeUtils.unescapeHtml4( request.getParameter( PARAMETER_TEXT ) ), storedFile.getTitle( ) );
        String strTitle = StringUtils.defaultIfBlank( StringEscapeUtils.unescapeHtml4( request.getParameter( PARAMETER_TITLE ) ), storedFile.getTitle( ) );
        String strTarget = TARGET_BLANK.equals( request.getParameter( PARAMETER_TARGET ) ) ? TARGET_BLANK : TARGET_SELF;
        String strUrl = CDI.current( ).select( StoredFileService.class ).get( ).getDownloadUrl( storedFile );

        return insertUrl( request, request.getParameter( PARAMETER_INPUT ), StringEscapeUtils.escapeEcmaScript( buildLink( strText, strUrl, strTitle, strTarget ) ) );
    }

    /**
     * Tells whether the current administrator holds the insertion right.
     *
     * @param request
     *            the request
     * @return true if authorized
     */
    private static boolean isAuthorized( HttpServletRequest request )
    {
        AdminUser user = AdminUserService.getAdminUser( request );

        return user != null && user.checkRight( RIGHT_INSERT_STORED_FILE );
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
}
