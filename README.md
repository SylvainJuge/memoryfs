# memoryfs
----------

[![Build Status](https://travis-ci.org/SylvainJuge/memoryfs.png?branch=master)](https://travis-ci.org/SylvainJuge/memoryfs)

Java7 in-memory filesystem implementation

Features
--------

Usage
------

* Maven dependencies are available in Sonatype OSS repository

[snapshots](https://oss.sonatype.org/content/repositories/snapshots/)

[releases](https://oss.sonatype.org/content/repositories/releases/) (none available yet)

* Usages
- testing without using temporary files

TODOs
-----

### Required

 - DONE read/write data in files
 - DONE file/folders operations : copy, move, rename
 - minimal read/write lock on files
 - basic file attribues read/write
 - minimal thread safety
 - usage documentation with code samples
 
### Improvements

 - path matcher
 - access control
 - allow to create readonly file{system,store}
 - control read-only/read-write at runtime for file stores
 - fs with limited capacity (currently heap is the limit)
 - fs with multiple stores
 - store files outside heap (memory-mapped file?)
 - load/save to/from file
 - create a view over current file sytem (potentially read-only, or with "copy on write" for modifications (and then allow to find what have been done)
