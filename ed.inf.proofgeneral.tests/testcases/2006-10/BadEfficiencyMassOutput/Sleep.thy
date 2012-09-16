theory Sleep imports Pure begin

(* Sleep for 10 secs.  This shouldn't cause blocking in Interface! *)

(* The message below is not displayed because by default we queue output
   until the next <ready/>.  However, urgent messages *should* be displayed
   immediately.  (The old PG message model was to discard messages before
   an urgent message; with PGIP we can obey the messages more exactly and
   say that urgent messages cause spillage/update of the display).
*)

ML_setup {* writeln "Sleeping..."; OS.Process.sleep(Time.fromSeconds 10); *}

(* This also allows us to test the behaviour of editing the processed
   region, according to the new preference setting (added Dec 06).
   Test is to edit one of these comments while queue is active:
   after the queue completes the right region should be coloured.
   We should be extending the processed region while editing.
   This is an UNRESOLVED bug.  The offsets need to be markers!
   (But this will happen in StateMarkers branch.) *)

ML_setup {*  OS.Process.sleep(Time.fromSeconds 30); *}

end

