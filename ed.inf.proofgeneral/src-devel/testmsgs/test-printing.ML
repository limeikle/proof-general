(* Tue Jun  8 22:43:54 BST 2004 *)

> writeln ("Hello you & me, what do you think of 'this'?:  " ^ (string_of_thm allI));
Hello you & me, what do you think of 'this'?"(!!x. ?P x) ==> ALL x. ?P x"

print_mode := ["xsymbols"];
Hello you & me, what do you think of 'this'?"(\\<And>x. ?P x) \\<Longrightarrow> \\<forall>x. ?P x"

print_mode := ["xsymbols","pgml"];  
Hello you &amp; me, what do you think of &apos;this&apos;?&quot;(!!x. ?P x) ==&gt; ALL x. ?P x&quot;
(* Just right! *)

> print_mode :=["testpgml","xsymbols"];
> writeln ("Hello you & me, what do you think of 'this'?" ^ (string_of_thm allI));
Hello you &amp; me, what do you think of &apos;this&apos;?&quot;(<sym name = "And"/>x. ?P x) <sym name = "Longrightarrow"/> <sym name = "forall"/>x. ?P x&quot;
(* Just right! *)



isabelle Pure
> context Pure.thy
> string_of_thm (thm "reflexive");
val it = "\\<^raw:\"?x == ?x\">" : string

But inside warning, we get:

> warning ("Look at this: " ^ (string_of_thm (thm "reflexive")));
### Look at this: "?x == ?x"
val it = () : unit

Or with x-symbols turned on:

print_mode:=["xsymbols"];
string_of_thm (thm "reflexive");
val it = "\\<^raw:\"?x \\\\<equiv>\\<^raw62>\\<^raw: ?x\">" : string  (* wrong?: raw62 is > *)
> warning ("Look at this: " ^ (string_of_thm (thm "reflexive")));
### Look at this: "?x \\<equiv> ?x"

With testpgml:

warning ("Look at this: " ^ (string_of_thm (thm "reflexive")));
### Look at this: &quot;?x == ?x&quot;


(* Wed Sep  3 21:10:27 BST 2003 *)

(* first, init to redirect print functions, and then quit PGIP to ML. *)

ProofGeneral.init_pgip true;
<pgip class="pa" id="X" seq="1"><quitpgip/></pgip>

(* now test output. *)

val allI = get_thm HOL.thy "allI";

print_mode := [];  (* ordinary verbatim *)

string_of_thm allI;
(* val it = "\"(!!x. ?P x) ==> ALL x. ?P x\"" : string   --- ML printer escs "'s *)

print_mode := ["PGML"];    (* XML escapes only *)

string_of_thm allI;
(*  val it = "&quot;(!!x. ?P x) ==&gt; ALL x. ?P x&quot;"  -- XML escapes for " and > *)

print_mode := ["xsymbols", "PGML", "ProofGeneral"];  (* symbols and XML escapes *)

string_of_thm allI;
(* val it =
   "&quot;(<sym name = \"And\"/>x. ?P x) <sym name = \"Longrightarrow\"/> <sym name = \"forall\"/>x. ?P x&quot;" 
  
  -- Just right: PGML symbol names for xsymbols; spacing preserved between symbols
 *)


(* now with token translations too *)
print_mode := ["xsymbols", "PGML", "PGMLatoms", "ProofGeneral"]; (* all *)

(* even better:
val allI =
   &quot;(<sym name = "And"/><atom kind = "bound">x</atom>.
          <atom kind = "var">?P</atom>
           <atom kind = "bound">x</atom>) <sym name = "Longrightarrow"/>
      <sym name = "forall"/><atom kind = "bound">x</atom>.
         <atom kind = "var">?P</atom> <atom kind = "bound">x</atom>&quot;
*)


val li = get_thm Main.thy "list.induct";
ML> print_mode := [];
val it = () : unit
ML> li;
val it = "[| ?P []; !!a list. ?P list ==> ?P (a # list) |] ==> ?P ?list"




ML> allI;
"got symbol: \\<forall>"
"got symbol:  \\<Longrightarrow>"
"got symbol: \\<And>"
val it =
   &quot;(<sym name = "And"/><atom kind = "bound">x</atom>.
          <atom kind = "var">?P</atom>
           <atom kind = "bound">x</atom>) <sym name = "Longrightarrow"/>
      <sym name = "forall"/><atom kind = "bound">x</atom>.
         <atom kind = "var">?P</atom> <atom kind = "bound">x</atom>&quot;
: Thm.thm
ML> li;
"got symbol: \\<rbrakk>"
"got symbol:  \\<Longrightarrow>"
"got symbol: \\<And>"
"got symbol: \\<lbrakk>"
"got symbol: \\<Longrightarrow> "
val it =
   &quot;<sym name = "lbrakk"/><atom kind = "var">?P</atom> [];
         <sym name = "And"/><atom kind = "bound">a</atom>
            <atom kind = "bound">list</atom>.
            <atom kind = "var">?P</atom>
             <atom kind = "bound">list</atom> <sym name = "Longrightarrow"/>
            <atom kind = "var">?P</atom>
             (<atom kind = "bound">a</atom> #
              <atom kind = "bound">list</atom>)<sym name = "rbrakk"/>
      <sym name = "Longrightarrow"/> <atom kind = "var">?P</atom> <atom kind = "var">?list</atom>&quot;
: Thm.thm


ML> string_of_thm li;
"got symbol: \\<rbrakk>"
"got symbol:  \\<Longrightarrow>"
"got symbol: \\<And>"
"got symbol: \\<lbrakk>"
"got symbol: \\<Longrightarrow> "
val it =
   "&quot;<sym name = \"lbrakk\"/><atom kind = \"var\">?P</atom> []; <sym name = \"And\"/><atom kind = \"bound\">a</atom> <atom kind = \"bound\">list</atom>. <atom kind = \"var\">?P</atom> <atom kind = \"bound\">list</atom> <sym name = \"Longrightarrow\"/> <atom kind = \"var\">?P</atom> (<atom kind = \"bound\">a</atom> # <atom kind = \"bound\">list</atom>)<sym name = \"rbrakk\"/> <sym name = \"Longrightarrow\"/> <atom kind = \"var\">?P</atom> <atom kind = \"var\">?list</atom>&quot;"
: string
ML> 

(* notice space between final longrightarrow and atom P *)
