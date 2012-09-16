Notes on this package/symbol handling generally:

The reason for this diff reference provider is to provide a version of
the on-disk file which is de-symbolised, so that we do not see bars in
the quick diff for automatically replaced symbols.

This is a *terrible* solution:

1. Users have to select this manually, in an obscure
   place (Preferences -> Text Editor -> Quick Diff references source)

2. It doesn't work for other reference sources, they would need similar duplicates

3. It duplicates code making only a small change.

It seems like we really need another hook/tweak here.  The current implementation in Eclipse
creates a plain Document by reading the file, whereas we want it really to create a
ProofScriptDocument (so we can use the overridden set method to allow for the fact
that the document in memory doesn't have the exact contents of the document on disk).  
(Possible solutions?  Maybe use DocumentProvider to give a default IDocument constructor?)

TODO:
 * report this as a bug/feature request, or ask for a better solution on Eclipse forums
 * experiment with this for a while
 * make this more user-transparent by doing something hacky with the user's preferences:
    If symbols are turned on, try to hack the Quick Diff reference setting if
    it's the plan on disk version.  Or at least, give a message to the user.

Symbol encoding/decoding could work smoothly if it was added to the JVM as a custom
charset encoding.  However, that needs low level changes/installation into the JVM
(CHECK: is it possible to do this at run time?).  And we would potentially need to
overlay symbols on top of other encodings --- although probably we could get away
with only ASCII (Isabelle) or UTF-8  (Coq?).

Other possible solutions to this problem could be:
 
 - try to insulate the line difference engine from symbol changes (tricky/hacky)
 - investigate text editor (or document provider? or encoding support?) further 
   to see if we can edit symbols indirectly, without actually replacing text 
   in the model (document).  Would be a much better solution.
   Another possibility: use IDocumentAdapter
 
 At the moment it looks as if we should stick with the interim solution of
 changing the document contents temporarily back and forth.  SoC project on
 using IDocumentAdapter to supply word wrap to text editor suggests that
 working there isn't good enough to make editing perfect.  Would be best
 to have a general, standalone solution which works independently of PG.
 
[da, 19.11.06]
