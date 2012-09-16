theory CutToEndOfLine imports Main
begin
  (* Try cut-to-end-of-line (C-k in Emacs keys).
     19.12.06 16:30: gives NullPointerException.
     19.12.06 16:50: fix added in ProofScriptDocument:
        seems that replace can be called with null argument
        for deleted text.
        
     STATUS: resolved
  *)
    
end
