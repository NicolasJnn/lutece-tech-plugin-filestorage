![](https://dev.lutece.paris.fr/jenkins/buildStatus/icon?job=tech-plugin-filestorage-deploy)
# Plugin filestorage

## Introduction

This plugin lets a Lutece 8 site publish its own files without any manual deposit on its servers. Files are uploaded and managed from the back office, kept on a persistent volume, and their links are inserted into contents from the editor.

Main features :

* upload of files from the back office, restricted by type and size
* management table : search by title, modification of the title and the description, replacement of the content, removal
* separate permissions to create, modify and remove files (RBAC)
* insertion of a link to a file into any content, through the insert services of the editor
* non predictable links that never expire
* PDF, images and HTML pages displayed in the browser, the HTML pages in a sandbox where no script runs
* storage on disk by default, or in the database, chosen by configuration

## Back office

The feature **File storage** (right `FILESTORAGE_MANAGEMENT`, level 1) gives access to the management table.

The actions on the files are granted through the RBAC resource type `FILESTORAGE_FILE` and its permissions `CREATE`, `MODIFY` and `DELETE`. The role `filestorage_manager` grants all of them.

The insert service **Stored file** is offered by the insert service selector of the core. Using it requires the right `FILESTORAGE_INSERT` (level 3), in addition to the right of the core that opens the selector.

## Security

* the type of an uploaded file is checked three times : its extension, its declared MIME type and the signature of its content
* every action is protected against cross-site request forgery
* files are served with `X-Content-Type-Options: nosniff`, a `Content-Security-Policy` that forbids any framing by another site, and the security headers of the site
* every file but the PDF is served in a sandbox : an HTML page cannot run scripts nor reach the session of the site
* the servlet only serves the files of this plugin

## Configuration

The properties are defined in `WEB-INF/conf/plugins/filestorage.properties`. Each of them can be overridden by an environment variable : the key in upper case, dots replaced by underscores (for example `FILESTORAGE_FILESYSTEM_DIRECTORY`).

| Property | Default | Description |
|---|---|---|
| `filestorage.allowedTypes` | `pdf,html,jpeg,png` | allowed types ; a type without a known signature is only checked on its extension and is always downloaded |
| `filestorage.maxFileSize` | `15728640` | maximum size, in bytes |
| `filestorage.fileStoreServiceProvider.fileStoreService` | `filestorage.fileSystemFileService` | storage : on disk, or `localDatabaseFileService` for the database |
| `filestorage.fileSystem.directory` | `/opt/data/filestorage` | directory of the disk storage ; it must exist, be absolute and writable |
| `filestorage.fileStoreServiceProvider.downloadService` | `filestorage.fileDownloadService` | service building the links |
| `filestorage.fileStoreServiceProvider.rbacService` | `defaultFileNoRBACService` | access control when a file is opened ; none by default, files are public |
| `filestorage.storedFile.itemsPerPage` | `50` | rows per page of the management table |
| `filestorage.cacheMaxAge` | `3600` | browser cache duration of a file, in seconds |
| `filestorage.cleanup.minAge` | `3600` | minimum age of an orphan file before its removal, in seconds |
| `daemon.filestorageCleanup.interval` | `3600` | interval of the cleaning daemon, in seconds |
| `daemon.filestorageCleanup.onstartup` | `1` | run the cleaning daemon at startup |

## Disk storage

Each file is kept as `<directory>/<first two characters of its key>/<key>`, next to `<key>.properties` that holds its title, MIME type, size, owner and date. Keys are random UUIDs, files are created with the mode `0640`.

The plugin never creates the root directory : when the volume is missing or cannot be written, the upload is refused and the error is logged at startup.

The daemon **File storage - cleaning of the disk store** removes the files left behind by interrupted uploads and deletions, and the files no longer referenced by the catalogue. It does nothing when the catalogue cannot be read.

In a container, the directory must be a persistent volume, writable by the user running the application server, shared in read-write mode between replicas and included in the backups.

## Installation

Add the dependency to the pom of the site :

```xml
<dependency>
    <groupId>fr.paris.lutece.plugins</groupId>
    <artifactId>plugin-filestorage</artifactId>
    <type>lutece-plugin</type>
</dependency>
```

The SQL scripts of the plugin create the table `filestorage_file`, the rights and the role. The site declares the plugin installed with the keys `core.plugins.status.filestorage.installed` and `core.plugins.status.filestorage.pool`.

## Limits

* replacing the content of a file changes its link : the links already inserted in contents no longer work
* an HTML page is displayed alone : its relative resources (style sheets, images) are not served
* no dedicated button in the editor : the insertion goes through the insert service selector of the core

[Maven documentation and reports](https://dev.lutece.paris.fr/plugins/plugin-filestorage/)



 *generated by [xdoc2md](https://github.com/lutece-platform/tools-maven-xdoc2md-plugin) - do not edit directly.*
