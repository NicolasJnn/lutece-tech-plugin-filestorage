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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

/**
 * File store service that keeps the files on disk, under a configurable directory, each content next to its metadata.
 */
@ApplicationScoped
@FileStorage
@Named( FileStorageFileSystemFileService.BEAN_NAME )
public class FileStorageFileSystemFileService implements IFileStoreService
{
    public static final String BEAN_NAME = "filestorage.fileSystemFileService";

    private static final String PROPERTY_DIRECTORY = "filestorage.fileSystem.directory";
    private static final String DEFAULT_DIRECTORY = "/opt/data/filestorage";
    private static final Pattern PATTERN_KEY = Pattern.compile( "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" );
    private static final Pattern PATTERN_SHARD = Pattern.compile( "[0-9a-f]{2}" );
    private static final int SHARD_LENGTH = 2;
    private static final String METADATA_EXTENSION = ".properties";
    private static final String TEMPORARY_PREFIX = ".tmp-";
    private static final String POSIX = "posix";
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = PosixFilePermissions.fromString( "rw-r-----" );
    private static final String META_TITLE = "title";
    private static final String META_MIME_TYPE = "mimeType";
    private static final String META_SIZE = "size";
    private static final String META_ORIGIN = "origin";
    private static final String META_DATE_CREATION = "dateCreation";
    private static final String MESSAGE_STORE_FAILED = "Unable to store a file storage file in ";
    private static final String MESSAGE_UNAVAILABLE = "The file storage directory is missing, relative, or cannot be written: ";
    private static final String MESSAGE_SIZE_MISMATCH = "The written size of the file storage file differs from its declared size";
    private static final String MESSAGE_NO_CONTENT = "No content to store";

    private final Path _directory;

    /**
     * Constructor used by CDI for the proxy.
     */
    protected FileStorageFileSystemFileService( )
    {
        _directory = null;
    }

    /**
     * Constructor.
     *
     * @param strDirectory
     *            the directory where the files are kept, absolute
     */
    @Inject
    public FileStorageFileSystemFileService( @ConfigProperty( name = PROPERTY_DIRECTORY, defaultValue = DEFAULT_DIRECTORY ) String strDirectory )
    {
        _directory = Paths.get( strDirectory ).normalize( );
    }

    /**
     * Reports, when the application starts, the directory in use and whether it can be written.
     *
     * @param context
     *            the servlet context
     */
    public void onStartup( @Observes @Initialized( ApplicationScoped.class ) ServletContext context )
    {
        if ( isAvailable( ) )
        {
            AppLogService.info( "The file storage keeps its files on disk in {}", _directory );
        }
        else
        {
            AppLogService.error( "{}{}", MESSAGE_UNAVAILABLE, _directory );
        }
    }

    /**
     * Tells whether the root directory exists, is absolute and can be written. Nothing is created.
     *
     * @return true if files can be stored
     */
    @Override
    public boolean healthCheck( )
    {
        return isAvailable( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeFile( File file, String strProviderName ) throws FileServiceException
    {
        if ( file == null || file.getPhysicalFile( ) == null || file.getPhysicalFile( ).getValue( ) == null )
        {
            throw new FileServiceException( MESSAGE_NO_CONTENT );
        }

        byte [ ] content = file.getPhysicalFile( ).getValue( );

        try
        {
            return store( new ByteArrayInputStream( content ), content.length, file.getTitle( ), file.getMimeType( ), strProviderName );
        }
        catch( AppException e )
        {
            throw new FileServiceException( e.getMessage( ), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeBytes( byte [ ] blob, String strProviderName )
    {
        if ( blob == null )
        {
            throw new AppException( MESSAGE_NO_CONTENT );
        }

        return store( new ByteArrayInputStream( blob ), blob.length, null, null, strProviderName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeInputStream( InputStream inputStream, String strProviderName )
    {
        if ( inputStream == null )
        {
            throw new AppException( MESSAGE_NO_CONTENT );
        }

        return store( inputStream, -1, null, null, strProviderName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeFileItem( MultipartItem fileItem, String strProviderName )
    {
        if ( fileItem == null )
        {
            throw new AppException( MESSAGE_NO_CONTENT );
        }

        try ( InputStream inputStream = fileItem.getInputStream( ) )
        {
            return store( inputStream, fileItem.getSize( ), fileItem.getName( ), fileItem.getContentType( ), strProviderName );
        }
        catch( IOException e )
        {
            throw new AppException( MESSAGE_STORE_FAILED + _directory, e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getFile( String strKey, String strProviderName ) throws FileServiceException
    {
        File file = getFileMetaData( strKey, strProviderName );

        if ( file == null )
        {
            return null;
        }

        try ( InputStream inputStream = Channels.newInputStream( Files.newByteChannel( contentPath( strKey ), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS ) ) )
        {
            PhysicalFile physicalFile = new PhysicalFile( );
            physicalFile.setValue( inputStream.readAllBytes( ) );
            file.setPhysicalFile( physicalFile );

            return file;
        }
        catch( NoSuchFileException e )
        {
            return null;
        }
        catch( IOException e )
        {
            throw new FileServiceException( "Unable to read the file storage file " + strKey, e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getFileMetaData( String strKey, String strProviderName )
    {
        if ( !isValidKey( strKey ) || !isRealDirectory( shardPath( strKey ) ) || !Files.isRegularFile( contentPath( strKey ), LinkOption.NOFOLLOW_LINKS ) )
        {
            return null;
        }

        Properties metadata = readMetadata( strKey );

        if ( metadata == null || strProviderName == null || !strProviderName.equals( metadata.getProperty( META_ORIGIN ) ) )
        {
            return null;
        }

        try
        {
            File file = new File( );
            file.setFileKey( strKey );
            file.setTitle( metadata.getProperty( META_TITLE ) );
            file.setMimeType( metadata.getProperty( META_MIME_TYPE ) );
            file.setSize( Math.toIntExact( Long.parseLong( metadata.getProperty( META_SIZE, "0" ) ) ) );
            file.setOrigin( metadata.getProperty( META_ORIGIN ) );
            file.setDateCreation( new Timestamp( Long.parseLong( metadata.getProperty( META_DATE_CREATION, "0" ) ) ) );

            return file;
        }
        catch( IllegalArgumentException | ArithmeticException e )
        {
            AppLogService.error( "Corrupted metadata for the file storage file {}", strKey, e );
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream( String strKey, String strProviderName )
    {
        if ( getFileMetaData( strKey, strProviderName ) == null )
        {
            return null;
        }

        try
        {
            return Channels.newInputStream( Files.newByteChannel( contentPath( strKey ), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS ) );
        }
        catch( NoSuchFileException e )
        {
            return null;
        }
        catch( IOException e )
        {
            throw new AppException( "Unable to read the file storage file " + strKey, e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( String strKey )
    {
        if ( !isValidKey( strKey ) || !isRealDirectory( shardPath( strKey ) ) )
        {
            return;
        }

        try
        {
            Files.deleteIfExists( contentPath( strKey ) );
            Files.deleteIfExists( metadataPath( strKey ) );
        }
        catch( IOException e )
        {
            throw new AppException( "Unable to delete the file storage file " + strKey, e );
        }
    }

    /**
     * Removes the files left behind by interrupted writes and deletions, and the files no longer in the catalogue, when older
     * than a minimum age.
     *
     * @param catalogueKeys
     *            the keys referenced by the catalogue
     * @param minAge
     *            the minimum age of a file before it may be removed
     * @return what was removed
     */
    public CleanupReport cleanOrphans( Set<String> catalogueKeys, Duration minAge )
    {
        CleanupReport report = new CleanupReport( );

        if ( !isRealDirectory( _directory ) )
        {
            return report;
        }

        Instant limit = Instant.now( ).minus( minAge );

        try ( DirectoryStream<Path> shards = Files.newDirectoryStream( _directory ) )
        {
            for ( Path shard : shards )
            {
                if ( isRealDirectory( shard ) && PATTERN_SHARD.matcher( shard.getFileName( ).toString( ) ).matches( ) )
                {
                    cleanShard( shard, catalogueKeys, limit, report );
                }
            }
        }
        catch( IOException e )
        {
            AppLogService.error( "Unable to clean the file storage directory {}", _directory, e );
        }

        return report;
    }

    /**
     * Removes the orphans of a sub-directory.
     *
     * @param shard
     *            the sub-directory
     * @param catalogueKeys
     *            the keys referenced by the catalogue
     * @param limit
     *            files modified after this instant are kept
     * @param report
     *            the report to complete
     * @throws IOException
     *             if the sub-directory cannot be read
     */
    private void cleanShard( Path shard, Set<String> catalogueKeys, Instant limit, CleanupReport report ) throws IOException
    {
        try ( DirectoryStream<Path> entries = Files.newDirectoryStream( shard ) )
        {
            for ( Path entry : entries )
            {
                if ( !Files.isRegularFile( entry, LinkOption.NOFOLLOW_LINKS ) || !isOlderThan( entry, limit ) )
                {
                    continue;
                }

                String strName = entry.getFileName( ).toString( );
                String strKey = StringUtils.removeEnd( strName, METADATA_EXTENSION );
                boolean bMetadata = strName.endsWith( METADATA_EXTENSION );

                if ( strName.startsWith( TEMPORARY_PREFIX ) )
                {
                    deleteQuietly( entry );
                    report._nTemporaryFiles++;
                }
                else if ( !isValidKey( strKey ) || !strKey.startsWith( shard.getFileName( ).toString( ) ) )
                {
                    continue;
                }
                else if ( bMetadata && !Files.exists( contentPath( strKey ), LinkOption.NOFOLLOW_LINKS ) )
                {
                    deleteQuietly( entry );
                    report._nMetadataWithoutContent++;
                }
                else if ( !bMetadata && !Files.exists( metadataPath( strKey ), LinkOption.NOFOLLOW_LINKS ) )
                {
                    deleteQuietly( entry );
                    report._nContentsWithoutMetadata++;
                }
                else if ( !bMetadata && !catalogueKeys.contains( strKey ) )
                {
                    deleteQuietly( entry );
                    deleteQuietly( metadataPath( strKey ) );
                    report._nFilesNotCatalogued++;
                }
            }
        }
    }

    /**
     * Writes a content then its metadata, each through a temporary file moved in place.
     *
     * @param inputStream
     *            the content
     * @param lExpectedSize
     *            the declared size, or a negative value when unknown
     * @param strTitle
     *            the title
     * @param strMimeType
     *            the MIME type
     * @param strProviderName
     *            the provider that owns the file
     * @return the key of the stored file
     */
    private String store( InputStream inputStream, long lExpectedSize, String strTitle, String strMimeType, String strProviderName )
    {
        if ( !isAvailable( ) )
        {
            throw new AppException( MESSAGE_UNAVAILABLE + _directory );
        }

        String strKey = UUID.randomUUID( ).toString( );
        Path content = contentPath( strKey );
        Path temporary = null;

        try
        {
            Path shard = Files.createDirectories( shardPath( strKey ) );

            if ( !isRealDirectory( shard ) )
            {
                throw new AppException( MESSAGE_STORE_FAILED + shard );
            }

            temporary = createTemporaryFile( shard );

            long lSize;

            try ( OutputStream outputStream = Files.newOutputStream( temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS ) )
            {
                lSize = inputStream.transferTo( outputStream );
            }

            if ( lExpectedSize >= 0 && lSize != lExpectedSize )
            {
                throw new AppException( MESSAGE_SIZE_MISMATCH );
            }

            move( temporary, content );
            temporary = null;
            writeMetadata( strKey, strTitle, strMimeType, lSize, strProviderName );

            return strKey;
        }
        catch( IOException | RuntimeException e )
        {
            deleteQuietly( temporary );
            deleteQuietly( content );

            if ( e instanceof AppException )
            {
                throw (AppException) e;
            }

            throw new AppException( MESSAGE_STORE_FAILED + _directory, e );
        }
    }

    /**
     * Writes the metadata of a file.
     *
     * @param strKey
     *            the key of the file
     * @param strTitle
     *            the title
     * @param strMimeType
     *            the MIME type
     * @param lSize
     *            the size
     * @param strProviderName
     *            the provider that owns the file
     * @throws IOException
     *             if the metadata cannot be written
     */
    private void writeMetadata( String strKey, String strTitle, String strMimeType, long lSize, String strProviderName ) throws IOException
    {
        Properties metadata = new Properties( );
        metadata.setProperty( META_TITLE, StringUtils.defaultString( strTitle, strKey ) );
        metadata.setProperty( META_MIME_TYPE, StringUtils.defaultString( strMimeType ) );
        metadata.setProperty( META_SIZE, String.valueOf( lSize ) );
        metadata.setProperty( META_ORIGIN, StringUtils.defaultString( strProviderName ) );
        metadata.setProperty( META_DATE_CREATION, String.valueOf( System.currentTimeMillis( ) ) );

        Path temporary = createTemporaryFile( shardPath( strKey ) );

        try
        {
            try ( Writer writer = Channels.newWriter( Files.newByteChannel( temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS ),
                    StandardCharsets.UTF_8 ) )
            {
                metadata.store( writer, null );
            }

            move( temporary, metadataPath( strKey ) );
        }
        finally
        {
            deleteQuietly( temporary );
        }
    }

    /**
     * Reads the metadata of a file.
     *
     * @param strKey
     *            the key of the file
     * @return the metadata, or null when they are missing or cannot be read
     */
    private Properties readMetadata( String strKey )
    {
        Properties metadata = new Properties( );

        try ( Reader reader = Channels.newReader( Files.newByteChannel( metadataPath( strKey ), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS ),
                StandardCharsets.UTF_8 ) )
        {
            metadata.load( reader );
            return metadata;
        }
        catch( NoSuchFileException e )
        {
            return null;
        }
        catch( AccessDeniedException e )
        {
            AppLogService.error( "Access denied to the metadata of the file storage file {}: check the owner and the permissions of the volume",
                    strKey, e );
            return null;
        }
        catch( IOException | IllegalArgumentException e )
        {
            AppLogService.error( "Unable to read the metadata of the file storage file {}", strKey, e );
            return null;
        }
    }

    /**
     * Creates a temporary file readable by the group, in a sub-directory.
     *
     * @param shard
     *            the sub-directory
     * @return the temporary file
     * @throws IOException
     *             if the file cannot be created
     */
    private static Path createTemporaryFile( Path shard ) throws IOException
    {
        Path temporary = Files.createTempFile( shard, TEMPORARY_PREFIX, null );

        if ( shard.getFileSystem( ).supportedFileAttributeViews( ).contains( POSIX ) )
        {
            Files.setPosixFilePermissions( temporary, FILE_PERMISSIONS );
        }

        return temporary;
    }

    /**
     * Moves a temporary file in place, atomically when the file system allows it.
     *
     * @param source
     *            the temporary file
     * @param target
     *            the final file
     * @throws IOException
     *             if the file cannot be moved
     */
    private static void move( Path source, Path target ) throws IOException
    {
        try
        {
            Files.move( source, target, StandardCopyOption.ATOMIC_MOVE );
        }
        catch( AtomicMoveNotSupportedException e )
        {
            Files.move( source, target, StandardCopyOption.REPLACE_EXISTING );
        }
    }

    /**
     * Deletes a file, logging the failure instead of propagating it.
     *
     * @param path
     *            the file, or null
     */
    private static void deleteQuietly( Path path )
    {
        if ( path == null )
        {
            return;
        }

        try
        {
            Files.deleteIfExists( path );
        }
        catch( IOException e )
        {
            AppLogService.error( "Unable to delete the file storage file {}", path, e );
        }
    }

    /**
     * Tells whether a file was last modified before an instant.
     *
     * @param path
     *            the file
     * @param limit
     *            the instant
     * @return true if the file is older
     */
    private static boolean isOlderThan( Path path, Instant limit )
    {
        try
        {
            return Files.getLastModifiedTime( path, LinkOption.NOFOLLOW_LINKS ).toInstant( ).isBefore( limit );
        }
        catch( IOException e )
        {
            return false;
        }
    }

    /**
     * Tells whether the root directory is absolute, exists as a real directory and can be written.
     *
     * @return true if files can be stored
     */
    private boolean isAvailable( )
    {
        return _directory.isAbsolute( ) && isRealDirectory( _directory ) && Files.isWritable( _directory );
    }

    /**
     * Tells whether a path is a directory and not a symbolic link.
     *
     * @param path
     *            the path
     * @return true if it is a real directory
     */
    private static boolean isRealDirectory( Path path )
    {
        return Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS );
    }

    /**
     * Tells whether a key is a canonical UUID, the only form this service ever produces.
     *
     * @param strKey
     *            the key
     * @return true if the key is valid
     */
    private static boolean isValidKey( String strKey )
    {
        return strKey != null && PATTERN_KEY.matcher( strKey ).matches( );
    }

    /**
     * Returns the sub-directory of a file.
     *
     * @param strKey
     *            a valid key
     * @return the path
     */
    private Path shardPath( String strKey )
    {
        return _directory.resolve( strKey.substring( 0, SHARD_LENGTH ) );
    }

    /**
     * Returns the path of the content of a file.
     *
     * @param strKey
     *            a valid key
     * @return the path
     */
    private Path contentPath( String strKey )
    {
        return shardPath( strKey ).resolve( strKey );
    }

    /**
     * Returns the path of the metadata of a file.
     *
     * @param strKey
     *            a valid key
     * @return the path
     */
    private Path metadataPath( String strKey )
    {
        return shardPath( strKey ).resolve( strKey + METADATA_EXTENSION );
    }

    /**
     * What a cleaning removed.
     */
    public static final class CleanupReport
    {
        private int _nTemporaryFiles;
        private int _nContentsWithoutMetadata;
        private int _nMetadataWithoutContent;
        private int _nFilesNotCatalogued;

        /**
         * Returns the number of temporary files removed.
         *
         * @return the number
         */
        public int getTemporaryFiles( )
        {
            return _nTemporaryFiles;
        }

        /**
         * Returns the number of contents without metadata removed.
         *
         * @return the number
         */
        public int getContentsWithoutMetadata( )
        {
            return _nContentsWithoutMetadata;
        }

        /**
         * Returns the number of metadata without content removed.
         *
         * @return the number
         */
        public int getMetadataWithoutContent( )
        {
            return _nMetadataWithoutContent;
        }

        /**
         * Returns the number of files no longer in the catalogue removed.
         *
         * @return the number
         */
        public int getFilesNotCatalogued( )
        {
            return _nFilesNotCatalogued;
        }

        /**
         * Returns the total number of entries removed.
         *
         * @return the number
         */
        public int getTotal( )
        {
            return _nTemporaryFiles + _nContentsWithoutMetadata + _nMetadataWithoutContent + _nFilesNotCatalogued;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( )
        {
            return "temporary files: " + _nTemporaryFiles + ", contents without metadata: " + _nContentsWithoutMetadata
                    + ", metadata without content: " + _nMetadataWithoutContent + ", files not catalogued: " + _nFilesNotCatalogued;
        }
    }
}
