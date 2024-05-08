
[UNRELEASED] Release 2.0.0:
- BREAKING CHANGE to `JsonPathFunctionExtension`: breaks specification syntax for function extensions, but provides simpler definition syntax.
  - The function extensions no longer hold a name. A name must only be provided when adding a function extension to a repository
  - The function extension classes are no longer inheritable. Instances must be created from constructors.
- BREAKING CHANGE to `JsonPathFunctionExtensionRepository`: 
  - Changed `addExtension`: 
    - Takes an additional parameter for the function extension name  
    - The function extension is now constructed only when necessary, which has been changed mostly because the definition syntax now feels cleaner.
  - Added `clone`: Provides a way for users to make a hard-copy of the repository, which is especially useful for testing custom function extensions
- BREAKING CHANGE to `JsonPathCompiler`:
  - Changed `compile`: Takes the function extension retriever as second argument now

Release 1.0.0:
- Add `JsonPath`: JsonPath compiler and query functionality
- Add `JsonPathDependencyManager`: Dependency manager for the library
- Add `JsonPathFunctionExtensionRepository`: Give users a way to add custom function extensions 

