# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
    - Implement two new endpoints: query/user and query/l2RasterProducts
### Deprecated
### Removed
### Fixed
### Security

## [1.2.1]
### Fixed
     - [swodlr/issues/147](https://github.com/podaac/swodlr/issues/147) - Update EDL public JWKS endpoints


## [1.2.0]

### Added
     
     - Implement permissions model #92
     - As a user, I do not want to see stacktraces when errors occur #36 
     - Initial filtering implementation (#41)
     - Update graphql schema with new filtering endpoint
     - Hotfix product filtering
### Fixed
     - Cache Fix + Invalidate Change (#40)

## [1.1.0]

### Added
    - GraphQL Request Logging #35
### Deprecated 
### Removed
### Fixed
    - Update OPS tea mapping #31
    - Fix product caching #33
### Security
    - NGAP DIT Implementation #32
