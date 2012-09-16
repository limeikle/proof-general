theory SymbolChangeJump imports Main
begin

(* Test: 
    - scroll down to view symbols at end
    - try Proof General -> Symbols item to turn symbols on/off
    - editor jumps briefly to display top of file before having position
      restored.
    - presumably: we get events which cause the caret position to be reset to 0
      (probably on main set(text)) method.  Then we restore the position afterwards.
      Both events get processed, hence the glitch.
      
  See commentary in ProofScriptEditor/setUsingSymbols.
  
  STATUS: Will not solve now.  Would be better to change 
  and display symbolised view rather than underlying text.
*)




























term "\<forall> x. P(x)"  (* should be on disk as \ <forall> *)
term "\<exists> x. P(x)"  (* should be on disk as \ <exists> *)
term "P ==> P"    (* should be on disk as \ <Longrightarrow> *) 
term "P \<inter> Q"      (* should be on disk as \ <inter> *)
end

end
