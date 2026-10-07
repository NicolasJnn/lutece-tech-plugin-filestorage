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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.upload.MultipartItem;

/**
 * Unit tests of {@link FileStorageFileSystemFileService}.
 */
public class FileStorageFileSystemFileServiceTest
{
    private static final String PROVIDER = "fileStorageFileStoreProvider";
    private static final String OTHER_PROVIDER = "otherProvider";
    private static final byte [ ] CONTENT = "%PDF-1.7 content".getBytes( StandardCharsets.US_ASCII );

    @TempDir
    Path _directory;

    /**
     * Builds the service on the temporary directory.
     *
     * @return the service
     */
    private FileStorageFileSystemFileService service( )
    {
        return new FileStorageFileSystemFileService( _directory.toString( ) );
    }

    /**
     * Builds an uploaded file.
     *
     * @param content
     *            its content
     * @param lDeclaredSize
     *            its declared size
     * @return the uploaded file
     */
    private static MultipartItem item( byte [ ] content, long lDeclaredSize )
    {
        return new MultipartItem( )
        {
            @Override
            public String getFieldName( )
            {
                return "stored_file";
            }

            @Override
            public String getName( )
            {
                return "rapport.pdf";
            }

            @Override
            public String getContentType( )
            {
                return "application/pdf";
            }

            @Override
            public long getSize( )
            {
                return lDeclaredSize;
            }

            @Override
            public InputStream getInputStream( )
            {
                return new ByteArrayInputStream( content );
            }

            @Override
            public byte [ ] get( )
            {
                return content.clone( );
            }

            @Override
            public void delete( )
            {
            }
        };
    }

    /**
     * Lists every regular file of the directory.
     *
     * @return the files
     * @throws IOException
     *             if the directory cannot be read
     */
    private List<Path> files( ) throws IOException
    {
        try ( Stream<Path> stream = Files.walk( _directory ) )
        {
            return stream.filter( Files::isRegularFile ).toList( );
        }
    }

    /**
     * An uploaded file is stored with its metadata and read back, content and metadata.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testStoreFileItemRoundTrip( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        String strKey = service.storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );

        File file = service.getFile( strKey, PROVIDER );
        assertNotNull( file );
        assertEquals( strKey, file.getFileKey( ) );
        assertEquals( "rapport.pdf", file.getTitle( ) );
        assertEquals( "application/pdf", file.getMimeType( ) );
        assertEquals( CONTENT.length, file.getSize( ) );
        assertEquals( PROVIDER, file.getOrigin( ) );
        assertNotNull( file.getDateCreation( ) );
        assertArrayEquals( CONTENT, file.getPhysicalFile( ).getValue( ) );

        File metadata = service.getFileMetaData( strKey, PROVIDER );
        assertEquals( "rapport.pdf", metadata.getTitle( ) );
        assertNull( metadata.getPhysicalFile( ) );

        try ( InputStream stream = service.getInputStream( strKey, PROVIDER ) )
        {
            assertArrayEquals( CONTENT, stream.readAllBytes( ) );
        }
    }

    /**
     * Two files of the same name never overwrite each other.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testKeysAreUnique( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        String strFirst = service.storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );
        String strSecond = service.storeFileItem( item( "other".getBytes( StandardCharsets.US_ASCII ), 5 ), PROVIDER );

        assertNotEquals( strFirst, strSecond );
        assertArrayEquals( CONTENT, service.getFile( strFirst, PROVIDER ).getPhysicalFile( ).getValue( ) );
    }

    /**
     * Bytes, streams and core files are stored for real, with their size and origin.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testOtherStoreMethods( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );

        File fromBytes = service.getFile( service.storeBytes( CONTENT, PROVIDER ), PROVIDER );
        assertArrayEquals( CONTENT, fromBytes.getPhysicalFile( ).getValue( ) );
        assertEquals( CONTENT.length, fromBytes.getSize( ) );

        File fromStream = service.getFile( service.storeInputStream( new ByteArrayInputStream( CONTENT ), PROVIDER ), PROVIDER );
        assertArrayEquals( CONTENT, fromStream.getPhysicalFile( ).getValue( ) );
        assertEquals( CONTENT.length, fromStream.getSize( ) );

        File source = new File( );
        source.setTitle( "page.html" );
        source.setMimeType( "text/html" );
        PhysicalFile physicalFile = new PhysicalFile( );
        physicalFile.setValue( CONTENT );
        source.setPhysicalFile( physicalFile );
        File fromFile = service.getFile( service.storeFile( source, PROVIDER ), PROVIDER );
        assertEquals( "page.html", fromFile.getTitle( ) );
        assertEquals( "text/html", fromFile.getMimeType( ) );
        assertEquals( PROVIDER, fromFile.getOrigin( ) );
        assertArrayEquals( CONTENT, fromFile.getPhysicalFile( ).getValue( ) );
    }

    /**
     * A file whose written size differs from its declared size is refused, and nothing is left on disk.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testSizeMismatchIsRefused( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );

        assertThrows( AppException.class, ( ) -> service.storeFileItem( item( CONTENT, CONTENT.length + 1L ), PROVIDER ) );
        assertTrue( files( ).isEmpty( ) );
    }

    /**
     * A file is only served to the provider that stored it.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testOriginIsChecked( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        String strKey = service.storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );

        assertNull( service.getFile( strKey, OTHER_PROVIDER ) );
        assertNull( service.getFileMetaData( strKey, OTHER_PROVIDER ) );
        assertNull( service.getInputStream( strKey, OTHER_PROVIDER ) );
        assertNull( service.getFile( strKey, null ) );
        assertNotNull( service.getFile( strKey, PROVIDER ) );
    }

    /**
     * An unknown key returns nothing.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testUnknownKey( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );

        assertNull( service.getFile( "123e4567-e89b-42d3-a456-426614174000", PROVIDER ) );
        assertNull( service.getInputStream( "123e4567-e89b-42d3-a456-426614174000", PROVIDER ) );
    }

    /**
     * A key that is not a UUID never reaches the file system: no read, no deletion outside the directory.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testInvalidKeysNeverTouchTheFileSystem( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        Path outside = Files.createTempFile( "pwtest-filestorage", ".txt" );

        try
        {
            for ( String strKey : new String [ ] { "../" + outside.getFileName( ), outside.toString( ), "125", "", null,
                    "123e4567-e89b-42d3-a456-426614174000/../../x" } )
            {
                assertNull( service.getFile( strKey, PROVIDER ), String.valueOf( strKey ) );
                assertNull( service.getFileMetaData( strKey, PROVIDER ), String.valueOf( strKey ) );
                assertNull( service.getInputStream( strKey, PROVIDER ), String.valueOf( strKey ) );
                service.delete( strKey );
            }

            assertTrue( Files.exists( outside ), "a file outside the directory is never deleted" );
        }
        finally
        {
            Files.deleteIfExists( outside );
        }
    }

    /**
     * Deletion removes the content and its metadata, and can be repeated.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testDelete( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        String strKey = service.storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );
        assertEquals( 2, files( ).size( ), "content and metadata" );

        service.delete( strKey );
        service.delete( strKey );

        assertTrue( files( ).isEmpty( ) );
        assertNull( service.getFile( strKey, PROVIDER ) );
    }

    /**
     * A missing root directory is reported and refuses the store, and is never created: a volume that is not mounted must not be
     * replaced by the disk of the container.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testMissingRootIsRefusedAndNotCreated( ) throws Exception
    {
        Path missing = _directory.resolve( "volume" );
        FileStorageFileSystemFileService service = new FileStorageFileSystemFileService( missing.toString( ) );

        assertFalse( service.healthCheck( ) );
        assertThrows( AppException.class, ( ) -> service.storeBytes( CONTENT, PROVIDER ) );
        assertFalse( Files.exists( missing ), "the root is not created" );

        Files.createDirectory( missing );
        assertTrue( service.healthCheck( ) );
        String strKey = service.storeBytes( CONTENT, PROVIDER );
        assertArrayEquals( CONTENT, service.getFile( strKey, PROVIDER ).getPhysicalFile( ).getValue( ), "positive control" );
    }

    /**
     * A relative directory is refused.
     */
    @Test
    public void testRelativeDirectoryIsRefused( )
    {
        FileStorageFileSystemFileService service = new FileStorageFileSystemFileService( "relative/filestorage" );

        assertFalse( service.healthCheck( ) );
        assertThrows( AppException.class, ( ) -> service.storeBytes( CONTENT, PROVIDER ) );
        assertFalse( Files.exists( Path.of( "relative" ) ) );
    }

    /**
     * A directory that cannot be written is reported, and refuses the store.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testReadOnlyDirectory( ) throws Exception
    {
        assumeFalse( "root".equals( System.getProperty( "user.name" ) ), "root ignores permissions" );
        assumeTrue( _directory.getFileSystem( ).supportedFileAttributeViews( ).contains( "posix" ) );
        Path readOnly = Files.createDirectory( _directory.resolve( "ro" ) );
        Files.setPosixFilePermissions( readOnly, PosixFilePermissions.fromString( "r-xr-xr-x" ) );

        try
        {
            FileStorageFileSystemFileService service = new FileStorageFileSystemFileService( readOnly.toString( ) );

            assertFalse( service.healthCheck( ) );
            assertThrows( AppException.class, ( ) -> service.storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER ) );
        }
        finally
        {
            Files.setPosixFilePermissions( readOnly, PosixFilePermissions.fromString( "rwxr-xr-x" ) );
        }

        try ( Stream<Path> stream = Files.list( readOnly ) )
        {
            assertEquals( 0, stream.count( ), "nothing is left behind" );
        }
    }

    /**
     * Files are spread in sub-directories named after the first characters of their key.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testLayout( ) throws Exception
    {
        String strKey = service( ).storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );
        Path shard = _directory.resolve( strKey.substring( 0, 2 ) );

        assertTrue( Files.isRegularFile( shard.resolve( strKey ) ) );
        assertTrue( Files.isRegularFile( shard.resolve( strKey + ".properties" ) ) );
    }

    /**
     * Stored files are readable by the group, and by nobody else.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testPermissions( ) throws Exception
    {
        assumeTrue( _directory.getFileSystem( ).supportedFileAttributeViews( ).contains( "posix" ) );
        String strKey = service( ).storeFileItem( item( CONTENT, CONTENT.length ), PROVIDER );
        Path shard = _directory.resolve( strKey.substring( 0, 2 ) );

        assertEquals( "rw-r-----", PosixFilePermissions.toString( Files.getPosixFilePermissions( shard.resolve( strKey ) ) ) );
        assertEquals( "rw-r-----", PosixFilePermissions.toString( Files.getPosixFilePermissions( shard.resolve( strKey + ".properties" ) ) ) );
    }

    /**
     * A content without metadata, and metadata without content, are invisible.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testIncompleteFilesAreInvisible( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        String strWithoutMetadata = service.storeBytes( CONTENT, PROVIDER );
        Files.delete( metadata( strWithoutMetadata ) );
        String strWithoutContent = service.storeBytes( CONTENT, PROVIDER );
        Files.delete( content( strWithoutContent ) );

        for ( String strKey : new String [ ] { strWithoutMetadata, strWithoutContent } )
        {
            assertNull( service.getFile( strKey, PROVIDER ) );
            assertNull( service.getFileMetaData( strKey, PROVIDER ) );
            assertNull( service.getInputStream( strKey, PROVIDER ) );
        }
    }

    /**
     * Corrupted metadata make the file invisible instead of failing.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testCorruptedMetadata( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );

        for ( String strCorruption : new String [ ] { "size=abc", "dateCreation=x", "title=\\uZZZZ" } )
        {
            String strKey = service.storeBytes( CONTENT, PROVIDER );
            Files.writeString( metadata( strKey ), Files.readString( metadata( strKey ) ) + strCorruption + "\n" );

            assertNull( service.getFileMetaData( strKey, PROVIDER ), strCorruption );
            assertNull( service.getFile( strKey, PROVIDER ), strCorruption );
        }
    }

    /**
     * Symbolic links on the volume are never followed, neither for a content nor for a sub-directory.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testSymbolicLinksAreNotFollowed( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        Path outside = Files.createTempFile( "PWTEST-filestorage-outside", ".txt" );

        try
        {
            Files.writeString( outside, "secret" );

            String strContentLink = service.storeBytes( CONTENT, PROVIDER );
            Files.delete( content( strContentLink ) );
            Files.createSymbolicLink( content( strContentLink ), outside );
            assertNull( service.getFile( strContentLink, PROVIDER ), "content replaced by a link" );
            assertNull( service.getInputStream( strContentLink, PROVIDER ), "content replaced by a link" );

            String strShardLink = service.storeBytes( CONTENT, PROVIDER );
            Path shard = content( strShardLink ).getParent( );
            Path moved = Files.move( shard, _directory.resolve( "moved" ) );
            Files.createSymbolicLink( shard, moved );
            assertNull( service.getFile( strShardLink, PROVIDER ), "sub-directory replaced by a link" );
        }
        finally
        {
            Files.deleteIfExists( outside );
        }
    }

    /**
     * Old orphans are removed, each kind counted; recent ones and catalogued files are kept.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testCleanOrphans( ) throws Exception
    {
        FileStorageFileSystemFileService service = service( );
        FileTime old = FileTime.from( Instant.now( ).minus( Duration.ofHours( 2 ) ) );

        String strCatalogued = service.storeBytes( CONTENT, PROVIDER );
        String strNotCatalogued = service.storeBytes( CONTENT, PROVIDER );
        String strWithoutMetadata = service.storeBytes( CONTENT, PROVIDER );
        Files.delete( metadata( strWithoutMetadata ) );
        String strWithoutContent = service.storeBytes( CONTENT, PROVIDER );
        Files.delete( content( strWithoutContent ) );
        Path temporary = Files.createFile( content( strCatalogued ).getParent( ).resolve( ".tmp-PWTEST" ) );
        String strRecent = service.storeBytes( CONTENT, PROVIDER );

        for ( Path file : files( ) )
        {
            if ( !file.getFileName( ).toString( ).startsWith( strRecent ) )
            {
                Files.setLastModifiedTime( file, old );
            }
        }

        FileStorageFileSystemFileService.CleanupReport report = service.cleanOrphans( Set.of( strCatalogued ), Duration.ofHours( 1 ) );

        assertEquals( 1, report.getTemporaryFiles( ) );
        assertEquals( 1, report.getContentsWithoutMetadata( ) );
        assertEquals( 1, report.getMetadataWithoutContent( ) );
        assertEquals( 1, report.getFilesNotCatalogued( ) );
        assertFalse( Files.exists( temporary ) );
        assertNull( service.getFile( strNotCatalogued, PROVIDER ) );
        assertFalse( Files.exists( content( strWithoutMetadata ) ) );
        assertFalse( Files.exists( metadata( strWithoutContent ) ) );
        assertNotNull( service.getFile( strCatalogued, PROVIDER ), "a catalogued file is kept" );
        assertNotNull( service.getFile( strRecent, PROVIDER ), "a recent file is kept, it may be about to be catalogued" );
    }

    /**
     * Cleaning a missing root does nothing.
     *
     * @throws Exception
     *             if the test fails
     */
    @Test
    public void testCleanMissingRoot( ) throws Exception
    {
        FileStorageFileSystemFileService service = new FileStorageFileSystemFileService( _directory.resolve( "absent" ).toString( ) );

        assertEquals( 0, service.cleanOrphans( Set.of( ), Duration.ofHours( 1 ) ).getTotal( ) );
        assertFalse( Files.exists( _directory.resolve( "absent" ) ) );
    }

    /**
     * Returns the path of a content.
     *
     * @param strKey
     *            the key
     * @return the path
     */
    private Path content( String strKey )
    {
        return _directory.resolve( strKey.substring( 0, 2 ) ).resolve( strKey );
    }

    /**
     * Returns the path of metadata.
     *
     * @param strKey
     *            the key
     * @return the path
     */
    private Path metadata( String strKey )
    {
        return _directory.resolve( strKey.substring( 0, 2 ) ).resolve( strKey + ".properties" );
    }
}
