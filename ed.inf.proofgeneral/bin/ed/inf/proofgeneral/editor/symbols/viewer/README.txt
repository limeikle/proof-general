Code here unused so far.  Keep for pointers to way of providing
symbol view on document without changing it to store Unicode
symbols.  May be a better way of providing symbol support, by providing
a view on edited text.

However, I'm not sure if the editing infrastructure will support
this smoothly (see notes in quickdiff/README.txt: other Eclipse
projects suggest this may be tricky).  So we're probably
best using piecemeal solutions for time being.

Status:
 - Need to hook up SymbolTextViewer to replace TextViewer in
 ProofScriptEditor.  Seems to be via TextSourceViewerConfiguration?  
 (not sure)
 

[da, Nov 06]
