theory TestHiddenCommands imports Main
begin

ML {* map (Path.implode o Path.expand o Path.explode) (show_path ()) *}
end
