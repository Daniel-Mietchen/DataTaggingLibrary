package edu.harvard.iq.datatags.tools;

import edu.harvard.iq.datatags.model.charts.ChartEntity;
import edu.harvard.iq.datatags.model.charts.FlowChartSet;
import edu.harvard.iq.datatags.model.charts.nodes.AskNode;
import edu.harvard.iq.datatags.model.charts.nodes.EndNode;
import edu.harvard.iq.datatags.model.charts.nodes.RejectNode;
import edu.harvard.iq.datatags.model.types.CompoundType;
import edu.harvard.iq.datatags.parser.exceptions.BadSetInstructionException;
import edu.harvard.iq.datatags.parser.flowcharts.FlowChartASTParser;
import edu.harvard.iq.datatags.parser.flowcharts.FlowChartSetComplier;
import edu.harvard.iq.datatags.parser.flowcharts.references.InstructionNodeRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Naomi
 */
public class UnreachableNodeValidatorTest {
    
    UnreachableNodeValidator instance;
    FlowChartSetComplier fcsc;
    FlowChartSet fcs;
    FlowChartASTParser astParser;
    
    public UnreachableNodeValidatorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        instance = new UnreachableNodeValidator();
        fcsc = new FlowChartSetComplier(new CompoundType("", ""));
        astParser = new FlowChartASTParser();
    }
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void validateUnreachableNodesTest_reachableNodes() throws BadSetInstructionException {
        System.out.println("\n\nReachable");
        String code = "(>ask1< ask: (text: Will this work?)"
                + "(yes: (call:shouldWork) ))(>end1< end)"
                + "(>shouldWork< ask: (text: This should work.)"
                + "(yes: (>reject1< reject: Good, it works.))"
                + "(no: (>reject2< reject: This should have worked.)))";
        List<InstructionNodeRef> refs = astParser.graphParser().parse(code);
        
        System.out.println("refs = " + refs);
        
        fcs = fcsc.parse(refs, "unitName");
        List<NodeValidationMessage> messages = instance.validateUnreachableNodes(fcs);
        
        Set<ChartEntity> expected = Collections.<ChartEntity>emptySet();
        Set<ChartEntity> actualEntities = new HashSet<>();
        Set<NodeValidationMessage.Level> actualLevels = EnumSet.noneOf(NodeValidationMessage.Level.class);
        
        for ( NodeValidationMessage vm : messages ) {
            actualEntities.addAll(vm.getEntities());
            actualLevels.add(vm.getLevel());
        }
        System.out.println("actual = " + actualEntities);
        System.out.println("expected = " + expected);
        
        assertEquals( new HashSet<>(), actualLevels);
        assertEquals(expected, actualEntities);
        
        System.out.println("/Reachable\n\n");
    }
   
    @Test
    public void validateUnreachableNodesTest_minimal() throws BadSetInstructionException {
        String code = "(>r< end)(>nr< end)";
        List<InstructionNodeRef> refs = astParser.graphParser().parse(code);
        
        fcs = fcsc.parse(refs, "unitName");
        List<NodeValidationMessage> messages = instance.validateUnreachableNodes(fcs);
        
        Set<ChartEntity> expected = Collections.singleton( new EndNode("nr") );
        Set<ChartEntity> actualEntities = new HashSet<>();
        Set<ValidationMessage.Level> actualLevels = EnumSet.noneOf(ValidationMessage.Level.class);
        
        messages.forEach( vm -> {
            actualEntities.addAll(vm.getEntities());
            actualLevels.add(vm.getLevel());
        });
        
        assertEquals( EnumSet.of(ValidationMessage.Level.WARNING), actualLevels);
        assertEquals(expected, actualEntities);
        
    }
    
    @Test
    public void validateUnreachableNodesTest_unreachableNodes() throws BadSetInstructionException {
        String code = "(>ask1< ask: (text: Will this work?)"
                + "(yes: (>reject1< reject: No.))"
                + "(no: (>reject2< reject: Still no.)))"
                + "(>ask2< ask: (text: This shouldn't work.)"
                + "(yes: (>reject3< reject: No.))"
                + "(no: (>reject4< reject: Still no.)))"
                + "(>end1< end)";
        List<InstructionNodeRef> refs = astParser.graphParser().parse(code);
        
        fcs = fcsc.parse(refs, "unitName");
        List<NodeValidationMessage> messages = instance.validateUnreachableNodes(fcs);
        
        Set<String> expectedEntityIds = new HashSet<>(Arrays.asList("reject4","reject3","end1","ask2"));
        
        Set<String> actualEntitiesIds = new HashSet<>();
        Set<ValidationMessage.Level> actualLevels = EnumSet.noneOf(ValidationMessage.Level.class);
        
        messages.stream().forEach((vm) -> {
            actualEntitiesIds.addAll(
                    vm.getEntities().stream().map( ChartEntity::getId ).collect(Collectors.toSet()) );
            actualLevels.add(vm.getLevel());
        });
        
        assertEquals( EnumSet.of(ValidationMessage.Level.WARNING), actualLevels );
        assertEquals( expectedEntityIds, actualEntitiesIds );
    }

    
}
