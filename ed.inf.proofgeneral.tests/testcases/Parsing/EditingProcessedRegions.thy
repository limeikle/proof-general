(* try inserting text right at the beginning of the file
   when it is unprocessed. This should not ruin subsequent
   processing steps *)
theory EditingProcessedRegions imports Main
begin



(* This test demonstrates that script management 
   offsets in the document are now properly updated as editing occurs.  
   Instead of storing offsets, we use Position and register
   position updaters.  Editing near the script management regions
   should be good (currently in progress)
*)

lemma Foo : "P-->P" by auto
(* Another test: edit right at end of processed region (insert, etc).
   This should not make things go haywire (illegal scroll positions, whatever)
   CURRENTLY (24.3) this is buggy.
*)



(* TEST:
   1. parse whole document, with folding.
   2. process to here.
   3. enable edits of processed region.  
   4. Test that deleting and inserting text above processed region
      causes region end to move properly (i.e., appear *not* to move!).
      The processed region end can be found by hitting the activate
      button, which moves the caret.
*)

(* Note: we're using/experimenting with a non-standard position updater
   which does not move empty regions forward on document inserts.
   *)

end