# memoryfs
----------

[![Build Status](https://travis-ci.org/SylvainJuge/memoryfs.png?branch=master)](https://travis-ci.org/SylvainJuge/memoryfs)

Java7 in-memory filesystem implementation

TODOs
-----

### Required

 - read/write data in files
 - minimal read/write lock on files
 - basic file attribues read/write
 - minimal thread safety
 - usage documentation with code samples
 - file/folders operations : copy, move, rename
 
### Improvements

 - path matcher
 - access control
 - control read-only/read-write at runtime for file stores
 - fs with limited capacity (currently heap is the limit)
 - fs with multiple stores
 - store files outside heap
