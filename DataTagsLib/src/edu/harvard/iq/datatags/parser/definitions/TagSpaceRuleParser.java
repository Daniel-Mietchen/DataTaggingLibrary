package edu.harvard.iq.datatags.parser.definitions;

import edu.harvard.iq.datatags.parser.definitions.ast.AbstractSlot;
import edu.harvard.iq.datatags.parser.definitions.ast.AggregateSlot;
import edu.harvard.iq.datatags.parser.definitions.ast.AtomicSlot;
import edu.harvard.iq.datatags.parser.definitions.ast.CompoundSlot;
import edu.harvard.iq.datatags.parser.definitions.ast.TodoSlot;
import edu.harvard.iq.datatags.parser.definitions.ast.ValueDefinition;
import java.util.List;
import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.codehaus.jparsec.Terminals;

/**
 * Parses the Tag Space language, tokenized by the {@link TagSpaceTerminalParser}, into an AST.
 * @author michael
 */
public class TagSpaceRuleParser {
    
    final static Parser<ValueDefinition> VALUE_DEFINITION_RULE = Parsers.sequence(
            Terminals.Identifier.PARSER,
            Terminals.fragment("description").optional(),
            (String n, String d) -> new ValueDefinition(n, cleanDescription(d) )
    );
    
    /**
     * Generate a parser that will accept the type of the slot. 
     * Needed as the sequence method cannot take more than 5 items.
     * @param keyword
     * @return a parser accepting {@code ": [keyword] of "}
     */
    static Parser<?> slotTypeParser( String keyword ) {
        return Parsers.sequence(
            TagSpaceTerminalParser.keyword(":"),
            TagSpaceTerminalParser.keyword( keyword ),
            TagSpaceTerminalParser.keyword("of")
        );
    }
    
    final static Parser<AtomicSlot> ATOMIC_SLOT_RULE = Parsers.sequence(
            Terminals.Identifier.PARSER,
            Terminals.fragment("description").optional(),
            slotTypeParser("one"),
            VALUE_DEFINITION_RULE.sepBy( TagSpaceTerminalParser.keyword(",") ),
            TagSpaceTerminalParser.keyword("."), 
            ( String name, String desc, Object _slotType, List<ValueDefinition> defs, Object _d ) -> new AtomicSlot(name, cleanDescription(desc), defs)
    );
    
    final static Parser<AggregateSlot> AGGREGATE_SLOT_RULE = Parsers.sequence(
            Terminals.Identifier.PARSER,
            Terminals.fragment("description").optional(),
            slotTypeParser("some"),
            VALUE_DEFINITION_RULE.sepBy( TagSpaceTerminalParser.keyword(",") ),
            TagSpaceTerminalParser.keyword("."), 
            ( String name, String desc, Object _slotType, List<ValueDefinition> defs, Object _d ) -> new AggregateSlot(name, cleanDescription(desc), defs)
    );
    
    final static Parser<CompoundSlot> COMPOUND_SLOT_RULE = Parsers.sequence(
            Terminals.Identifier.PARSER,
            Terminals.fragment("description").optional(),
            slotTypeParser("consists"),
            Terminals.Identifier.PARSER.sepBy( TagSpaceTerminalParser.keyword(",") ),
            TagSpaceTerminalParser.keyword("."), 
            ( String name, String desc, Object _slotType, List<String> defs, Object _d ) -> new CompoundSlot(name, cleanDescription(desc), defs)
    );
    
    final static Parser<TodoSlot> TODO_RULE = Parsers.sequence(
            Terminals.Identifier.PARSER,
            Terminals.fragment("description").optional(),
            TagSpaceTerminalParser.keyword(":"),
            Terminals.fragment("todo"),
            TagSpaceTerminalParser.keyword("."), 
            ( String name, String description, Object _a, Object _b, Object _c) -> new TodoSlot( name, cleanDescription(description) )
    );
    
    final static Parser<? extends AbstractSlot> RULE = Parsers.or( TODO_RULE, COMPOUND_SLOT_RULE, AGGREGATE_SLOT_RULE, ATOMIC_SLOT_RULE);
    final static Parser<List<? extends AbstractSlot>> RULES = RULE.many().cast();
    
    /**
     * Cleans the description coming from the parser (e.g removes the brackets).
     * @param rawDescription
     * @return the clear description.
     */
    static String cleanDescription( String rawDescription ) {
        return ( (rawDescription==null) ? "" : rawDescription.substring(1, rawDescription.length()-1));
    }
    
}
